package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader

object InputMethodManager {
    private val logger = Logger.getInstance(InputMethodManager::class.java)

    private val osName = System.getProperty("os.name", "").lowercase()
    private val isMacOS = osName.contains("mac")
    private val isWindows = osName.contains("win")

    // 切换冷却时间（避免频繁切换）
    private const val SWITCH_COOLDOWN_MS = 300L

    @Volatile
    private var lastSwitchTime: Long = 0

    @Volatile
    private var lastSwitchedTo: InputMethodType? = null

    // 记录当前实际输入法状态
    @Volatile
    private var currentActualMethod: InputMethodType = InputMethodType.ENGLISH

    // 缓存检测到的当前输入法
    @Volatile
    private var cachedCurrentIM: String? = null
    @Volatile
    private var cacheTime: Long = 0
    private const val CACHE_DURATION_MS = 500L
    
    // 标记是否是插件自动切换（用于区分手动切换）
    @Volatile
    private var isAutoSwitching: Boolean = false
    private var autoSwitchEndTime: Long = 0

    // macOS 输入法 ID 配置
    private var macChineseIMId: String? = null
    private var macEnglishIMId: String? = null

    // Windows 输入法 Locale ID 配置
    private var winChineseLocale: String? = null
    private var winEnglishLocale: String? = null

    data class SwitchResult(
        val success: Boolean,
        val message: String,
        val actualMethod: InputMethodType
    )

    /**
     * 配置 macOS 输入法 ID
     */
    fun configureMacInputMethods(chineseId: String?, englishId: String?) {
        macChineseIMId = chineseId
        macEnglishIMId = englishId
        logger.info("配置 macOS 输入法: 中文=$chineseId, 英文=$englishId")
    }

    /**
     * 配置 Windows 输入法 Locale
     */
    fun configureWindowsInputMethods(chineseLocale: String?, englishLocale: String?) {
        winChineseLocale = chineseLocale
        winEnglishLocale = englishLocale
        logger.info("配置 Windows 输入法: 中文=$chineseLocale, 英文=$englishLocale")
    }

    /**
     * 初始化输入法管理器
     */
    fun initialize(): Boolean {
        return when {
            isMacOS -> initializeMacOS()
            isWindows -> initializeWindows()
            else -> {
                logger.warn("不支持的操作系统: $osName")
                false
            }
        }
    }

    private fun initializeMacOS(): Boolean {
        logger.info("初始化 macOS 输入法管理器")
        // 检测当前输入法
        val current = detectCurrentInputMethod()
        logger.info("当前输入法: $current")
        return true
    }

    private fun initializeWindows(): Boolean {
        logger.info("初始化 Windows 输入法管理器")
        // 检测当前输入法
        val current = detectCurrentInputMethod()
        logger.info("当前输入法: $current")
        return true
    }

