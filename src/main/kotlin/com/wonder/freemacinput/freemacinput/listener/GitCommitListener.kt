package com.wonder.freemacinput.freemacinput.listener

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.core.GitCommitSceneManager
import com.wonder.freemacinput.freemacinput.service.InputMethodService
import com.wonder.freemacinput.freemacinput.ui.ToastManager
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextArea
import javax.swing.JEditorPane
import javax.swing.SwingUtilities

/**
 * Git 提交场景监听器
 * 检测 Git 提交窗口并自动切换到中文输入法
 */
class GitCommitListener(private val project: Project) {
    
    private val logger = Logger.getInstance(GitCommitListener::class.java)
    private val inputMethodService = InputMethodService.getInstance(project)
    private val registeredComponents = mutableSetOf<Component>()
    
    /**
     * 注册监听器
     */
    fun register() {
        logger.info("注册 Git 提交场景监听器")
        
        // 监听工具窗口变化
        val connection = project.messageBus.connect(project)
        connection.subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                checkCommitToolWindow()
            }
        })
        
        // 立即检查一次
        ApplicationManager.getApplication().invokeLater {
            checkCommitToolWindow()
        }
    }
    
    /**
     * 检查 Commit 工具窗口
     */
    private fun checkCommitToolWindow() {
        try {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            
            // 查找 Commit 工具窗口
            val commitToolWindow = toolWindowManager.getToolWindow("Commit")
            
            if (commitToolWindow != null && commitToolWindow.isVisible) {
                logger.info("🔍 检测到 Commit 工具窗口可见")
                
                // 查找提交消息输入框
                val contentManager = commitToolWindow.contentManager
                contentManager.addContentManagerListener(object : ContentManagerListener {
                    override fun contentAdded(event: ContentManagerEvent) {
                        logger.info("Commit 内容添加")
                        findAndMonitorCommitMessageField(event.content.component)
                    }
                    
                    override fun selectionChanged(event: ContentManagerEvent) {
                        logger.info("Commit 选择变化")
                        findAndMonitorCommitMessageField(event.content.component)
                    }
                })
                
                // 检查当前内容
                val selectedContent = contentManager.selectedContent
                if (selectedContent != null) {
                    logger.info("检查当前 Commit 内容")
                    findAndMonitorCommitMessageField(selectedContent.component)
                }
            }
            
            // 也检查 Version Control 工具窗口（旧版本）
            val vcsToolWindow = toolWindowManager.getToolWindow("Version Control")
            if (vcsToolWindow != null && vcsToolWindow.isVisible) {
                logger.info("🔍 检测到 Version Control 工具窗口可见")
                val contentManager = vcsToolWindow.contentManager
                val selectedContent = contentManager.selectedContent
                if (selectedContent != null) {
                    findAndMonitorCommitMessageField(selectedContent.component)
                }
            }
            
        } catch (e: Exception) {
            logger.warn("检查 Commit 工具窗口时出错: ${e.message}", e)
        }
    }
    
    /**
     * 查找并监控提交消息输入框
     */
    private fun findAndMonitorCommitMessageField(component: Component) {
        try {
            logger.info("查找提交消息输入框，组件类型: ${component.javaClass.name}")
            
            // 递归查找所有文本组件
            val textComponents = mutableListOf<Component>()
            findTextComponents(component, textComponents)
            
            logger.info("找到 ${textComponents.size} 个文本组件")
            
            for (textComponent in textComponents) {
                if (registeredComponents.contains(textComponent)) {
                    continue
                }
                
                val componentClass = textComponent.javaClass.name
                logger.info("文本组件: $componentClass")
                
                // 检查是否是提交消息输入框
                if (isCommitMessageField(textComponent)) {
                    logger.info("✅ 找到提交消息输入框: $componentClass")
                    registeredComponents.add(textComponent)
                    monitorCommitMessageField(textComponent)
                }
            }
            
        } catch (e: Exception) {
            logger.warn("查找提交消息输入框时出错: ${e.message}", e)
        }
    }
    
    /**
     * 递归查找所有文本组件
     */
    private fun findTextComponents(component: Component, result: MutableList<Component>) {
        // 查找所有可能的文本输入组件
        if (component is JTextArea || 
            component is JEditorPane ||
            component is javax.swing.JTextField ||
            component is javax.swing.text.JTextComponent) {
            result.add(component)
            logger.info("  -> 找到文本组件: ${component.javaClass.name}, 可编辑: ${(component as? javax.swing.text.JTextComponent)?.isEditable}")
        }
        
        if (component is java.awt.Container) {
            for (child in component.components) {
                findTextComponents(child, result)
            }
        }
    }
    
    /**
     * 判断是否是提交消息输入框
     */
    private fun isCommitMessageField(component: Component): Boolean {
        val className = component.javaClass.name
        
        // 检查类名是否包含提交相关的关键字
        if (className.contains("CommitMessage", ignoreCase = true) ||
            className.contains("VcsCommit", ignoreCase = true) ||
            className.contains("CheckinPanel", ignoreCase = true)) {
            return true
        }
        
        // 检查是否是可编辑的多行文本框
        if (component is JTextArea && component.isEditable && component.rows > 1) {
            return true
        }
        
        // 检查是否是可编辑的 JEditorPane
        if (component is JEditorPane && component.isEditable) {
            return true
        }
        
        // 检查是否是可编辑的 JTextComponent（更通用）
        if (component is javax.swing.text.JTextComponent && component.isEditable) {
            // 排除单行文本框（通常是搜索框等）
            if (component !is javax.swing.JTextField) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 监控提交消息输入框
     */
    private fun monitorCommitMessageField(component: Component) {
        logger.info("========== Git 提交场景 ==========")
        
        val settings = inputMethodService.getSettings()
        if (!settings.isEnabled) {
            logger.info("插件未启用，跳过")
            return
        }
        
        // 添加焦点监听器
        component.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                logger.info("Git 提交输入框获得焦点")
                
                // 标记进入 Git 提交场景
                GitCommitSceneManager.setInGitCommitScene(true)
                
                SwingUtilities.invokeLater {
                    val currentMethod = InputMethodManager.getCurrentInputMethod()
                    logger.info("当前输入法: $currentMethod")
                    
                    if (currentMethod != InputMethodType.CHINESE) {
                        logger.info("切换到中文输入法...")
                        val switchResult = InputMethodManager.switchTo(InputMethodType.CHINESE, settings)
                        logger.info("切换结果: ${switchResult.success}, 实际: ${switchResult.actualMethod}")
                        
                        // 显示提示
                        if (settings.isShowHints && switchResult.success) {
                            // 由于没有 Editor 对象，我们需要找到当前活动的编辑器来显示 Toast
                            ApplicationManager.getApplication().invokeLater {
                                val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                                val editor = fileEditorManager.selectedTextEditor
                                if (editor != null) {
                                    ToastManager.showToast(editor, "Git 提交场景 → 中文", true)
                                }
                            }
                        }
                    } else {
                        logger.info("已是中文输入法，无需切换")
                        
                        // 仍然显示提示
                        if (settings.isShowHints) {
                            ApplicationManager.getApplication().invokeLater {
                                val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                                val editor = fileEditorManager.selectedTextEditor
                                if (editor != null) {
                                    ToastManager.showToast(editor, "Git 提交场景", true)
                                }
                            }
                        }
                    }
                }
            }
            
            override fun focusLost(e: FocusEvent?) {
                logger.info("Git 提交输入框失去焦点")
                
                // 标记离开 Git 提交场景
                GitCommitSceneManager.setInGitCommitScene(false)
            }
        })
        
        logger.info("========== Git 提交场景监听器已设置 ==========")
    }
}
