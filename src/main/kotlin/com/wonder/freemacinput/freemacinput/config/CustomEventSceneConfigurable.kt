package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.ui.CustomEventRulePanel
import javax.swing.*
import java.awt.*

/**
 * 自定义事件场景配置页面
 */
class CustomEventSceneConfigurable : Configurable {
    
    private var panel: JPanel? = null
    private var enableEventLoggingCheckBox: JCheckBox? = null
    private var rulePanel: CustomEventRulePanel? = null
    
    override fun getDisplayName(): String = "自定义事件场景"
    
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
        val descLabel = JLabel("<html><b>自定义事件场景</b><br>" +
                "IDE中的很多操作都会触发事件，通过监听事件可以实现自动切换输入法。<br>" +
                "例如：监听\"Show Translation Dialog...\"事件，在打开翻译窗口时自动切换为中文输入法。</html>")
        mainPanel.add(descLabel, gbc)
        
        // 分隔线
        gbc.gridy = row++
        mainPanel.add(JSeparator(), gbc)
        
        // 开启事件日志记录
        gbc.gridy = row++
        enableEventLoggingCheckBox = JCheckBox("开启事件日志记录")
        mainPanel.add(enableEventLoggingCheckBox!!, gbc)
        
        // 说明文本
        gbc.gridy = row++
        val logDescLabel = JLabel("<html><i>开启后，IDE中的事件会记录到日志中，方便您找到需要监听的事件名称。<br>" +
                "查看日志：Help -> Show Log in Explorer -> 打开 idea.log 文件，搜索 \"FreeMacInput EventPerformed\"</i></html>")
        logDescLabel.foreground = Color.GRAY
        mainPanel.add(logDescLabel, gbc)
        
        // 分隔线
        gbc.gridy = row++
        mainPanel.add(JSeparator(), gbc)
        
        // 自定义事件规则
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        rulePanel = CustomEventRulePanel()
        mainPanel.add(rulePanel!!, gbc)
        
        panel = mainPanel
        return mainPanel
    }
    
    override fun isModified(): Boolean {
        val settings = getSettings()
        return enableEventLoggingCheckBox?.isSelected != settings.enableEventLogging ||
               rulePanel?.isModified(settings.customEventRules) == true
    }
    
    override fun apply() {
        val settings = getSettings()
        settings.enableEventLogging = enableEventLoggingCheckBox?.isSelected ?: false
        settings.customEventRules = rulePanel?.getRules() ?: mutableListOf()
    }
    
    override fun reset() {
        val settings = getSettings()
        enableEventLoggingCheckBox?.isSelected = settings.enableEventLogging
        rulePanel?.setRules(settings.customEventRules)
    }
    
    private fun getSettings(): SettingsState {
        return ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
