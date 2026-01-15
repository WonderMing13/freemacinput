package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 高级输入法切换器
 * 实现方案B和方案C
 */
object AdvancedInputMethodSwitcher {
    
    private val logger = Logger.getInstance(AdvancedInputMethodSwitcher::class.java)
    
    /**
     * 方案B：使用系统API直接切换
     */
    fun switchWithSystemAPI(targetId: String): Boolean {
        return try {
            logger.info("方案B：使用系统API切换到 $targetId")
            
            // 优先使用编译好的可执行文件
            val executable = getCompiledExecutable()
            val result = if (executable != null && executable.exists() && executable.canExecute()) {
                logger.info("使用编译好的可执行文件")
                executeCompiledCommand(executable, "switch", targetId)
            } else {
                logger.warn("未找到编译好的可执行文件，使用 swift 解释器")
                val swiftScript = getSwiftScript()
                executeSwiftCommand(swiftScript, "switch", targetId)
            }
            
            if (result?.contains("SUCCESS") == true) {
                logger.info("✅ 方案B切换成功")
                // 等待90ms让UI更新
                Thread.sleep(90)
                true
            } else {
                logger.error("❌ 方案B切换失败: $result")
                false
            }
        } catch (e: Exception) {
            logger.error("方案B切换异常", e)
            false
        }
    }
    
    /**
     * 方案C：使用系统API识别 + 快捷键切换
     */
    fun switchWithShortcut(modifiers: List<String>, keyCode: Int): Boolean {
        return try {
            logger.info("方案C：使用快捷键切换 (modifiers=$modifiers, keyCode=$keyCode)")
            
            // 优先使用编译好的可执行文件
            val executable = getCompiledExecutable()
            val modifiersStr = modifiers.joinToString(",")
            val result = if (executable != null && executable.exists() && executable.canExecute()) {
                logger.info("使用编译好的可执行文件")
                executeCompiledCommand(executable, "shortcut", modifiersStr, keyCode.toString())
            } else {
                logger.warn("未找到编译好的可执行文件，使用 swift 解释器")
                val swiftScript = getSwiftScript()
                executeSwiftCommand(swiftScript, "shortcut", modifiersStr, keyCode.toString())
            }
            
            if (result?.contains("SUCCESS") == true) {
                logger.info("✅ 方案C切换成功")
                // 等待200ms让系统完成切换
                Thread.sleep(200)
                true
            } else {
                logger.error("❌ 方案C切换失败: $result")
                false
            }
        } catch (e: Exception) {
            logger.error("方案C切换异常", e)
            false
        }
    }
    
    /**
     * 获取当前输入法ID（使用系统API）
     */
    fun getCurrentInputMethodId(): String? {
        return try {
            // 优先使用编译好的可执行文件
            val executable = getCompiledExecutable()
            val result = if (executable != null && executable.exists() && executable.canExecute()) {
                executeCompiledCommand(executable, "current")
            } else {
                val swiftScript = getSwiftScript()
                executeSwiftCommand(swiftScript, "current")
            }
            result?.trim()
        } catch (e: Exception) {
            logger.error("获取当前输入法ID失败", e)
            null
        }
    }
    
    /**
     * 判断输入法是否为中文
     */
    fun isChineseInputMethod(id: String): Boolean {
        return try {
            // 优先使用编译好的可执行文件
            val executable = getCompiledExecutable()
            val result = if (executable != null && executable.exists() && executable.canExecute()) {
                executeCompiledCommand(executable, "isChinese", id)
            } else {
                val swiftScript = getSwiftScript()
                executeSwiftCommand(swiftScript, "isChinese", id)
            }
            result?.trim() == "true"
        } catch (e: Exception) {
            logger.error("判断输入法类型失败", e)
            false
        }
    }
    
    /**
     * 获取 Swift 脚本内容
     */
    private fun getSwiftScript(): String {
        val resource = javaClass.classLoader.getResourceAsStream("InputMethodSwitcher.swift")
        return resource?.bufferedReader()?.use { it.readText() } 
            ?: throw IllegalStateException("无法加载 InputMethodSwitcher.swift")
    }
    
