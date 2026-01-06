package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger

/**
 * 上下文检测服务
 *
 * 负责检测当前编辑位置属于什么场景，支持以下场景：
 * - DEFAULT: 默认代码区域
 * - COMMENT: 注释区域
 * - STRING: 字符串字面量
 * - GIT_COMMIT: Git提交信息
 * - TOOL_WINDOW: 工具窗口（暂未实现）
 * - CUSTOM_EVENT: 自定义事件
 * - CUSTOM_RULE: 自定义规则
 */
class ContextDetector {
    private val logger = Logger.getInstance(ContextDetector::class.java)

    // 自定义规则列表，用于用户自定义匹配规则
    private val customRules = mutableListOf<CustomRule>()

    /**
     * 检测当前编辑上下文场景
     *
     * @param documentText 文档文本
     * @param isGitCommit 是否为Git提交窗口
     * @param caretOffset 光标位置
     * @return ContextInfo 包含检测到的场景类型和相关信息的对象
     */
    fun detectContext(documentText: String, isGitCommit: Boolean, caretOffset: Int): ContextInfo {
        return try {
            logger.info("detectContext: textLength=${documentText.length}, isGitCommit=$isGitCommit, offset=$caretOffset")

            // 1. 检测Git提交窗口
            if (isGitCommit) {
                logger.info("检测到Git提交窗口")
                ContextInfo(
                    type = ContextType.GIT_COMMIT,
                    toolWindowType = null,
                    reason = "Git提交信息输入"
                )
            } else {
                // 2. 检测光标所在的当前行
                val currentLine = getCurrentLine(documentText, caretOffset)
                logger.info("当前行: '$currentLine'")

                // 检测字符串
                val (inString, stringName, isChineseString) = detectStringContext(currentLine)
                if (inString) {
                    if (isChineseString) {
                        // 中文字符串 → 切换中文
                        logger.info("检测到中文字符串: $stringName")
                        ContextInfo(
                            type = ContextType.STRING,
                            toolWindowType = null,
                            stringName = stringName,
                            reason = "中文字符串: $stringName"
                        )
                    } else {
                        // 英文字符串 → 保持英文
                        logger.info("检测到英文字符串: $stringName")
                        ContextInfo(
                            type = ContextType.DEFAULT,
                            toolWindowType = null,
                            stringName = stringName,
                            reason = "英文字符串: $stringName"
                        )
                    }
                } else if (isInCommentLine(currentLine)) {
                    // 3. 检测注释区域（当前行是注释）
                    logger.info("检测到注释区域")
                    ContextInfo(
                        type = ContextType.COMMENT,
                        toolWindowType = null,
                        reason = "注释区域"
                    )
                } else {
                    // 4. 检测块注释
                    if (isInsideBlockComment(documentText, caretOffset)) {
                        logger.info("检测到块注释区域")
                        ContextInfo(
                            type = ContextType.COMMENT,
                            toolWindowType = null,
                            reason = "块注释区域"
                        )
                    } else {
                        // 5. 检测自定义规则
                        val customMatch = checkCustomRules(currentLine)
                        if (customMatch != null) {
                            logger.info("检测到自定义规则: $customMatch")
                            ContextInfo(
                                type = ContextType.CUSTOM_RULE,
                                toolWindowType = null,
                                customRuleName = customMatch,
                                reason = "自定义规则匹配"
                            )
                        } else {
                            // 默认返回代码区域
                            logger.info("默认代码区域")
                            ContextInfo(
                                type = ContextType.DEFAULT,
                                toolWindowType = null,
                                reason = "代码区域"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("detectContext 异常: ${e.javaClass.simpleName}: ${e.message}", e)
            ContextInfo(
                type = ContextType.UNKNOWN,
                toolWindowType = null,
                reason = "检测异常: ${e.message}"
            )
        }
    }

    /**
     * 获取光标所在的当前行
     */
    private fun getCurrentLine(text: String, offset: Int): String {
        val safeOffset = minOf(offset, text.length)

        val lastNewline = text.lastIndexOf('\n', safeOffset - 1)
        val lineStart = if (lastNewline >= 0) lastNewline + 1 else 0

        val nextNewline = text.indexOf('\n', safeOffset)
        val lineEnd = if (nextNewline >= 0) nextNewline else text.length

        return text.substring(lineStart, lineEnd)
    }

    /**
     * 检测当前行是否在字符串中
     *
     * @return Triple<是否在字符串中, 变量名, 是否为中文字符串>
     */
    private fun detectStringContext(line: String): Triple<Boolean, String?, Boolean> {
        // 查找当前行中的所有双引号对（非转义）
        val quotes = mutableListOf<Int>()
        for (i in line.indices) {
            val char = line[i]
            if (char == '"' && (i == 0 || line[i - 1] != '\\')) {
                quotes.add(i)
            }
        }

        if (quotes.size < 2) {
            // 没有完整的字符串对，检查是否有未闭合的字符串
            if (quotes.size == 1) {
                // 光标在未闭合字符串中
                val beforeQuote = line.substring(0, quotes[0]).trim()
                val varName = extractVariableName(beforeQuote)
                // 检查字符串内容是否为中文
                val stringContent = extractStringContent(line, quotes[0], line.length)
                val isChinese = isChineseContent(stringContent)
                return Triple(true, varName, isChinese)
            }
            return Triple(false, null, false)
        }

        // 查找光标位置应该在哪一对引号之间
        // 假设光标在行末（取最后一个字符的位置）
        val cursorPos = line.length - 1

        // 遍历引号对，检查光标在哪一对之间
        for (i in 0 until quotes.size - 1 step 2) {
            val openQuote = quotes[i]
            val closeQuote = quotes[i + 1]

            if (cursorPos >= openQuote && cursorPos <= closeQuote) {
                // 光标在这对引号之间
                val beforeQuote = line.substring(0, openQuote).trim()
                val varName = extractVariableName(beforeQuote)
                // 检查字符串内容是否为中文
                val stringContent = extractStringContent(line, openQuote, closeQuote)
                val isChinese = isChineseContent(stringContent)
                return Triple(true, varName, isChinese)
            }
        }

        return Triple(false, null, false)
    }

    /**
     * 提取字符串内容（不包括引号）
     */
    private fun extractStringContent(line: String, startQuote: Int, endQuote: Int): String {
        if (startQuote + 1 >= endQuote) return ""
        return line.substring(startQuote + 1, minOf(endQuote, line.length))
    }

    /**
     * 判断字符串内容是否为中文
     *
     * 判断逻辑：
     * 1. 包含中文字符（Unicode范围）
     * 2. 包含中文标点符号
     * 3. 如果主要是中文，判定为中文
     */
    private fun isChineseContent(content: String): Boolean {
        if (content.isEmpty()) return false

        var chineseCount = 0
        var totalCount = 0

        // 中文标点集合（使用Unicode转义避免语法问题）
        val chinesePunctuation = setOf(
            '\uFF0C', '\u3002', '\uFF01', '\uFF1F', '\uFF1B', '\uFF1A',  // ，。！？；：
            '\u201C', '\u201D', '\u2018', '\u2019', '\uFF08', '\uFF09',  // ""''
            '\u3010', '\u3011', '\u300A', '\u300B', '\u300E', '\u300F',  // 【】《》
            '\u3014', '\u3015', '\u300C', '\u300D'                       // 『』「」
        )

        for (char in content) {
            when {
                // 中文字符范围
                char in '\u4E00'..'\u9FFF' -> chineseCount++
                // 中文标点
                char in chinesePunctuation -> chineseCount++
                // 全角符号
                char in '\uFF00'..'\uFFEF' -> chineseCount++
                // 其他CJK符号
                char in '\u3000'..'\u303F' -> chineseCount++
            }
            // 只统计有效字符（排除空格和转义字符）
            if (!char.isWhitespace() && char != '\\') {
                totalCount++
            }
        }

        // 如果中文字符占比超过30%，认为是中文内容
        return if (totalCount > 0) {
            chineseCount.toFloat() / totalCount > 0.3f
        } else {
            false
        }
    }

    /**
     * 从文本中提取变量名
     */
    private fun extractVariableName(text: String): String? {
        val words = text.split(Regex("\\s+"))
        for (i in words.indices.reversed()) {
            val word = words[i]
            if (word.isNotEmpty() && word.all { it.isLetterOrDigit() || it == '_' }) {
                if (word.length >= 2) return word
            }
        }
        return null
    }

    /**
     * 检测当前行是否是注释行（包含 // 注释）
     */
    private fun isInCommentLine(line: String): Boolean {
        val trimmed = line.trim()

        // 整行注释
        if (trimmed.startsWith("//")) {
            return true
        }

        // 行内注释检测 - 检查 // 是否在字符串外面
        var inString = false
        var inChar = false
        var i = 0
        while (i < line.length) {
            val char = line[i]

            // 处理字符串
            if (char == '"' && (i == 0 || line[i - 1] != '\\')) {
                inString = !inString
            }
            // 处理字符常量
            else if (char == '\'' && (i == 0 || line[i - 1] != '\\')) {
                inChar = !inChar
            }
            // 检测 //
            else if (!inString && !inChar && i + 1 < line.length && line[i] == '/' && line[i + 1] == '/') {
                return true
            }

            i++
        }

        return false
    }

    /**
     * 检测光标是否在块注释内部
     * 修复版本：同时检查光标之前和当前行的注释标记
     */
    private fun isInsideBlockComment(text: String, offset: Int): Boolean {
        val safeOffset = minOf(offset, text.length)

        // 方法1: 忽略字符串/字符常量中的注释标记，计算到光标为止的注释深度
        var depth = 0
        var i = 0
        var inString = false
        var inChar = false

        while (i < safeOffset) {
            val ch = text[i]
            if (ch == '"' && (i == 0 || text[i - 1] != '\\')) {
                inString = !inString
                i++
                continue
            }
            if (ch == '\'' && (i == 0 || text[i - 1] != '\\')) {
                inChar = !inChar
                i++
                continue
            }
            if (!inString && !inChar && i + 1 < safeOffset) {
                val c1 = text[i]
                val c2 = text[i + 1]
                if (c1 == '/' && c2 == '*') {
                    depth++
                    i += 2
                    continue
                }
                if (c1 == '*' && c2 == '/') {
                    if (depth > 0) depth--
                    i += 2
                    continue
                }
            }
            i++
        }

        if (depth > 0) return true

        // 方法2: 当前行快速检查（忽略字符串与字符常量）
        val currentLine = getCurrentLine(text, safeOffset)
        return lineHasUnbalancedBlockComment(currentLine)
    }

    private fun lineHasUnbalancedBlockComment(line: String): Boolean {
        var inString = false
        var inChar = false
        var opens = 0
        var closes = 0
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"' && (i == 0 || line[i - 1] != '\\')) {
                inString = !inString
                i++
                continue
            }
            if (ch == '\'' && (i == 0 || line[i - 1] != '\\')) {
                inChar = !inChar
                i++
                continue
            }
            if (!inString && !inChar && i + 1 < line.length) {
                val c1 = line[i]
                val c2 = line[i + 1]
                if (c1 == '/' && c2 == '*') {
                    opens++
                    i += 2
                    continue
                }
                if (c1 == '*' && c2 == '/') {
                    closes++
                    i += 2
                    continue
                }
            }
            i++
        }
        return opens > closes
    }

    private fun countOccurrences(text: String, sub: String): Int {
        if (sub.isEmpty()) return 0
        var count = 0
        var index = 0
        while (true) {
            index = text.indexOf(sub, index)
            if (index < 0) break
            count++
            index += sub.length
        }
        return count
    }

    /**
     * 检测自定义规则
     */
    private fun checkCustomRules(line: String): String? {
        for (rule in customRules) {
            if (rule.matches(line)) {
                return rule.name
            }
        }
        return null
    }

    /**
     * 添加自定义规则
     */
    fun addCustomRule(name: String, pattern: String, method: InputMethodType) {
        customRules.add(CustomRule(name, pattern, method))
    }

    /**
     * 清空自定义规则
     */
    fun clearCustomRules() {
        customRules.clear()
    }
}
