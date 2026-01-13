package com.wonder.freemacinput.freemacinput.config

import com.wonder.freemacinput.freemacinput.core.InputMethodType

/**
 * 字符串场景规则
 * 用于配置字符串字面量的输入法切换规则
 */
data class StringSceneRule(
    var language: String = "",
    var expression: String = "",
    var defaultInputMethod: InputMethodType = InputMethodType.CHINESE
) {
    companion object {
        fun createDefault(): StringSceneRule {
            return StringSceneRule(
                language = "java",
                expression = "variableName=*",
                defaultInputMethod = InputMethodType.CHINESE
            )
        }
    }
}

/**
 * 字符串场景用户习惯记录
 * 自动记录用户在特定变量/参数中的输入法切换习惯
 */
data class StringSceneHabit(
    var language: String = "",
    var expression: String = "",
    var preferredInputMethod: InputMethodType = InputMethodType.CHINESE,
    var recordTime: Long = System.currentTimeMillis()
)
