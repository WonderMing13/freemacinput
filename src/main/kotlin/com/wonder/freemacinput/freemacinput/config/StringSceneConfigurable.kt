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
 * 字符串场景配置页面
 */
class StringSceneConfigurable : Configurable {

    private var settingsState: SettingsState? = null
    private var mainPanel: JPanel? = null
    private var enableRescueCombo: JComboBox<String>? = null
    private var defaultMethodCombo: JComboBox<String>? = null
    private var tableModel: StringSceneTableModel? = null
    private var table: JTable? = null

    override fun getDisplayName(): String = "字符串场景"

    override fun createComponent(): JComponent {
        if (mainPanel != null) return mainPanel!!

        settingsState = ApplicationManager.getApplication().getService(SettingsState::class.java)

        mainPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        }

        // 顶部面板
        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            // 补救功能
            val rescuePanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("从英文切换到中文输入法时补救之前的输入："))
                enableRescueCombo = JComboBox(arrayOf("开启", "关闭")).apply {
                    preferredSize = Dimension(100, 25)
                }
                add(enableRescueCombo)
            }
            add(rescuePanel)
            add(Box.createVerticalStrut(15))
            
            // 默认输入法
            val defaultPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("输入字符串字面量时默认切换输入法为："))
                defaultMethodCombo = JComboBox(arrayOf("英文", "中文")).apply {
                    preferredSize = Dimension(100, 25)
                }
                add(defaultMethodCombo)
            }
            add(defaultPanel)
            add(Box.createVerticalStrut(10))
            
            // 说明文字
            add(JLabel("说明：").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                font = font.deriveFont(12f).deriveFont(java.awt.Font.BOLD)
            })
            add(JLabel("• 插件会自动记录您在字符串中的输入法切换习惯").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                font = font.deriveFont(12f)
            })
            add(JLabel("• 自动记录的习惯会显示在下方表格中，来源列显示为\"自动记录\"").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                font = font.deriveFont(12f)
            })
            add(JLabel("• 手动添加的规则来源列显示为\"手动配置\"").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                font = font.deriveFont(12f)
            })
            add(Box.createVerticalStrut(15))
        }

        // 表格 - 整合规则和习惯
        tableModel = StringSceneTableModel()
        table = JTable(tableModel).apply {
            fillsViewportHeight = true
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            rowHeight = 25
        }
        val scrollPane = JScrollPane(table)

        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("添加规则").apply {
                addActionListener {
                    tableModel?.addRule(StringSceneRule.createDefault())
                }
            })
            add(JButton("删除选中").apply {
                addActionListener {
                    val selectedRow = table?.selectedRow ?: -1
                    if (selectedRow >= 0) {
                        tableModel?.removeRow(selectedRow)
                    }
                }
            })
            add(Box.createHorizontalStrut(20))
            add(JButton("清空自动记录").apply {
                addActionListener {
                    val result = JOptionPane.showConfirmDialog(
                        mainPanel,
                        "确定要清空所有自动记录的习惯吗？\n手动配置的规则不会被删除。",
                        "确认",
                        JOptionPane.YES_NO_OPTION
                    )
                    if (result == JOptionPane.YES_OPTION) {
                        settingsState?.stringSceneHabits?.clear()
                        tableModel?.refresh()
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
        return (enableRescueCombo?.selectedItem == "开启") != state.enableStringRescue ||
                getMethodFromCombo(defaultMethodCombo) != state.stringMethod ||
                tableModel?.getRules() != state.stringSceneRules
    }

    override fun apply() {
        val state = settingsState ?: return
        state.enableStringRescue = enableRescueCombo?.selectedItem == "开启"
        state.stringMethod = getMethodFromCombo(defaultMethodCombo)
        state.stringSceneRules.clear()
        state.stringSceneRules.addAll(tableModel?.getRules() ?: emptyList())
    }

    override fun reset() {
        val state = settingsState ?: return
        enableRescueCombo?.selectedItem = if (state.enableStringRescue) "开启" else "关闭"
        setMethodToCombo(defaultMethodCombo, state.stringMethod)
        tableModel?.setData(state.stringSceneRules, state.stringSceneHabits)
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
     * 整合的表格模型 - 同时显示规则和习惯
     */
    private inner class StringSceneTableModel : AbstractTableModel() {
        private val rules = mutableListOf<StringSceneRule>()
        private val columnNames = arrayOf("编程语言", "表达式/变量名", "默认切换为", "来源")

        override fun getRowCount(): Int {
            val habitsCount = settingsState?.stringSceneHabits?.size ?: 0
            return rules.size + habitsCount
        }
        
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            // 只有手动规则可以编辑，且来源列不可编辑
            return rowIndex < rules.size && columnIndex < 3
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            // 前面是手动规则，后面是自动习惯
            if (rowIndex < rules.size) {
                val rule = rules[rowIndex]
                return when (columnIndex) {
                    0 -> rule.language
                    1 -> rule.expression
                    2 -> when (rule.defaultInputMethod) {
                        InputMethodType.ENGLISH -> "英文"
                        InputMethodType.CHINESE -> "中文"
                        InputMethodType.UNKNOWN -> "未知"
                        else -> "自动"
                    }
                    3 -> "手动配置"
                    else -> ""
                }
            } else {
                // 自动习惯
                val habits = settingsState?.stringSceneHabits ?: return ""
                val habitIndex = rowIndex - rules.size
                if (habitIndex >= habits.size) return ""
                
                val habit = habits[habitIndex]
                return when (columnIndex) {
                    0 -> habit.language
                    1 -> habit.expression
                    2 -> when (habit.preferredInputMethod) {
                        InputMethodType.ENGLISH -> "英文"
                        InputMethodType.CHINESE -> "中文"
                        else -> "自动"
                    }
                    3 -> "自动记录"
                    else -> ""
                }
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (rowIndex >= rules.size) return // 习惯不可编辑
            
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

        fun removeRow(index: Int) {
            if (index < rules.size) {
                // 删除手动规则
                rules.removeAt(index)
                fireTableRowsDeleted(index, index)
            } else {
                // 删除自动习惯
                val habits = settingsState?.stringSceneHabits
                if (habits != null) {
                    val habitIndex = index - rules.size
                    if (habitIndex >= 0 && habitIndex < habits.size) {
                        habits.removeAt(habitIndex)
                        fireTableRowsDeleted(index, index)
                    }
                }
            }
        }

        fun setData(newRules: List<StringSceneRule>, habits: List<StringSceneHabit>) {
            rules.clear()
            rules.addAll(newRules.map { it.copy() })
            fireTableDataChanged()
        }

        fun getRules(): List<StringSceneRule> = rules.map { it.copy() }
        
        fun refresh() {
            fireTableDataChanged()
        }
    }
}
