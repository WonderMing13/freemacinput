package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.ui.CustomPatternRulePanel
import javax.swing.*
import java.awt.*

/**
 * 自定义规则场景配置页面
 */
class CustomPatternSceneConfigurable : Configurable {
    
    private var panel: JPanel? = null
    private var rulePanel: CustomPatternRulePanel? = null
    
    override fun getDisplayName(): String = "自定义规则场景"
    
    override fun createComponent(): JComponent {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        var row = 0
        
        // 说明文本
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        val descLabel = JLabel("<html><b>自定义规则场景</b><br>" +
                "通过正则表达式匹配光标左右两侧的文本，实现更精准的输入法切换。<br>" +
                "例如：当光标位于中文字符之间时，自动切换为中文输入法。</html>")
        mainPanel.add(descLabel, gbc)
        
        // 分隔线
        gbc.gridy = row++
        mainPanel.add(JSeparator(), gbc)
        
        // 自定义规则面板
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        rulePanel = CustomPatternRulePanel()
        mainPanel.add(rulePanel!!, gbc)
        
        panel = mainPanel
        return mainPanel
    }
    
    override fun isModified(): Boolean {
        val settings = getSettings()
        return rulePanel?.isModified(settings.customPatternRules) == true
    }
    
    override fun apply() {
        val settings = getSettings()
        settings.customPatternRules = rulePanel?.getRules() ?: mutableListOf()
    }
    
    override fun reset() {
        val settings = getSettings()
        rulePanel?.setRules(settings.customPatternRules)
    }
    
    private fun getSettings(): SettingsState {
        return ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
