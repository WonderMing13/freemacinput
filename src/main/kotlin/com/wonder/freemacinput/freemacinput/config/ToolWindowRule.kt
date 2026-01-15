package com.wonder.freemacinput.freemacinput.config

import com.wonder.freemacinput.freemacinput.core.InputMethodType

/**
 * 工具窗口规则
 */
data class ToolWindowRule(
    var toolWindowId: String = "",
    var displayName: String = "",
    var enabled: Boolean = false,
    var targetInputMethod: InputMethodType = InputMethodType.ENGLISH
)
