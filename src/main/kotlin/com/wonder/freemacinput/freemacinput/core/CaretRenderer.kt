package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import java.awt.Color

/**
 * 光标颜色主题枚举
 *
 * 定义不同输入法和CapsLock状态组合下的光标颜色
 */
enum class CaretColorTheme(
    /** 颜色值 */
    val color: Color,
    /** 显示名称 */
    val displayName: String
) {
    // ============ 英文输入法颜色 ============
    ENGLISH_NORMAL(Color(0, 200, 100), "英文-小写"),
    ENGLISH_CAPS(Color(0, 180, 80), "英文-大写"),

    // ============ 中文输入法颜色 ============
    CHINESE_NORMAL(Color(0, 150, 255), "中文-小写"),
    CHINESE_CAPS(Color(0, 120, 200), "中文-大写"),

    // ============ 未知状态 ============
    UNKNOWN(Color(150, 150, 150), "未知");

    companion object {
        /**
         * 根据输入法和CapsLock状态获取对应颜色主题
         */
        fun from(inputMethod: InputMethodType, capsLock: CapsLockState): CaretColorTheme {
            return when (inputMethod) {
                InputMethodType.ENGLISH -> if (capsLock == CapsLockState.ON) ENGLISH_CAPS else ENGLISH_NORMAL
                InputMethodType.CHINESE -> if (capsLock == CapsLockState.ON) CHINESE_CAPS else CHINESE_NORMAL
                InputMethodType.AUTO -> UNKNOWN
            }
        }

        /**
         * 获取所有可用颜色主题
         */
        fun allThemes(): List<CaretColorTheme> = entries.toList()
    }
}

/**
 * 光标颜色配置常量
 */
object CaretColorConstants {
    /** 绿色 - 英文/小写 */
    val GREEN = Color(0, 200, 100)

    /** 深绿色 - 英文/大写 */
    val DARK_GREEN = Color(0, 180, 80)

    /** 蓝色 - 中文/小写 */
    val BLUE = Color(0, 150, 255)

    /** 深蓝色 - 中文/大写 */
    val DARK_BLUE = Color(0, 120, 200)

    /** 灰色 - 未知状态 */
    val GRAY = Color(150, 150, 150)
}

/**
 * 光标颜色渲染器
 *
 * 根据当前输入法和大小写状态显示不同颜色的光标提示。
 *
 * 功能：
 * 1. 光标颜色 - 根据输入法显示不同颜色
 * 2. 状态栏显示 - 在编辑器状态栏显示当前状态
 * 3. 自动更新 - 当输入法或CapsLock状态变化时自动更新
 *
 * 颜色方案：
 * - 英文 + 小写: 绿色 (RGB: 0, 200, 100)
 * - 英文 + 大写: 深绿色 (RGB: 0, 180, 80)
 * - 中文 + 小写: 蓝色 (RGB: 0, 150, 255)
 * - 中文 + 大写: 深蓝色 (RGB: 0, 120, 200)
 */
class CaretRenderer(private val editor: Editor) {

    // ==================== 状态管理 ====================

    private var currentInputMethod: InputMethodType = InputMethodType.ENGLISH
    private var currentCapsLock: CapsLockState = CapsLockState.OFF
    private var isEnabled: Boolean = true

    // 保存原始光标颜色
    private var originalCaretColor: Color? = null

    init {
        saveOriginalSettings()
    }

    // ==================== 公开方法 ====================

    /**
     * 设置当前输入法类型
     *
     * @param method 当前输入法类型
     */
    fun setInputMethod(method: InputMethodType) {
        if (currentInputMethod != method) {
            currentInputMethod = method
            updateCaretColor()
        }
    }

    /**
     * 设置CapsLock状态
     *
     * @param state CapsLock状态
     */
    fun setCapsLockState(state: CapsLockState) {
        if (currentCapsLock != state) {
            currentCapsLock = state
            updateCaretColor()
        }
    }

