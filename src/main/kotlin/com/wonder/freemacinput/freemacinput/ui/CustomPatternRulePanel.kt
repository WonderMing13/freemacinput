package com.wonder.freemacinput.freemacinput.ui

import com.wonder.freemacinput.freemacinput.config.CustomPatternRule
import com.wonder.freemacinput.freemacinput.config.MatchStrategy
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.core.ContextType
import javax.swing.*
import javax.swing.table.AbstractTableModel
import java.awt.*

/**
 * 自定义规则面板
 */
class CustomPatternRulePanel : JPanel(BorderLayout()) {
    
    private val tableModel = CustomPatternRuleTableModel()
    private val table = JTable(tableModel)
    
    init {
        // 表格设置
        table.fillsViewportHeight = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        
        // 双击编辑
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        editRule(selectedRow)
                    }
                }
            }
        })
        
        // 添加滚动面板
        val scrollPane = JScrollPane(table)
        add(scrollPane, BorderLayout.CENTER)
        
        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        val addButton = JButton("添加")
        addButton.addActionListener {
            val dialog = CustomPatternRuleDialog(SwingUtilities.getWindowAncestor(this), null)
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
                editRule(selectedRow)
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
    
    private fun editRule(index: Int) {
        val rule = tableModel.getRule(index)
        val dialog = CustomPatternRuleDialog(SwingUtilities.getWindowAncestor(this), rule)
        dialog.isVisible = true
        if (dialog.isOk) {
            val updatedRule = dialog.getRule()
            tableModel.updateRule(index, updatedRule)
        }
    }
    
    fun setRules(rules: List<CustomPatternRule>) {
        tableModel.setRules(rules.map { it.copy() }.toMutableList())
    }
    
    fun getRules(): MutableList<CustomPatternRule> {
        return tableModel.getRules()
    }
    
    fun isModified(originalRules: List<CustomPatternRule>): Boolean {
        val currentRules = tableModel.getRules()
        if (currentRules.size != originalRules.size) return true
        
        for (i in currentRules.indices) {
            val current = currentRules[i]
            val original = originalRules[i]
            if (current != original) {
                return true
            }
        }
        return false
    }
    
    /**
     * 表格模型
     */
    private class CustomPatternRuleTableModel : AbstractTableModel() {
        private val rules = mutableListOf<CustomPatternRule>()
        private val columnNames = arrayOf("启用", "规则名称", "左匹配", "右匹配", "满足条件", "输入法")
        
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
                1 -> rule.name
                2 -> if (rule.leftPattern.length > 20) rule.leftPattern.substring(0, 20) + "..." else rule.leftPattern
                3 -> if (rule.rightPattern.length > 20) rule.rightPattern.substring(0, 20) + "..." else rule.rightPattern
                4 -> rule.matchStrategy.displayName
                5 -> if (rule.targetInputMethod == InputMethodType.CHINESE) "中文" else "英文"
                else -> ""
            }
        }
        
        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0 && aValue is Boolean) {
                rules[rowIndex].enabled = aValue
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }
        
        fun setRules(newRules: MutableList<CustomPatternRule>) {
            rules.clear()
            rules.addAll(newRules)
            fireTableDataChanged()
        }
        
        fun getRules(): MutableList<CustomPatternRule> {
            return rules.map { it.copy() }.toMutableList()
        }
        
        fun addRule(rule: CustomPatternRule) {
            rules.add(rule)
            fireTableRowsInserted(rules.size - 1, rules.size - 1)
        }
        
        fun updateRule(index: Int, rule: CustomPatternRule) {
            rules[index] = rule
            fireTableRowsUpdated(index, index)
        }
        
        fun removeRule(index: Int) {
            rules.removeAt(index)
            fireTableRowsDeleted(index, index)
        }
        
        fun getRule(index: Int): CustomPatternRule {
            return rules[index].copy()
        }
    }
}
