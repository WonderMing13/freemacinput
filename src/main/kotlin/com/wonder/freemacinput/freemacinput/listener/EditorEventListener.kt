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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ScheduledFuture

/**
 * 编辑器事件监听器
 */
class EditorEventListener(private val project: Project) : CaretListener, DocumentListener {
    private val logger = Logger.getInstance(EditorEventListener::class.java)

    private val contextDetector = ContextDetector()
    private val inputMethodService = InputMethodService.getInstance(project)

    private var lastContextInfo: ContextInfo? = null
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var scheduledFuture: ScheduledFuture<*>? = null
    private val switchDelayMs = 120L

    fun onEditorActivated(editor: Editor) {
        logger.info("onEditorActivated called")
        val data = extractEditorData(editor)
        logger.info("onEditorActivated: ${data.fileName}, offset=${data.caretOffset}")
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.isGitCommit, data.caretOffset, 400L)
    }

    private fun extractEditorData(editor: Editor): EditorData {
        val fileName = editor.virtualFile?.name ?: "unknown"
        val documentText = editor.document.text
        val caretOffset = editor.caretModel.offset
        val isGitCommit = isGitCommitFile(fileName)
        return EditorData(fileName, documentText, isGitCommit, caretOffset)
    }

    private fun isGitCommitFile(fileName: String): Boolean {
        return fileName.contains("COMMIT_EDITMSG") || fileName.endsWith(".tmp")
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
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.isGitCommit, data.caretOffset)
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
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.isGitCommit, data.caretOffset, 100L)
    }

    override fun beforeDocumentChange(event: DocumentEvent) {}

    private fun updateCaretRenderer(editor: Editor) {
        val renderer = CaretRendererManager.getOrCreate(editor)
        renderer.setEnabled(inputMethodService.isEnableCaretColor())
        renderer.setCapsLockState(InputMethodManager.getCapsLockState())
        renderer.refresh()
    }

    private fun scheduleInputMethodSwitch(
        fileName: String,
        documentText: String,
        isGitCommit: Boolean,
        caretOffset: Int,
        delayMs: Long = switchDelayMs
    ) {
        logger.info("scheduleInputMethodSwitch: file=$fileName, offset=$caretOffset")
        scheduledFuture?.cancel(false)
        scheduledFuture = null

        val task = Runnable {
            logger.info("scheduled task running...")
            scheduledFuture = null
            detectAndSwitch(fileName, documentText, isGitCommit, caretOffset)
        }

        scheduledFuture = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS)
        logger.info("task scheduled with delay=${delayMs}ms")
    }

    private fun detectAndSwitch(
        fileName: String,
        documentText: String,
        isGitCommit: Boolean,
        caretOffset: Int
    ) {
        val startTs = System.currentTimeMillis()
        if (!inputMethodService.isEnabled()) {
            logger.info("插件未启用")
            return
        }

        logger.info("开始检测上下文... file=$fileName")

        val contextInfo = contextDetector.detectContext(documentText, isGitCommit, caretOffset)
        logger.info("检测到上下文: ${contextInfo.type}, 原因: ${contextInfo.reason}")

        val contextChanged = lastContextInfo == null || contextInfo.type != lastContextInfo?.type
        if (!contextChanged) {
            logger.info("上下文无变化，跳过")
            return
        }

        lastContextInfo = contextInfo
        val targetMethod = determineInputMethod(contextInfo)
        logger.info("目标输入法: $targetMethod, 当前上下文: ${contextInfo.type}")

        // 使用 InputMethodManager 的内部状态与冷却判定，不再依赖 CapsLock 误判
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
    }

    private fun determineInputMethod(contextInfo: ContextInfo): InputMethodType {
        val settings = inputMethodService.getSettings()
        return when (contextInfo.type) {
            ContextType.DEFAULT -> settings.defaultMethod
            ContextType.COMMENT -> settings.commentMethod
            ContextType.STRING -> settings.commentMethod
            ContextType.GIT_COMMIT -> settings.gitCommitMethod
            ContextType.TOOL_WINDOW -> settings.defaultMethod
            ContextType.CUSTOM_EVENT -> settings.customEventMethod
            ContextType.CUSTOM_RULE -> settings.customRuleMethod
            ContextType.UNKNOWN -> settings.defaultMethod
        }
    }

    fun dispose() {
        scheduler.shutdown()
        CaretRendererManager.disposeAll()
    }

    private data class EditorData(
        val fileName: String,
        val documentText: String,
        val isGitCommit: Boolean,
        val caretOffset: Int
    )
}
