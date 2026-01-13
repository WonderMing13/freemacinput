package com.wonder.freemacinput.freemacinput.core

data class ContextInfo(
    val type: ContextType,
    val reason: String,
    val variableName: String? = null,  // 字符串场景的变量名或函数名
    val language: String? = null        // 编程语言
)
