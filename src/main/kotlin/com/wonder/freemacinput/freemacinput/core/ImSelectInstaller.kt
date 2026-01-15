package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * im-select 安装器
 * 自动检测和安装 im-select 工具
 */
object ImSelectInstaller {
    
    private val logger = Logger.getInstance(ImSelectInstaller::class.java)
    
    private val osName = System.getProperty("os.name", "").lowercase()
    private val isMacOS = osName.contains("mac")
    private val isWindows = osName.contains("win")
    private val isLinux = osName.contains("linux")
    
    /**
     * 检查 im-select 是否已安装
     */
    fun isInstalled(): Boolean {
        return try {
            val command = if (isWindows) "where im-select" else "which im-select"
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            logger.warn("检查 im-select 失败", e)
            false
        }
    }
    
    /**
     * 获取 im-select 的路径
     */
    fun getPath(): String? {
        return try {
            val command = if (isWindows) "where im-select" else "which im-select"
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val path = reader.readLine()
            process.waitFor()
            path?.trim()
        } catch (e: Exception) {
            logger.warn("获取 im-select 路径失败", e)
            null
        }
    }
    
    /**
     * 检查是否可以自动安装
     */
    fun canAutoInstall(): Boolean {
        return when {
            isMacOS -> isHomebrewInstalled()
            isWindows -> false  // Windows 需要手动下载
            isLinux -> false    // Linux 需要手动下载
            else -> false
        }
    }
    
    /**
     * 检查 Homebrew 是否已安装（macOS）
     */
    private fun isHomebrewInstalled(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("which brew")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 自动安装 im-select
     * 返回：(成功, 消息)
     */
    fun install(): Pair<Boolean, String> {
        return when {
            isMacOS -> installOnMacOS()
            isWindows -> Pair(false, "Windows 系统请手动下载 im-select.exe\n下载地址：https://github.com/daipeihust/im-select/releases")
            isLinux -> Pair(false, "Linux 系统请手动下载 im-select\n下载地址：https://github.com/daipeihust/im-select/releases")
            else -> Pair(false, "不支持的操作系统")
        }
    }
    
    /**
     * 在 macOS 上通过 Homebrew 安装
     */
    private fun installOnMacOS(): Pair<Boolean, String> {
        if (!isHomebrewInstalled()) {
            return Pair(false, "未检测到 Homebrew，请先安装 Homebrew\n安装命令：/bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\"")
        }
        
        return try {
            logger.info("开始通过 Homebrew 安装 im-select...")
            
            // 执行安装命令
            val process = Runtime.getRuntime().exec(arrayOf("brew", "install", "im-select"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
                logger.info("brew output: $line")
            }
            
            val error = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
                logger.warn("brew error: $line")
            }
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.info("✅ im-select 安装成功")
                Pair(true, "✅ im-select 安装成功！\n路径：${getPath() ?: "未知"}")
            } else {
                logger.error("❌ im-select 安装失败，退出码：$exitCode")
                Pair(false, "❌ 安装失败\n错误信息：\n$error")
            }
        } catch (e: Exception) {
            logger.error("安装 im-select 异常", e)
            Pair(false, "❌ 安装异常：${e.message}")
        }
    }
    
    /**
     * 获取安装说明
     */
    fun getInstallInstructions(): String {
        return when {
            isMacOS -> """
                macOS 安装方法：
                1. 自动安装（推荐）：点击下方"安装 im-select"按钮
                2. 手动安装：在终端执行 brew install im-select
                
                如果没有 Homebrew，请先安装：
                /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            """.trimIndent()
            
            isWindows -> """
                Windows 安装方法：
                1. 访问 https://github.com/daipeihust/im-select/releases
                2. 下载 im-select-win.exe
                3. 重命名为 im-select.exe
                4. 将文件放到系统 PATH 路径中（如 C:\Windows\System32）
                   或者放到任意目录，然后添加到系统环境变量 PATH
            """.trimIndent()
            
            isLinux -> """
                Linux 安装方法：
                1. 访问 https://github.com/daipeihust/im-select/releases
                2. 下载对应架构的 im-select 文件
                3. 添加执行权限：chmod +x im-select
                4. 移动到 PATH 路径：sudo mv im-select /usr/local/bin/
            """.trimIndent()
            
            else -> "不支持的操作系统"
        }
    }
    
    /**
     * 获取下载链接
     */
    fun getDownloadUrl(): String {
        return "https://github.com/daipeihust/im-select/releases"
    }
}
