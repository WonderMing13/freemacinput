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
 * 上下文场景类型
 */
enum class ContextType {
    DEFAULT,           // 默认代码区域
    COMMENT,           // 注释区域
    STRING,            // 字符串字面量
    UNKNOWN            // 未知
}

/**
   * 上下文信息
 */
data class ContextInfo(
    val type: ContextType,
    val reason: String
)
