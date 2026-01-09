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

    fun onEditorActivated(editor: Editor) {
        logger.info("onEditorActivated called")
        val data = extractEditorData(editor)
        logger.info("onEditorActivated: ${data.fileName}, offset=${data.caretOffset}")
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.caretOffset, 450L)
    }

    private fun extractEditorData(editor: Editor): EditorData {
        val fileName = editor.virtualFile?.name ?: "unknown"
        val documentText = editor.document.text
        val caretOffset = editor.caretModel.offset
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

        val contextChanged = lastContextInfo == null || contextInfo.type != lastContextInfo?.type
        if (!contextChanged) {
            logger.info("上下文无变化，跳过")
            return
        }

        lastContextInfo = contextInfo
        val targetMethod = determineInputMethod(contextInfo)
        logger.info("目标输入法: $targetMethod, 当前上下文: ${contextInfo.type}")

        // 使用 InputMethodManager 的内部状态与冷却判定
        val (should, reason) = InputMethodManager.shouldSwitch(targetMethod)
        if (!should) {
            logger.info("shouldSwitch=false: $reason")
            return
        }

        logger.info("调用 InputMethodManager.switchTo($targetMethod)...")
        val settings = inputMethodService.getSettings()
        val success = InputMethodManager.switchTo(targetMethod, settings)
        val elapsed = System.currentTimeMillis() - startTs
        logger.info("switchTo 返回: $success, 耗时: ${elapsed}ms")

        // 显示 Toast 提示
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null && success) {
            val toastMessage = generateToastMessage(contextInfo, targetMethod, fileName)
            val isChinese = targetMethod == InputMethodType.CHINESE
            ToastManager.showToast(editor, toastMessage, isChinese)
        }
    }

    /**
     * 根据上下文和目标输入法生成提示消息
     */
    private fun generateToastMessage(
        contextInfo: ContextInfo,
        targetMethod: InputMethodType,
        fileName: String
    ): String {
        return when (targetMethod) {
            InputMethodType.CHINESE -> {
                when (contextInfo.type) {
                    ContextType.COMMENT -> "中文文字之间自动切换为中文"
                    ContextType.STRING -> contextInfo.reason
                    else -> "已切换为中文"
                }
            }
            InputMethodType.ENGLISH -> {
                // 根据文件类型生成消息
                val fileType = getFileType(fileName)
                when (contextInfo.type) {
                    ContextType.DEFAULT -> {
                        when (fileType) {
                            FileType.JAVA -> "Java 文件默认切换为英文"
                            FileType.KOTLIN -> "Kotlin 文件默认切换为英文"
                            FileType.PYTHON -> "Python 文件默认切换为英文"
                            FileType.GO -> "Go 文件默认切换为英文"
                            FileType.JAVASCRIPT -> "JavaScript 文件默认切换为英文"
                            FileType.TYPESCRIPT -> "TypeScript 文件默认切换为英文"
                            FileType.C_CPP -> "C/C++ 文件默认切换为英文"
                            FileType.OTHER -> "代码区域切换为英文"
                        }
                    }
                    ContextType.STRING -> {
                        // 英文字符串保持英文
                        "字符串区域保持英文"
                    }
                    else -> "已切换为英文"
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
            ContextType.DEFAULT -> settings.defaultMethod
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
