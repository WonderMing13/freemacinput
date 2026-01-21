package com.wonder.freemacinput.freemacinput.ui

import com.wonder.freemacinput.freemacinput.config.CustomPatternRule
import com.wonder.freemacinput.freemacinput.config.MatchStrategy
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import javax.swing.*
import java.awt.*

/**
 * 自定义规则对话框
 */
class CustomPatternRuleDialog(
    parent: Window?,
    private val originalRule: CustomPatternRule?
) : JDialog(parent, if (originalRule == null) "添加自定义规则" else "编辑自定义规则", ModalityType.APPLICATION_MODAL) {
    
    private val enabledCheckBox = JCheckBox("开启", true)
    private val nameField = JTextField(30)
    private val descriptionField = JTextField(30)
    
    // 文件类型
    private val fileTypeCheckBoxes = mutableMapOf<String, JCheckBox>()
    
    // 编辑区域
    private val allAreasCheckBox = JCheckBox("ALL", true)
    private val stringAreaCheckBox = JCheckBox("字符串区域", false)
    private val commentAreaCheckBox = JCheckBox("注释区域", false)
    private val codeAreaCheckBox = JCheckBox("默认区域", false)
    
    // 匹配规则
    private val leftPatternField = JTextField(30)
    private val rightPatternField = JTextField(30)
    
    // 满足条件
    private val matchStrategyComboBox = JComboBox(arrayOf("同时满足", "满足任意一个"))
    
    // 目标输入法
    private val targetInputMethodComboBox = JComboBox(arrayOf("中文", "英文", "大写锁定"))
    
    // 测试区域
    private val testLeftTextField = JTextField(20)
    private val testRightTextField = JTextField(20)
    
    var isOk = false
        private set
    
    init {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        var row = 0
        
        // 开启状态
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        panel.add(enabledCheckBox, gbc)
        
        // 规则名称
        gbc.gridy = row
        gbc.gridwidth = 1
        panel.add(JLabel("规则名称："), gbc)
        
        gbc.gridx = 1
        panel.add(nameField, gbc)
        
        // 描述
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("描述："), gbc)
        
        gbc.gridx = 1
        panel.add(descriptionField, gbc)
        
        // 文件类型
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("文件类型："), gbc)
        
        gbc.gridx = 1
        val fileTypePanel = JPanel(GridLayout(0, 6, 5, 5))
        val fileTypes = listOf("ALL", "java", "kt", "groovy", "py", "php", "xml", "cpp",
                               "c", "go", "html", "js", "ts", "jsx", "tsx", "vue",
                               "css", "yml", "properties", "gitignore", "md", "sql", "rs", "dart", "sc")
        for (type in fileTypes) {
            val checkBox = JCheckBox(type, false)
            fileTypeCheckBoxes[type] = checkBox
            fileTypePanel.add(checkBox)
        }
        panel.add(fileTypePanel, gbc)
        
        // 编辑区域
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("编辑区域："), gbc)
        
        gbc.gridx = 1
        val areaPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        areaPanel.add(allAreasCheckBox)
        areaPanel.add(stringAreaCheckBox)
        areaPanel.add(commentAreaCheckBox)
        areaPanel.add(codeAreaCheckBox)
        panel.add(areaPanel, gbc)
        
        // ALL选中时禁用其他选项
        allAreasCheckBox.addActionListener {
            val enabled = !allAreasCheckBox.isSelected
            stringAreaCheckBox.isEnabled = enabled
            commentAreaCheckBox.isEnabled = enabled
            codeAreaCheckBox.isEnabled = enabled
        }
        
        // 左正则匹配
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("左正则匹配："), gbc)
        
        gbc.gridx = 1
        panel.add(leftPatternField, gbc)
        
        // 右正则匹配
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("右正则匹配："), gbc)
        
        gbc.gridx = 1
        panel.add(rightPatternField, gbc)
        
        // 满足条件
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("满足条件："), gbc)
        
        gbc.gridx = 1
        panel.add(matchStrategyComboBox, gbc)
        
        // 输入法切换为
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("输入法切换为："), gbc)
        
        gbc.gridx = 1
        panel.add(targetInputMethodComboBox, gbc)
        
        // 分隔线
        gbc.gridx = 0
        gbc.gridy = ++row
        gbc.gridwidth = 2
        panel.add(JSeparator(), gbc)
        
        // 测试自定义规则
        gbc.gridy = ++row
        panel.add(JLabel("<html><b>测试自定义规则</b></html>"), gbc)
        
        // 测试左侧文本
        gbc.gridy = ++row
        gbc.gridwidth = 1
        panel.add(JLabel("填试时请输入光标左侧的内容："), gbc)
        
        gbc.gridx = 1
        panel.add(testLeftTextField, gbc)
        
        // 测试右侧文本
        gbc.gridx = 0
        gbc.gridy = ++row
        panel.add(JLabel("填试时请输入光标右侧的内容："), gbc)
        
        gbc.gridx = 1
        panel.add(testRightTextField, gbc)
        
        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        
        val clearButton = JButton("清除")
        clearButton.addActionListener {
            testLeftTextField.text = ""
            testRightTextField.text = ""
        }
        buttonPanel.add(clearButton)
        
        val testButton = JButton("测试")
        testButton.addActionListener {
            testRule()
        }
        buttonPanel.add(testButton)
        
        val okButton = JButton("保存")
        okButton.addActionListener {
            if (validateInput()) {
                isOk = true
                dispose()
            }
        }
        buttonPanel.add(okButton)
        
        val cancelButton = JButton("取消")
        cancelButton.addActionListener {
            dispose()
        }
        buttonPanel.add(cancelButton)
        
        // 加载原始数据
        if (originalRule != null) {
            loadRule(originalRule)
        }
        
        contentPane.layout = BorderLayout()
        contentPane.add(JScrollPane(panel), BorderLayout.CENTER)
        contentPane.add(buttonPanel, BorderLayout.SOUTH)
        
        setSize(800, 700)
        setLocationRelativeTo(parent)
    }
    
    private fun loadRule(rule: CustomPatternRule) {
        enabledCheckBox.isSelected = rule.enabled
        nameField.text = rule.name
        descriptionField.text = rule.description
        
        // 文件类型
        if (rule.fileTypes.isEmpty()) {
            fileTypeCheckBoxes["ALL"]?.isSelected = true
        } else {
            for (type in rule.fileTypes) {
                fileTypeCheckBoxes[type]?.isSelected = true
            }
        }
        
        // 编辑区域
        allAreasCheckBox.isSelected = rule.applyToAllAreas
        stringAreaCheckBox.isSelected = rule.applyToStringArea
        commentAreaCheckBox.isSelected = rule.applyToCommentArea
        codeAreaCheckBox.isSelected = rule.applyToCodeArea
        
        leftPatternField.text = rule.leftPattern
        rightPatternField.text = rule.rightPattern
        matchStrategyComboBox.selectedIndex = if (rule.matchStrategy == MatchStrategy.BOTH) 0 else 1
        targetInputMethodComboBox.selectedIndex = when (rule.targetInputMethod) {
            InputMethodType.CHINESE -> 0
            InputMethodType.ENGLISH -> 1
            InputMethodType.CAPS_LOCK -> 2
            else -> 1
        }
    }
    
    private fun validateInput(): Boolean {
        if (nameField.text.isBlank()) {
            JOptionPane.showMessageDialog(this, "规则名称不能为空", "错误", JOptionPane.ERROR_MESSAGE)
            return false
        }
        
        if (leftPatternField.text.isBlank() && rightPatternField.text.isBlank()) {
            JOptionPane.showMessageDialog(this, "左右匹配规则至少填写一个", "错误", JOptionPane.ERROR_MESSAGE)
            return false
        }
        
        // 验证正则表达式
        try {
            if (leftPatternField.text.isNotBlank()) {
                leftPatternField.text.toRegex()
            }
            if (rightPatternField.text.isNotBlank()) {
                rightPatternField.text.toRegex()
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "正则表达式格式错误：${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
            return false
        }
        
        return true
    }
    
    private fun testRule() {
        val leftText = testLeftTextField.text
        val rightText = testRightTextField.text
        
        if (leftText.isBlank() && rightText.isBlank()) {
            JOptionPane.showMessageDialog(this, "请输入测试文本", "提示", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        
        try {
            val leftMatches = if (leftPatternField.text.isBlank()) true else {
                leftPatternField.text.toRegex().matches(leftText)
            }
            
            val rightMatches = if (rightPatternField.text.isBlank()) true else {
                rightPatternField.text.toRegex().matches(rightText)
            }
            
            val strategy = if (matchStrategyComboBox.selectedIndex == 0) MatchStrategy.BOTH else MatchStrategy.EITHER
            val finalMatch = when (strategy) {
                MatchStrategy.BOTH -> leftMatches && rightMatches
                MatchStrategy.EITHER -> leftMatches || rightMatches
            }
            
            val message = """
                左侧匹配：${if (leftMatches) "✓ 匹配" else "✗ 不匹配"}
                右侧匹配：${if (rightMatches) "✓ 匹配" else "✗ 不匹配"}
                满足条件：${strategy.displayName}
                最终结果：${if (finalMatch) "✓ 规则生效" else "✗ 规则不生效"}
            """.trimIndent()
            
            JOptionPane.showMessageDialog(
                this,
                message,
                "测试结果",
                if (finalMatch) JOptionPane.INFORMATION_MESSAGE else JOptionPane.WARNING_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "测试失败：${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
        }
    }
    
    fun getRule(): CustomPatternRule {
        // 获取选中的文件类型
        val selectedFileTypes = mutableListOf<String>()
        if (fileTypeCheckBoxes["ALL"]?.isSelected != true) {
            for ((type, checkBox) in fileTypeCheckBoxes) {
                if (type != "ALL" && checkBox.isSelected) {
                    selectedFileTypes.add(type)
                }
            }
        }
        
        return CustomPatternRule(
            enabled = enabledCheckBox.isSelected,
            name = nameField.text.trim(),
            description = descriptionField.text.trim(),
            fileTypes = selectedFileTypes,
            applyToAllAreas = allAreasCheckBox.isSelected,
            applyToStringArea = stringAreaCheckBox.isSelected,
            applyToCommentArea = commentAreaCheckBox.isSelected,
            applyToCodeArea = codeAreaCheckBox.isSelected,
            leftPattern = leftPatternField.text.trim(),
            rightPattern = rightPatternField.text.trim(),
            matchStrategy = if (matchStrategyComboBox.selectedIndex == 0) MatchStrategy.BOTH else MatchStrategy.EITHER,
            targetInputMethod = when (targetInputMethodComboBox.selectedIndex) {
                0 -> InputMethodType.CHINESE
                1 -> InputMethodType.ENGLISH
                2 -> InputMethodType.CAPS_LOCK
                else -> InputMethodType.ENGLISH
            }
        )
    }
}
