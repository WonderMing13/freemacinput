package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

/**
 * 插件配置界面
 * 提供用户界面来配置各场景的输入法设置
 *
 * 实现 Application 级别的配置页面，使用无参构造函数
 */
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
    private var gitCommitMethodCombo: JComboBox<String>? = null
    private var terminalMethodCombo: JComboBox<String>? = null
    private var debugMethodCombo: JComboBox<String>? = null
    private var projectMethodCombo: JComboBox<String>? = null

    // 输入源与切换偏好组件
    private var englishInputSourceField: JTextField? = null
    private var chineseInputSourceField: JTextField? = null
    private var preferImSelectCheckbox: JCheckBox? = null
    private var fallbackHotkeyCombo: JComboBox<String>? = null

    override fun getDisplayName(): String = "FreeMacInput"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        // 获取应用级服务实例
        settingsState = ApplicationManager.getApplication().getService(SettingsState::class.java).state

        mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        // ============ 通用设置 ============
        addSection("通用设置")
        addCheckbox("启用插件", true).also { enabledCheckbox = it }
        addCheckbox("显示切换提示", true).also { showHintsCheckbox = it }
        addCheckbox("启用光标颜色提示", true).also { caretColorCheckbox = it }

        addVerticalSpace(20)

        // ============ 代码区域设置 ============
        addSection("代码区域")
        addRow("默认代码区域:", createMethodCombo()).let { defaultMethodCombo = it }
        addRow("注释区域:", createMethodCombo()).let { commentMethodCombo = it }
        addRow("Git提交信息:", createMethodCombo()).let { gitCommitMethodCombo = it }

        addVerticalSpace(20)

        // ============ 工具窗口设置 ============
        addSection("工具窗口")
        addRow("Terminal:", createMethodCombo()).let { terminalMethodCombo = it }
        addRow("Debug:", createMethodCombo()).let { debugMethodCombo = it }
        addRow("Project:", createMethodCombo()).let { projectMethodCombo = it }

        addVerticalSpace(20)

        // ============ 输入源与切换偏好 ============
        addSection("输入源与切换偏好")
        englishInputSourceField = JTextField(24)
        chineseInputSourceField = JTextField(24)
        addRowComponent("英文输入源 ID:", englishInputSourceField!!)
        addRowComponent("中文输入源 ID:", chineseInputSourceField!!)
        preferImSelectCheckbox = addCheckbox("优先使用 im-select 精确切换", true)
        addRow("回退快捷键:", createHotkeyCombo()).let { fallbackHotkeyCombo = it }

        return mainPanel!!
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
     * 添加一行配置（标签+任意组件，如文本框）
     */
    private fun addRowComponent(label: String, component: JComponent) {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val jLabel = JLabel(label).apply {
            preferredSize = Dimension(150, 25)
        }
        panel.add(jLabel)
        panel.add(component)
        panel.add(Box.createHorizontalGlue())

        mainPanel?.add(panel)
        mainPanel?.add(Box.createVerticalStrut(8))
    }

    override fun isModified(): Boolean {
        val state = settingsState ?: return false

        return enabledCheckbox?.isSelected != state.isEnabled ||
                showHintsCheckbox?.isSelected != state.isShowHints ||
                caretColorCheckbox?.isSelected != state.isEnableCaretColor ||
                getMethodFromCombo(defaultMethodCombo) != state.defaultMethod ||
                getMethodFromCombo(commentMethodCombo) != state.commentMethod ||
                getMethodFromCombo(gitCommitMethodCombo) != state.gitCommitMethod ||
                getMethodFromCombo(terminalMethodCombo) != state.terminalMethod ||
                getMethodFromCombo(debugMethodCombo) != state.debugMethod ||
                getMethodFromCombo(projectMethodCombo) != state.projectWindowMethod ||
                (englishInputSourceField?.text ?: "") != state.englishInputSource ||
                (chineseInputSourceField?.text ?: "") != state.chineseInputSource ||
                (preferImSelectCheckbox?.isSelected ?: true) != state.preferImSelect ||
                getHotkeyFromCombo(fallbackHotkeyCombo) != state.fallbackHotkey
    }

    override fun apply() {
        val state = settingsState ?: return

        state.isEnabled = enabledCheckbox?.isSelected ?: true
        state.isShowHints = showHintsCheckbox?.isSelected ?: true
        state.isEnableCaretColor = caretColorCheckbox?.isSelected ?: true
        state.defaultMethod = getMethodFromCombo(defaultMethodCombo)
        state.commentMethod = getMethodFromCombo(commentMethodCombo)
        state.gitCommitMethod = getMethodFromCombo(gitCommitMethodCombo)
        state.terminalMethod = getMethodFromCombo(terminalMethodCombo)
        state.debugMethod = getMethodFromCombo(debugMethodCombo)
        state.projectWindowMethod = getMethodFromCombo(projectMethodCombo)
        state.englishInputSource = englishInputSourceField?.text ?: state.englishInputSource
        state.chineseInputSource = chineseInputSourceField?.text ?: state.chineseInputSource
        state.preferImSelect = preferImSelectCheckbox?.isSelected ?: true
        state.fallbackHotkey = getHotkeyFromCombo(fallbackHotkeyCombo)
    }

    override fun reset() {
        val state = settingsState ?: return

        enabledCheckbox?.isSelected = state.isEnabled
        showHintsCheckbox?.isSelected = state.isShowHints
        caretColorCheckbox?.isSelected = state.isEnableCaretColor

        setMethodToCombo(defaultMethodCombo, state.defaultMethod)
        setMethodToCombo(commentMethodCombo, state.commentMethod)
        setMethodToCombo(gitCommitMethodCombo, state.gitCommitMethod)
        setMethodToCombo(terminalMethodCombo, state.terminalMethod)
        setMethodToCombo(debugMethodCombo, state.debugMethod)
        setMethodToCombo(projectMethodCombo, state.projectWindowMethod)
        englishInputSourceField?.text = state.englishInputSource
        chineseInputSourceField?.text = state.chineseInputSource
        preferImSelectCheckbox?.isSelected = state.preferImSelect
        setHotkeyToCombo(fallbackHotkeyCombo, state.fallbackHotkey)
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
     * 从下拉框获取选择的输入法类型
     */
    private fun getMethodFromCombo(combo: JComboBox<String>?): InputMethodType {
        return when (combo?.selectedIndex) {
            0 -> InputMethodType.ENGLISH
            1 -> InputMethodType.CHINESE
            else -> InputMethodType.AUTO
        }
    }

    private fun createHotkeyCombo(): JComboBox<String> {
        return JComboBox(arrayOf("Ctrl+Space", "Alt+Space", "Ctrl+Shift+Space")).apply {
            preferredSize = Dimension(160, 25)
        }
    }

    private fun getHotkeyFromCombo(combo: JComboBox<String>?): String {
        return when (combo?.selectedIndex) {
            0 -> "CTRL_SPACE"
            1 -> "ALT_SPACE"
            else -> "CTRL_SHIFT_SPACE"
        }
    }

    private fun setHotkeyToCombo(combo: JComboBox<String>?, value: String) {
        val idx = when (value) {
            "CTRL_SPACE" -> 0
            "ALT_SPACE" -> 1
            "CTRL_SHIFT_SPACE" -> 2
            else -> 0
        }
        combo?.selectedIndex = idx
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