    /**
     * 切换到指定输入法
     */
    fun switchTo(method: InputMethodType, settings: com.wonder.freemacinput.freemacinput.config.SettingsState?): SwitchResult {
        logger.info("========================================")
        logger.info("   switchTo 被调用: method=$method")
        logger.info("========================================")
        
        // 标记为自动切换，持续1秒
        isAutoSwitching = true
        autoSwitchEndTime = System.currentTimeMillis() + 1000

        if (!isMacOS && !isWindows) {
            val result = SwitchResult(false, "不支持当前操作系统", InputMethodType.AUTO)
            logger.warn("不支持的操作系统: $osName")
            return result
        }

        if (method == InputMethodType.AUTO) {
            val result = SwitchResult(true, "AUTO模式，跳过切换", InputMethodType.AUTO)
            logger.info("AUTO 模式")
            return result
        }

        // 从配置中获取输入法ID
        if (settings != null) {
            macChineseIMId = settings.chineseInputMethodId
            macEnglishIMId = settings.englishInputMethodId
            logger.info("使用配置的输入法ID: 中文=$macChineseIMId, 英文=$macEnglishIMId")
        }

        // 冷却时间检查
        val now = System.currentTimeMillis()
        if (now - lastSwitchTime < SWITCH_COOLDOWN_MS && lastSwitchedTo == method) {
            logger.info("冷却中，跳过切换（距上次切换: ${now - lastSwitchTime}ms）")
            return SwitchResult(true, "冷却中", currentActualMethod)
        }

        // 检测当前输入法
        val currentIM = detectCurrentInputMethod()
        val isChinese = isChineseInputMethod(currentIM)
        val currentType = if (isChinese) InputMethodType.CHINESE else InputMethodType.ENGLISH

        // 如果目标与当前相同，不需要切换
        if (currentType == method) {
            logger.info("目标与当前相同，无需切换: $method")
            currentActualMethod = method
            return SwitchResult(true, "已经是${if (method == InputMethodType.CHINESE) "中文" else "英文"}输入法", method)
        }

        logger.info("🎯 开始切换: $currentType → $method")

        // 根据切换方案执行切换
        val strategy = settings?.switchStrategy ?: SwitchStrategy.IM_SELECT
        logger.info("使用切换方案: ${strategy.getDisplayName()}")

        val success = when (strategy) {
            SwitchStrategy.IM_SELECT -> {
                when {
                    isMacOS -> switchMacOS(method)
                    isWindows -> switchWindows(method)
                    else -> false
                }
            }
            SwitchStrategy.STRATEGY_B -> {
                logger.info("使用方案B：系统API切换")
                if (isMacOS) {
                    switchWithStrategyB(method)
                } else {
                    logger.warn("方案B仅支持 macOS")
                    false
                }
            }
            SwitchStrategy.STRATEGY_C -> {
                logger.info("使用方案C：API识别 + 快捷键")
                if (isMacOS) {
                    switchWithStrategyC(method, settings)
                } else {
                    logger.warn("方案C仅支持 macOS")
                    false
                }
            }
        }

        if (success) {
            lastSwitchTime = now
            lastSwitchedTo = method
            currentActualMethod = method
            cachedCurrentIM = null // 清除缓存
            val message = "成功切换为${if (method == InputMethodType.CHINESE) "中文" else "英文"}输入法"
            logger.info("✅ $message")
            return SwitchResult(true, message, method)
        } else {
            val message = "切换到${if (method == InputMethodType.CHINESE) "中文" else "英文"}失败"
            logger.error("❌ $message")
            return SwitchResult(false, message, currentActualMethod)
        }
    }

    /**
     * macOS 输入法切换
     */
    private fun switchMacOS(method: InputMethodType): Boolean {
        // 检查 im-select 是否可用
        if (!isImSelectAvailable()) {
            logger.error("im-select 工具未安装，请运行: brew install im-select")
            return false
        }

        // 获取目标输入法ID
        val targetId = if (method == InputMethodType.CHINESE) macChineseIMId else macEnglishIMId

        // 如果配置了具体的输入法ID，使用精确切换
        if (targetId != null && targetId.isNotEmpty()) {
            logger.info("使用配置的输入法ID切换到: $targetId")
            val success = executeCommand("im-select", targetId)
            if (success) {
                // 切换后等待足够长的时间，让系统完全完成切换
                Thread.sleep(200)
                logger.info("输入法切换完成，已等待 200ms")
            }
            return success
        }

        // 如果没有配置，使用自动检测切换
        logger.info("未配置输入法ID，使用自动检测切换")
        val success = switchMacOSWithImSelectAuto(method == InputMethodType.CHINESE)
        if (success) {
            Thread.sleep(200)
            logger.info("输入法切换完成，已等待 200ms")
        }
        return success
    }
    
    /**
     * 使用 im-select 自动检测并切换
     */
    private fun switchMacOSWithImSelectAuto(toChinese: Boolean): Boolean {
        // 如果要切换到中文，先检查是否有中文输入法
        if (toChinese) {
            // 尝试常见的中文输入法 ID（按优先级排序）
            val chineseIMIds = listOf(
                "com.apple.inputmethod.SCIM.Pinyin",      // 拼音
                "com.apple.inputmethod.SCIM.ITABC",       // 五笔
                "com.apple.inputmethod.SCIM.Shuangpin",   // 双拼
                "com.apple.inputmethod.SCIM.Wubi",        // 五笔（另一种）
                "com.apple.inputmethod.TCIM.Cangjie",     // 仓颉
                "com.apple.inputmethod.TCIM.Zhuyin",      // 注音
                "com.sogou.inputmethod.sogou",            // 搜狗
                "com.baidu.inputmethod.BaiduIM"           // 百度
            )
            
            for (imId in chineseIMIds) {
                if (executeCommand("im-select", imId)) {
                    logger.info("✅ 切换到中文输入法: $imId")
                    return true
                }
            }
            
            logger.warn("未找到可用的中文输入法，请在系统设置中添加中文输入法")
            return false
        } else {
            // 切换到英文
            val englishIMIds = listOf(
                "com.apple.keylayout.ABC",
                "com.apple.keylayout.US",
                "com.apple.keylayout.USExtended"
            )
            
            for (imId in englishIMIds) {
                if (executeCommand("im-select", imId)) {
                    logger.info("✅ 切换到英文输入法: $imId")
                    return true
                }
            }
            
            logger.warn("未找到可用的英文输入法")
            return false
        }
    }

