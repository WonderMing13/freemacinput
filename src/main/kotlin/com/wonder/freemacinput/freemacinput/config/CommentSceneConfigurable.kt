package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * 注释场景配置页面
 */
class CommentSceneConfigurable : Configurable {

    private var settingsState: SettingsState? = null
    private var mainPanel: JPanel? = null
    private var commentMethodCombo: JComboBox<String>? = null
    private var showHintCheckbox: JCheckBox? = null

    override fun getDisplayName(): String = "注释场景"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        settingsState = ApplicationManager.getApplication().getService(SettingsState::class.java)

        mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        }

        // 默认输入法设置
        addSectionTitle("注释区域默认输入法")
        val methodPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            add(JLabel("输入注释时默认切换为："))
            commentMethodCombo = JComboBox(arrayOf("英文", "中文")).apply {
                preferredSize = Dimension(100, 25)
            }
            add(commentMethodCombo)
        }
        mainPanel?.add(methodPanel)
        
        addVerticalSpace(20)

        // 提示设置
        addSectionTitle("注释场景提示")
        showHintCheckbox = addCheckbox("输入 // 或 /* */ 时显示\"注释场景\"提示")
        
        addVerticalSpace(15)
        
        // 说明文字
        addLabel("说明：")
        addLabel("• 当光标进入注释区域时，插件会自动切换到配置的输入法")
        addLabel("• 支持行注释（//）和块注释（/* */）")
        addLabel("• 提示功能可以帮助您了解当前所在的编辑场景")

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val state = settingsState ?: return false
        return getMethodFromCombo(commentMethodCombo) != state.commentMethod ||
                showHintCheckbox?.isSelected != state.showCommentSceneHint
    }

    override fun apply() {
        val state = settingsState ?: return
        state.commentMethod = getMethodFromCombo(commentMethodCombo)
        state.showCommentSceneHint = showHintCheckbox?.isSelected ?: true
    }

    override fun reset() {
        val state = settingsState ?: return
        setMethodToCombo(commentMethodCombo, state.commentMethod)
        showHintCheckbox?.isSelected = state.showCommentSceneHint
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

    private fun getMethodFromCombo(combo: JComboBox<String>?): InputMethodType {
        return when (combo?.selectedItem as? String) {
            "英文" -> InputMethodType.ENGLISH
            "中文" -> InputMethodType.CHINESE
            else -> InputMethodType.CHINESE
        }
    }

    private fun setMethodToCombo(combo: JComboBox<String>?, method: InputMethodType) {
        combo?.selectedItem = when (method) {
            InputMethodType.ENGLISH -> "英文"
            InputMethodType.CHINESE -> "中文"
            else -> "中文"
        }
    }
}
