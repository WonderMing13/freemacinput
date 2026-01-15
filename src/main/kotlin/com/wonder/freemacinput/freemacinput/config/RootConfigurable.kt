package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.options.Configurable
import javax.swing.*

/**
 * 根配置页面（空页面，只作为父节点）
 */
class RootConfigurable : Configurable {
    
    override fun getDisplayName(): String = "FreeMacInput"
    
    override fun createComponent(): JComponent {
        val panel = JPanel()
        panel.add(JLabel("请选择左侧的配置项"))
        return panel
    }
    
    override fun isModified(): Boolean = false
    
    override fun apply() {}
    
    override fun reset() {}
}
