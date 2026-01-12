package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger

/**
 * 上下文检测服务
 *
 * 负责检测当前编辑位置属于什么场景，支持以下场景：
 * - DEFAULT: 默认代码区域
 * - COMMENT: 注释区域
 * - STRING: 字符串字面量
 */
class ContextDetector {
    private val logger = Logger.getInstance(ContextDetector::class.java)

    /**
     * 检测当前编辑上下文场景
     *
     * @param documentText 文档文本
     * @param caretOffset 光标位置
     * @return ContextInfo 包含检测到的场景类型和相关信息的对象
     */
    fun detectContext(documentText: String, caretOffset: Int): ContextInfo {
        return try {
            logger.info("detectContext: textLength=${documentText.length}, offset=$caretOffset")

            // 获取光标所在的当前行及行位置信息
            val safeOffset = kotlin.math.min(caretOffset, documentText.length)
            val lastNewline = documentText.lastIndexOf('\n', safeOffset - 1)
            val lineStartOffset = if (lastNewline >= 0) lastNewline + 1 else 0
            val nextNewline = documentText.indexOf('\n', safeOffset)
            val lineEndOffset = if (nextNewline >= 0) nextNewline else documentText.length
            val currentLine = documentText.substring(lineStartOffset, lineEndOffset)
            val lineOffset = caretOffset - lineStartOffset

            // 检测字符串
            val inString = detectStringContext(currentLine, lineOffset)
            if (inString) {
                logger.info("检测到字符串")
                return ContextInfo(
                    type = ContextType.STRING,
                    reason = "字符串区域"
                )
            }

            // 检测注释
            val inComment = detectCommentContext(documentText, caretOffset, lineStartOffset, lineEndOffset, currentLine, lineOffset)
            if (inComment) {
                logger.info("检测到注释")
                return ContextInfo(
                    type = ContextType.COMMENT,
                    reason = "注释区域"
                )
            }

             // 代码区域 → 默认切换英文
            logger.info("检测到代码区域")
            ContextInfo(
                type = ContextType.CODE,
                reason = "代码区域"
            )
        } catch (e: Exception) {
            logger.warn("上下文检测异常: ${e.message}", e)
            ContextInfo(
                type = ContextType.UNKNOWN,
                reason = "检测异常"
            )
        }
    }

    /**
     * 检测字符串上下文
     *
     * @param currentLine 当前行文本
     * @param lineOffset 光标在行内的偏移
     * @return 是否在字符串内
     */
    private fun detectStringContext(
        currentLine: String,
        lineOffset: Int
    ): Boolean {
        if (lineOffset < 0 || lineOffset > currentLine.length) {
            return false
        }

        // 简单的字符串检测：检查光标位置是否在引号内
        var inString = false
        var stringType: Char? = null
        var escape = false

        for (i in 0 until currentLine.length) {
            val c = currentLine[i]
            if (escape) {
                escape = false
                continue
            }

            if (c == '\\') {
                escape = true
                continue
            }

            if (c == '"' || c == '\'') {
                if (inString && c == stringType) {
                    inString = false
                    stringType = null
                } else if (!inString) {
                    inString = true
                    stringType = c
                }
            }

            if (i == lineOffset) {
                break
            }
        }

        return inString
    }

    /**
     * 检测注释上下文（优化版本，支持跨行块注释）
     *
     * @param documentText 完整文档文本
     * @param caretOffset 光标位置
     * @param lineStartOffset 当前行起始位置
     * @param lineEndOffset 当前行结束位置
     * @param currentLine 当前行文本
     * @param lineOffset 光标在行内的偏移
     * @return 是否在注释内
     */
    private fun detectCommentContext(
        documentText: String,
        caretOffset: Int,
        lineStartOffset: Int,
        lineEndOffset: Int,
        currentLine: String,
        lineOffset: Int
    ): Boolean {
        if (lineOffset < 0 || lineOffset > currentLine.length) {
            return false
        }

        // 检测行注释：// 开头的行
        val lineContent = currentLine.trimStart()
        if (lineContent.startsWith("//")) {
            return true
        }

        // 检测行内注释：代码后面跟 // 的情况
        val beforeCaret = currentLine.substring(0, lineOffset)
        val commentIndex = beforeCaret.indexOf("//")
        if (commentIndex >= 0) {
            // 检查 // 是否在字符串内
            val beforeComment = beforeCaret.substring(0, commentIndex)
            if (!isInString(beforeComment)) {
                return true
            }
        }

        // 检测块注释：需要向前扫描查找 /* 和 */
        // 从文档开始到光标位置，查找未闭合的块注释
        var inBlockComment = false
        var i = 0
        val scanEnd = kotlin.math.min(caretOffset + 1, documentText.length)
        
        while (i < scanEnd) {
            if (i < documentText.length - 1) {
                val c = documentText[i]
                val next = documentText[i + 1]
                
                // 跳过字符串内的内容
                if (c == '"' || c == '\'') {
                    val stringEnd = findStringEnd(documentText, i, c)
                    if (stringEnd > i) {
                        i = stringEnd + 1
                        continue
                    }
                }
                
                if (c == '/' && next == '*') {
                    inBlockComment = true
                    i += 2
                    continue
                } else if (c == '*' && next == '/') {
                    inBlockComment = false
                    i += 2
                    continue
                }
            }
            i++
            
            // 如果已经扫描到光标位置，可以提前退出
            if (i >= caretOffset) {
                break
            }
        }

        return inBlockComment
    }
    
    /**
     * 检查文本是否在字符串内（简化版，用于注释检测）
     */
    private fun isInString(text: String): Boolean {
        var inString = false
        var stringType: Char? = null
        var escape = false
        
        for (c in text) {
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"' || c == '\'') {
                if (inString && c == stringType) {
                    inString = false
                    stringType = null
                } else if (!inString) {
                    inString = true
                    stringType = c
                }
            }
        }
        return inString
    }
    
    /**
     * 查找字符串结束位置
     */
    private fun findStringEnd(text: String, start: Int, quoteChar: Char): Int {
        var i = start + 1
        var escape = false
        while (i < text.length) {
            val c = text[i]
            if (escape) {
                escape = false
                i++
                continue
            }
            if (c == '\\') {
                escape = true
                i++
                continue
            }
            if (c == quoteChar) {
                return i
            }
            i++
        }
        return -1
    }
}