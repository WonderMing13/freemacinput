package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.wonder.freemacinput.freemacinput.core.InputMethodType

/**
 * 插件设置状态
 */
@Service(Service.Level.APP)
@State(
    name = "FreeMacInputSettings",
    storages = [Storage("freeMacInputSettings.xml")]
)
class SettingsState : PersistentStateComponent<SettingsState> {

    // 启用控制
    var isEnabled: Boolean = true
    var isShowHints: Boolean = true
    var isEnableCaretColor: Boolean = true

    // 光标颜色配置
    var chineseCaretColor: String = "EF1616"  // 红色
    var englishCaretColor: String = "DCDCD9"  // 灰色
    var capsLockCaretColor: String = "F6E30E"  // 黄色

    // 各场景默认输入法
    var defaultMethod: InputMethodType = InputMethodType.ENGLISH
    var commentMethod: InputMethodType = InputMethodType.CHINESE
    var stringMethod: InputMethodType = InputMethodType.CHINESE

    // 默认场景配置（文件类型规则）
    var fileTypeRules: MutableList<FileTypeRule> = mutableListOf()

    // 注释场景配置
    var showCommentSceneHint: Boolean = true

    // 字符串场景配置
    var stringSceneRules: MutableList<StringSceneRule> = mutableListOf()
    var stringSceneHabits: MutableList<StringSceneHabit> = mutableListOf()
    var enableStringRescue: Boolean = true

    // 离开IDE场景配置（仅macOS）
    var leaveIDEStrategy: LeaveIDEStrategy = LeaveIDEStrategy.RESTORE_PREVIOUS
    var inputMethodBeforeEnterIDE: String? = null

    // 工具窗口场景配置
    var toolWindowRules: MutableList<ToolWindowRule> = mutableListOf()
    
    // 自定义事件场景配置
    var enableEventLogging: Boolean = false
    var customEventRules: MutableList<CustomEventRule> = mutableListOf()
    
    // 自定义规则场景配置
    var customPatternRules: MutableList<CustomPatternRule> = mutableListOf()
    
    // 输入法切换方案配置
    var switchStrategy: com.wonder.freemacinput.freemacinput.core.SwitchStrategy = 
        com.wonder.freemacinput.freemacinput.core.SwitchStrategy.IM_SELECT
    var chineseInputMethodId: String = "com.apple.inputmethod.SCIM.Pinyin"  // 简体拼音
    var englishInputMethodId: String = "com.apple.keylayout.ABC"  // ABC
    
    init {
        // 初始化默认的自定义规则
        if (customPatternRules.isEmpty()) {
            initDefaultCustomPatternRules()
        }
    }
    
