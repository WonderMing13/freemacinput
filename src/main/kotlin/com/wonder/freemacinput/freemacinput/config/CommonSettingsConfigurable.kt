package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * 常用设置配置页面
 */
class CommonSettingsConfigurable : Configurable {

    private var settingsState: SettingsState? = null
    private var mainPanel: JPanel? = null
    private var enabledCheckbox: JCheckBox? = null
    private var showHintsCheckbox: JCheckBox? = null
    private var leaveIDEStrategyCombo: JComboBox<String>? = null

    override fun getDisplayName(): String = "常用设置"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        settingsState = ApplicationManager.getApplication().getService(SettingsState::class.java)

        mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        }

        // 插件开关
        addSectionTitle("插件开关")
        enabledCheckbox = addCheckbox("启用自动切换输入法")
        
        addVerticalSpace(20)

        // 提示设置
        addSectionTitle("提示设置")
        showHintsCheckbox = addCheckbox("显示切换提示")

        addVerticalSpace(20)

        // 离开IDE场景
        addSectionTitle("离开IDE场景（仅macOS）")
        addLabel("Windows系统每个APP的输入法状态是独立的，因此不需要此功能")
        addLabel("macOS系统可以设置离开IDE时的输入法切换策略：")
        
        val strategies = LeaveIDEStrategy.values().map { it.getDisplayName() }.toTypedArray()
        leaveIDEStrategyCombo = JComboBox(strategies).apply {
            maximumSize = Dimension(300, 30)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        
        val strategyPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            add(JLabel("切换策略："))
            add(leaveIDEStrategyCombo)
        }
        mainPanel?.add(strategyPanel)

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val state = settingsState ?: return false
        return enabledCheckbox?.isSelected != state.isEnabled ||
                showHintsCheckbox?.isSelected != state.isShowHints ||
                getLeaveIDEStrategy() != state.leaveIDEStrategy
    }

    override fun apply() {
        val state = settingsState ?: return
        state.isEnabled = enabledCheckbox?.isSelected ?: true
        state.isShowHints = showHintsCheckbox?.isSelected ?: true
        state.leaveIDEStrategy = getLeaveIDEStrategy()
    }

    override fun reset() {
        val state = settingsState ?: return
        enabledCheckbox?.isSelected = state.isEnabled
        showHintsCheckbox?.isSelected = state.isShowHints
        setLeaveIDEStrategy(state.leaveIDEStrategy)
    }

    private fun addSectionTitle(title: String) {
        val label = JLabel(title).apply {
            font = font.deriveFont(14f).deriveFont(java.awt.Font.BOLD)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        mainPanel?.add(label)
        mainPanel?.add(Box.createVerticalStrut(10))
    }

    private fun addLabel(text: String) {
        val label = JLabel(text).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            font = font.deriveFont(12f)
        }
        mainPanel?.add(label)
        mainPanel?.add(Box.createVerticalStrut(5))
    }

    private fun addCheckbox(text: String): JCheckBox {
        val checkbox = JCheckBox(text).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
        mainPanel?.add(checkbox)
        mainPanel?.add(Box.createVerticalStrut(5))
        return checkbox
    }

    private fun addVerticalSpace(height: Int) {
        mainPanel?.add(Box.createVerticalStrut(height))
    }

    private fun getLeaveIDEStrategy(): LeaveIDEStrategy {
        val selected = leaveIDEStrategyCombo?.selectedItem as? String
        return LeaveIDEStrategy.fromDisplayName(selected ?: "")
    }

    private fun setLeaveIDEStrategy(strategy: LeaveIDEStrategy) {
        leaveIDEStrategyCombo?.selectedItem = strategy.getDisplayName()
    }
}
