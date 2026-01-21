package com.wonder.freemacinput.freemacinput.ui

import com.wonder.freemacinput.freemacinput.config.StringSceneRule
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.AbstractTableModel

/**
 * 字符串场景规则配置面板
 */
class StringSceneRulePanel : JPanel() {
    
    private val tableModel = StringSceneRuleTableModel()
    private val table = JTable(tableModel)
    
    init {
        layout = BorderLayout()
        
        // 创建表格
        table.apply {
            fillsViewportHeight = true
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        }
        
        val scrollPane = JScrollPane(table).apply {
            preferredSize = Dimension(600, 200)
        }
        
        // 创建按钮面板
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            
            add(JButton("添加").apply {
                addActionListener {
                    tableModel.addRule(StringSceneRule.createDefault())
                }
            })
            
            add(Box.createHorizontalStrut(5))
            
            add(JButton("删除").apply {
                addActionListener {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        tableModel.removeRule(selectedRow)
                    }
                }
            })
            
            add(Box.createHorizontalGlue())
        }
        
        add(scrollPane, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }
    
    fun setRules(rules: List<StringSceneRule>) {
        tableModel.setRules(rules)
    }
    
    fun getRules(): List<StringSceneRule> {
        return tableModel.getRules()
    }
    
    /**
     * 字符串场景规则表格模型
     */
    private class StringSceneRuleTableModel : AbstractTableModel() {
        
        private val rules = mutableListOf<StringSceneRule>()
        private val columnNames = arrayOf("编程语言", "表达式", "默认输入法")
        
        override fun getRowCount(): Int = rules.size
        
        override fun getColumnCount(): Int = columnNames.size
        
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val rule = rules[rowIndex]
            return when (columnIndex) {
                0 -> rule.language
                1 -> rule.expression
                2 -> when (rule.defaultInputMethod) {
                    InputMethodType.ENGLISH -> "英文"
                    InputMethodType.CHINESE -> "中文"
                    InputMethodType.AUTO -> "自动"
                    InputMethodType.UNKNOWN -> "未知"
                    InputMethodType.CAPS_LOCK -> "大写锁定"
                }
                else -> ""
            }
        }
        
        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val rule = rules[rowIndex]
            when (columnIndex) {
                0 -> rule.language = aValue as String
                1 -> rule.expression = aValue as String
                2 -> rule.defaultInputMethod = when (aValue as String) {
                    "英文" -> InputMethodType.ENGLISH
                    "中文" -> InputMethodType.CHINESE
                    else -> InputMethodType.AUTO
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }
        
        fun addRule(rule: StringSceneRule) {
            rules.add(rule)
            fireTableRowsInserted(rules.size - 1, rules.size - 1)
        }
        
        fun removeRule(index: Int) {
            if (index >= 0 && index < rules.size) {
                rules.removeAt(index)
                fireTableRowsDeleted(index, index)
            }
        }
        
        fun setRules(newRules: List<StringSceneRule>) {
            rules.clear()
            rules.addAll(newRules.map { it.copy() })
            fireTableDataChanged()
        }
        
        fun getRules(): List<StringSceneRule> {
            return rules.map { it.copy() }
        }
    }
}
