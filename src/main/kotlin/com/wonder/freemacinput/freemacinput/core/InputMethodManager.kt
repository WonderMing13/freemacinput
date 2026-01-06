package com.wonder.freemacinput.freemacinput.core

import com.intellij.openapi.diagnostic.Logger
import com.wonder.freemacinput.freemacinput.config.SettingsState
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.awt.Toolkit
import java.awt.event.KeyEvent

/**
 *
 * 输入法切换管理器（macOS平台实现）
 */
object InputMethodManager {
    private val logger = Logger.getInstance(InputMethodManager::class.java)

    private const val SWITCH_COOLDOWN_MS = 160L
    private const val MAX_SWITCH_ATTEMPTS = 3
    private const val SWITCH_DELAY_MS = 170L

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

        // 优先尝试使用 im-select 进行精确切换（如果配置与工具可用且偏好启用）
        if (settings != null && settings.preferImSelect) {
            val targetBundleId = when (method) {
                InputMethodType.CHINESE -> settings.chineseInputSource
                else -> settings.englishInputSource
            }

            if (targetBundleId.isNotBlank()) {
                val imOk = switchByImSelect(targetBundleId)
                if (imOk) {
                    currentTargetMethod = method
                    lastSwitchedTo = method
                    lastSwitchTime = System.currentTimeMillis()
                    logger.info("切换成功（im-select） -> $targetBundleId")
                    return true
                } else {
                    logger.info("im-select 切换未成功，回退到按键模拟")
                }
            } else {
                logger.info("目标Bundle ID为空，跳过 im-select，回退到按键模拟")
            }
        }

        if (!robotInitSuccess) {
            logger.info("Robot 未初始化，使用 AppleScript 回退")
            return fallbackWithAppleScript()
        }

        var success = false
        for (i in 0 until MAX_SWITCH_ATTEMPTS) {
            logger.info("执行切换尝试 ${i + 1}/$MAX_SWITCH_ATTEMPTS (fallback=${settings?.fallbackHotkey})")
            if (performHotkey(settings?.fallbackHotkey)) {
                success = true
                break
            }
            Thread.sleep(SWITCH_DELAY_MS)
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
     * 是否需要执行切换，结合冷却期与最近目标来避免抖动
     */
    fun shouldSwitch(targetMethod: InputMethodType): Pair<Boolean, String> {
        if (targetMethod == InputMethodType.AUTO) {
            return false to "AUTO模式，跳过"
        }

        val now = System.currentTimeMillis()
        if (now - lastSwitchTime < SWITCH_COOLDOWN_MS) {
            return false to "冷却期内，跳过"
        }

        if (targetMethod == lastSwitchedTo && targetMethod == currentTargetMethod) {
            return false to "与最近目标相同，跳过"
        }

        return true to "需要切换"
    }

    // ============== im-select 精确切换支持 ==============
    private fun findImSelectPath(): String? {
        try {
            val candidates = listOf(
                "/opt/homebrew/bin/im-select",
                "/usr/local/bin/im-select",
                "/usr/bin/im-select"
            )
            for (c in candidates) {
                if (File(c).exists()) {
                    logger.info("检测到 im-select: $c")
                    return c
                }
            }

            val whichProc = Runtime.getRuntime().exec(arrayOf("/usr/bin/which", "im-select"))
            val out = BufferedReader(InputStreamReader(whichProc.inputStream)).readLine()
            val exit = whichProc.waitFor()
            if (exit == 0 && out != null && out.isNotBlank()) {
                val p = out.trim()
                logger.info("which 找到 im-select: $p")
                return p
            } else {
                logger.info("which 未找到 im-select（exit=$exit）")
            }
        } catch (e: Exception) {
            logger.warn("检测 im-select 异常: ${e.message}", e)
        }
        return null
    }

    private fun switchByImSelect(bundleId: String): Boolean {
        val path = findImSelectPath() ?: run {
            logger.info("im-select 未安装或未找到，跳过")
            return false
        }
        return try {
            logger.info("使用 im-select 切换到: $bundleId ($path)")
            val process = Runtime.getRuntime().exec(arrayOf(path, bundleId))
            // 避免长时间阻塞：最多等待 600ms（更稳）
            val finished = process.waitFor(600, java.util.concurrent.TimeUnit.MILLISECONDS)
            val exitCode = if (finished) process.exitValue() else -1
            if (!finished) {
                logger.warn("im-select 在超时内未结束，尝试销毁进程并回退")
                process.destroy()
            }
            logger.info("im-select exitCode: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            logger.warn("im-select 调用异常: ${e.message}", e)
            false
        }
    }

    private fun performCtrlSpace(): Boolean {
        return try {
            logger.info("发送 Ctrl+Space...")

            robot?.keyPress(KeyEvent.VK_CONTROL)
            Thread.sleep(15)

            robot?.keyPress(KeyEvent.VK_SPACE)
            Thread.sleep(15)

            robot?.keyRelease(KeyEvent.VK_SPACE)
            Thread.sleep(15)

            robot?.keyRelease(KeyEvent.VK_CONTROL)

            Thread.sleep(SWITCH_DELAY_MS)

            logger.info("按键发送完成")
            true
        } catch (e: Exception) {
            logger.warn("Robot 异常: ${e.message}", e)
            fallbackWithAppleScript()
        }
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
            Thread.sleep(SWITCH_DELAY_MS)
            logger.info("按键发送完成")
            true
        } catch (e: Exception) {
            logger.warn("Robot 异常: ${e.message}", e)
            fallbackWithAppleScript()
        }
    }

    private fun fallbackWithAppleScript(): Boolean {
        return try {
            logger.info("AppleScript 回退...")

            val script = "tell application \"System Events\" to key code 49 using control down"
            val process = Runtime.getRuntime().exec(arrayOf("osascript", "-e", script))
            val exitCode = process.waitFor()
            logger.info("AppleScript exitCode: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            logger.error("AppleScript 异常: ${e.message}", e)
            false
        }
    }

    fun getCapsLockState(): CapsLockState {
        return try {
            val toolkit = Toolkit.getDefaultToolkit()
            val capsLockOn = toolkit.getLockingKeyState(KeyEvent.VK_CAPS_LOCK)
            if (capsLockOn) CapsLockState.ON else CapsLockState.OFF
        } catch (e: Exception) {
            logger.warn("获取 CapsLock 状态异常: ${e.message}", e)
            CapsLockState.UNKNOWN
        }
    }

    fun resetState() {
        lastSwitchedTo = null
        currentTargetMethod = InputMethodType.ENGLISH
    }

    fun getLastSwitchedTo(): InputMethodType? = lastSwitchedTo
    fun getCurrentTarget(): InputMethodType = currentTargetMethod
}
