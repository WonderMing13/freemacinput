package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import java.awt.Color

/**
 * 光标颜色管理器
 * 根据输入法状态改变光标颜色
 */
object CursorColorManager {
    
    private val logger = Logger.getInstance(CursorColorManager::class.java)
    
    // 默认颜色
    private var originalCursorColor: Color? = null
    
    /**
     * 设置光标颜色
     */
    fun setCursorColor(editor: Editor?, color: Color) {
        if (editor == null) {
            logger.warn("Editor 为 null，无法设置光标颜色")
            return
        }
        
        try {
            // 保存原始颜色（第一次设置时）
            if (originalCursorColor == null) {
                originalCursorColor = editor.colorsScheme.defaultForeground
            }
            
            // 设置光标颜色
            editor.colorsScheme.setColor(
                com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR,
                color
            )
            
            // 刷新编辑器
            editor.contentComponent.repaint()
            
            logger.info("光标颜色已设置为: ${colorToHex(color)}")
        } catch (e: Exception) {
            logger.error("设置光标颜色失败", e)
        }
    }
    
    /**
     * 根据输入法类型设置光标颜色
     */
    fun setCursorColorByInputMethod(
        editor: Editor?,
        inputMethod: InputMethodType,
        chineseColor: Color,
        englishColor: Color,
        capsLockColor: Color
    ) {
        if (editor == null) return
        
        val color = when (inputMethod) {
            InputMethodType.CHINESE -> chineseColor
            InputMethodType.ENGLISH -> {
                // 检查是否是大写锁定状态
                if (isCapsLockOn()) {
                    capsLockColor
                } else {
                    englishColor
                }
            }
            else -> englishColor
        }
        
        setCursorColor(editor, color)
    }
    
    /**
     * 恢复原始光标颜色
     */
    fun restoreOriginalColor(editor: Editor?) {
        if (editor == null || originalCursorColor == null) return
        
        try {
            editor.colorsScheme.setColor(
                com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR,
                originalCursorColor
            )
            editor.contentComponent.repaint()
            logger.info("光标颜色已恢复")
        } catch (e: Exception) {
            logger.error("恢复光标颜色失败", e)
        }
    }
    
    /**
     * 检查大写锁定是否开启
     */
    private fun isCapsLockOn(): Boolean {
        return try {
            java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 设置大写锁定状态
     */
    fun setCapsLockState(enabled: Boolean): Boolean {
        return try {
            val toolkit = java.awt.Toolkit.getDefaultToolkit()
            val currentState = toolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK)
            logger.info("当前 Caps Lock 状态: $currentState, 目标状态: $enabled")
            
            if (currentState != enabled) {
                // 使用 Toolkit.setLockingKeyState() 直接设置状态（跨平台支持）
                toolkit.setLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK, enabled)
                
                // 等待状态更新
                Thread.sleep(100)
                
                val newState = toolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK)
                logger.info("Caps Lock 切换: $currentState -> $newState (目标: $enabled)")
                
                return newState == enabled
            }
            logger.info("Caps Lock 已经是目标状态，无需切换")
            true
        } catch (e: Exception) {
            logger.error("设置 Caps Lock 状态失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 将颜色转换为十六进制字符串
     */
    private fun colorToHex(color: Color): String {
        return String.format("#%02X%02X%02X", color.red, color.green, color.blue)
    }
    
    /**
     * 从十六进制字符串解析颜色
     */
    fun parseColor(hex: String): Color? {
        return try {
            val cleanHex = hex.removePrefix("#")
            Color(
                cleanHex.substring(0, 2).toInt(16),
                cleanHex.substring(2, 4).toInt(16),
                cleanHex.substring(4, 6).toInt(16)
            )
        } catch (e: Exception) {
            logger.error("解析颜色失败: $hex", e)
            null
        }
    }
}
