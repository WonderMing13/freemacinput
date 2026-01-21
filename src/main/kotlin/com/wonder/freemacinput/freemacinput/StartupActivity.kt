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
import com.intellij.util.messages.MessageBusConnection
import com.wonder.freemacinput.freemacinput.core.InputMethodManager

import com.wonder.freemacinput.freemacinput.listener.EditorEventListener
import com.wonder.freemacinput.freemacinput.listener.IDEFocusListener
import com.wonder.freemacinput.freemacinput.listener.GitCommitListener
import com.wonder.freemacinput.freemacinput.listener.ToolWindowFocusListener
import com.wonder.freemacinput.freemacinput.listener.CustomEventListener
import com.wonder.freemacinput.freemacinput.service.InputMethodService
import com.wonder.freemacinput.freemacinput.ui.ToastManager

/**
 * 启动活动 - 项目打开后执行初始化
 */
class StartupActivity : IJStartupActivity, DumbAware {

    private val logger = Logger.getInstance(StartupActivity::class.java)

    private val registeredEditors = mutableSetOf<Long>()
    private var editorListener: EditorEventListener? = null
    private var ideFocusListener: IDEFocusListener? = null
    private var gitCommitListener: GitCommitListener? = null
    private var toolWindowListener: ToolWindowFocusListener? = null
    private var connection: MessageBusConnection? = null

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

            // 初始化输入法管理器
            logger.info("初始化输入法管理器...")
            val initResult = InputMethodManager.initialize()
            logger.info("输入法管理器初始化结果: $initResult")

            if (!initResult) {
                logger.warn("输入法管理器初始化失败")
                ApplicationManager.getApplication().invokeLater {
                    Messages.showWarningDialog(
                        "输入法切换功能初始化失败！\n\n" +
                        "可能原因：\n" +
                        "1. 不支持当前操作系统\n" +
                        "2. 缺少必要的系统权限\n\n" +
                        "macOS 用户请检查：\n" +
                        "系统设置 → 隐私与安全性 → 辅助功能/自动化\n" +
                        "确保 IntelliJ IDEA 有权限\n\n" +
                        "Windows 用户请检查：\n" +
                        "确保已安装所需的输入语言",
                        "FreeMacInput"
                    )
                }
                return
            }

            logger.info("✅ 输入法管理器初始化成功")
            
            // 显示当前输入法信息
            val currentIM = InputMethodManager.getCurrentInputMethodName()
            logger.info("当前输入法: $currentIM")

            // 创建事件监听器
            editorListener = EditorEventListener(project)
            // 创建持久化的事件总线连接
            connection = project.messageBus.connect(project)
            
            // 创建并注册 IDE 焦点监听器（仅 macOS）
            ideFocusListener = IDEFocusListener(project)
            ideFocusListener?.register()
            
            // 创建并注册 Git 提交场景监听器
            gitCommitListener = GitCommitListener(project)
            gitCommitListener?.register()
            
            // 创建并注册工具窗口监听器
            toolWindowListener = ToolWindowFocusListener(project)
            connection?.subscribe(
                com.intellij.openapi.wm.ex.ToolWindowManagerListener.TOPIC,
                toolWindowListener!!
            )
            // 为工具窗口组件添加焦点监听
            toolWindowListener?.attachFocusListeners()
            
            // 注册自定义事件监听器
            CustomEventListener.register(project)
            logger.info("✅ 自定义事件监听器已注册")

            // 注册编辑器工厂事件监听
            EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor ?: return
                    val editorId = System.identityHashCode(editor).toLong()

                    if (registeredEditors.contains(editorId)) {
                        logger.info("编辑器已注册，跳过: ${editor.virtualFile?.name ?: "untitled"}")
                        return
                    }

                    val fileName = editor.virtualFile?.name ?: "untitled"
                    logger.info("编辑器创建: $fileName")
                    registeredEditors.add(editorId)

                    // 为每个编辑器创建独立的监听器实例
                    val editorSpecificListener = EditorEventListener(project)
                    registerListeners(editor, editorSpecificListener)
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    event.editor?.let { editor ->
                        registeredEditors.remove(System.identityHashCode(editor).toLong())
                        ToastManager.dismissToast(editor)
                    }
                }
            }, project)
            
            // 为已经打开的编辑器注册监听器
            ApplicationManager.getApplication().invokeLater {
                val allEditors = EditorFactory.getInstance().allEditors
                logger.info("为 ${allEditors.size} 个已打开的编辑器注册监听器")
                for (editor in allEditors) {
                    val editorId = System.identityHashCode(editor).toLong()
                    if (!registeredEditors.contains(editorId)) {
                        val fileName = editor.virtualFile?.name ?: "untitled"
                        logger.info("为已打开的编辑器注册监听器: $fileName")
                        registeredEditors.add(editorId)
                        // 为每个编辑器创建独立的监听器实例
                        val editorSpecificListener = EditorEventListener(project)
                        registerListeners(editor, editorSpecificListener)
                    }
                }
            }

            // 注册文件切换监听
            connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
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
        val fileName = editor.virtualFile?.name ?: "untitled"
        logger.info("📌 为编辑器注册监听器: $fileName")
        logger.info("   - 添加 CaretListener")
        editor.caretModel.addCaretListener(listener)
        logger.info("   - 添加 DocumentListener")
        editor.document.addDocumentListener(listener)
        logger.info("   - 添加 EditorMouseListener")
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                logger.info("🖱️ 鼠标点击编辑器: $fileName")
                listener.onEditorActivated(event.editor)
            }
        })
        logger.info("✅ 监听器注册完成: $fileName")
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
}