    /**
     * 初始化默认的自定义规则
     */
    private fun initDefaultCustomPatternRules() {
        customPatternRules.addAll(listOf(
            // 中文文字之间切换为中文
            CustomPatternRule(
                enabled = true,
                name = "中文文字之间切换为中文",
                description = "当光标位于中文字符之间时，切换为中文输入法",
                fileTypes = mutableListOf(),
                applyToAllAreas = true,
                leftPattern = ".*[\\u4e00-\\u9fa5]$",
                rightPattern = "^[\\u4e00-\\u9fa5].*",
                matchStrategy = MatchStrategy.BOTH,
                targetInputMethod = InputMethodType.CHINESE
            ),
            // 大写字母之间切换为大写
            CustomPatternRule(
                enabled = true,
                name = "大写字母之间切换为大写",
                description = "当光标位于大写字母之间时，保持英文输入法",
                fileTypes = mutableListOf(),
                applyToAllAreas = true,
                leftPattern = ".*[A-Z]$",
                rightPattern = "^[A-Z].*",
                matchStrategy = MatchStrategy.BOTH,
                targetInputMethod = InputMethodType.ENGLISH
            ),
            // 连续大写字母切换为大写
            CustomPatternRule(
                enabled = true,
                name = "连续大写字母切换为大写",
                description = "当光标左侧是连续大写字母时，保持英文输入法",
                fileTypes = mutableListOf(),
                applyToAllAreas = true,
                leftPattern = ".*[A-Z]{2,}$",
                rightPattern = "",
                matchStrategy = MatchStrategy.EITHER,
                targetInputMethod = InputMethodType.ENGLISH
            ),
            // 小写字母切换为英文（仅在java文件的private/static等关键字后）
            CustomPatternRule(
                enabled = true,
                name = "小写字母切换为英文（Java关键字）",
                description = "在Java文件中，private/static等关键字后切换为英文",
                fileTypes = mutableListOf("java"),
                applyToCodeArea = true,
                applyToAllAreas = false,
                leftPattern = ".*(\\?:private|static).*",
                rightPattern = "",
                matchStrategy = MatchStrategy.EITHER,
                targetInputMethod = InputMethodType.ENGLISH
            ),
            // 英文字母之间切换为英文
            CustomPatternRule(
                enabled = true,
                name = "英文字母之间切换为英文",
                description = "当光标位于英文字母之间时，切换为英文输入法",
                fileTypes = mutableListOf(),
                applyToAllAreas = true,
                leftPattern = ".*[a-zA-Z]$",
                rightPattern = "^[a-zA-Z].*",
                matchStrategy = MatchStrategy.BOTH,
                targetInputMethod = InputMethodType.ENGLISH
            ),
            // 连续英文字母切换为英文（java文件）
            CustomPatternRule(
                enabled = true,
                name = "连续英文字母切换为英文",
                description = "当光标左侧是连续英文字母时，切换为英文输入法",
                fileTypes = mutableListOf("java"),
                applyToStringArea = true,
                applyToCommentArea = true,
                applyToAllAreas = false,
                leftPattern = ".*[a-zA-Z]{2,}$",
                rightPattern = "",
                matchStrategy = MatchStrategy.EITHER,
                targetInputMethod = InputMethodType.ENGLISH
            )
        ))
    }

    override fun getState(): SettingsState = this

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /**
     * 根据文件类型获取默认输入法
     */
    fun getInputMethodForFileType(fileType: String): InputMethodType {
        val rule = fileTypeRules.find { 
            it.enabled && it.fileType.equals(fileType, ignoreCase = true) 
        }
        return rule?.defaultInputMethod ?: defaultMethod
    }

    /**
     * 根据语言和表达式获取字符串场景的输入法
     * 返回 null 表示没有配置，不应该自动切换
     */
    fun getInputMethodForString(language: String, expression: String): InputMethodType? {
        // 1. 查找用户习惯（最高优先级）
        val habit = stringSceneHabits.find {
            it.language.equals(language, ignoreCase = true) && 
            matchExpression(it.expression, expression)
        }
        if (habit != null) {
            return habit.preferredInputMethod
        }

        // 2. 查找手动配置的规则
        val rule = stringSceneRules.find {
            it.language.equals(language, ignoreCase = true) && 
            matchExpression(it.expression, expression)
        }
        if (rule != null) {
            return rule.defaultInputMethod
        }

        // 3. 没有配置，返回 null（不自动切换）
        return null
    }

    /**
     * 记录用户在字符串场景的输入法习惯
     */
    fun recordStringSceneHabit(language: String, expression: String, inputMethod: InputMethodType) {
        val existing = stringSceneHabits.find {
            it.language.equals(language, ignoreCase = true) && 
            it.expression.equals(expression, ignoreCase = true)
        }

        if (existing != null) {
            existing.preferredInputMethod = inputMethod
            existing.recordTime = System.currentTimeMillis()
        } else {
            stringSceneHabits.add(StringSceneHabit(
                language = language,
                expression = expression,
                preferredInputMethod = inputMethod,
                recordTime = System.currentTimeMillis()
            ))
        }
    }

    /**
     * 匹配表达式
     */
    private fun matchExpression(pattern: String, expression: String): Boolean {
        if (pattern == "*") return true
        if (pattern == expression) return true
        
        val regex = pattern.replace("*", ".*").toRegex(RegexOption.IGNORE_CASE)
        return regex.matches(expression)
    }
}