    /**
     * 执行 Swift 命令
     */
    private fun executeSwiftCommand(script: String, vararg args: String): String? {
        return try {
            // 首先尝试使用编译好的可执行文件
            val compiledExecutable = getCompiledExecutable()
            if (compiledExecutable != null && compiledExecutable.exists() && compiledExecutable.canExecute()) {
                logger.info("使用编译好的可执行文件: ${compiledExecutable.absolutePath}")
                return executeCompiledCommand(compiledExecutable, *args)
            }
            
            // 如果没有编译好的文件，回退到使用 swift 解释器
            logger.warn("未找到编译好的可执行文件，使用 swift 解释器")
            
            // 创建临时文件
            val tempFile = java.io.File.createTempFile("input_method_switcher", ".swift")
            tempFile.writeText(script)
            tempFile.deleteOnExit()
            
            // 构建命令
            val command = mutableListOf("swift", tempFile.absolutePath)
            command.addAll(args)
            
            logger.info("执行命令: ${command.joinToString(" ")}")
            
            // 执行命令
            val process = Runtime.getRuntime().exec(command.toTypedArray())
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = reader.readText().trim()
            val error = errorReader.readText().trim()
            
            val exitCode = process.waitFor()
            
            if (error.isNotEmpty()) {
                logger.warn("Swift 脚本错误输出: $error")
            }
            
            if (exitCode != 0) {
                logger.error("Swift 脚本执行失败，退出码: $exitCode")
                return null
            }
            
            output
        } catch (e: Exception) {
            logger.error("执行 Swift 命令失败", e)
            null
        }
    }
    
    /**
     * 获取编译好的可执行文件
     */
    private fun getCompiledExecutable(): java.io.File? {
        return try {
            // 尝试从 resources/bin 目录加载
            val resource = javaClass.classLoader.getResource("bin/InputMethodSwitcher")
            if (resource != null) {
                // 如果是 jar 包内的资源，需要提取到临时目录
                if (resource.protocol == "jar") {
                    val tempFile = java.io.File.createTempFile("InputMethodSwitcher", "")
                    tempFile.deleteOnExit()
                    
                    javaClass.classLoader.getResourceAsStream("bin/InputMethodSwitcher")?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // 设置可执行权限
                    tempFile.setExecutable(true)
                    logger.info("从 jar 包提取可执行文件到: ${tempFile.absolutePath}")
                    return tempFile
                } else {
                    // 如果是文件系统中的资源，直接使用
                    val file = java.io.File(resource.toURI())
                    logger.info("找到编译好的可执行文件: ${file.absolutePath}")
                    return file
                }
            }
            
            // 尝试从开发环境的路径加载
            val devPath = java.io.File("src/main/resources/bin/InputMethodSwitcher")
            if (devPath.exists()) {
                logger.info("找到开发环境的可执行文件: ${devPath.absolutePath}")
                return devPath
            }
            
            logger.warn("未找到编译好的可执行文件")
            null
        } catch (e: Exception) {
            logger.error("获取编译好的可执行文件失败", e)
            null
        }
    }
    
    /**
     * 执行编译好的可执行文件
     */
    private fun executeCompiledCommand(executable: java.io.File, vararg args: String): String? {
        return try {
            // 构建命令
            val command = mutableListOf(executable.absolutePath)
            command.addAll(args)
            
            logger.info("执行编译好的命令: ${command.joinToString(" ")}")
            
            // 执行命令
            val process = Runtime.getRuntime().exec(command.toTypedArray())
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = reader.readText().trim()
            val error = errorReader.readText().trim()
            
            val exitCode = process.waitFor()
            
            if (error.isNotEmpty()) {
                logger.warn("可执行文件错误输出: $error")
            }
            
            if (exitCode != 0) {
                logger.error("可执行文件执行失败，退出码: $exitCode")
                return null
            }
            
            output
        } catch (e: Exception) {
            logger.error("执行编译好的命令失败", e)
            null
        }
    }
    
    /**
     * 检查是否有辅助功能权限
     */
    fun checkAccessibilityPermission(): Boolean {
        return try {
            val script = """
                tell application "System Events"
                    return true
                end tell
            """.trimIndent()
            
            val process = Runtime.getRuntime().exec(arrayOf("osascript", "-e", script))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            logger.error("检查辅助功能权限失败", e)
            false
        }
    }
}
