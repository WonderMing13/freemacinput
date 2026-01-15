package com.wonder.freemacinput.freemacinput.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.wonder.freemacinput.freemacinput.config.LeaveIDEStrategy
import com.wonder.freemacinput.freemacinput.config.SettingsState
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.core.GitCommitSceneManager
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

/**
 * IDE焦点监听器
 * 监听IDE窗口的焦点变化，实现离开IDE场景的输入法切换
 * 
 * 注意：此功能仅适用于 macOS 系统
 */
class IDEFocusListener(private val project: Project) : WindowFocusListener {
    
    private val logger = Logger.getInstance(IDEFocusListener::class.java)
    private val isMacOS = System.getProperty("os.name", "").lowercase().contains("mac")
    
    private var isRegistered = false
    
    /**
     * 注册监听器
     */
    fun register() {
        if (!isMacOS) {
            logger.info("非 macOS 系统，跳过 IDE 焦点监听器注册")
            return
        }
        
        if (isRegistered) {
            return
        }
        
        try {
            // 获取所有窗口并添加监听器
            val windows = Window.getWindows()
            windows.forEach { window ->
                window.addWindowFocusListener(this)
            }
            
            isRegistered = true
            logger.info("✅ IDE 焦点监听器注册成功")
            
        } catch (e: Exception) {
            logger.error("IDE 焦点监听器注册失败", e)
        }
    }
    
    /**
     * 注销监听器
     */
    fun unregister() {
        if (!isRegistered) {
            return
        }
        
        try {
            val windows = Window.getWindows()
            windows.forEach { window ->
                window.removeWindowFocusListener(this)
            }
            
            isRegistered = false
            logger.info("IDE 焦点监听器已注销")
            
        } catch (e: Exception) {
            logger.error("IDE 焦点监听器注销失败", e)
        }
    }
    
    /**
     * 窗口获得焦点
     */
    override fun windowGainedFocus(e: WindowEvent?) {
        logger.info("IDE 窗口获得焦点")
        
        // 记录进入IDE前的输入法
        val settings = getSettings()
        if (settings.leaveIDEStrategy == LeaveIDEStrategy.RESTORE_PREVIOUS) {
            val currentIM = InputMethodManager.getCurrentInputMethodName()
            settings.inputMethodBeforeEnterIDE = currentIM
            logger.info("记录进入IDE前的输入法: $currentIM")
        }
    }
    
    /**
     * 窗口失去焦点
     */
    override fun windowLostFocus(e: WindowEvent?) {
        logger.info("IDE 窗口失去焦点")
        
        // 如果当前在特殊场景中（Git 提交、工具窗口等），不要切换
        if (GitCommitSceneManager.isInAnySpecialScene()) {
            logger.info("当前在特殊场景中，跳过离开IDE的输入法切换")
            return
        }
        
        val settings = getSettings()
        val strategy = settings.leaveIDEStrategy
        
        logger.info("离开IDE策略: ${strategy.getDisplayName()}")
        
        when (strategy) {
            LeaveIDEStrategy.RESTORE_PREVIOUS -> {
                // 恢复进入IDE前的输入法
                val previousIM = settings.inputMethodBeforeEnterIDE
                if (previousIM != null) {
                    logger.info("恢复进入IDE前的输入法: $previousIM")
                    restoreInputMethod(previousIM)
                }
            }
            
            LeaveIDEStrategy.ENGLISH -> {
                // 切换为英文
                logger.info("切换为英文输入法")
                InputMethodManager.switchTo(InputMethodType.ENGLISH, settings)
            }
            
            LeaveIDEStrategy.NATIVE_LANGUAGE -> {
                // 切换为中文
                logger.info("切换为中文输入法")
                InputMethodManager.switchTo(InputMethodType.CHINESE, settings)
            }
            
            LeaveIDEStrategy.NO_CHANGE -> {
                // 不切换
                logger.info("不切换输入法")
            }
        }
    }
    
    /**
     * 恢复指定的输入法
     */
    private fun restoreInputMethod(inputMethodId: String) {
        try {
            // 使用 im-select 直接切换到指定的输入法
            val process = Runtime.getRuntime().exec(arrayOf("im-select", inputMethodId))
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.info("✅ 成功恢复输入法: $inputMethodId")
            } else {
                logger.warn("恢复输入法失败，退出码: $exitCode")
            }
            
        } catch (e: Exception) {
            logger.error("恢复输入法失败", e)
        }
    }
    
    /**
     * 获取设置
     */
    private fun getSettings(): SettingsState {
        return ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
