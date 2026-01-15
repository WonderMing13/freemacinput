package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import java.awt.Robot
import java.awt.event.KeyEvent

/**
 * 字符串输入补救功能
 * 当用户在字符串区域想要输入中文却使用了英文输入法时，
 * 主动切换输入法之后，插件会自动帮助删除之前的输入然后重新输入，利用中文输入法转换拼音
 * 
 * 例如：用户想输入"测试"，但用英文输入法打了"ceshi"，切换到中文后，
 * 插件会删除"ceshi"，然后自动重新输入"ceshi"，利用中文输入法转换为"测试"
 */
object StringInputRescue {
    
    private val logger = Logger.getInstance(StringInputRescue::class.java)
    
    /**
     * 记录用户输入
     */
    fun recordInput(editor: Editor, text: String, offset: Int) {
        // 暂时不需要记录
    }
    
    /**
     * 执行输入补救
     * 当用户切换输入法时调用
     */
    fun rescueInput(
        editor: Editor,
        project: Project,
        fromMethod: InputMethodType,
        toMethod: InputMethodType
    ): Boolean {
        // 只在从英文切换到中文时执行补救
        if (fromMethod != InputMethodType.ENGLISH || toMethod != InputMethodType.CHINESE) {
            logger.info("不支持的切换方向: $fromMethod -> $toMethod，跳过补救")
            return false
        }
        
        logger.info("========== 开始执行字符串输入补救 ==========")
        logger.info("切换方向: $fromMethod -> $toMethod")
        
        try {
            // 必须在 Read Action 中访问编辑器数据
            val (currentOffset, text) = ApplicationManager.getApplication().runReadAction<Pair<Int, String>> {
                val offset = editor.caretModel.offset
                val docText = editor.document.text
                Pair(offset, docText)
            }
            
            logger.info("当前光标位置: $currentOffset, 文档长度: ${text.length}")
            
            // 查找光标所在的字符串范围
            val stringRange = findStringRange(text, currentOffset)
            if (stringRange == null) {
                logger.info("未找到字符串范围，取消补救")
                return false
            }
            
            val (stringStart, stringEnd) = stringRange
            
            // 安全检查：确保范围有效
            if (stringStart < 0 || stringEnd > text.length || stringStart >= stringEnd) {
                logger.warn("字符串范围无效: [$stringStart, $stringEnd], 文档长度: ${text.length}")
                return false
            }
            
            val stringContent = text.substring(stringStart, stringEnd)
            
            logger.info("字符串范围: [$stringStart, $stringEnd], 内容: '$stringContent'")
            
            // 检查字符串内容是否包含英文字符（拼音）
            if (!containsEnglishChars(stringContent)) {
                logger.info("字符串内容不包含英文字符，无需补救")
                return false
            }
            
            // 只删除光标前的英文字符，不删除整个字符串
            val deleteStart = findEnglishTextStart(text, currentOffset, stringStart)
            val deleteEnd = currentOffset
            
            if (deleteStart >= deleteEnd) {
                logger.info("没有需要删除的英文字符")
                return false
            }
            
            val toDelete = text.substring(deleteStart, deleteEnd)
            logger.info("准备补救输入：删除光标前的英文字符 '$toDelete' (范围: [$deleteStart, $deleteEnd])")
            
            // 在 EDT 线程中执行删除，并确保光标位置正确
            ApplicationManager.getApplication().invokeLater {
                try {
                    logger.info("开始删除英文字符...")
                    WriteCommandAction.runWriteCommandAction(project) {
                        // 删除英文字符
                        editor.document.deleteString(deleteStart, deleteEnd)
                        
                        // 确保光标在删除位置
                        val newOffset = deleteStart
                        // 验证新位置是否有效
                        if (newOffset >= 0 && newOffset <= editor.document.textLength) {
                            editor.caretModel.moveToOffset(newOffset)
                            // 滚动到光标位置
                            editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE)
                            logger.info("✅ 输入补救成功：已删除 '$toDelete'，光标位置: $newOffset")
                        } else {
                            logger.warn("光标位置无效: $newOffset, 文档长度: ${editor.document.textLength}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("补救输入时出错: ${e.message}", e)
                    e.printStackTrace()
                }
            }
            
            return true
            
        } catch (e: Exception) {
            logger.error("输入补救失败: ${e.message}", e)
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * 使用 Robot 重新输入文本
     */
    private fun replayInputWithRobot(text: String): Boolean {
        return try {
            logger.info("创建 Robot 实例...")
            val robot = Robot()
            robot.autoDelay = 20
            
            logger.info("开始逐字符输入: '$text'")
            for ((index, char) in text.withIndex()) {
                logger.info("输入第 ${index + 1} 个字符: '$char'")
                typeChar(robot, char)
            }
            
            logger.info("Robot 输入完成")
            true
        } catch (e: Exception) {
            logger.error("Robot 输入失败: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 使用 Robot 输入单个字符
     */
    private fun typeChar(robot: Robot, char: Char) {
        val keyCode = when (char.lowercaseChar()) {
            'a' -> KeyEvent.VK_A
            'b' -> KeyEvent.VK_B
            'c' -> KeyEvent.VK_C
            'd' -> KeyEvent.VK_D
            'e' -> KeyEvent.VK_E
            'f' -> KeyEvent.VK_F
            'g' -> KeyEvent.VK_G
            'h' -> KeyEvent.VK_H
            'i' -> KeyEvent.VK_I
            'j' -> KeyEvent.VK_J
            'k' -> KeyEvent.VK_K
            'l' -> KeyEvent.VK_L
            'm' -> KeyEvent.VK_M
            'n' -> KeyEvent.VK_N
            'o' -> KeyEvent.VK_O
            'p' -> KeyEvent.VK_P
            'q' -> KeyEvent.VK_Q
            'r' -> KeyEvent.VK_R
            's' -> KeyEvent.VK_S
            't' -> KeyEvent.VK_T
            'u' -> KeyEvent.VK_U
            'v' -> KeyEvent.VK_V
            'w' -> KeyEvent.VK_W
            'x' -> KeyEvent.VK_X
            'y' -> KeyEvent.VK_Y
            'z' -> KeyEvent.VK_Z
            ' ' -> KeyEvent.VK_SPACE
            else -> {
                logger.warn("不支持的字符: '$char'")
                return
            }
        }
        
        try {
            // 如果是大写字母，需要按住 Shift
            if (char.isUpperCase()) {
                robot.keyPress(KeyEvent.VK_SHIFT)
            }
            
            robot.keyPress(keyCode)
            robot.keyRelease(keyCode)
            
            if (char.isUpperCase()) {
                robot.keyRelease(KeyEvent.VK_SHIFT)
            }
            
            logger.info("成功输入字符: '$char'")
        } catch (e: Exception) {
            logger.error("输入字符 '$char' 失败: ${e.message}", e)
        }
    }
    
    /**
     * 查找光标前连续英文字符的起始位置
     * 只删除光标前的连续英文字符，不删除整个字符串
     */
    private fun findEnglishTextStart(text: String, offset: Int, stringStart: Int): Int {
        var start = offset - 1
        
        // 向前查找，直到遇到非英文字符或到达字符串开始
        while (start >= stringStart) {
            val c = text[start]
            if (c !in 'a'..'z' && c !in 'A'..'Z') {
                break
            }
            start--
        }
        
        // start 现在指向最后一个非英文字符，所以返回 start + 1
        return start + 1
    }
    
    /**
     * 查找光标所在的字符串范围（不包括引号）
     */
    private fun findStringRange(text: String, offset: Int): Pair<Int, Int>? {
        try {
            if (offset < 0 || offset > text.length) {
                return null
            }
            
            // 向前查找开引号
            var stringStart = -1
            var quoteChar: Char? = null
            var i = offset - 1
            while (i >= 0) {
                val c = text[i]
                if (c == '"' || c == '\'') {
                    var escapeCount = 0
                    var j = i - 1
                    while (j >= 0 && text[j] == '\\') {
                        escapeCount++
                        j--
                    }
                    if (escapeCount % 2 == 0) {
                        stringStart = i + 1
                        quoteChar = c
                        break
                    }
                }
                i--
            }
            
            if (stringStart == -1 || quoteChar == null) return null
            
            // 向后查找闭引号
            var stringEnd = -1
            i = offset
            while (i < text.length) {
                val c = text[i]
                if (c == quoteChar) {
                    var escapeCount = 0
                    var j = i - 1
                    while (j >= 0 && text[j] == '\\') {
                        escapeCount++
                        j--
                    }
                    if (escapeCount % 2 == 0) {
                        stringEnd = i
                        break
                    }
                }
                i++
            }
            
            if (stringEnd == -1) return null
            
            return Pair(stringStart, stringEnd)
        } catch (e: Exception) {
            logger.error("查找字符串范围时出错: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 判断是否包含英文字符
     */
    private fun containsEnglishChars(text: String): Boolean {
        if (text.isEmpty()) return false
        return text.any { it in 'a'..'z' || it in 'A'..'Z' }
    }
    
    /**
     * 清除记录
     */
    fun clear(editor: Editor) {
        // 暂时不需要
    }
    
    /**
     * 清除所有记录
     */
    fun clearAll() {
        // 暂时不需要
    }
}
