package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import javax.swing.*
import java.awt.*

/**
 * 注释场景配置页面
 */
class CommentSceneConfigurable : Configurable {

    private var panel: JPanel? = null
    private var commentMethodCombo: JComboBox<String>? = null
    private var showHintCheckbox: JCheckBox? = null

    override fun getDisplayName(): String = "注释场景"

    override fun createComponent(): JComponent {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        // 默认输入法设置
        gbc.gridx = 0
        gbc.gridy = 0
        mainPanel.add(JLabel("注释区域默认输入法："), gbc)
        
        gbc.gridx = 1
        commentMethodCombo = JComboBox(arrayOf("英文", "中文"))
        mainPanel.add(commentMethodCombo!!, gbc)
        
        // 提示设置标题
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        val hintLabel = JLabel("注释场景提示：")
        hintLabel.font = hintLabel.font.deriveFont(java.awt.Font.BOLD)
        mainPanel.add(hintLabel, gbc)
        
        // 提示开关
        gbc.gridy = 2
        showHintCheckbox = JCheckBox("输入 // 或 /* */ 时显示\"注释场景\"提示")
        mainPanel.add(showHintCheckbox!!, gbc)
        
        // 说明文字
        gbc.gridy = 3
        val descLabel = JLabel("<html><i>当光标进入注释区域时，插件会自动切换到配置的输入法</i></html>")
        descLabel.foreground = Color.GRAY
        mainPanel.add(descLabel, gbc)
        
        // 填充剩余空间
        gbc.gridy = 4
        gbc.weighty = 1.0
        mainPanel.add(Box.createVerticalGlue(), gbc)
        
        panel = mainPanel
        return mainPanel
    }

    override fun isModified(): Boolean {
        val state = getSettings()
        return getMethodFromCombo(commentMethodCombo) != state.commentMethod ||
                showHintCheckbox?.isSelected != state.showCommentSceneHint
    }

    override fun apply() {
        val state = getSettings()
        state.commentMethod = getMethodFromCombo(commentMethodCombo)
        state.showCommentSceneHint = showHintCheckbox?.isSelected ?: true
    }

    override fun reset() {
        val state = getSettings()
        setMethodToCombo(commentMethodCombo, state.commentMethod)
        showHintCheckbox?.isSelected = state.showCommentSceneHint
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
    
    private fun getSettings(): SettingsState {
        return ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
