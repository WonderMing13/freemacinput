package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger
import com.wonder.freemacinput.freemacinput.config.SettingsState
import java.awt.event.KeyEvent

/**
 *
 * 输入法切换管理器（macOS平台实现）
 */
object InputMethodManager {
    private val logger = Logger.getInstance(InputMethodManager::class.java)

    // 切换冷却时间 - 增加到200ms以减少抖动
    private const val SWITCH_COOLDOWN_MS = 200L
    // 最大重试次数
    private const val MAX_SWITCH_ATTEMPTS = 3
    // 初始重试间隔
    private const val INITIAL_SWITCH_DELAY_MS = 100L
    // 重试间隔乘数（指数退避）
    private const val RETRY_DELAY_MULTIPLIER = 1.5f

    @Volatile
    private var lastSwitchTime: Long = 0
    @Volatile
    private var currentTargetMethod: InputMethodType = InputMethodType.ENGLISH
    @Volatile
    private var lastSwitchedTo: InputMethodType? = null

    private var robot: java.awt.Robot? = null
    private var robotInitSuccess: Boolean = false

    init {
        try {
            robot = java.awt.Robot()
            robot!!.autoDelay = 12
            robotInitSuccess = true
            logger.info("Robot 初始化成功")
        } catch (e: Exception) {
            logger.warn("Robot 初始化失败: ${e.message}", e)
            robotInitSuccess = false
        }
    }

    fun switchTo(method: InputMethodType, settings: SettingsState? = null): Boolean {
        if (method == InputMethodType.AUTO) return true

        logger.info("switchTo: target=$method, robotReady=$robotInitSuccess")

        if (!robotInitSuccess) {
            logger.info("Robot 未初始化，使用 AppleScript 回退")
            return fallbackWithAppleScript(method)
        }

        var success = false
        for (i in 0 until MAX_SWITCH_ATTEMPTS) {
            logger.info("执行切换尝试 ${i + 1}/$MAX_SWITCH_ATTEMPTS (hotkey=${settings?.fallbackHotkey})")
            if (performHotkey(settings?.fallbackHotkey)) {
                success = true
                break
            }
            // 指数退避策略：重试间隔随尝试次数增加而增加
            val delay = (INITIAL_SWITCH_DELAY_MS * Math.pow(RETRY_DELAY_MULTIPLIER.toDouble(), i.toDouble())).toLong()
            Thread.sleep(delay)
        }

        if (success) {
            currentTargetMethod = method
            lastSwitchedTo = method
            lastSwitchTime = System.currentTimeMillis()
            logger.info("切换成功")
        } else {
            logger.warn("切换失败")
        }

        return success
    }

    /**
     * 是否需要执行切换
     * - 目标变化了：立即切换（精准响应）
     * - 目标相同：冷却期内跳过（防抖）
     */
    fun shouldSwitch(targetMethod: InputMethodType): Pair<Boolean, String> {
        if (targetMethod == InputMethodType.AUTO) {
            return false to "AUTO模式，跳过"
        }

        val now = System.currentTimeMillis()

        // 目标变化了？立即切换（精准响应）
        if (targetMethod != lastSwitchedTo) {
            return true to "目标变化，立即切换: $targetMethod"
        }

        // 目标相同，检查冷却期
        if (now - lastSwitchTime < SWITCH_COOLDOWN_MS) {
            return false to "目标相同且在冷却期内，跳过"
        }

        return true to "需要切换"
    }

    private fun performHotkey(hotkey: String?): Boolean {
        val hk = hotkey ?: "CTRL_SPACE"
        return try {
            when (hk) {
                "ALT_SPACE" -> {
                    logger.info("发送 Alt+Space...")
                    robot?.keyPress(KeyEvent.VK_ALT)
                    Thread.sleep(15)
                    robot?.keyPress(KeyEvent.VK_SPACE)
                    Thread.sleep(15)
                    robot?.keyRelease(KeyEvent.VK_SPACE)
                    Thread.sleep(15)
                    robot?.keyRelease(KeyEvent.VK_ALT)
                }
                "CTRL_SHIFT_SPACE" -> {
                    logger.info("发送 Ctrl+Shift+Space...")
                    robot?.keyPress(KeyEvent.VK_CONTROL)
                    robot?.keyPress(KeyEvent.VK_SHIFT)
                    Thread.sleep(15)
                    robot?.keyPress(KeyEvent.VK_SPACE)
                    Thread.sleep(15)
                    robot?.keyRelease(KeyEvent.VK_SPACE)
                    Thread.sleep(15)
                    robot?.keyRelease(KeyEvent.VK_SHIFT)
                    robot?.keyRelease(KeyEvent.VK_CONTROL)
                }
                else -> {
                    logger.info("发送 Ctrl+Space...")
                    robot?.keyPress(KeyEvent.VK_CONTROL)
                    Thread.sleep(15)
                    robot?.keyPress(KeyEvent.VK_SPACE)
                    Thread.sleep(15)
                    robot?.keyRelease(KeyEvent.VK_SPACE)
                    Thread.sleep(15)
                    robot?.keyRelease(KeyEvent.VK_CONTROL)
                }
            }
            // 使用初始延迟确保切换完成
            Thread.sleep(INITIAL_SWITCH_DELAY_MS)
            logger.info("按键发送完成")
            true
        } catch (e: Exception) {
            logger.warn("Robot 异常: ${e.message}", e)
            false
        }
    }

    private fun fallbackWithAppleScript(method: InputMethodType): Boolean {
        return try {
            logger.info("AppleScript 回退: target=$method")

            val script = "tell application \"System Events\" to key code 49 using control down"
            val process = Runtime.getRuntime().exec(arrayOf("osascript", "-e", script))

            // 读取输出和错误流
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()
            logger.info("AppleScript exitCode: $exitCode")
            if (output.isNotBlank()) logger.info("AppleScript output: $output")
            if (error.isNotBlank()) logger.warn("AppleScript error: $error")

            val success = exitCode == 0
            if (success) {
                // 更新状态变量
                currentTargetMethod = method
                lastSwitchedTo = method
                lastSwitchTime = System.currentTimeMillis()
                logger.info("AppleScript 切换成功")
            }
            success
        } catch (e: Exception) {
            logger.error("AppleScript 异常: ${e.message}", e)
            false
        }
    }

}
