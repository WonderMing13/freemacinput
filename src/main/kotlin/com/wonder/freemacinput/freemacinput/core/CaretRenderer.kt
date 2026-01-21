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
 * 定义不同输入法状态下的光标颜色
 */
enum class CaretColorTheme(
    /** 颜色值 */
    val color: Color
) {
    // ============ 英文输入法颜色 ============
    ENGLISH(Color(0, 200, 100)),

    // ============ 中文输入法颜色 ============
    CHINESE(Color(0, 150, 255)),

    // ============ 未知状态 ============
    UNKNOWN(Color(150, 150, 150));

    companion object {
        /**
         * 根据输入法获取对应颜色主题
         */
        fun from(inputMethod: InputMethodType): CaretColorTheme {
            return when (inputMethod) {
                InputMethodType.ENGLISH -> ENGLISH
                InputMethodType.CHINESE -> CHINESE
                InputMethodType.AUTO -> UNKNOWN
                InputMethodType.UNKNOWN -> UNKNOWN
                InputMethodType.CAPS_LOCK -> ENGLISH  // Caps Lock 使用英文主题
            }
        }
    }
}

/**
 * 光标颜色渲染器
 *
 * 根据当前输入法显示不同颜色的光标提示。
 *
 * 功能：
 * 1. 光标颜色 - 根据输入法显示不同颜色
 * 2. 自动更新 - 当输入法变化时自动更新
 *
 * 颜色方案：
 * - 英文: 绿色 (RGB: 0, 200, 100)
 * - 中文: 蓝色 (RGB: 0, 150, 255)
 */
class CaretRenderer(private val editor: Editor) {

    // ==================== 状态管理 ====================

    private var currentInputMethod: InputMethodType = InputMethodType.ENGLISH
    private var isEnabled: Boolean = true

    // 保存原始光标颜色
    private var originalCaretColor: Color? = null

    init {
        saveOriginalSettings()
        updateCaretColor()
    }

    // ==================== 设置保存与恢复 ====================

    /**
     * 保存编辑器的原始设置
     */
    private fun saveOriginalSettings() {
        val scheme = EditorColorsManager.getInstance().globalScheme
        originalCaretColor = scheme.getColor(EditorColors.CARET_COLOR)
    }

    /**
     * 恢复编辑器的原始设置
     */
    fun restoreOriginalSettings() {
        if (originalCaretColor != null) {
            val scheme = EditorColorsManager.getInstance().globalScheme
            scheme.setColor(EditorColors.CARET_COLOR, originalCaretColor)
        }
    }

    // ==================== 状态更新 ====================

    /**
     * 设置是否启用光标颜色渲染
     */
    fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        isEnabled = enabled
        if (enabled) {
            updateCaretColor()
        } else {
            restoreOriginalSettings()
        }
    }

    /**
     * 更新光标颜色
     */
    fun updateCaretColor() {
        if (!isEnabled) {
            restoreOriginalSettings()
            return
        }

        val theme = CaretColorTheme.from(currentInputMethod)
        val color = theme.color
        val scheme = EditorColorsManager.getInstance().globalScheme
        scheme.setColor(EditorColors.CARET_COLOR, color)
    }

    /**
     * 刷新光标颜色
     */
    fun refresh() {
        updateCaretColor()
    }
}

/**
 * 光标渲染器管理器
 *
 * 管理多个编辑器的光标渲染器实例，提供全局访问和管理功能。
 */
object CaretRendererManager {

    private val renderers = mutableMapOf<Editor, CaretRenderer>()

    /**
     * 获取或创建指定编辑器的光标渲染器
     */
    fun getOrCreate(editor: Editor): CaretRenderer {
        return renderers.computeIfAbsent(editor) {
            CaretRenderer(it)
        }
    }

    /**
     * 移除指定编辑器的光标渲染器
     */
    fun remove(editor: Editor) {
        renderers.remove(editor)?.let {
            it.restoreOriginalSettings()
        }
    }

    /**
     * 清除所有渲染器
     */
    fun disposeAll() {
        renderers.values.forEach { it.restoreOriginalSettings() }
        renderers.clear()
    }
}