    /**
     * Windows 输入法切换
     */
    private fun switchWindows(method: InputMethodType): Boolean {
        val targetLocale = if (method == InputMethodType.CHINESE) winChineseLocale else winEnglishLocale

        // 方法1: 使用 PowerShell InputLanguage（推荐）
        if (targetLocale != null) {
            logger.info("使用 PowerShell 切换到 Locale: $targetLocale")
            return switchWindowsWithPowerShell(targetLocale)
        }

        // 方法2: 使用默认 Locale ID
        logger.info("使用默认 Locale ID 切换")
        val defaultLocale = if (method == InputMethodType.CHINESE) "0804" else "0409"
        return switchWindowsWithPowerShell(defaultLocale)
    }

    /**
     * 使用 PowerShell 切换 Windows 输入法
     */
    private fun switchWindowsWithPowerShell(localeId: String): Boolean {
        val script = """
            Add-Type -AssemblyName System.Windows.Forms
            ${'$'}lang = [System.Windows.Forms.InputLanguage]::InstalledInputLanguages | Where-Object { ${'$'}_.Culture.LCID -eq 0x$localeId }
            if (${'$'}lang) {
                [System.Windows.Forms.InputLanguage]::CurrentInputLanguage = ${'$'}lang
                exit 0
            } else {
                exit 1
            }
        """.trimIndent()

        return executePowerShell(script)
    }

    /**
     * 获取当前输入法类型
     * 用于监听用户主动切换输入法的行为
     */
    fun getCurrentInputMethod(): InputMethodType {
        val currentIM = detectCurrentInputMethod()
        
        return when {
            currentIM == "unknown" -> InputMethodType.UNKNOWN
            isChineseInputMethod(currentIM) -> InputMethodType.CHINESE
            else -> InputMethodType.ENGLISH
        }
    }

    /**
     * 获取当前输入法的完整ID/名称
     * 用于离开IDE场景的输入法恢复
     */
    fun getCurrentInputMethodName(): String {
        return detectCurrentInputMethod()
    }

    /**
     * 检测当前输入法
     */
    private fun detectCurrentInputMethod(): String {
        // 检查缓存
        val now = System.currentTimeMillis()
        if (cachedCurrentIM != null && now - cacheTime < CACHE_DURATION_MS) {
            return cachedCurrentIM!!
        }

        val result = when {
            isMacOS -> detectMacOSInputMethod()
            isWindows -> detectWindowsInputMethod()
            else -> "unknown"
        }

        cachedCurrentIM = result
        cacheTime = now
        return result
    }

    /**
     * 检测 macOS 当前输入法
     */
    private fun detectMacOSInputMethod(): String {
        // 使用 im-select
        if (isImSelectAvailable()) {
            val result = executeCommandWithOutput("im-select")
            if (result != null && result.isNotEmpty()) {
                logger.info("im-select 检测到: $result")
                return result.trim()
            }
        }
        
        logger.warn("无法检测当前输入法，请安装 im-select: brew install im-select")
        return "unknown"
    }

    /**
     * 检测 Windows 当前输入法
     */
    private fun detectWindowsInputMethod(): String {
        val script = """
            Add-Type -AssemblyName System.Windows.Forms
            [System.Windows.Forms.InputLanguage]::CurrentInputLanguage.Culture.DisplayName
        """.trimIndent()

        return executePowerShellWithOutput(script) ?: "unknown"
    }

    /**
     * 判断是否为中文输入法
     */
    private fun isChineseInputMethod(imName: String): Boolean {
        val chineseKeywords = listOf(
            "中文", "拼音", "简体", "繁体", "五笔", "仓颉",
            "Chinese", "Pinyin", "Simplified", "Traditional", "Wubi", "Cangjie",
            "SCIM", "TCIM", "0804", "0404"
        )
        return chineseKeywords.any { imName.contains(it, ignoreCase = true) }
    }

