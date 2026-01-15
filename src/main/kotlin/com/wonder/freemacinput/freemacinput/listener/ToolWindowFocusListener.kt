package com.wonder.freemacinput.freemacinput.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.wonder.freemacinput.freemacinput.config.SettingsState
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.core.GitCommitSceneManager
import com.wonder.freemacinput.freemacinput.ui.ToastManager
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.SwingUtilities

/**
 * 工具窗口焦点监听器
 */
class ToolWindowFocusListener(private val project: Project) : ToolWindowManagerListener {
    
    private val logger = Logger.getInstance(ToolWindowFocusListener::class.java)
    private val inputMethodManager = InputMethodManager
    private val settings: SettingsState
        get() = ApplicationManager.getApplication().getService(SettingsState::class.java)
    
    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        if (!settings.isEnabled) return
        
        // 获取当前活动的工具窗口
        val activeToolWindowId = toolWindowManager.activeToolWindowId
        
        if (activeToolWindowId == null) {
            // 没有活动的工具窗口，清除标记
            logger.info("没有活动的工具窗口，清除工具窗口场景标记")
            GitCommitSceneManager.setInToolWindowScene(false)
            return
        }
        
        // 查找对应的规则
        val rule = settings.toolWindowRules.find { 
            it.toolWindowId == activeToolWindowId && it.enabled 
        }
        
        if (rule != null) {
            logger.info("工具窗口 ${rule.displayName} 获得焦点，切换到 ${rule.targetInputMethod}")
            
            // 标记进入工具窗口场景
            GitCommitSceneManager.setInToolWindowScene(true)
            
            // 切换输入法
            inputMethodManager.switchTo(rule.targetInputMethod, settings)
            
            // 显示提示
            if (settings.isShowHints) {
                ApplicationManager.getApplication().invokeLater {
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    if (editor != null) {
                        val methodName = if (rule.targetInputMethod == InputMethodType.CHINESE) "中文" else "英文"
                        ToastManager.showToast(
                            editor,
                            "工具窗口场景：${rule.displayName} → $methodName",
                            rule.targetInputMethod == InputMethodType.CHINESE
                        )
                    }
                }
            }
        } else {
            // 工具窗口没有配置规则，清除标记
            logger.info("工具窗口 $activeToolWindowId 没有配置规则，清除工具窗口场景标记")
            GitCommitSceneManager.setInToolWindowScene(false)
        }
    }
    
    /**
     * 为工具窗口的组件添加焦点监听
     */
    fun attachFocusListeners() {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        
        toolWindowManager.toolWindowIds.forEach { id ->
            val toolWindow = toolWindowManager.getToolWindow(id) ?: return@forEach
            val component = toolWindow.component
            
            // 添加焦点监听器
            component.addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    if (!settings.isEnabled) return
                    
                    val rule = settings.toolWindowRules.find { 
                        it.toolWindowId == id && it.enabled 
                    }
                    
                    if (rule != null) {
                        logger.info("工具窗口组件 ${rule.displayName} 获得焦点")
                        
                        // 标记进入工具窗口场景
                        GitCommitSceneManager.setInToolWindowScene(true)
                        
                        SwingUtilities.invokeLater {
                            inputMethodManager.switchTo(rule.targetInputMethod, settings)
                            
                            if (settings.isShowHints) {
                                ApplicationManager.getApplication().invokeLater {
                                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                                    if (editor != null) {
                                        val methodName = if (rule.targetInputMethod == InputMethodType.CHINESE) "中文" else "英文"
                                        ToastManager.showToast(
                                            editor,
                                            "工具窗口场景：${rule.displayName} → $methodName",
                                            rule.targetInputMethod == InputMethodType.CHINESE
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                override fun focusLost(e: FocusEvent?) {
                    // 失去焦点时清除工具窗口场景标记
                    logger.info("工具窗口组件失去焦点，清除工具窗口场景标记")
                    GitCommitSceneManager.setInToolWindowScene(false)
                }
            })
        }
    }
}
