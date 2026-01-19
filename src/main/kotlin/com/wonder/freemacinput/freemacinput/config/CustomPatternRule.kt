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
        
        // 检查左侧匹配：只检查最后一个字符
        val leftMatches = if (leftPattern.isBlank()) {
            true
        } else {
            try {
                if (leftText.isEmpty()) {
                    false
                } else {
                    val lastChar = leftText.last().toString()
                    val matches = lastChar.matches(leftPattern.toRegex())
                    println("[$name] 左侧: 最后字符'${leftText.last()}' 匹配 '$leftPattern' = $matches")
                    matches
                }
            } catch (e: Exception) {
                println("[$name] 左侧正则错误: ${e.message}")
                false
            }
        }
        
        // 检查右侧匹配：只检查第一个字符
        val rightMatches = if (rightPattern.isBlank()) {
            true
        } else {
            try {
                if (rightText.isEmpty()) {
                    false
                } else {
                    val firstChar = rightText.first().toString()
                    val matches = firstChar.matches(rightPattern.toRegex())
                    println("[$name] 右侧: 第一字符'${rightText.first()}' 匹配 '$rightPattern' = $matches")
                    matches
                }
            } catch (e: Exception) {
                println("[$name] 右侧正则错误: ${e.message}")
                false
            }
        }
        
        val result = when (matchStrategy) {
            MatchStrategy.BOTH -> leftMatches && rightMatches
            MatchStrategy.EITHER -> leftMatches || rightMatches
        }
        
        if (result) {
            println("[$name] ✅ 匹配成功!")
        }
        
        return result
    }
    
    override fun toString(): String {
        return "$name -> ${if (targetInputMethod == InputMethodType.CHINESE) "中文" else "英文"}"
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
