package com.wonder.freemacinput.freemacinput.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.JPanel
import javax.swing.JLabel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Color
import javax.swing.border.EmptyBorder

/**
 * Toast 通知管理器
 *
 * 在编辑器中显示小白框提示，告知用户当前输入法切换状态
 */
object ToastManager {
    private val logger = Logger.getInstance(ToastManager::class.java)

    // 存储每个 Editor 的 Popup 实例
    private val activePopups = ConcurrentHashMap<Editor, JBPopup>()

    // 定时器用于自动消失
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    // Toast 显示配置
    private const val TOAST_DURATION_MS = 1200L  // 显示时长
    private const val TOAST_MIN_DELAY_MS = 200L  // 最小显示间隔

    @Volatile
    private var lastShowTime = 0L

    /**
     * 在指定编辑器位置显示 Toast 通知
     *
     * @param editor 编辑器实例
     * @param message 显示的消息
     * @param isChinese 是否为中文模式（影响颜色）
     */
    fun showToast(editor: Editor, message: String, isChinese: Boolean) {
        logger.info("尝试显示Toast: $message")
        val now = System.currentTimeMillis()
        if (now - lastShowTime < TOAST_MIN_DELAY_MS) {
            logger.info("Toast显示间隔太短，跳过")
            return
        }

        // 直接获取服务和设置
        val project = editor.project ?: run {
            logger.info("Editor没有project，跳过")
            return
        }
        val service = com.wonder.freemacinput.freemacinput.service.InputMethodService.getInstance(project)
        val settings = service.getSettings()
        
        logger.info("isShowHints设置: ${settings.isShowHints}")
        if (!settings.isShowHints) {
            logger.info("用户关闭了提示，跳过")
            return
        }

        // UI 操作必须在 EDT 线程执行
        ApplicationManager.getApplication().invokeLater {
            try {
                // 先移除现有的 popup
                dismissToast(editor)
                
                val content = createToastContent(message, isChinese)

                // 获取光标的屏幕位置
                val point = getCaretScreenPosition(editor) ?: return@invokeLater
                
                // 获取屏幕尺寸
                val graphicsEnvironment = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                val screenBounds = graphicsEnvironment.defaultScreenDevice.defaultConfiguration.bounds
                
                // 估算提示框的大小（为了位置计算）
                val estimatedWidth = message.length * 8 + 32 // 估算宽度
                val estimatedHeight = 34 // 固定高度
                
                // 调整位置，使提示显示在光标下方，同时确保不会超出屏幕边界
                var adjustedX = point.x
                var adjustedY = point.y + 18
                
                // 确保提示框不超出屏幕右边界
                if (adjustedX + estimatedWidth > screenBounds.width) {
                    adjustedX = screenBounds.width - estimatedWidth - 10
                }
                
                // 确保提示框不超出屏幕左边界
                if (adjustedX < 10) {
                    adjustedX = 10
                }
                
                // 确保提示框不超出屏幕下边界
                if (adjustedY + estimatedHeight > screenBounds.height) {
                    adjustedY = point.y - estimatedHeight - 10
                }
                
                // 确保提示框不超出屏幕上边界
                if (adjustedY < 10) {
                    adjustedY = 10
                }
                
                val adjustedPoint = Point(adjustedX, adjustedY)

                val popup = JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(content, null)
                    .setFocusable(false)
                    .setRequestFocus(false)
                    .setCancelOnClickOutside(true)
                    .setCancelOnWindowDeactivation(true)
                    .createPopup()

                popup.show(RelativePoint(adjustedPoint))

                activePopups[editor] = popup
                lastShowTime = System.currentTimeMillis()

                // 定时自动消失
                scheduler.schedule({
                    // 关闭 popup 也需要在 EDT 线程执行
                    ApplicationManager.getApplication().invokeLater {
                        dismissToast(editor)
                    }
                }, TOAST_DURATION_MS, TimeUnit.MILLISECONDS)

                logger.info("显示 Toast: $message")

            } catch (e: Exception) {
                logger.warn("显示 Toast 异常: ${e.message}", e)
            }
        }
    }

