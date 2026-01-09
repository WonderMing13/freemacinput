package com.wonder.freemacinput.freemacinput.service

import com.wonder.freemacinput.freemacinput.config.SettingsState
import com.wonder.freemacinput.freemacinput.config.StringHabitData
import com.wonder.freemacinput.freemacinput.core.InputMethodType

/**
 * 字符串输入法习惯记忆服务
 *
 * 本服务负责记录和回忆用户在不同字符串字面量上的输入法使用习惯。
 *
 * 功能：
 * 1. 记录 - 记录用户在特定字符串上使用的输入法
 * 2. 回忆 - 根据历史习惯自动切换到用户常用的输入法
 * 3. 统计 - 统计每个字符串的输入法使用频率
 * 4. 管理 - 提供习惯的增删改查接口
 *
 * 使用方式：
 * - 当用户在一个字符串中切换输入法时，记录该字符串名称和输入法
 * - 当用户再次编辑同一字符串时，查询历史记录并自动切换
 *
 * @param settingsState 持久化存储的状态
 */
class StringHabitService(private val settingsState: SettingsState) {

    // 习惯记录存储
    private val habitMap: MutableMap<String, StringHabitData>
        get() = settingsState.stringHabitsMap

    /**
     * 记录用户在某个字符串上使用的输入法
     *
     * @param stringName 字符串名称/标识
     * @param method 使用的输入法类型
     */
    fun recordHabit(stringName: String, method: InputMethodType) {
        if (stringName.isBlank()) return

        val existingHabit = habitMap[stringName]

        if (existingHabit == null) {
            // 新记录
            habitMap[stringName] = StringHabitData(
                name = stringName,
                englishCount = if (method == InputMethodType.ENGLISH) 1 else 0,
                chineseCount = if (method == InputMethodType.CHINESE) 1 else 0,
                lastUsedMethod = method,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            // 更新现有记录
            habitMap[stringName] = existingHabit.copy(
                englishCount = existingHabit.englishCount + if (method == InputMethodType.ENGLISH) 1 else 0,
                chineseCount = existingHabit.chineseCount + if (method == InputMethodType.CHINESE) 1 else 0,
                lastUsedMethod = method,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * 查询用户对某个字符串的偏好输入法
     *
     * 根据历史使用频率判断用户偏好
     *
     * @param stringName 字符串名称
     * @return 偏好的输入法类型，如果无记录返回null
     */
    fun getPreferredMethod(stringName: String): InputMethodType? {
        val habit = habitMap[stringName] ?: return null

        // 根据使用频率判断偏好
        return when {
            habit.englishCount > habit.chineseCount -> InputMethodType.ENGLISH
            habit.chineseCount > habit.englishCount -> InputMethodType.CHINESE
            habit.englishCount == habit.chineseCount && habit.englishCount > 0 -> habit.lastUsedMethod
            else -> null
        }
    }

    /**
     * 获取某个字符串的详细习惯数据
     *
     * @param stringName 字符串名称
     * @return 习惯数据，如果没有记录返回null
     */
    fun getHabitDetail(stringName: String): StringHabitData? {
        return habitMap[stringName]
    }

    /**
     * 获取所有字符串习惯记录
     *
     * @return 习惯记录列表，按名称排序
     */
    fun getAllHabits(): List<StringHabitData> {
        return habitMap.values.toList().sortedBy { it.name }
    }

    /**
     * 删除某个字符串的习惯记录
     *
     * @param stringName 字符串名称
     * @return true表示删除成功，false表示记录不存在
     */
    fun removeHabit(stringName: String): Boolean {
        return habitMap.remove(stringName) != null
    }

    /**
     * 清空所有习惯记录
     */
    fun clearAllHabits() {
        habitMap.clear()
    }

    /**
     * 获取习惯记录数量
     *
     * @return 记录数量
     */
    fun getHabitCount(): Int {
        return habitMap.size
    }

    /**
     * 获取使用频率最高的字符串
     *
     * @param limit 返回数量限制
     * @return 按使用频率排序的习惯列表
     */
    fun getMostUsedStrings(limit: Int = 10): List<StringHabitData> {
        return habitMap.values
            .filter { it.totalCount > 0 }
            .sortedByDescending { it.totalCount }
            .take(limit)
    }

    /**
     * 快速判断字符串是否有记录
     *
     * @param stringName 字符串名称
     * @return true表示有记录
     */
    fun hasHabit(stringName: String): Boolean {
        return habitMap.containsKey(stringName)
    }

    /**
     * 获取最近使用的习惯记录
     *
     * @param limit 返回数量限制
     * @return 按最后使用时间排序的习惯列表
     */
    fun getRecentlyUsed(limit: Int = 5): List<StringHabitData> {
        return habitMap.values
            .sortedByDescending { it.lastUpdated }
            .take(limit)
    }

    /**
     * 统计信息汇总
     *
     * @return 统计信息字符串
     */
    fun getStatisticsSummary(): String {
        val totalHabits = habitMap.size
        val totalUsages = habitMap.values.sumOf { it.totalCount }
        val englishPreferred = habitMap.values.count { it.preferredMethod == InputMethodType.ENGLISH }
        val chinesePreferred = habitMap.values.count { it.preferredMethod == InputMethodType.CHINESE }

        return """
            | 字符串习惯统计 |
            |--------------|
            | 总记录数: $totalHabits |
            | 总使用次数: $totalUsages |
            | 偏好英文: $englishPreferred |
            | 偏好中文: $chinesePreferred |
        """.trimMargin()
    }

    /**
     * 导出习惯数据为JSON格式
     *
     * @return JSON格式的习惯数据
     */
    fun exportToJson(): String {
        val habits = habitMap.values.toList()
        if (habits.isEmpty()) return "[]"

        val habitJsonList = habits.map { habit ->
            """
            {
                "name": "${habit.name.replace("\"", "\\\"")}",
                "englishCount": ${habit.englishCount},
                "chineseCount": ${habit.chineseCount},
                "lastUsedMethod": "${habit.lastUsedMethod}",
                "lastUpdated": ${habit.lastUpdated},
                "preferredMethod": "${habit.preferredMethod}"
            }""".trimIndent()
        }

        return "[\n${habitJsonList.joinToString(",\n")}\n]"
    }
}

