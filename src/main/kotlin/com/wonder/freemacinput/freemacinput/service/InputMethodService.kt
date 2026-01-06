package com.wonder.freemacinput.freemacinput.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.wonder.freemacinput.freemacinput.config.SettingsState
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.config.CustomRuleData
import com.wonder.freemacinput.freemacinput.config.StringHabitData

/**
 * 输入法服务
 *
 * 提供全局的输入法切换控制和设置访问。
 * 本服务作为插件的核心接口，封装了所有与输入法相关的操作。
 *
 * 主要功能：
 * 1. 插件开关控制
 * 2. 各场景的输入法配置管理
 * 3. 字符串习惯记忆管理
 * 4. 自定义规则管理
 */
@Service(Service.Level.PROJECT)
class InputMethodService(private val project: Project) {

    // 应用级设置状态（通过服务获取）
    private val settingsState: SettingsState by lazy {
        ApplicationManager.getApplication().getService(SettingsState::class.java).state
    }

    // 字符串习惯服务（延迟初始化）
    private val stringHabitService: StringHabitService by lazy {
        StringHabitService(settingsState)
    }

    // ==================== 插件开关 ====================

    /**
     * 是否启用插件
     */
    fun isEnabled(): Boolean = settingsState.isEnabled

    /**
     * 是否显示切换提示
     */
    fun isShowHints(): Boolean = settingsState.isShowHints

    /**
     * 是否启用光标颜色
     */
    fun isEnableCaretColor(): Boolean = settingsState.isEnableCaretColor

    /**
     * 获取当前设置状态
     */
    fun getSettings(): SettingsState = settingsState

    // ==================== 字符串习惯管理 ====================

    /**
     * 获取字符串输入法的用户习惯
     *
     * @param stringName 字符串名称
     * @return 偏好的输入法类型，如果没有习惯记录返回null
     */
    fun getStringHabit(stringName: String): InputMethodType? {
        return stringHabitService.getPreferredMethod(stringName)
    }

    /**
     * 记录字符串的输入法习惯
     *
     * 当用户在一个字符串上手动切换输入法时调用
     *
     * @param stringName 字符串名称
     * @param method 使用的输入法类型
     */
    fun recordStringHabit(stringName: String, method: InputMethodType) {
        stringHabitService.recordHabit(stringName, method)
    }

    /**
     * 获取所有字符串习惯记录
     *
     * @return 习惯数据列表
     */
    fun getAllStringHabits(): List<StringHabitData> {
        return stringHabitService.getAllHabits()
    }

    /**
     * 删除某个字符串的习惯记录
     *
     * @param stringName 字符串名称
     * @return true表示删除成功
     */
    fun removeStringHabit(stringName: String): Boolean {
        return stringHabitService.removeHabit(stringName)
    }

    /**
     * 清除所有字符串习惯
     */
    fun clearAllStringHabits() {
        stringHabitService.clearAllHabits()
    }

    /**
     * 获取字符串习惯数量
     *
     * @return 习惯记录数量
     */
    fun getStringHabitCount(): Int {
        return stringHabitService.getHabitCount()
    }

    /**
     * 获取最常使用的字符串习惯
     *
     * @param limit 返回数量限制
     * @return 按使用频率排序的习惯列表
     */
    fun getMostUsedStringHabits(limit: Int = 10): List<StringHabitData> {
        return stringHabitService.getMostUsedStrings(limit)
    }

    /**
     * 获取字符串习惯统计信息
     *
     * @return 统计信息字符串
     */
    fun getStringHabitStatistics(): String {
        return stringHabitService.getStatisticsSummary()
    }

    // ==================== 自定义规则管理 ====================

    /**
     * 添加自定义规则
     *
     * @param name 规则名称
     * @param pattern 正则表达式模式
     * @param method 匹配时切换的输入法
     */
    fun addCustomRule(name: String, pattern: String, method: InputMethodType) {
        val rule = CustomRuleData(
            name = name,
            pattern = pattern,
            method = method
        )
        settingsState.customRules = settingsState.customRules + rule
    }

    /**
     * 移除自定义规则
     *
     * @param name 规则名称
     */
    fun removeCustomRule(name: String) {
        settingsState.customRules = settingsState.customRules.filter { it.name != name }
    }

    /**
     * 获取所有自定义规则
     */
    fun getCustomRules(): List<CustomRuleData> {
        return settingsState.customRules
    }

    // ==================== 手动切换 ====================

    /**
     * 临时切换输入法（用户手动触发）
     */
    fun toggleMethod() {
        val current = getCurrentMethod()
        val newMethod = when (current) {
            InputMethodType.ENGLISH -> InputMethodType.CHINESE
            else -> InputMethodType.ENGLISH
        }
        settingsState.defaultMethod = newMethod
    }

    /**
     * 获取当前输入法状态
     */
    private fun getCurrentMethod(): InputMethodType {
        return settingsState.defaultMethod
    }

    // ==================== 输入源管理 ====================

    /**
     * 获取英文输入源Bundle ID
     */
    fun getEnglishInputSource(): String = settingsState.englishInputSource

    /**
     * 设置英文输入源Bundle ID
     */
    fun setEnglishInputSource(inputSource: String) {
        settingsState.englishInputSource = inputSource
    }

    /**
     * 获取中文输入源Bundle ID
     */
    fun getChineseInputSource(): String = settingsState.chineseInputSource

    /**
     * 设置中文输入源Bundle ID
     */
    fun setChineseInputSource(inputSource: String) {
        settingsState.chineseInputSource = inputSource
    }

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目实例
         * @return InputMethodService实例
         */
        fun getInstance(project: Project): InputMethodService {
            return project.getService(InputMethodService::class.java)
        }
    }
}
