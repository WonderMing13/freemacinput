package com.wonder.freemacinput.freemacinput.listener

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.service.InputMethodService

/**
 * 自定义事件监听器
 * 监听IDE中的Action事件，根据配置的规则自动切换输入法
 */
class CustomEventListener(private val project: Project) : AnActionListener {
    
    private val logger = Logger.getInstance(CustomEventListener::class.java)
    private val inputMethodService = InputMethodService.getInstance(project)
    
    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        try {
            val settings = inputMethodService.getSettings()
            
            // 如果没有启用事件日志记录，也不需要检查自定义事件
            if (!settings.enableEventLogging && settings.customEventRules.isEmpty()) {
                return
            }
            
            // 获取action的文本描述
            val actionText = event.presentation.text ?: return
            
            // 记录事件日志
            if (settings.enableEventLogging) {
                logger.info("FreeMacInput EventPerformed, EventName: $actionText")
            }
            
            // 检查是否匹配自定义事件规则
            val matchedRule = settings.customEventRules.find { rule ->
                rule.enabled && rule.eventName.equals(actionText, ignoreCase = true)
            }
            
            if (matchedRule != null) {
                logger.info("✅ 匹配到自定义事件规则: ${matchedRule.eventName} -> ${matchedRule.targetInputMethod}")
                
                // 切换输入法
                val switchResult = InputMethodManager.switchTo(matchedRule.targetInputMethod, settings)
                
                if (switchResult.success) {
                    logger.info("✅ 自定义事件触发输入法切换成功: ${matchedRule.targetInputMethod}")
                } else {
                    logger.warn("⚠️ 自定义事件触发输入法切换失败: ${switchResult.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("处理自定义事件失败", e)
        }
    }
    
    companion object {
        /**
         * 注册监听器
         */
        fun register(project: Project) {
            val connection = ApplicationManager.getApplication().messageBus.connect(project)
            connection.subscribe(AnActionListener.TOPIC, CustomEventListener(project))
        }
    }
}