    /**
     * 获取光标的屏幕位置（在 ReadAction 中执行）
     */
    private fun getCaretScreenPosition(editor: Editor): Point? {
        return try {
            ReadAction.compute<Point?, Exception> {
                val caret = editor.caretModel.primaryCaret
                val offset = caret.offset
                val document = editor.document
                if (offset > document.textLength) {
                    return@compute null
                }

                val logicalPos = editor.offsetToLogicalPosition(offset)
                val visualPos = editor.logicalToVisualPosition(logicalPos)
                val start = editor.visualPositionToXY(visualPos)

                // 转换为屏幕坐标
                val editorComponent = editor.contentComponent
                val locationOnScreen = editorComponent.locationOnScreen

                Point(locationOnScreen.x + start.x, locationOnScreen.y + start.y)
            }
        } catch (e: Exception) {
            logger.warn("获取光标位置异常: ${e.message}")
            null
        }
    }

    /**
     * 创建 Toast 内容面板
     */
    private fun createToastContent(message: String, isChinese: Boolean): JPanel {
        val (bgColor, textColor) = if (isChinese) {
            CHINESE_BG to CHINESE_TEXT
        } else {
            ENGLISH_BG to ENGLISH_TEXT
        }

        val panel = object : JPanel(BorderLayout()) {
            override fun isOpaque(): Boolean = true
            override fun getBackground(): Color = bgColor
            
            // 添加圆角效果
            override fun paintComponent(g: java.awt.Graphics) {
                val g2d = g as java.awt.Graphics2D
                val radius = 8.0f
                
                // 设置抗锯齿
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                
                // 绘制圆角背景
                g2d.color = bgColor
                g2d.fillRoundRect(0, 0, width, height, radius.toInt(), radius.toInt())
                
                // 添加阴影效果
                val shadowColor = Color(0, 0, 0, 30)
                g2d.color = shadowColor
                for (i in 1..3) {
                    g2d.fillRoundRect(i, i, width - 2 * i, height - 2 * i, radius.toInt(), radius.toInt())
                }
                
                super.paintComponent(g)
            }
        }.apply {
            border = EmptyBorder(10, 16, 10, 16)
        }

        val label = JLabel(message).apply {
            foreground = textColor
            font = java.awt.Font("SF Pro Text", java.awt.Font.PLAIN, 13)
        }

        panel.add(label, BorderLayout.CENTER)
        panel.preferredSize = Dimension(label.preferredSize.width + 32, 34)

        return panel
    }

    /**
     * 关闭指定编辑器的 Toast
     */
    fun dismissToast(editor: Editor) {
        val popup = activePopups.remove(editor) ?: return
        
        // 关闭 popup 必须在 EDT 线程执行
        if (ApplicationManager.getApplication().isDispatchThread) {
            try {
                popup.cancel()
            } catch (e: Exception) {
                // 忽略异常
            }
        } else {
            ApplicationManager.getApplication().invokeLater {
                try {
                    popup.cancel()
                } catch (e: Exception) {
                    // 忽略异常
                }
            }
        }
    }

    /**
     * 关闭所有 Toast
     */
    fun dismissAll() {
        // 使用副本避免并发修改异常
        val editors = activePopups.keys.toList()
        
        // 在 EDT 线程执行所有关闭操作
        if (ApplicationManager.getApplication().isDispatchThread) {
            editors.forEach { editor ->
                dismissToast(editor)
            }
        } else {
            ApplicationManager.getApplication().invokeLater {
                editors.forEach { editor ->
                    dismissToast(editor)
                }
            }
        }
    }

    /**
     * 清理资源
     */
    fun dispose() {
        dismissAll()
        scheduler.shutdown()
    }

    // 颜色定义
    private val CHINESE_BG = Color(232, 250, 246)
    private val CHINESE_TEXT = Color(0, 102, 85)
    private val ENGLISH_BG = Color(245, 245, 245)
    private val ENGLISH_TEXT = Color(60, 60, 60)
}
