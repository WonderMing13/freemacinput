package com.wonder.freemacinput.freemacinput.core

/**
 * 输入法类型枚举
 */
enum class InputMethodType {
    ENGLISH,     // 英文输入法
    CHINESE,     // 中文输入法
    AUTO         // 自动检测
}

/**
 * 大小写状态
 */
enum class CapsLockState {
    OFF,         // 小写
    ON,          // 大写
    UNKNOWN      // 未知
}

/**
 * 上下文场景类型
 */
enum class ContextType {
    DEFAULT,           // 默认代码区域
    COMMENT,           // 注释区域
    STRING,            // 字符串字面量
    GIT_COMMIT,        // Git提交信息
    TOOL_WINDOW,       // 工具窗口
    CUSTOM_EVENT,      // 自定义事件
    CUSTOM_RULE,       // 自定义规则
    UNKNOWN            // 未知
}

/**
 * 工具窗口类型
 */
enum class ToolWindowType {
    PROJECT,
    TERMINAL,
    DEBUG,
    VERSION_CONTROL,
    FIND,
    OTHER
}

/**
 * 上下文信息
 */
data class ContextInfo(
    val type: ContextType,
    val toolWindowType: ToolWindowType?,
    val reason: String,
    val stringName: String? = null,
    val customRuleName: String? = null
)

/**
 * 自定义规则
 */
data class CustomRule(
    val name: String,
    val pattern: String,
    val method: InputMethodType
) {
    private val compiledPattern = java.util.regex.Pattern.compile(pattern)

    fun matches(text: String): Boolean {
        return compiledPattern.matcher(text).find()
    }
}

/**
 * 字符串信息
 */
data class StringInfo(
    val name: String,
    val offset: Int,
    val length: Int
)
