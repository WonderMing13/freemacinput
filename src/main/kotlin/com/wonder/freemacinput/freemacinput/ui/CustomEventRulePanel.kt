package com.wonder.freemacinput.freemacinput.ui

import com.wonder.freemacinput.freemacinput.config.CustomEventRule
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import javax.swing.*
import javax.swing.table.AbstractTableModel
import java.awt.*

/**
 * 自定义事件规则面板
 */
class CustomEventRulePanel : JPanel(BorderLayout()) {
    
    private val tableModel = CustomEventRuleTableModel()
    private val table = JTable(tableModel)
    
    init {
        // 表格设置
        table.fillsViewportHeight = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        
        // 添加滚动面板
        val scrollPane = JScrollPane(table)
        add(scrollPane, BorderLayout.CENTER)
        
        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        val addButton = JButton("添加")
        addButton.addActionListener {
            val dialog = CustomEventRuleDialog(SwingUtilities.getWindowAncestor(this), null)
            dialog.isVisible = true
            if (dialog.isOk) {
                val rule = dialog.getRule()
                tableModel.addRule(rule)
            }
        }
        buttonPanel.add(addButton)
        
        val editButton = JButton("编辑")
        editButton.addActionListener {
            val selectedRow = table.selectedRow
            if (selectedRow >= 0) {
                val rule = tableModel.getRule(selectedRow)
                val dialog = CustomEventRuleDialog(SwingUtilities.getWindowAncestor(this), rule)
                dialog.isVisible = true
                if (dialog.isOk) {
                    val updatedRule = dialog.getRule()
                    tableModel.updateRule(selectedRow, updatedRule)
                }
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一条规则", "提示", JOptionPane.INFORMATION_MESSAGE)
            }
        }
        buttonPanel.add(editButton)
        
        val deleteButton = JButton("删除")
        deleteButton.addActionListener {
            val selectedRow = table.selectedRow
            if (selectedRow >= 0) {
                val confirm = JOptionPane.showConfirmDialog(
                    this,
                    "确定要删除这条规则吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION
                )
                if (confirm == JOptionPane.YES_OPTION) {
                    tableModel.removeRule(selectedRow)
                }
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一条规则", "提示", JOptionPane.INFORMATION_MESSAGE)
            }
        }
        buttonPanel.add(deleteButton)
        
        add(buttonPanel, BorderLayout.SOUTH)
    }
    
    fun setRules(rules: List<CustomEventRule>) {
        tableModel.setRules(rules.map { it.copy() }.toMutableList())
    }
    
    fun getRules(): MutableList<CustomEventRule> {
        return tableModel.getRules()
    }
    
    fun isModified(originalRules: List<CustomEventRule>): Boolean {
        val currentRules = tableModel.getRules()
        if (currentRules.size != originalRules.size) return true
        
        for (i in currentRules.indices) {
            val current = currentRules[i]
            val original = originalRules[i]
            if (current.eventName != original.eventName ||
                current.targetInputMethod != original.targetInputMethod ||
                current.description != original.description ||
                current.enabled != original.enabled) {
                return true
            }
        }
        return false
    }
    
    /**
     * 表格模型
     */
    private class CustomEventRuleTableModel : AbstractTableModel() {
        private val rules = mutableListOf<CustomEventRule>()
        private val columnNames = arrayOf("启用", "事件名称", "目标输入法", "描述")
        
        override fun getRowCount(): Int = rules.size
        
        override fun getColumnCount(): Int = columnNames.size
        
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> java.lang.Boolean::class.java
                else -> String::class.java
            }
        }
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0  // 只有启用列可以直接编辑
        }
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val rule = rules[rowIndex]
            return when (columnIndex) {
                0 -> rule.enabled
                1 -> rule.eventName
                2 -> if (rule.targetInputMethod == InputMethodType.CHINESE) "中文" else "英文"
                3 -> rule.description
                else -> ""
            }
        }
        
        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0 && aValue is Boolean) {
                rules[rowIndex].enabled = aValue
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }
        
        fun setRules(newRules: MutableList<CustomEventRule>) {
            rules.clear()
            rules.addAll(newRules)
            fireTableDataChanged()
        }
        
        fun getRules(): MutableList<CustomEventRule> {
            return rules.map { it.copy() }.toMutableList()
        }
        
        fun addRule(rule: CustomEventRule) {
            rules.add(rule)
            fireTableRowsInserted(rules.size - 1, rules.size - 1)
        }
        
        fun updateRule(index: Int, rule: CustomEventRule) {
            rules[index] = rule
            fireTableRowsUpdated(index, index)
        }
        
        fun removeRule(index: Int) {
            rules.removeAt(index)
            fireTableRowsDeleted(index, index)
        }
        
        fun getRule(index: Int): CustomEventRule {
            return rules[index].copy()
        }
    }
}

/**
 * 自定义事件规则对话框
 */
class CustomEventRuleDialog(
    parent: Window?,
    private val originalRule: CustomEventRule?
) : JDialog(parent, if (originalRule == null) "添加自定义事件" else "编辑自定义事件", ModalityType.APPLICATION_MODAL) {
    
    private val eventNameField = JTextField(30)
    private val targetInputMethodComboBox = JComboBox(arrayOf("中文", "英文"))
    private val descriptionField = JTextField(30)
    private val enabledCheckBox = JCheckBox("启用", true)
    
    var isOk = false
        private set
    
    init {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        var row = 0
        
        // 事件名称
        gbc.gridx = 0
        gbc.gridy = row
        panel.add(JLabel("事件名称："), gbc)
        
        gbc.gridx = 1
        panel.add(eventNameField, gbc)
        
        // 目标输入法
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("目标输入法："), gbc)
        
        gbc.gridx = 1
        panel.add(targetInputMethodComboBox, gbc)
        
        // 描述
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("描述："), gbc)
        
        gbc.gridx = 1
        panel.add(descriptionField, gbc)
        
        // 启用
        gbc.gridx = 0
        gbc.gridy = ++row
        gbc.gridwidth = 2
        panel.add(enabledCheckBox, gbc)
        
        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val okButton = JButton("确定")
        okButton.addActionListener {
            if (eventNameField.text.isBlank()) {
                JOptionPane.showMessageDialog(this, "事件名称不能为空", "错误", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            isOk = true
            dispose()
        }
        buttonPanel.add(okButton)
        
        val cancelButton = JButton("取消")
        cancelButton.addActionListener {
            dispose()
        }
        buttonPanel.add(cancelButton)
        
        // 加载原始数据
        if (originalRule != null) {
            eventNameField.text = originalRule.eventName
            targetInputMethodComboBox.selectedIndex = if (originalRule.targetInputMethod == InputMethodType.CHINESE) 0 else 1
            descriptionField.text = originalRule.description
            enabledCheckBox.isSelected = originalRule.enabled
        }
        
        contentPane.layout = BorderLayout()
        contentPane.add(panel, BorderLayout.CENTER)
        contentPane.add(buttonPanel, BorderLayout.SOUTH)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    fun getRule(): CustomEventRule {
        return CustomEventRule(
            eventName = eventNameField.text.trim(),
            targetInputMethod = if (targetInputMethodComboBox.selectedIndex == 0) InputMethodType.CHINESE else InputMethodType.ENGLISH,
            description = descriptionField.text.trim(),
            enabled = enabledCheckBox.isSelected
        )
    }
}
