package com.wonder.freemacinput.freemacinput

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity as IJStartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.CapsLockState
import com.wonder.freemacinput.freemacinput.listener.EditorEventListener
import com.wonder.freemacinput.freemacinput.service.InputMethodService
import com.wonder.freemacinput.freemacinput.ui.ToastManager

/**
 * 启动活动 - 项目打开后执行初始化
 */
class StartupActivity : IJStartupActivity, DumbAware {

    private val logger = Logger.getInstance(StartupActivity::class.java)

    private val registeredEditors = mutableSetOf<Long>()
    private var editorListener: EditorEventListener? = null

    init {
        logger.info("StartupActivity 实例创建")
    }

    companion object {}

    override fun runActivity(project: Project) {
        initialize(project)
    }

    private fun initialize(project: Project) {
        logger.info("=== StartupActivity.initialize 开始 ===")

        try {
            val inputMethodService = InputMethodService.getInstance(project)
            val settings = inputMethodService.getSettings()
            logger.info("插件启用状态: ${settings.isEnabled}")

            if (!settings.isEnabled) {
                logger.info("插件已禁用")
                return
            }

            // 创建事件监听器
            editorListener = EditorEventListener(project)
            val connection = project.messageBus.connect()

            // 注册编辑器工厂事件监听
            EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor ?: return
                    val editorId = System.identityHashCode(editor).toLong()

                    if (registeredEditors.contains(editorId)) return

                    val fileName = editor.virtualFile?.name ?: "untitled"
                    logger.info("编辑器创建: $fileName")
                    registeredEditors.add(editorId)

                    if (editor.virtualFile != null) {
                        registerListeners(editor, editorListener!!)
                    }
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    event.editor?.let { editor ->
                        registeredEditors.remove(System.identityHashCode(editor).toLong())
                        ToastManager.dismissToast(editor)
                    }
                }
            }, connection)

            // 注册文件切换监听
            connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileOpened(source: com.intellij.openapi.fileEditor.FileEditorManager, file: VirtualFile) {
                    logger.info("文件打开: ${file.name}")
                    triggerDetection(project)
                }

                override fun fileClosed(source: com.intellij.openapi.fileEditor.FileEditorManager, file: VirtualFile) {
                    // 文件关闭时也关闭 Toast
                    ToastManager.dismissAll()
                }

                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                    logger.info("文件选择变化")
                    triggerDetection(project)
                }
            })

            // 启动 CapsLock 监控
            startCapsLockMonitor()

        // 延迟触发首次检测（在后台线程执行）
        Thread { 
            try {
                Thread.sleep(200)
                triggerDetection(project)
            } catch (e: Exception) {
                logger.warn("延迟检测异常: ${e.message}", e)
            }
        }.start()

        logger.info("=== 初始化完成 ===")
        } catch (e: Exception) {
            logger.error("异常: ${e.message}", e)
        }
    }

    private fun registerListeners(editor: Editor, listener: EditorEventListener) {
        editor.caretModel.addCaretListener(listener)
        editor.document.addDocumentListener(listener)
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                listener.onEditorActivated(event.editor)
            }
        })
    }

    private fun triggerDetection(project: Project) {
        try {
            ApplicationManager.getApplication().runReadAction {
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                if (editor != null) {
                    editorListener?.onEditorActivated(editor)
                }
            }
        } catch (e: Exception) {
            logger.warn("triggerDetection 异常: ${e.message}", e)
        }
    }

    private fun startCapsLockMonitor() {
        CapsLockMonitor.start()
        CapsLockMonitor.addListener(object : CapsLockStateListener {
            override fun onCapsLockStateChanged(state: CapsLockState) {
                logger.info("CapsLock: $state")
            }
        })
        logger.info("CapsLock监控已启动")
    }
}

object CapsLockMonitor {
    private var isRunning = false
    private var lastState: CapsLockState = CapsLockState.UNKNOWN
    private val listeners = mutableListOf<CapsLockStateListener>()

    fun start() {
        if (isRunning) return
        isRunning = true
        monitorCapsLock()
    }

    private fun monitorCapsLock() {
        Thread {
            while (isRunning) {
                try {
                    val currentState = InputMethodManager.getCapsLockState()
                    if (currentState != lastState) {
                        lastState = currentState
                        notifyListeners(currentState)
                    }
                    Thread.sleep(100)
                } catch (e: Exception) {
                    Thread.sleep(500)
                }
            }
        }.start()
    }

    fun addListener(listener: CapsLockStateListener) {
        listeners.add(listener)
    }

    private fun notifyListeners(state: CapsLockState) {
        listeners.forEach { it.onCapsLockStateChanged(state) }
    }
}

interface CapsLockStateListener {
    fun onCapsLockStateChanged(state: CapsLockState)
}
