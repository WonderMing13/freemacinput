package com.wonder.freemacinput.freemacinput.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.wonder.freemacinput.freemacinput.core.InputMethodType
import com.wonder.freemacinput.freemacinput.core.SwitchStrategy
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.InputMethodDetector
import com.wonder.freemacinput.freemacinput.core.CursorColorManager
import javax.swing.*
import java.awt.*
import java.awt.event.ActionListener

/**
 * 基本设置配置页面
 */
class BasicSettingsConfigurable : Configurable {
    
    private var panel: JPanel? = null
    private var enableCheckBox: JCheckBox? = null
    private var showHintsCheckBox: JCheckBox? = null
    private var enableCaretColorCheckBox: JCheckBox? = null
    private var leaveIDEComboBox: JComboBox<String>? = null
    
    // 新增：输入法选择和切换方案
    private var chineseIMComboBox: JComboBox<String>? = null
    private var englishIMComboBox: JComboBox<String>? = null
    private var switchStrategyComboBox: JComboBox<String>? = null
    
    // 光标颜色配置
    private var chineseCaretColorField: JTextField? = null
    private var englishCaretColorField: JTextField? = null
    private var capsLockCaretColorField: JTextField? = null
    
    override fun getDisplayName(): String = "基本设置"
    
    override fun createComponent(): JComponent {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        var row = 0
        
        // 插件开关
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        enableCheckBox = JCheckBox("启用插件")
        mainPanel.add(enableCheckBox!!, gbc)
        
        // 提示设置
        gbc.gridy = row++
        showHintsCheckBox = JCheckBox("显示切换提示")
        mainPanel.add(showHintsCheckBox!!, gbc)
        
        // 光标颜色设置
        gbc.gridy = row++
        enableCaretColorCheckBox = JCheckBox("启用光标颜色指示")
        mainPanel.add(enableCaretColorCheckBox!!, gbc)
        
        // 光标颜色配置面板
        gbc.gridy = row++
        gbc.gridwidth = 2
        val colorPanel = JPanel(GridBagLayout())
        val colorGbc = GridBagConstraints()
        colorGbc.anchor = GridBagConstraints.WEST
        colorGbc.fill = GridBagConstraints.HORIZONTAL
        colorGbc.insets = Insets(2, 20, 2, 5)
        
        var colorRow = 0
        
        // 中文输入法光标颜色
        colorGbc.gridx = 0
        colorGbc.gridy = colorRow
        colorGbc.gridwidth = 1
        colorPanel.add(JLabel("中文输入法："), colorGbc)
        
        colorGbc.gridx = 1
        chineseCaretColorField = JTextField(8)
        colorPanel.add(chineseCaretColorField!!, colorGbc)
        
        colorGbc.gridx = 2
        val chineseColorPreview = JLabel("   ")
        chineseColorPreview.isOpaque = true
        chineseColorPreview.border = BorderFactory.createLineBorder(Color.BLACK)
        colorPanel.add(chineseColorPreview, colorGbc)
        
        colorGbc.gridx = 3
        val chineseColorButton = JButton("选择")
        chineseColorButton.addActionListener {
            val currentColor = CursorColorManager.parseColor(chineseCaretColorField?.text ?: "EF1616") ?: Color(0xEF, 0x16, 0x16)
            val newColor = JColorChooser.showDialog(mainPanel, "选择中文输入法光标颜色", currentColor)
            if (newColor != null) {
                val hex = String.format("%02X%02X%02X", newColor.red, newColor.green, newColor.blue)
                chineseCaretColorField?.text = hex
            }
        }
        colorPanel.add(chineseColorButton, colorGbc)
        
        // 英文输入法光标颜色
        colorGbc.gridx = 0
        colorGbc.gridy = ++colorRow
        colorPanel.add(JLabel("英文输入法："), colorGbc)
        
        colorGbc.gridx = 1
        englishCaretColorField = JTextField(8)
        colorPanel.add(englishCaretColorField!!, colorGbc)
        
        colorGbc.gridx = 2
        val englishColorPreview = JLabel("   ")
        englishColorPreview.isOpaque = true
        englishColorPreview.border = BorderFactory.createLineBorder(Color.BLACK)
        colorPanel.add(englishColorPreview, colorGbc)
        
        colorGbc.gridx = 3
        val englishColorButton = JButton("选择")
        englishColorButton.addActionListener {
            val currentColor = CursorColorManager.parseColor(englishCaretColorField?.text ?: "DCDCD9") ?: Color(0xDC, 0xDC, 0xD9)
            val newColor = JColorChooser.showDialog(mainPanel, "选择英文输入法光标颜色", currentColor)
            if (newColor != null) {
                val hex = String.format("%02X%02X%02X", newColor.red, newColor.green, newColor.blue)
                englishCaretColorField?.text = hex
            }
        }
        colorPanel.add(englishColorButton, colorGbc)
        
        // 大写锁定光标颜色
        colorGbc.gridx = 0
        colorGbc.gridy = ++colorRow
        colorPanel.add(JLabel("大写锁定："), colorGbc)
        
        colorGbc.gridx = 1
        capsLockCaretColorField = JTextField(8)
        colorPanel.add(capsLockCaretColorField!!, colorGbc)
        
        colorGbc.gridx = 2
        val capsLockColorPreview = JLabel("   ")
        capsLockColorPreview.isOpaque = true
        capsLockColorPreview.border = BorderFactory.createLineBorder(Color.BLACK)
        colorPanel.add(capsLockColorPreview, colorGbc)
        
        colorGbc.gridx = 3
        val capsLockColorButton = JButton("选择")
        capsLockColorButton.addActionListener {
            val currentColor = CursorColorManager.parseColor(capsLockCaretColorField?.text ?: "F6E30E") ?: Color(0xF6, 0xE3, 0x0E)
            val newColor = JColorChooser.showDialog(mainPanel, "选择大写锁定光标颜色", currentColor)
            if (newColor != null) {
                val hex = String.format("%02X%02X%02X", newColor.red, newColor.green, newColor.blue)
                capsLockCaretColorField?.text = hex
            }
        }
        colorPanel.add(capsLockColorButton, colorGbc)
        
        mainPanel.add(colorPanel, gbc)
        
        // 添加颜色预览更新监听器
        val updateColorPreview = { field: JTextField, preview: JLabel ->
            val hex = field.text
            val color = com.wonder.freemacinput.freemacinput.core.CursorColorManager.parseColor(hex)
            if (color != null) {
                preview.background = color
            }
        }
        
        chineseCaretColorField?.document?.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(chineseCaretColorField!!, chineseColorPreview)
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(chineseCaretColorField!!, chineseColorPreview)
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(chineseCaretColorField!!, chineseColorPreview)
        })
        
        englishCaretColorField?.document?.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(englishCaretColorField!!, englishColorPreview)
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(englishCaretColorField!!, englishColorPreview)
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(englishCaretColorField!!, englishColorPreview)
        })
        
        capsLockCaretColorField?.document?.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(capsLockCaretColorField!!, capsLockColorPreview)
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(capsLockCaretColorField!!, capsLockColorPreview)
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateColorPreview(capsLockCaretColorField!!, capsLockColorPreview)
        })
        
        // 分隔线
        gbc.gridy = row++
        mainPanel.add(JSeparator(), gbc)
        
        // 选择中文输入法
        gbc.gridy = row
        gbc.gridwidth = 1
        mainPanel.add(JLabel("选择中文输入法："), gbc)
        
        gbc.gridx = 1
        val chineseIMs = InputMethodDetector.getChineseInputMethods()
        val chineseIMNames = if (chineseIMs.isNotEmpty()) {
            chineseIMs.map { it.displayName }.toTypedArray()
        } else {
            arrayOf("未检测到中文输入法")
        }
        chineseIMComboBox = JComboBox(chineseIMNames)
        mainPanel.add(chineseIMComboBox!!, gbc)
        
        // 选择英文输入法
        gbc.gridx = 0
        gbc.gridy = ++row
        mainPanel.add(JLabel("选择英文输入法："), gbc)
        
        gbc.gridx = 1
        val englishIMs = InputMethodDetector.getEnglishInputMethods()
        val englishIMNames = if (englishIMs.isNotEmpty()) {
            englishIMs.map { it.displayName }.toTypedArray()
        } else {
            arrayOf("未检测到英文输入法")
        }
        englishIMComboBox = JComboBox(englishIMNames)
        mainPanel.add(englishIMComboBox!!, gbc)
        
        // 选择切换方案
        gbc.gridx = 0
        gbc.gridy = ++row
        mainPanel.add(JLabel("选择切换方案："), gbc)
        
        gbc.gridx = 1
        val strategies = SwitchStrategy.values().map { it.getDisplayName() }.toTypedArray()
        switchStrategyComboBox = JComboBox(strategies)
        mainPanel.add(switchStrategyComboBox!!, gbc)
        
        // im-select 状态和安装按钮（仅当选择 im-select 方案时显示）
        gbc.gridx = 0
        gbc.gridy = ++row
        gbc.gridwidth = 2
        val imSelectPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        val imSelectStatusLabel = JLabel()
        val imSelectInstallButton = JButton("安装 im-select")
        val imSelectRefreshButton = JButton("刷新状态")
        imSelectPanel.add(imSelectStatusLabel)
        imSelectPanel.add(imSelectInstallButton)
        imSelectPanel.add(imSelectRefreshButton)
        imSelectPanel.isVisible = false  // 默认隐藏
        mainPanel.add(imSelectPanel, gbc)
        
        // 更新 im-select 状态
        val updateImSelectStatus = {
            val isInstalled = com.wonder.freemacinput.freemacinput.core.ImSelectInstaller.isInstalled()
            if (isInstalled) {
                val path = com.wonder.freemacinput.freemacinput.core.ImSelectInstaller.getPath()
                imSelectStatusLabel.text = "✅ im-select 已安装：$path"
                imSelectInstallButton.isVisible = false
            } else {
                imSelectStatusLabel.text = "❌ im-select 未安装"
                imSelectInstallButton.isVisible = com.wonder.freemacinput.freemacinput.core.ImSelectInstaller.canAutoInstall()
            }
        }
        
        // 切换方案时显示/隐藏 im-select 面板
        switchStrategyComboBox?.addActionListener {
            val selectedStrategy = getSelectedSwitchStrategy()
            imSelectPanel.isVisible = (selectedStrategy == SwitchStrategy.IM_SELECT)
            if (imSelectPanel.isVisible) {
                updateImSelectStatus()
            }
        }
        
        // 安装按钮事件
        imSelectInstallButton.addActionListener {
            imSelectInstallButton.isEnabled = false
            imSelectStatusLabel.text = "⏳ 正在安装 im-select..."
            
            // 在后台线程执行安装
            Thread {
                val result = com.wonder.freemacinput.freemacinput.core.ImSelectInstaller.install()
                val success = result.first
                val message = result.second
                
                // 在 EDT 线程更新 UI
                javax.swing.SwingUtilities.invokeLater {
                    if (success) {
                        updateImSelectStatus()
                        javax.swing.JOptionPane.showMessageDialog(
                            mainPanel,
                            message,
                            "安装成功",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE
                        )
                    } else {
                        imSelectStatusLabel.text = "❌ 安装失败"
                        imSelectInstallButton.isEnabled = true
                        javax.swing.JOptionPane.showMessageDialog(
                            mainPanel,
                            message + "\n\n" + com.wonder.freemacinput.freemacinput.core.ImSelectInstaller.getInstallInstructions(),
                            "安装失败",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }.start()
        }
        
        // 刷新按钮事件
        imSelectRefreshButton.addActionListener {
            updateImSelectStatus()
        }
        
        // 测试切换输入法按钮
        gbc.gridx = 0
        gbc.gridy = ++row
        gbc.gridwidth = 1
        mainPanel.add(JLabel("测试切换输入法："), gbc)
        
        gbc.gridx = 1
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        val testEnglishButton = JButton("切换为英文")
        val testChineseButton = JButton("切换为中文")
        buttonPanel.add(testEnglishButton)
        buttonPanel.add(testChineseButton)
        mainPanel.add(buttonPanel, gbc)
        
        // 测试按钮事件
        testEnglishButton.addActionListener {
            testSwitchInputMethod(InputMethodType.ENGLISH)
        }
        testChineseButton.addActionListener {
            testSwitchInputMethod(InputMethodType.CHINESE)
        }
        
        // 排查输入法切换问题链接
        gbc.gridx = 0
        gbc.gridy = ++row
        gbc.gridwidth = 2
        val troubleshootLink = JLabel("<html><a href=''>排查输入法切换问题</a></html>")
        troubleshootLink.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        troubleshootLink.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                showTroubleshootDialog()
            }
        })
        mainPanel.add(troubleshootLink, gbc)
        
        // 分隔线
        gbc.gridy = ++row
        mainPanel.add(JSeparator(), gbc)
        
        // 离开IDE场景（仅macOS）
        gbc.gridy = ++row
        gbc.gridwidth = 1
        val leaveIDELabel = JLabel("离开IDE场景（仅macOS）：")
        mainPanel.add(leaveIDELabel, gbc)
        
        gbc.gridx = 1
        val leaveStrategies = arrayOf(
            "恢复进入前的输入法",
            "切换为英文",
            "切换为中文",
            "不切换"
        )
        leaveIDEComboBox = JComboBox(leaveStrategies)
        mainPanel.add(leaveIDEComboBox!!, gbc)
        
        // 添加说明文本
        gbc.gridx = 0
        gbc.gridy = ++row
        gbc.gridwidth = 2
        val descLabel = JLabel("<html><i>离开IDE场景：当IDE窗口失去焦点时的输入法切换策略</i></html>")
        descLabel.foreground = Color.GRAY
        mainPanel.add(descLabel, gbc)
        
        // 填充剩余空间
        gbc.gridy = ++row
        gbc.weighty = 1.0
        mainPanel.add(Box.createVerticalGlue(), gbc)
        
        panel = mainPanel
        return mainPanel
    }
    
    /**
     * 测试切换输入法
     */
    private fun testSwitchInputMethod(targetMethod: InputMethodType) {
        val settings = getSettings()
        
        // 根据配置的切换方案和输入法ID进行切换
        val targetId = if (targetMethod == InputMethodType.CHINESE) {
            getSelectedChineseIM()
        } else {
            getSelectedEnglishIM()
        }
        
        val strategy = getSelectedSwitchStrategy()
        
        // 根据切换方案执行不同的切换逻辑
        val result = when (strategy) {
            SwitchStrategy.IM_SELECT -> {
                // 使用 im-select 切换
                testSwitchWithImSelect(targetId)
            }
            SwitchStrategy.STRATEGY_B -> {
                // 使用方案B
                testSwitchWithStrategyB(targetId)
            }
            SwitchStrategy.STRATEGY_C -> {
                // 使用方案C（不需要targetId，使用快捷键）
                testSwitchWithStrategyC()
            }
        }
        
        val message = if (result.success) {
            "切换成功：${result.message}\n目标输入法：$targetId\n切换方案：${strategy.getDisplayName()}"
        } else {
            "切换失败：${result.message}\n目标输入法：$targetId\n切换方案：${strategy.getDisplayName()}"
        }
        
        JOptionPane.showMessageDialog(
            panel,
            message,
            "测试结果",
            if (result.success) JOptionPane.INFORMATION_MESSAGE else JOptionPane.ERROR_MESSAGE
        )
    }
    
    /**
     * 使用 im-select 测试切换
     */
    private fun testSwitchWithImSelect(targetId: String): SwitchResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("im-select", targetId))
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Thread.sleep(200) // 等待切换完成
                SwitchResult(true, "成功切换到 $targetId", InputMethodType.UNKNOWN)
            } else {
                SwitchResult(false, "im-select 返回错误码: $exitCode", InputMethodType.UNKNOWN)
            }
        } catch (e: Exception) {
            SwitchResult(false, "执行失败: ${e.message}", InputMethodType.UNKNOWN)
        }
    }
    
    /**
     * 使用方案A测试切换
     */
    /**
     * 使用方案B测试切换
     */
    private fun testSwitchWithStrategyB(targetId: String): SwitchResult {
        return try {
            val success = com.wonder.freemacinput.freemacinput.core.AdvancedInputMethodSwitcher.switchWithSystemAPI(targetId)
            if (success) {
                SwitchResult(true, "方案B切换成功", InputMethodType.UNKNOWN)
            } else {
                SwitchResult(false, "方案B切换失败", InputMethodType.UNKNOWN)
            }
        } catch (e: Exception) {
            SwitchResult(false, "方案B异常: ${e.message}", InputMethodType.UNKNOWN)
        }
    }
    
    /**
     * 使用方案C测试切换
     */
    private fun testSwitchWithStrategyC(): SwitchResult {
        return try {
            // 使用默认快捷键 Control+Space
            val modifiers = listOf("control")
            val keyCode = 49 // Space key
            
            val success = com.wonder.freemacinput.freemacinput.core.AdvancedInputMethodSwitcher.switchWithShortcut(modifiers, keyCode)
            if (success) {
                SwitchResult(true, "方案C切换成功（使用快捷键 Control+Space）", InputMethodType.UNKNOWN)
            } else {
                SwitchResult(false, "方案C切换失败，请确保已设置\"选择上一个输入法\"快捷键", InputMethodType.UNKNOWN)
            }
        } catch (e: Exception) {
            SwitchResult(false, "方案C异常: ${e.message}", InputMethodType.UNKNOWN)
        }
    }
    
    /**
     * 切换结果
     */
    private data class SwitchResult(
        val success: Boolean,
        val message: String,
        val actualMethod: InputMethodType
    )
    
    /**
     * 显示排查问题对话框
     */
    private fun showTroubleshootDialog() {
        val message = """
            <html>
            <h3>输入法切换问题排查</h3>
            <br>
            <b>推荐方案：</b><br>
            • <b>方案B</b>：最稳定，直接使用系统API切换，可能有轻微延迟<br>
            • <b>方案C</b>：性能最好，需要设置快捷键<br>
            <br>
            <b>方案A（实验性）：</b><br>
            1. 需要授予 <b>IntelliJ IDEA</b> 录屏权限（不是 swift 或其他进程）<br>
            2. 需要授予辅助功能权限<br>
            3. macOS 版本 >= 12.3<br>
            4. 只安装两种输入法<br>
            5. 设置"选择上一个输入法"快捷键<br>
            <br>
            <b>授予 IntelliJ IDEA 录屏权限：</b><br>
            系统设置 -> 隐私与安全性 -> 屏幕录制 -> 添加 IntelliJ IDEA<br>
            <br>
            <b>方案B：</b><br>
            1. 确保已授予 IntelliJ IDEA 辅助功能权限<br>
            2. 如果出现卡顿，请尝试其他方案<br>
            <br>
            <b>方案C：</b><br>
            1. 确保已授予 IntelliJ IDEA 辅助功能权限<br>
            2. 确保已设置"选择上一个输入法"快捷键<br>
            3. 系统设置 -> 键盘 -> 键盘快捷键 -> 输入法<br>
            <br>
            <b>im-select方案：</b><br>
            1. 确保已安装 im-select：brew install im-select<br>
            2. 在终端运行 im-select 测试是否正常<br>
            </html>
        """.trimIndent()
        
        JOptionPane.showMessageDialog(
            panel,
            message,
            "排查输入法切换问题",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    override fun isModified(): Boolean {
        val settings = getSettings()
        return enableCheckBox?.isSelected != settings.isEnabled ||
               showHintsCheckBox?.isSelected != settings.isShowHints ||
               enableCaretColorCheckBox?.isSelected != settings.isEnableCaretColor ||
               chineseCaretColorField?.text != settings.chineseCaretColor ||
               englishCaretColorField?.text != settings.englishCaretColor ||
               capsLockCaretColorField?.text != settings.capsLockCaretColor ||
               getSelectedStrategy() != settings.leaveIDEStrategy ||
               getSelectedSwitchStrategy() != settings.switchStrategy ||
               getSelectedChineseIM() != settings.chineseInputMethodId ||
               getSelectedEnglishIM() != settings.englishInputMethodId
    }
    
    override fun apply() {
        val settings = getSettings()
        settings.isEnabled = enableCheckBox?.isSelected ?: true
        settings.isShowHints = showHintsCheckBox?.isSelected ?: true
        settings.isEnableCaretColor = enableCaretColorCheckBox?.isSelected ?: true
        settings.chineseCaretColor = chineseCaretColorField?.text ?: "EF1616"
        settings.englishCaretColor = englishCaretColorField?.text ?: "DCDCD9"
        settings.capsLockCaretColor = capsLockCaretColorField?.text ?: "F6E30E"
        settings.leaveIDEStrategy = getSelectedStrategy()
        settings.switchStrategy = getSelectedSwitchStrategy()
        settings.chineseInputMethodId = getSelectedChineseIM()
        settings.englishInputMethodId = getSelectedEnglishIM()
    }
    
    override fun reset() {
        val settings = getSettings()
        enableCheckBox?.isSelected = settings.isEnabled
        showHintsCheckBox?.isSelected = settings.isShowHints
        enableCaretColorCheckBox?.isSelected = settings.isEnableCaretColor
        chineseCaretColorField?.text = settings.chineseCaretColor
        englishCaretColorField?.text = settings.englishCaretColor
        capsLockCaretColorField?.text = settings.capsLockCaretColor
        setSelectedStrategy(settings.leaveIDEStrategy)
        setSelectedSwitchStrategy(settings.switchStrategy)
        setSelectedChineseIM(settings.chineseInputMethodId)
        setSelectedEnglishIM(settings.englishInputMethodId)
    }
    
    private fun getSelectedStrategy(): LeaveIDEStrategy {
        return when (leaveIDEComboBox?.selectedIndex) {
            0 -> LeaveIDEStrategy.RESTORE_PREVIOUS
            1 -> LeaveIDEStrategy.ENGLISH
            2 -> LeaveIDEStrategy.NATIVE_LANGUAGE
            3 -> LeaveIDEStrategy.NO_CHANGE
            else -> LeaveIDEStrategy.RESTORE_PREVIOUS
        }
    }
    
    private fun setSelectedStrategy(strategy: LeaveIDEStrategy) {
        leaveIDEComboBox?.selectedIndex = when (strategy) {
            LeaveIDEStrategy.RESTORE_PREVIOUS -> 0
            LeaveIDEStrategy.ENGLISH -> 1
            LeaveIDEStrategy.NATIVE_LANGUAGE -> 2
            LeaveIDEStrategy.NO_CHANGE -> 3
        }
    }
    
    private fun getSelectedSwitchStrategy(): SwitchStrategy {
        return when (switchStrategyComboBox?.selectedIndex) {
            0 -> SwitchStrategy.STRATEGY_B
            1 -> SwitchStrategy.STRATEGY_C
            2 -> SwitchStrategy.IM_SELECT
            else -> SwitchStrategy.IM_SELECT
        }
    }
    
    private fun setSelectedSwitchStrategy(strategy: SwitchStrategy) {
        switchStrategyComboBox?.selectedIndex = when (strategy) {
            SwitchStrategy.STRATEGY_B -> 0
            SwitchStrategy.STRATEGY_C -> 1
            SwitchStrategy.IM_SELECT -> 2
        }
    }
    
    private fun getSelectedChineseIM(): String {
        val selectedName = chineseIMComboBox?.selectedItem as? String ?: return "com.apple.inputmethod.SCIM.Pinyin"
        val chineseIMs = InputMethodDetector.getChineseInputMethods()
        val found = chineseIMs.find { it.displayName == selectedName }
        return found?.id ?: "com.apple.inputmethod.SCIM.Pinyin"
    }
    
    private fun setSelectedChineseIM(imId: String) {
        val chineseIMs = InputMethodDetector.getChineseInputMethods()
        val found = chineseIMs.find { it.id == imId }
        if (found != null) {
            chineseIMComboBox?.selectedItem = found.displayName
        } else {
            // 如果找不到，选择第一个
            if (chineseIMComboBox?.itemCount ?: 0 > 0) {
                chineseIMComboBox?.selectedIndex = 0
            }
        }
    }
    
    private fun getSelectedEnglishIM(): String {
        val selectedName = englishIMComboBox?.selectedItem as? String ?: return "com.apple.keylayout.ABC"
        val englishIMs = InputMethodDetector.getEnglishInputMethods()
        val found = englishIMs.find { it.displayName == selectedName }
        return found?.id ?: "com.apple.keylayout.ABC"
    }
    
    private fun setSelectedEnglishIM(imId: String) {
        val englishIMs = InputMethodDetector.getEnglishInputMethods()
        val found = englishIMs.find { it.id == imId }
        if (found != null) {
            englishIMComboBox?.selectedItem = found.displayName
        } else {
            // 如果找不到，选择第一个
            if (englishIMComboBox?.itemCount ?: 0 > 0) {
                englishIMComboBox?.selectedIndex = 0
            }
        }
    }
    
    private fun getSettings(): SettingsState {
        return ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
