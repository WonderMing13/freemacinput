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
            val inComment = detectCommentContext(currentLine, lineOffset)
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
                type = ContextType.DEFAULT,
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
     * 检测注释上下文
     *
     * @param currentLine 当前行文本
     * @param lineOffset 光标在行内的偏移
     * @return 是否在注释内
     */
    private fun detectCommentContext(
        currentLine: String,
        lineOffset: Int
    ): Boolean {
        if (lineOffset < 0 || lineOffset > currentLine.length) {
            return false
        }

        // 简单的注释检测
        val lineContent = currentLine.trimStart()
        if (lineContent.startsWith("//")) {
            return true
        }

        // 检测是否在块注释内
        var inBlockComment = false
        var i = 0

        while (i < currentLine.length) {
            val c = currentLine[i]
            if (i < currentLine.length - 1) {
                val next = currentLine[i + 1]
                if (c == '/' && next == '*') {
                    inBlockComment = true
                    i++
                } else if (c == '*' && next == '/') {
                    inBlockComment = false
                    i++
                }
            }

            if (i == lineOffset) {
                break
            }
            i++
        }

        return inBlockComment
    }
}