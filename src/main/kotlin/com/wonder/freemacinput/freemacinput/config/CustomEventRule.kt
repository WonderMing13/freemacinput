package com.wonder.freemacinput.freemacinput.config

import com.wonder.freemacinput.freemacinput.core.InputMethodType

/**
 * 自定义事件规则
 * 用于监听特定IDE事件并切换输入法
 */
data class CustomEventRule(
    var eventName: String = "",
    var targetInputMethod: InputMethodType = InputMethodType.CHINESE,
    var description: String = "",
    var enabled: Boolean = true
) {
    override fun toString(): String {
        return "$eventName -> ${if (targetInputMethod == InputMethodType.CHINESE) "中文" else "英文"}"
    }
}
