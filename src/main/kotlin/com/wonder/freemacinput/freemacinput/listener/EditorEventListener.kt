package com.wonder.freemacinput.freemacinput.listener

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.wonder.freemacinput.freemacinput.core.*
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.service.InputMethodService
import com.wonder.freemacinput.freemacinput.ui.ToastManager
import com.intellij.openapi.application.ApplicationManager

import java.util.Timer
import java.util.TimerTask

/**
 * 编辑器事件监听器
 */
class EditorEventListener(private val project: Project) : CaretListener, DocumentListener {
    private val logger = Logger.getInstance(EditorEventListener::class.java)

    private val contextDetector = ContextDetector()
    private val inputMethodService = InputMethodService.getInstance(project)

    private var lastContextInfo: ContextInfo? = null
    private val timer: Timer = Timer("InputMethodSwitchTimer", true)
    private var scheduledTask: TimerTask? = null
    private val switchDelayMs = 150L
    
    // 缓存文档文本，避免频繁获取
    private var cachedDocumentText: String? = null
    private var cachedDocumentLength: Int = -1

    fun onEditorActivated(editor: Editor) {
        logger.info("onEditorActivated called")
        val data = extractEditorData(editor)
        logger.info("onEditorActivated: ${data.fileName}, offset=${data.caretOffset}")
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.caretOffset, 450L)
    }

    private fun extractEditorData(editor: Editor): EditorData {
        val fileName = editor.virtualFile?.name ?: "unknown"
        val document = editor.document
        val caretOffset = editor.caretModel.offset
        
        // 优化：只在文档长度变化时重新获取文本，或使用缓存
        val currentLength = document.textLength
        val documentText = if (cachedDocumentText != null && cachedDocumentLength == currentLength) {
            cachedDocumentText!!
        } else {
            val text = document.text
            cachedDocumentText = text
            cachedDocumentLength = currentLength
            text
        }
        
        return EditorData(fileName, documentText, caretOffset)
    }

    override fun caretPositionChanged(e: CaretEvent) {
        val editor = e.editor ?: run {
            logger.info("caretPositionChanged: editor is null")
            return
        }
        val fileName = editor.virtualFile?.name ?: "unknown"
        val offset = e.caret?.offset ?: -1
        logger.info("光标位置变化: $fileName, offset=$offset")

        val data = extractEditorData(editor)
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.caretOffset)
    }

    override fun caretAdded(e: CaretEvent) {
        e.editor?.let { updateCaretRenderer(it) }
    }

    override fun caretRemoved(e: CaretEvent) {
        e.editor?.let { CaretRendererManager.remove(it) }
    }

    override fun documentChanged(event: DocumentEvent) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        // 文档变化时清除缓存
        cachedDocumentText = null
        cachedDocumentLength = -1
        val data = extractEditorData(editor)
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.caretOffset, 120L)
    }

    override fun beforeDocumentChange(event: DocumentEvent) {}

    private fun updateCaretRenderer(editor: Editor) {
        val renderer = CaretRendererManager.getOrCreate(editor)
        renderer.setEnabled(inputMethodService.isEnableCaretColor())
        renderer.refresh()
    }

    private fun scheduleInputMethodSwitch(
        fileName: String,
        documentText: String,
        caretOffset: Int,
        delayMs: Long = switchDelayMs
    ) {
        logger.info("scheduleInputMethodSwitch: file=$fileName, offset=$caretOffset")
        scheduledTask?.cancel()
        scheduledTask = null

        val task = object : TimerTask() {
            override fun run() {
                logger.info("scheduled task running...")
                detectAndSwitch(fileName, documentText, caretOffset)
            }
        }

        timer.schedule(task, delayMs)
        scheduledTask = task
        logger.info("task scheduled with delay=${delayMs}ms")
    }

    private fun detectAndSwitch(
        fileName: String,
        documentText: String,
        caretOffset: Int
    ) {
        val startTs = System.currentTimeMillis()
        if (!inputMethodService.isEnabled()) {
            logger.info("插件未启用")
            return
        }

        logger.info("开始检测上下文... file=$fileName")

        val contextInfo = contextDetector.detectContext(documentText, caretOffset)
        logger.info("检测到上下文: ${contextInfo.type}, 原因: ${contextInfo.reason}")

        // 不再根据上下文类型提前返回，让 shouldSwitch 决定是否需要切换

        val targetMethod = determineInputMethod(contextInfo)
        logger.info("目标输入法: $targetMethod, 当前上下文: ${contextInfo.type}")

        // 使用 InputMethodManager 的内部状态与冷却判定
        val (should, reason) = InputMethodManager.shouldSwitch(targetMethod)
        logger.info("shouldSwitch=$should, 原因: $reason")

        if (!should) {
            logger.info("shouldSwitch=false: $reason")
            return
        }

        logger.info("调用 InputMethodManager.switchTo($targetMethod)...")
        val settings = inputMethodService.getSettings()
        logger.info("Settings: isEnabled=${settings.isEnabled}, isShowHints=${settings.isShowHints}")
        val switchResult = InputMethodManager.switchTo(targetMethod, settings)
        val elapsed = System.currentTimeMillis() - startTs
        logger.info("switchTo 返回: success=${switchResult.success}, 消息: ${switchResult.message}, 耗时: ${elapsed}ms, actualMethod=${switchResult.actualMethod}")

        // 显示 Toast 提示 - 根据实际切换结果显示
        if (settings.isShowHints) {
            ApplicationManager.getApplication().invokeLater {
                val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                logger.info("准备显示 Toast: activeEditor=${activeEditor != null}, switchResult.success=${switchResult.success}")
                if (activeEditor != null) {
                    if (switchResult.success) {
                        // 切换成功，根据实际切换后的输入法显示Toast
                        val toastMessage = generateToastMessage(contextInfo, switchResult.actualMethod, fileName, switchResult.message)
                        val isChinese = switchResult.actualMethod == InputMethodType.CHINESE
                        ToastManager.showToast(activeEditor, toastMessage, isChinese)
                        logger.info("触发 Toast 显示: $toastMessage (actualMethod=${switchResult.actualMethod})")
                    } else {
                        // 切换失败，显示详细失败原因
                        val failureMessage = when {
                            switchResult.message.contains("不支持") -> "输入法切换失败：不支持当前操作系统"
                            switchResult.message.contains("权限") -> "输入法切换失败：缺少系统权限"
                            switchResult.message.contains("冷却中") -> "输入法切换失败：切换过于频繁"
                            else -> "输入法切换失败：${switchResult.message}"
                        }
                        ToastManager.showToast(activeEditor, failureMessage, false)
                        logger.info("触发 Toast 显示: $failureMessage")
                    }
                } else {
                    logger.info("未显示 Toast: 活跃编辑器为null")
                }
            }
        }

        lastContextInfo = contextInfo
    }

    /**
     * 根据上下文和实际输入法生成提示消息
     */
    private fun generateToastMessage(
        contextInfo: ContextInfo,
        actualMethod: InputMethodType,
        fileName: String,
        switchMessage: String
    ): String {
        logger.info("=== generateToastMessage 被调用 ===")
        logger.info("actualMethod: $actualMethod")
        logger.info("contextInfo.type: ${contextInfo.type}")
        logger.info("switchMessage: $switchMessage")

        return when (actualMethod) {
            InputMethodType.CHINESE -> {
                when (contextInfo.type) {
                    ContextType.CODE -> {
                        // 如果切换消息表明已经是当前输入法，显示简单消息
                        if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                            "保持中文输入法"
                        } else {
                            "已切换为中文"
                        }
                    }
                    ContextType.COMMENT -> "注释区域自动切换为中文"
                    ContextType.STRING -> "字符串区域自动切换为中文"
                    ContextType.UNKNOWN -> "已切换为中文"
                }
            }
            InputMethodType.ENGLISH -> {
                // 根据文件类型生成消息
                val fileType = getFileType(fileName)
                 when (contextInfo.type) {
                     ContextType.CODE -> {
                         // 如果切换消息表明已经是当前输入法，显示简单消息
                         if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                             "保持英文输入法"
                         } else {
                             when (fileType) {
                                 FileType.JAVA -> "Java 文件已切换为英文"
                                 FileType.KOTLIN -> "Kotlin 文件已切换为英文"
                                 FileType.PYTHON -> "Python 文件已切换为英文"
                                 FileType.GO -> "Go 文件已切换为英文"
                                 FileType.JAVASCRIPT -> "JavaScript 文件已切换为英文"
                                 FileType.TYPESCRIPT -> "TypeScript 文件已切换为英文"
                                 FileType.C_CPP -> "C/C++ 文件已切换为英文"
                                 FileType.OTHER -> "代码区域已切换为英文"
                             }
                         }
                     }
                    ContextType.STRING -> {
                        // 英文字符串保持英文
                        if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                            "保持英文输入法"
                        } else {
                            "字符串区域已切换为英文"
                        }
                    }
                    ContextType.COMMENT -> {
                        if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                            "保持英文输入法"
                        } else {
                            "注释区域已切换为英文"
                        }
                    }
                    ContextType.UNKNOWN -> "已切换为英文"
                }
            }
            else -> ""
        }
    }

    /**
     * 获取文件类型
     */
    private fun getFileType(fileName: String): FileType {
        return when {
            fileName.endsWith(".java", ignoreCase = true) -> FileType.JAVA
            fileName.endsWith(".kt", ignoreCase = true) -> FileType.KOTLIN
            fileName.endsWith(".py", ignoreCase = true) -> FileType.PYTHON
            fileName.endsWith(".go", ignoreCase = true) -> FileType.GO
            fileName.endsWith(".js", ignoreCase = true) -> FileType.JAVASCRIPT
            fileName.endsWith(".ts", ignoreCase = true) -> FileType.TYPESCRIPT
            fileName.endsWith(".c", ignoreCase = true) ||
            fileName.endsWith(".cpp", ignoreCase = true) ||
            fileName.endsWith(".h", ignoreCase = true) ||
            fileName.endsWith(".hpp", ignoreCase = true) -> FileType.C_CPP
            else -> FileType.OTHER
        }
    }

    /**
     * 文件类型枚举
     */
    private enum class FileType {
        JAVA, KOTLIN, PYTHON, GO, JAVASCRIPT, TYPESCRIPT, C_CPP, OTHER
    }

    private fun determineInputMethod(contextInfo: ContextInfo): InputMethodType {
        val settings = inputMethodService.getSettings()
        return when (contextInfo.type) {
            ContextType.CODE -> settings.defaultMethod
            ContextType.COMMENT -> settings.commentMethod
            ContextType.STRING -> settings.commentMethod
            ContextType.UNKNOWN -> settings.defaultMethod
        }
    }

    fun dispose() {
        timer.cancel()
        CaretRendererManager.disposeAll()
        ToastManager.dismissAll()
    }

    private data class EditorData(
        val fileName: String,
        val documentText: String,
        val caretOffset: Int
    )
}
