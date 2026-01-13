package com.wonder.freemacinput.freemacinput.config

import com.wonder.freemacinput.freemacinput.core.InputMethodType

/**
 * 文件类型规则
 * 用于配置特定文件类型的默认输入法
 */
data class FileTypeRule(
    var enabled: Boolean = true,
    var fileType: String = "",
    var defaultInputMethod: InputMethodType = InputMethodType.ENGLISH
) {
    companion object {
        fun createDefault(): FileTypeRule {
            return FileTypeRule(
                enabled = true,
                fileType = "java",
                defaultInputMethod = InputMethodType.ENGLISH
            )
        }
    }
}
