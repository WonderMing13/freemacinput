package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

class SettingsConfigurable : Configurable {

    private var settingsState: SettingsState? = null
    private var mainPanel: JPanel? = null

    // UI组件
    private var enabledCheckbox: JCheckBox? = null
    private var showHintsCheckbox: JCheckBox? = null
    private var caretColorCheckbox: JCheckBox? = null

    // 各场景的输入法下拉框
    private var defaultMethodCombo: JComboBox<String>? = null
    private var commentMethodCombo: JComboBox<String>? = null

    override fun getDisplayName(): String = "FreeMacInput"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        settingsState = ApplicationManager.getApplication().getService(SettingsState::class.java).state

        mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        addSection("通用设置")
        addCheckbox("启用插件", true).also { enabledCheckbox = it }
        addCheckbox("显示切换提示", true).also { showHintsCheckbox = it }
        addCheckbox("启用光标颜色提示", true).also { caretColorCheckbox = it }

        addVerticalSpace(20)

        addSection("代码区域")
        addRow("默认代码区域:", createMethodCombo()).let { defaultMethodCombo = it }
        addRow("注释区域:", createMethodCombo()).let { commentMethodCombo = it }

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val state = settingsState ?: return false

        return enabledCheckbox?.isSelected != state.isEnabled ||
                showHintsCheckbox?.isSelected != state.isShowHints ||
                caretColorCheckbox?.isSelected != state.isEnableCaretColor ||
                getMethodFromCombo(defaultMethodCombo) != state.defaultMethod ||
                getMethodFromCombo(commentMethodCombo) != state.commentMethod
    }

    override fun apply() {
        val state = settingsState ?: return

        state.isEnabled = enabledCheckbox?.isSelected ?: true
        state.isShowHints = showHintsCheckbox?.isSelected ?: true
        state.isEnableCaretColor = caretColorCheckbox?.isSelected ?: true
        state.defaultMethod = getMethodFromCombo(defaultMethodCombo)
        state.commentMethod = getMethodFromCombo(commentMethodCombo)
    }

    override fun reset() {
        val state = settingsState ?: return

        enabledCheckbox?.isSelected = state.isEnabled
        showHintsCheckbox?.isSelected = state.isShowHints
        caretColorCheckbox?.isSelected = state.isEnableCaretColor

        setMethodToCombo(defaultMethodCombo, state.defaultMethod)
        setMethodToCombo(commentMethodCombo, state.commentMethod)
    }

    // ============ UI构建辅助方法 ============

    /**
     * 添加配置分组标题
     */
    private fun addSection(title: String) {
        val label = JLabel(title).apply {
            font = font.deriveFont(16f)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        mainPanel?.add(label)
        mainPanel?.add(Box.createVerticalStrut(10))
    }

    /**
     * 添加复选框
     */
    private fun addCheckbox(text: String, defaultValue: Boolean): JCheckBox {
        val checkbox = JCheckBox(text, defaultValue).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
        mainPanel?.add(checkbox)
        mainPanel?.add(Box.createVerticalStrut(5))
        return checkbox
    }

    /**
     * 创建输入法选择下拉框
     */
    private fun createMethodCombo(): JComboBox<String> {
        return JComboBox(arrayOf("English", "Chinese", "Auto")).apply {
            preferredSize = Dimension(120, 25)
        }
    }

    /**
     * 添加一行配置（标签+下拉框）
     */
    private fun addRow(label: String, combo: JComboBox<String>): JComboBox<String> {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val jLabel = JLabel(label).apply {
            preferredSize = Dimension(150, 25)
        }
        panel.add(jLabel)
        panel.add(combo)
        panel.add(Box.createHorizontalGlue())

        mainPanel?.add(panel)
        mainPanel?.add(Box.createVerticalStrut(8))

        return combo
    }

    /**
     * 从下拉框获取选择的输入法类型
     */
    private fun getMethodFromCombo(combo: JComboBox<String>?): InputMethodType {
        return when (combo?.selectedIndex) {
            0 -> InputMethodType.ENGLISH
            1 -> InputMethodType.CHINESE
            else -> InputMethodType.AUTO
        }
    }

    /**
     * 设置下拉框的输入法选择
     */
    private fun setMethodToCombo(combo: JComboBox<String>?, method: InputMethodType) {
        combo?.selectedIndex = when (method) {
            InputMethodType.ENGLISH -> 0
            InputMethodType.CHINESE -> 1
            InputMethodType.AUTO -> 2
        }
    }

    /**
     * 添加垂直间距
     */
    private fun addVerticalSpace(height: Int) {
        mainPanel?.add(Box.createVerticalStrut(height))
    }
}
