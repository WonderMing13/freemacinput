package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 输入法检测器
 * 检测系统中已安装的输入法
 */
object InputMethodDetector {
    
    private val logger = Logger.getInstance(InputMethodDetector::class.java)
    private val osName = System.getProperty("os.name", "").lowercase()
    private val isMacOS = osName.contains("mac")
    
    data class InputMethodInfo(
        val id: String,
        val displayName: String,
        val isChinese: Boolean
    )
    
    /**
     * 获取系统中所有已安装的输入法
     */
    fun getInstalledInputMethods(): List<InputMethodInfo> {
        return when {
            isMacOS -> getMacOSInputMethods()
            else -> emptyList()
        }
    }
    
    /**
     * 获取 macOS 系统中的输入法列表
     */
    private fun getMacOSInputMethods(): List<InputMethodInfo> {
        val inputMethods = mutableListOf<InputMethodInfo>()
        
        try {
            // 方法1: 使用 Swift 脚本获取所有输入法（最准确）
            val swiftResult = getInputMethodsViaSwift()
            if (swiftResult.isNotEmpty()) {
                inputMethods.addAll(swiftResult)
                logger.info("通过 Swift API 检测到 ${swiftResult.size} 个输入法")
            }
            
            // 方法2: 如果方法1失败，读取系统配置文件
            if (inputMethods.isEmpty()) {
                logger.info("Swift API 失败，尝试读取配置文件")
                val process = Runtime.getRuntime().exec(arrayOf(
                    "defaults", "read",
                    System.getProperty("user.home") + "/Library/Preferences/com.apple.HIToolbox.plist",
                    "AppleEnabledInputSources"
                ))
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                
                logger.info("系统配置输出:\n$output")
                
                // 解析输出
                parseInputSourcesOutput(output, inputMethods)
            }
            
            // 方法3: 如果前两个方法都失败，尝试使用 im-select 检测当前输入法
            if (inputMethods.isEmpty() && isImSelectAvailable()) {
                val currentIM = executeCommandWithOutput("im-select")
                if (currentIM != null && currentIM.isNotEmpty()) {
                    val info = parseInputMethodId(currentIM.trim())
                    inputMethods.add(info)
                    logger.info("通过 im-select 检测到当前输入法: ${info.displayName}")
                }
            }
            
            logger.info("最终检测到 ${inputMethods.size} 个输入法")
            inputMethods.forEach {
                logger.info("  - ${it.displayName} (${it.id})")
            }
            
        } catch (e: Exception) {
            logger.error("检测输入法列表失败", e)
            return getDefaultMacOSInputMethods()
        }
        
        return inputMethods.ifEmpty { getDefaultMacOSInputMethods() }
    }
    
