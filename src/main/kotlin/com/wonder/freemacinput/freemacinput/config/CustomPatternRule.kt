package com.wonder.freemacinput.freemacinput.config

import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.core.ContextType

/**
 * 自定义规则
 * 通过正则表达式匹配光标左右两侧的文本来判断是否切换输入法
 */
data class CustomPatternRule(
    var enabled: Boolean = true,
    var name: String = "",
    var description: String = "",
    
    // 文件类型限制（空表示所有文件类型）
    var fileTypes: MutableList<String> = mutableListOf(),
    
    // 编辑区域限制
    var applyToAllAreas: Boolean = true,
    var applyToStringArea: Boolean = false,
    var applyToCommentArea: Boolean = false,
    var applyToCodeArea: Boolean = false,
    
    // 左侧匹配规则（匹配光标左侧的文本）
    var leftPattern: String = "",
    
    // 右侧匹配规则（匹配光标右侧的文本）
    var rightPattern: String = "",
    
    // 满足条件策略
    var matchStrategy: MatchStrategy = MatchStrategy.BOTH,
    
    // 目标输入法
    var targetInputMethod: InputMethodType = InputMethodType.CHINESE
) {
    /**
     * 检查是否匹配
     */
    fun matches(leftText: String, rightText: String, fileType: String, contextType: ContextType): Boolean {
        if (!enabled) {
            return false
        }
        
        // 检查文件类型
        if (fileTypes.isNotEmpty() && !fileTypes.any { it.equals(fileType, ignoreCase = true) }) {
            return false
        }
        
        // 检查编辑区域
        if (!applyToAllAreas) {
            val areaMatches = when (contextType) {
                ContextType.STRING -> applyToStringArea
                ContextType.COMMENT -> applyToCommentArea
                ContextType.CODE -> applyToCodeArea
                else -> false
            }
            if (!areaMatches) return false
        }
        
        // 检查左侧匹配：使用完整左侧文本与用户正则匹配（支持 ^/$/.* 等写法）
        val leftMatches = if (leftPattern.isBlank()) {
            true
        } else {
            try {
                if (leftText.isEmpty()) {
                    false
                } else {
                    // 去掉文本末尾的换行符，以便 $ 能正确匹配
                    val cleanText = leftText.trimEnd('\n', '\r')
                    val regex = leftPattern.toRegex()
                    // 对于包含 $ 的正则，使用 matches()；否则使用 find()
                    val matches = if (leftPattern.contains("$")) {
                        regex.matches(cleanText)
                    } else {
                        regex.find(cleanText) != null
                    }
                    // 调试信息：显示左侧文本的最后20个字符和实际长度
                    val preview = if (cleanText.length > 20) cleanText.takeLast(20) else cleanText
                    val lastChar = if (cleanText.isNotEmpty()) cleanText.last() else ' '
                    val lastCharCode = if (cleanText.isNotEmpty()) cleanText.last().code else 0
                    println("[$name] 左侧: 文本='$preview' (长度=${cleanText.length}, 最后字符='$lastChar'(${lastCharCode})) 匹配 '$leftPattern' = $matches")
                    matches
                }
            } catch (e: Exception) {
                println("[$name] 左侧正则错误: ${e.message}")
                false
            }
        }
        
        // 检查右侧匹配：使用完整右侧文本与用户正则匹配
        val rightMatches = if (rightPattern.isBlank()) {
            // 右侧模式为空时，不参与匹配判断
            null
        } else {
            try {
                if (rightText.isEmpty()) {
                    // 右侧文本为空但模式不为空，返回 false
                    false
                } else {
                    // 去掉文本开头的换行符，以便 ^ 能正确匹配
                    val cleanText = rightText.trimStart('\n', '\r')
                    val regex = rightPattern.toRegex()
                    // 对于包含 ^ 的正则，使用 matches()；否则使用 find()
                    val matches = if (rightPattern.contains("^")) {
                        regex.matches(cleanText)
                    } else {
                        regex.find(cleanText) != null
                    }
                    // 调试信息：显示右侧文本的前20个字符
                    val preview = if (cleanText.length > 20) cleanText.take(20) else cleanText
                    println("[$name] 右侧: 文本='$preview' 匹配 '$rightPattern' = $matches")
                    matches
                }
            } catch (e: Exception) {
                println("[$name] 右侧正则错误: ${e.message}")
                false
            }
        }
        
        // 计算最终结果
        val result = when {
            // 如果左右模式都为空，不匹配
            leftPattern.isBlank() && rightPattern.isBlank() -> false
            // 如果只有左侧模式，只检查左侧
            leftPattern.isNotBlank() && rightPattern.isBlank() -> leftMatches
            // 如果只有右侧模式，只检查右侧
            leftPattern.isBlank() && rightPattern.isNotBlank() -> rightMatches ?: false
            // 如果两个模式都有，根据策略判断
            else -> when (matchStrategy) {
                MatchStrategy.BOTH -> leftMatches && (rightMatches ?: false)
                MatchStrategy.EITHER -> leftMatches || (rightMatches ?: false)
            }
        }
        
        if (result) {
            println("[$name] ✅ 匹配成功!")
        }
        
        return result
    }
    
    override fun toString(): String {
        val methodName = when (targetInputMethod) {
            InputMethodType.CHINESE -> "中文"
            InputMethodType.ENGLISH -> "英文"
            InputMethodType.CAPS_LOCK -> "大写锁定"
            else -> "未知"
        }
        return "$name -> $methodName"
    }
}

/**
 * 匹配策略
 */
enum class MatchStrategy(val displayName: String) {
    BOTH("同时满足"),
    EITHER("满足任意一个");
    
    companion object {
        fun fromDisplayName(name: String): MatchStrategy {
            return values().find { it.displayName == name } ?: BOTH
        }
    }
}
