package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.wonder.freemacinput.freemacinput.core.InputMethodType

/**
 * 插件设置状态
 * 用于持久化保存用户的配置
 *
 * 使用 Application Service 模式，全局单例
 */
@Service(Service.Level.APP)
@State(
    name = "FreeMacInputSettings",
    storages = [Storage("freeMacInputSettings.xml")]
)
class SettingsState : PersistentStateComponent<SettingsState> {

    // ============ 启用控制 ============
    var isEnabled: Boolean = true
    var isShowHints: Boolean = true
    var isEnableCaretColor: Boolean = true

    // ============ 各场景默认输入法 ============
    var defaultMethod: InputMethodType = InputMethodType.ENGLISH
    var commentMethod: InputMethodType = InputMethodType.CHINESE





    // ============ 输入源配置 ============
    var englishInputSource: String = "com.apple.keylayout.ABC"
    var chineseInputSource: String = "com.apple.inputmethod.SCIM"

    // ============ 切换偏好与回退设置 ============
    // 是否优先使用 im-select 进行精确切换
    var preferImSelect: Boolean = true
    // 回退快捷键配置：可选值 "CTRL_SPACE", "ALT_SPACE", "CTRL_SHIFT_SPACE"
    var fallbackHotkey: String = "CTRL_SPACE"

    /**
     * 保存当前状态
     */
    override fun getState(): SettingsState = this

    /**
     * 加载保存的状态
     */
    override fun loadState(state: SettingsState) {
        // 逐个复制属性以确保类型安全
        this.isEnabled = state.isEnabled
        this.isShowHints = state.isShowHints
        this.isEnableCaretColor = state.isEnableCaretColor
        this.defaultMethod = state.defaultMethod
        this.commentMethod = state.commentMethod


        this.englishInputSource = state.englishInputSource
        this.chineseInputSource = state.chineseInputSource
        this.preferImSelect = state.preferImSelect
        this.fallbackHotkey = state.fallbackHotkey
    }
}