    /**
     * 使用 Swift API 获取输入法列表
     */
    private fun getInputMethodsViaSwift(): List<InputMethodInfo> {
        val inputMethods = mutableListOf<InputMethodInfo>()
        val seenIds = mutableSetOf<String>()
        
        try {
            // 创建临时 Swift 脚本
            val swiftScript = """
                import Carbon
                
                let inputSources = TISCreateInputSourceList(nil, false).takeRetainedValue() as! [TISInputSource]
                
                for source in inputSources {
                    if let sourceID = TISGetInputSourceProperty(source, kTISPropertyInputSourceID) {
                        let id = Unmanaged<CFString>.fromOpaque(sourceID).takeUnretainedValue() as String
                        
                        // 只输出键盘输入法和输入模式
                        if let category = TISGetInputSourceProperty(source, kTISPropertyInputSourceCategory) {
                            let cat = Unmanaged<CFString>.fromOpaque(category).takeUnretainedValue() as String
                            if cat == "TISCategoryKeyboardInputSource" || cat == "TISCategoryInputMode" {
                                if let sourceName = TISGetInputSourceProperty(source, kTISPropertyLocalizedName) {
                                    let name = Unmanaged<CFString>.fromOpaque(sourceName).takeUnretainedValue() as String
                                    print("\(id)|\(name)")
                                } else {
                                    print("\(id)|")
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            
            // 写入临时文件
            val tempFile = java.io.File.createTempFile("list_input_sources", ".swift")
            tempFile.writeText(swiftScript)
            tempFile.deleteOnExit()
            
            // 执行 Swift 脚本
            val process = Runtime.getRuntime().exec(arrayOf("swift", tempFile.absolutePath))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            reader.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size >= 2) {
                    val id = parts[0].trim()
                    val systemName = parts[1].trim()
                    
                    // 过滤掉一些不需要的输入法
                    if (!id.contains("CharacterPalette") && 
                        !id.contains("PressAndHold") && 
                        !id.contains("ironwood") &&
                        !id.contains("Emoji")) {
                        
                        // 过滤掉不完整的 Bundle ID（没有具体输入模式的）
                        // 例如：com.apple.inputmethod.SCIM 应该被过滤，保留 com.apple.inputmethod.SCIM.ITABC
                        val isIncompleteBundleId = (id == "com.apple.inputmethod.SCIM" || 
                                                    id == "com.apple.inputmethod.TCIM" ||
                                                    id.endsWith(".inputmethod") ||
                                                    (id.contains("inputmethod") && !id.contains(".SCIM.") && !id.contains(".TCIM.") && !id.contains("sogou") && !id.contains("baidu")))
                        
                        if (!isIncompleteBundleId) {
                            // 避免重复：使用规范化的 ID
                            val normalizedId = normalizeInputMethodId(id)
                            
                            if (!seenIds.contains(normalizedId)) {
                                seenIds.add(normalizedId)
                                
                                // 使用友好的中文名称
                                val info = parseInputMethodId(id, systemName)
                                inputMethods.add(info)
                            }
                        }
                    }
                }
            }
            
            process.waitFor()
            
        } catch (e: Exception) {
            logger.error("使用 Swift API 获取输入法失败", e)
        }
        
        return inputMethods
    }
    
    /**
     * 规范化输入法 ID，用于去重
     * 例如：com.sogou.inputmethod.sogou.pinyin 和 com.sogou.inputmethod.sogou 视为同一个
     */
    private fun normalizeInputMethodId(id: String): String {
        return when {
            id.contains("sogou") -> "com.sogou.inputmethod.sogou"
            id.contains("SCIM.ITABC") -> "com.apple.inputmethod.SCIM.ITABC"
            id.contains("SCIM.Pinyin") -> "com.apple.inputmethod.SCIM.Pinyin"
            id.contains("SCIM") && !id.contains(".SCIM.") -> "com.apple.inputmethod.SCIM"
            else -> id
        }
    }
    
    /**
     * 解析输入法 ID，生成友好的显示名称（带系统名称参考）
     */
    private fun parseInputMethodId(id: String, systemName: String = ""): InputMethodInfo {
        val displayName = when {
            // 第三方输入法（优先级最高）
            id.contains("sogou", ignoreCase = true) -> "搜狗拼音"
            id.contains("baidu", ignoreCase = true) -> "百度输入法"
            id.contains("QQInput", ignoreCase = true) -> "QQ输入法"
            id.contains("iFlyIME", ignoreCase = true) -> "讯飞输入法"
            
            // Apple 系统输入法
            id.contains("SCIM.Pinyin", ignoreCase = true) -> "简体拼音"
            id.contains("SCIM.Shuangpin", ignoreCase = true) -> "简体双拼"
            id.contains("SCIM.Wubi", ignoreCase = true) -> "简体五笔"
            id.contains("SCIM.ITABC", ignoreCase = true) -> "简体五笔"
            id.contains("TCIM.Cangjie", ignoreCase = true) -> "繁体仓颉"
            id.contains("TCIM.Zhuyin", ignoreCase = true) -> "繁体注音"
            id.contains("TCIM.Pinyin", ignoreCase = true) -> "繁体拼音"
            id.contains("SCIM", ignoreCase = true) && !id.contains(".SCIM.") -> "简体中文"
            id.contains("TCIM", ignoreCase = true) && !id.contains(".TCIM.") -> "繁体中文"
            
            // 英文键盘布局
            id.contains("ABC", ignoreCase = true) -> "ABC"
            id.contains("keylayout.US", ignoreCase = true) && !id.contains("Extended") -> "US"
            id.contains("USExtended", ignoreCase = true) -> "US Extended"
            id.contains("British", ignoreCase = true) -> "British"
            id.contains("Australian", ignoreCase = true) -> "Australian"
            id.contains("Canadian", ignoreCase = true) -> "Canadian"
            
            // 其他语言
            id.contains("Japanese", ignoreCase = true) -> "日语"
            id.contains("Korean", ignoreCase = true) -> "韩语"
            
            // 如果有系统名称，使用系统名称
            systemName.isNotEmpty() -> systemName
            
            // 未知输入法，使用 ID 的最后一部分
            else -> {
                val parts = id.split(".")
                parts.lastOrNull()?.replace("_", " ") ?: id
            }
        }
        
        val isChinese = isChineseInputMethod(id)
        
        return InputMethodInfo(id, displayName, isChinese)
    }
    
    /**
     * 解析 AppleEnabledInputSources 的输出
     */
    private fun parseInputSourcesOutput(output: String, inputMethods: MutableList<InputMethodInfo>) {
        try {
            // 查找所有的输入法ID
            // 格式1: "KeyboardLayout Name" = ABC;
            val keyboardLayoutPattern = """"KeyboardLayout Name"\s*=\s*([^;]+);""".toRegex()
            keyboardLayoutPattern.findAll(output).forEach { match ->
                val name = match.groupValues[1].trim()
                // 构造标准的输入法ID
                val id = "com.apple.keylayout.$name"
                val info = parseInputMethodId(id)
                if (!inputMethods.any { it.id == info.id }) {
                    inputMethods.add(info)
                }
            }
            
            // 格式2: "Input Mode" = "com.apple.inputmethod.SCIM.ITABC";
            val inputModePattern = """"Input Mode"\s*=\s*"([^"]+)"""".toRegex()
            inputModePattern.findAll(output).forEach { match ->
                val id = match.groupValues[1].trim()
                val info = parseInputMethodId(id)
                if (!inputMethods.any { it.id == info.id }) {
                    inputMethods.add(info)
                }
            }
            
            // 格式3: "Bundle ID" = "com.apple.inputmethod.SCIM"; (需要结合 InputSourceKind)
            val bundleIdPattern = """"Bundle ID"\s*=\s*"([^"]+)"""".toRegex()
            val kindPattern = """InputSourceKind\s*=\s*"([^"]+)"""".toRegex()
            
            // 分块解析（每个 {} 是一个输入源）
            val blocks = output.split("    },").filter { it.contains("Bundle ID") }
            blocks.forEach { block ->
                val bundleMatch = bundleIdPattern.find(block)
                val kindMatch = kindPattern.find(block)
                
                if (bundleMatch != null && kindMatch != null) {
                    val bundleId = bundleMatch.groupValues[1].trim()
                    val kind = kindMatch.groupValues[1].trim()
                    
                    // 只处理键盘输入法
                    if (kind == "Keyboard Input Method" || kind == "Input Mode") {
                        // 检查是否已经通过 Input Mode 添加过
                        val inputModeMatch = inputModePattern.find(block)
                        if (inputModeMatch == null) {
                            // 没有具体的 Input Mode，使用 Bundle ID
                            val info = parseInputMethodId(bundleId)
                            if (!inputMethods.any { it.id == info.id }) {
                                inputMethods.add(info)
                            }
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            logger.error("解析输入法配置失败", e)
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
     * 判断是否为中文输入法
     */
    private fun isChineseInputMethod(id: String): Boolean {
        val chineseKeywords = listOf(
            "SCIM", "TCIM", "Pinyin", "Wubi", "Cangjie", "Zhuyin", "Shuangpin", "ITABC",
            "sogou", "baidu", "QQInput", "iFlyIME",
            "Chinese", "中文", "简体", "繁体"
        )
        return chineseKeywords.any { id.contains(it, ignoreCase = true) }
    }
    
    /**
     * 获取默认的 macOS 输入法列表（当无法检测时使用）
     */
    private fun getDefaultMacOSInputMethods(): List<InputMethodInfo> {
        return listOf(
            InputMethodInfo("com.apple.keylayout.ABC", "ABC", false),
            InputMethodInfo("com.apple.keylayout.US", "US", false),
            InputMethodInfo("com.apple.inputmethod.SCIM.Pinyin", "简体拼音", true)
        )
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
     * 获取中文输入法列表
     */
    fun getChineseInputMethods(): List<InputMethodInfo> {
        return getInstalledInputMethods().filter { it.isChinese }
    }
    
    /**
     * 获取英文输入法列表
     */
    fun getEnglishInputMethods(): List<InputMethodInfo> {
        return getInstalledInputMethods().filter { !it.isChinese }
    }
}
