package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.table.AbstractTableModel

/**
 * 默认场景配置页面
 */
class DefaultSceneConfigurable : Configurable {

    private var settingsState: SettingsState? = null
    private var mainPanel: JPanel? = null
    private var defaultMethodCombo: JComboBox<String>? = null
    private var tableModel: FileTypeTableModel? = null
    private var table: JTable? = null

    override fun getDisplayName(): String = "默认场景"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        settingsState = ApplicationManager.getApplication().getService(SettingsState::class.java)

        mainPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        }

        // 顶部面板
        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            // 全局默认设置
            val defaultPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("全局默认代码区域输入法："))
                defaultMethodCombo = JComboBox(arrayOf("英文", "中文")).apply {
                    preferredSize = Dimension(100, 25)
                }
                add(defaultMethodCombo)
            }
            add(defaultPanel)
            add(Box.createVerticalStrut(15))
            
            // 说明文字
            add(JLabel("文件类型规则（优先级高于全局默认）：").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                font = font.deriveFont(14f).deriveFont(java.awt.Font.BOLD)
            })
            add(Box.createVerticalStrut(5))
            add(JLabel("配置特定文件类型的默认输入法，例如：java、kt、py 等").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                font = font.deriveFont(12f)
            })
            add(Box.createVerticalStrut(15))
        }

        // 表格
        tableModel = FileTypeTableModel()
        table = JTable(tableModel).apply {
            fillsViewportHeight = true
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            rowHeight = 25
        }
        val scrollPane = JScrollPane(table)

        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("添加").apply {
                addActionListener {
                    tableModel?.addRule(FileTypeRule.createDefault())
                }
            })
            add(JButton("删除").apply {
                addActionListener {
                    val selectedRow = table?.selectedRow ?: -1
                    if (selectedRow >= 0) {
                        tableModel?.removeRule(selectedRow)
                    }
                }
            })
        }

        mainPanel?.add(topPanel, BorderLayout.NORTH)
        mainPanel?.add(scrollPane, BorderLayout.CENTER)
        mainPanel?.add(buttonPanel, BorderLayout.SOUTH)

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val state = settingsState ?: return false
        return getMethodFromCombo(defaultMethodCombo) != state.defaultMethod ||
                tableModel?.getRules() != state.fileTypeRules
    }

    override fun apply() {
        val state = settingsState ?: return
        state.defaultMethod = getMethodFromCombo(defaultMethodCombo)
        state.fileTypeRules.clear()
        state.fileTypeRules.addAll(tableModel?.getRules() ?: emptyList())
    }

    override fun reset() {
        val state = settingsState ?: return
        setMethodToCombo(defaultMethodCombo, state.defaultMethod)
        tableModel?.setRules(state.fileTypeRules)
    }

    private fun getMethodFromCombo(combo: JComboBox<String>?): InputMethodType {
        return when (combo?.selectedItem as? String) {
            "英文" -> InputMethodType.ENGLISH
            "中文" -> InputMethodType.CHINESE
            else -> InputMethodType.ENGLISH
        }
    }

    private fun setMethodToCombo(combo: JComboBox<String>?, method: InputMethodType) {
        combo?.selectedItem = when (method) {
            InputMethodType.ENGLISH -> "英文"
            InputMethodType.CHINESE -> "中文"
            else -> "英文"
        }
    }

    /**
     * 文件类型表格模型
     */
    private class FileTypeTableModel : AbstractTableModel() {
        private val rules = mutableListOf<FileTypeRule>()
        private val columnNames = arrayOf("开启自动切换输入法", "文件类型", "默认区域切换")

        override fun getRowCount(): Int = rules.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> java.lang.Boolean::class.java
                else -> String::class.java
            }
        }
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val rule = rules[rowIndex]
            return when (columnIndex) {
                0 -> rule.enabled
                1 -> rule.fileType
                2 -> when (rule.defaultInputMethod) {
                    InputMethodType.ENGLISH -> "英文"
                    InputMethodType.CHINESE -> "中文"
                    else -> "自动"
                }
                else -> ""
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val rule = rules[rowIndex]
            when (columnIndex) {
                0 -> rule.enabled = aValue as Boolean
                1 -> rule.fileType = aValue as String
                2 -> rule.defaultInputMethod = when (aValue as String) {
                    "英文" -> InputMethodType.ENGLISH
                    "中文" -> InputMethodType.CHINESE
                    else -> InputMethodType.AUTO
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }

        fun addRule(rule: FileTypeRule) {
            rules.add(rule)
            fireTableRowsInserted(rules.size - 1, rules.size - 1)
        }

        fun removeRule(index: Int) {
            if (index >= 0 && index < rules.size) {
                rules.removeAt(index)
                fireTableRowsDeleted(index, index)
            }
        }

        fun setRules(newRules: List<FileTypeRule>) {
            rules.clear()
            rules.addAll(newRules.map { it.copy() })
            fireTableDataChanged()
        }

        fun getRules(): List<FileTypeRule> = rules.map { it.copy() }
    }
}