    /**
     * 启用/禁用光标颜色
     *
     * @param enabled 是否启用
     */
    fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            isEnabled = enabled
            if (enabled) {
                updateCaretColor()
            } else {
                restoreOriginalSettings()
            }
        }
    }

    /**
     * 强制刷新光标颜色
     */
    fun refresh() {
        if (isEnabled) {
            updateCaretColor()
        }
    }

    /**
     * 获取当前状态文本
     *
     * @return 状态描述文本，如 "EN↓", "CN↑"
     */
    fun getStatusText(): String {
        val methodText = when (currentInputMethod) {
            InputMethodType.ENGLISH -> "EN"
            InputMethodType.CHINESE -> "CN"
            InputMethodType.AUTO -> "??"
        }

        val capsText = when (currentCapsLock) {
            CapsLockState.ON -> "↑"     // 大写指示
            CapsLockState.OFF -> "↓"    // 小写指示
            CapsLockState.UNKNOWN -> "?"
        }

        return "$methodText $capsText"
    }

    /**
     * 获取当前颜色主题
     */
    fun getCurrentTheme(): CaretColorTheme {
        return CaretColorTheme.from(currentInputMethod, currentCapsLock)
    }

    /**
     * 获取当前颜色
     */
    fun getCurrentColor(): Color {
        return getCurrentTheme().color
    }

    /**
     * 释放资源
     */
    fun dispose() {
        restoreOriginalSettings()
    }

    // ==================== 私有方法 ====================

    /**
     * 保存原始设置
     */
    private fun saveOriginalSettings() {
        try {
            val colorsManager = EditorColorsManager.getInstance()
            val scheme = colorsManager.globalScheme
            originalCaretColor = scheme.getColor(EditorColors.CARET_COLOR)
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 更新光标颜色
     */
    private fun updateCaretColor() {
        if (!isEnabled) return

        try {
            val theme = getCurrentTheme()
            val colorsManager = EditorColorsManager.getInstance()
            val scheme = colorsManager.globalScheme

            scheme.setColor(EditorColors.CARET_COLOR, theme.color)
            editor.component.repaint()
        } catch (e: Exception) {
            applyTextAttributes()
        }
    }

    /**
     * 备选方案：使用TextAttributes进行行高亮
     */
    private fun applyTextAttributes() {
        try {
            val color = getCurrentColor()
            val textAttributes = TextAttributes().apply {
                effectColor = color
                effectType = EffectType.LINE_UNDERSCORE
            }

            val caretModel = editor.caretModel
            val lineNumber = caretModel.logicalPosition.line

            editor.markupModel.addLineHighlighter(
                lineNumber,
                HighlighterLayer.LAST,
                textAttributes
            )
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 恢复原始设置
     */
    private fun restoreOriginalSettings() {
        try {
            originalCaretColor?.let { originalColor ->
                val colorsManager = EditorColorsManager.getInstance()
                val scheme = colorsManager.globalScheme
                scheme.setColor(EditorColors.CARET_COLOR, originalColor)
                editor.component.repaint()
            }
        } catch (e: Exception) {
            // 忽略错误
        }
    }
}

/**
 * 光标渲染管理器
 *
 * 负责管理所有编辑器实例的光标渲染器
 */
object CaretRendererManager {

    private val renderers = mutableMapOf<Editor, CaretRenderer>()

    /**
     * 获取或创建光标渲染器
     */
    fun getOrCreate(editor: Editor): CaretRenderer {
        return renderers.getOrPut(editor) {
            CaretRenderer(editor)
        }
    }

    /**
     * 移除光标渲染器
     */
    fun remove(editor: Editor) {
        renderers[editor]?.dispose()
        renderers.remove(editor)
    }

    /**
     * 更新所有渲染器的输入法状态
     */
    fun updateAllInputMethod(method: InputMethodType) {
        renderers.values.forEach { it.setInputMethod(method) }
    }

    /**
     * 更新所有渲染器的CapsLock状态
     */
    fun updateAllCapsLock(state: CapsLockState) {
        renderers.values.forEach { it.setCapsLockState(state) }
    }

    /**
     * 释放所有资源
     */
    fun disposeAll() {
        renderers.values.forEach { it.dispose() }
        renderers.clear()
    }
}
