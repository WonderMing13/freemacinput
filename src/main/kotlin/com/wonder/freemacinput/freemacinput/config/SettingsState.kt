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
    var gitCommitMethod: InputMethodType = InputMethodType.CHINESE
    var customEventMethod: InputMethodType = InputMethodType.CHINESE
    var customRuleMethod: InputMethodType = InputMethodType.CHINESE

    // ============ 工具窗口输入法配置 ============
    var projectWindowMethod: InputMethodType = InputMethodType.ENGLISH
    var terminalMethod: InputMethodType = InputMethodType.ENGLISH
    var debugMethod: InputMethodType = InputMethodType.ENGLISH
    var versionControlMethod: InputMethodType = InputMethodType.ENGLISH
    var findMethod: InputMethodType = InputMethodType.ENGLISH

    // ============ 自定义规则 ============
    var customRules: List<CustomRuleData> = emptyList()

    // ============ 字符串习惯记忆 ============
    // 使用MutableMap存储详细的习惯数据
    // 格式：stringName -> StringHabitData(JSON)
    var stringHabitsMap: MutableMap<String, StringHabitData> = mutableMapOf()

    // ============ 输入源配置 ============
    var englishInputSource: String = "com.apple.keylayout.ABC"
    var chineseInputSource: String = "com.apple.inputmethod.SCIM.ITABC"

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
        this.gitCommitMethod = state.gitCommitMethod
        this.customEventMethod = state.customEventMethod
        this.customRuleMethod = state.customRuleMethod
        this.projectWindowMethod = state.projectWindowMethod
        this.terminalMethod = state.terminalMethod
        this.debugMethod = state.debugMethod
        this.versionControlMethod = state.versionControlMethod
        this.findMethod = state.findMethod
        this.customRules = state.customRules
        this.stringHabitsMap = state.stringHabitsMap
        this.englishInputSource = state.englishInputSource
        this.chineseInputSource = state.chineseInputSource
        this.preferImSelect = state.preferImSelect
        this.fallbackHotkey = state.fallbackHotkey
    }
}

/**
 * 自定义规则数据
 */
data class CustomRuleData(
    var name: String = "",
    var pattern: String = "",
    var method: InputMethodType = InputMethodType.CHINESE
)

/**
 * 字符串习惯数据
 * 用于持久化存储
 *
 * @param name 字符串名称/标识
 * @param englishCount 英文使用次数
 * @param chineseCount 中文使用次数
 * @param lastUsedMethod 最后使用的输入法
 * @param lastUpdated 最后更新时间
 */
data class StringHabitData(
    var name: String = "",
    var englishCount: Int = 0,
    var chineseCount: Int = 0,
    var lastUsedMethod: InputMethodType = InputMethodType.ENGLISH,
    var lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * 总使用次数
     */
    val totalCount: Int
        get() = englishCount + chineseCount

    /**
     * 获取偏好输入法
     * 基于使用频率判断用户偏好
     */
    val preferredMethod: InputMethodType
        get() = when {
            englishCount > chineseCount -> InputMethodType.ENGLISH
            chineseCount > englishCount -> InputMethodType.CHINESE
            else -> lastUsedMethod
        }
}
