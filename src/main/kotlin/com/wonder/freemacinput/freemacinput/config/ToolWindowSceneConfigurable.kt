package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.project.ProjectManager
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import javax.swing.*
import javax.swing.table.AbstractTableModel
import java.awt.*

/**
 * 工具窗口场景配置页面
 */
class ToolWindowSceneConfigurable : Configurable {

    private var panel: JPanel? = null
    private var table: JTable? = null
    private var tableModel: ToolWindowTableModel? = null

    override fun getDisplayName(): String = "工具窗口场景"

    override fun createComponent(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        
        // 说明文字
        val descPanel = JPanel(BorderLayout())
        val descLabel = JLabel("<html><i>IDE启动时自动检测所有工具窗口。选中表示开启自动切换，可修改目标输入法。</i></html>")
        descLabel.foreground = Color.GRAY
        descLabel.border = BorderFactory.createEmptyBorder(5, 5, 10, 5)
        descPanel.add(descLabel, BorderLayout.NORTH)
        mainPanel.add(descPanel, BorderLayout.NORTH)
        
        // 创建表格
        tableModel = ToolWindowTableModel()
        table = JTable(tableModel)
        
        // 设置列宽
        table?.columnModel?.getColumn(0)?.preferredWidth = 50  // 启用列
        table?.columnModel?.getColumn(1)?.preferredWidth = 200 // 工具窗口名称
        table?.columnModel?.getColumn(2)?.preferredWidth = 150 // 目标输入法
        
        // 设置输入法列的下拉框编辑器
        val inputMethodCombo = JComboBox(arrayOf("英文", "中文"))
        table?.columnModel?.getColumn(2)?.cellEditor = DefaultCellEditor(inputMethodCombo)
        
        val scrollPane = JScrollPane(table)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        
        // 刷新按钮
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val refreshButton = JButton("刷新工具窗口列表")
        refreshButton.addActionListener {
            detectToolWindows()
            tableModel?.fireTableDataChanged()
        }
        buttonPanel.add(refreshButton)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        panel = mainPanel
        
        // 初始加载
        detectToolWindows()
        
        return mainPanel
    }

    override fun isModified(): Boolean {
        val settings = getSettings()
        val currentRules = tableModel?.rules ?: return false
        
        if (currentRules.size != settings.toolWindowRules.size) return true
        
        for (i in currentRules.indices) {
            val current = currentRules[i]
            val saved = settings.toolWindowRules.find { it.toolWindowId == current.toolWindowId }
            if (saved == null || 
                saved.enabled != current.enabled || 
                saved.targetInputMethod != current.targetInputMethod) {
                return true
            }
        }
        
        return false
    }

    override fun apply() {
        val settings = getSettings()
        settings.toolWindowRules.clear()
        tableModel?.rules?.forEach { rule ->
            settings.toolWindowRules.add(rule.copy())
        }
    }

    override fun reset() {
        detectToolWindows()
        tableModel?.fireTableDataChanged()
    }

    /**
     * 检测所有工具窗口
     */
    private fun detectToolWindows() {
        val settings = getSettings()
        val detectedRules = mutableListOf<ToolWindowRule>()
        
        // 获取所有打开的项目
        val projects = ProjectManager.getInstance().openProjects
        val toolWindowIds = mutableSetOf<String>()
        
        for (project in projects) {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            toolWindowManager.toolWindowIds.forEach { id ->
                toolWindowIds.add(id)
            }
        }
        
        // 为每个工具窗口创建规则
        toolWindowIds.sorted().forEach { id ->
            // 查找已保存的规则
            val existingRule = settings.toolWindowRules.find { it.toolWindowId == id }
            
            if (existingRule != null) {
                detectedRules.add(existingRule.copy())
            } else {
                // 创建新规则，默认不启用
                detectedRules.add(ToolWindowRule(
                    toolWindowId = id,
                    displayName = getDisplayName(id),
                    enabled = false,
                    targetInputMethod = InputMethodType.ENGLISH
                ))
            }
        }
        
        tableModel?.rules = detectedRules
    }

    /**
     * 获取工具窗口的显示名称
     */
    private fun getDisplayName(toolWindowId: String): String {
        // 尝试从打开的项目中获取工具窗口的显示名称
        val projects = ProjectManager.getInstance().openProjects
        for (project in projects) {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow(toolWindowId)
            if (toolWindow != null) {
                return toolWindow.stripeTitle
            }
        }
        return toolWindowId
    }

    private fun getSettings(): SettingsState {
        return ApplicationManager.getApplication().getService(SettingsState::class.java)
    }

    /**
     * 工具窗口表格模型
     */
    private class ToolWindowTableModel : AbstractTableModel() {
        var rules: MutableList<ToolWindowRule> = mutableListOf()
        
        private val columnNames = arrayOf("启用", "工具窗口", "目标输入法")

        override fun getRowCount(): Int = rules.size

        override fun getColumnCount(): Int = 3

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> java.lang.Boolean::class.java
                else -> String::class.java
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0 || columnIndex == 2
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val rule = rules[rowIndex]
            return when (columnIndex) {
                0 -> rule.enabled
                1 -> rule.displayName
                2 -> when (rule.targetInputMethod) {
                    InputMethodType.ENGLISH -> "英文"
                    InputMethodType.CHINESE -> "中文"
                    else -> "英文"
                }
                else -> ""
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val rule = rules[rowIndex]
            when (columnIndex) {
                0 -> rule.enabled = aValue as Boolean
                2 -> rule.targetInputMethod = when (aValue as String) {
                    "中文" -> InputMethodType.CHINESE
                    else -> InputMethodType.ENGLISH
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }
}