    /**
     * 检查 im-select 是否可用
     */
    private fun isImSelectAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "im-select"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 执行命令
     */
    private fun executeCommand(vararg command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            logger.error("执行命令失败: ${command.joinToString(" ")}", e)
            false
        }
    }

    /**
     * 执行命令并获取输出
     */
    private fun executeCommandWithOutput(vararg command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText().trim()
            process.waitFor()
            output
        } catch (e: Exception) {
            logger.error("执行命令失败: ${command.joinToString(" ")}", e)
            null
        }
    }

    /**
     * 执行 PowerShell
     */
    private fun executePowerShell(script: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script
            ))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            logger.error("执行 PowerShell 失败", e)
            false
        }
    }

    /**
     * 执行 PowerShell 并获取输出
     */
    private fun executePowerShellWithOutput(script: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script
            ))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText().trim()
            process.waitFor()
            output
        } catch (e: Exception) {
            logger.error("执行 PowerShell 失败", e)
            null
        }
    }

    fun shouldSwitch(targetMethod: InputMethodType): Pair<Boolean, String> {
        if (targetMethod == InputMethodType.AUTO) {
            return false to "AUTO模式，跳过"
        }

        // 检测当前输入法
        val currentIM = detectCurrentInputMethod()
        val isChinese = isChineseInputMethod(currentIM)
        val currentType = if (isChinese) InputMethodType.CHINESE else InputMethodType.ENGLISH

        // 如果目标与当前相同，不需要切换
        if (currentType == targetMethod) {
            return false to "当前已经是$targetMethod"
        }

        return true to "需要切换到 $targetMethod"
    }

    fun clearCache() {
        lastSwitchTime = 0
        lastSwitchedTo = null
        currentActualMethod = InputMethodType.ENGLISH
        cachedCurrentIM = null
        cacheTime = 0
    }

    fun getCurrentMethod(): InputMethodType {
        val currentIM = detectCurrentInputMethod()
        val isChinese = isChineseInputMethod(currentIM)
        return if (isChinese) InputMethodType.CHINESE else InputMethodType.ENGLISH
    }

    fun setCurrentMethod(method: InputMethodType) {
        currentActualMethod = method
        logger.info("手动设置当前输入法: $method")
    }
    
    /**
     * 方案B：使用系统API直接切换
     */
    private fun switchWithStrategyB(method: InputMethodType): Boolean {
        val targetId = if (method == InputMethodType.CHINESE) macChineseIMId else macEnglishIMId
        
        if (targetId == null || targetId.isEmpty()) {
            logger.error("方案B：未配置目标输入法ID")
            return false
        }
        
        return AdvancedInputMethodSwitcher.switchWithSystemAPI(targetId)
    }
    
    /**
     * 方案C：使用系统API识别 + 快捷键切换
     */
    private fun switchWithStrategyC(method: InputMethodType, settings: com.wonder.freemacinput.freemacinput.config.SettingsState?): Boolean {
        // 首先检查当前输入法
        val currentId = AdvancedInputMethodSwitcher.getCurrentInputMethodId()
        if (currentId == null) {
            logger.error("方案C：无法获取当前输入法")
            return false
        }
        
        val isChinese = AdvancedInputMethodSwitcher.isChineseInputMethod(currentId)
        val currentType = if (isChinese) InputMethodType.CHINESE else InputMethodType.ENGLISH
        
        // 如果已经是目标输入法，不需要切换
        if (currentType == method) {
            logger.info("方案C：当前已是目标输入法")
            return true
        }
        
        // 使用快捷键切换
        // 默认使用 Control+Space (keyCode 49)
        // 用户可以在配置中自定义
        val modifiers = listOf("control")
        val keyCode = 49 // Space key
        
        return AdvancedInputMethodSwitcher.switchWithShortcut(modifiers, keyCode)
    }
    
    /**
     * 检查当前是否是插件自动切换
     * 如果在自动切换后的1秒内，认为是自动切换
     */
    fun isAutoSwitching(): Boolean {
        val now = System.currentTimeMillis()
        if (now > autoSwitchEndTime) {
            isAutoSwitching = false
        }
        return isAutoSwitching
    }

}

