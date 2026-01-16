package com.wonder.freemacinput.freemacinput.listener

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.wonder.freemacinput.freemacinput.core.*
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.GitCommitSceneManager
import com.wonder.freemacinput.freemacinput.service.InputMethodService
import com.wonder.freemacinput.freemacinput.ui.ToastManager
import com.wonder.freemacinput.freemacinput.ui.CommentSceneHintManager
import com.wonder.freemacinput.freemacinput.config.SettingsState
import com.intellij.openapi.application.ApplicationManager
import java.awt.Color

import java.util.Timer
import java.util.TimerTask

/**
 * 编辑器事件监听器
 */
class EditorEventListener(private val project: Project) : CaretListener, DocumentListener {
    private val logger = Logger.getInstance(EditorEventListener::class.java)

    private val contextDetector = ContextDetector()
    private val inputMethodService = InputMethodService.getInstance(project)

    private var lastContextInfo: ContextInfo? = null
    private val timer: Timer = Timer("InputMethodSwitchTimer", true)
    private var scheduledTask: TimerTask? = null
    private val switchDelayMs = 150L
    
    // 缓存文档文本，避免频繁获取
    private var cachedDocumentText: String? = null
    private var cachedDocumentLength: Int = -1
    
    // 大写锁定状态监听
    private var capsLockMonitorTimer: Timer? = null
    private var capsLockMonitorTask: TimerTask? = null
    private var lastCapsLockState: Boolean = false
    
    // 输入法状态监听（用于检测用户手动切换）
    private var inputMethodMonitorTimer: Timer? = null
    private var inputMethodMonitorTask: TimerTask? = null
    private var lastInputMethod: InputMethodType = InputMethodType.UNKNOWN
    private var lastInputMethodChangeTime: Long = 0  // 上次输入法变化时间
    
    // 字符串场景状态管理
    private var inStringScene = false
    private var stringSceneVariableName: String? = null
    private var stringSceneLanguage: String? = null
    private var stringSceneInputMethod: InputMethodType? = null
    private var stringSceneSwitched = false  // 记录是否已经切换过输入法
    
    // 用户手动切换监听定时器
    private var userSwitchMonitorTimer: Timer? = null
    private var userSwitchMonitorTask: TimerTask? = null
    
    // 补救功能状态管理
    private var rescueInProgress = false
    private var rescueEndTime = 0L

    fun onEditorActivated(editor: Editor) {
        logger.info("onEditorActivated called")
        val data = extractEditorData(editor)
        logger.info("onEditorActivated: ${data.fileName}, offset=${data.caretOffset}")
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.caretOffset, 450L)
        
        // 启动大写锁定监听
        startCapsLockMonitoring()
        
        // 启动输入法状态监听
        startInputMethodMonitoring()
    }

    private fun extractEditorData(editor: Editor): EditorData {
        val fileName = editor.virtualFile?.name ?: "unknown"
        val document = editor.document
        val caretOffset = editor.caretModel.offset
        
        // 优化：只在文档长度变化时重新获取文本，或使用缓存
        val currentLength = document.textLength
        val documentText = if (cachedDocumentText != null && cachedDocumentLength == currentLength) {
            cachedDocumentText!!
        } else {
            val text = document.text
            cachedDocumentText = text
            cachedDocumentLength = currentLength
            text
        }
        
        return EditorData(fileName, documentText, caretOffset)
    }

    override fun caretPositionChanged(e: CaretEvent) {
        val editor = e.editor ?: run {
            logger.info("caretPositionChanged: editor is null")
            return
        }
        val fileName = editor.virtualFile?.name ?: "unknown"
        val offset = e.caret?.offset ?: -1
        logger.info("光标位置变化: $fileName, offset=$offset")

        val data = extractEditorData(editor)
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.caretOffset)
    }

    override fun caretAdded(e: CaretEvent) {
        e.editor?.let { updateCaretRenderer(it) }
    }

    override fun caretRemoved(e: CaretEvent) {
        e.editor?.let { CaretRendererManager.remove(it) }
    }

    override fun documentChanged(event: DocumentEvent) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        
        // 检查是否输入了注释标记
        val settings = inputMethodService.getSettings()
        if (settings.showCommentSceneHint) {
            checkAndShowCommentHint(editor, event)
        }
        
        // 记录字符串场景的输入（用于补救功能）
        if (settings.enableStringRescue && event.newLength > 0) {
            val offset = event.offset
            StringInputRescue.recordInput(editor, event.newFragment.toString(), offset)
        }
        
        // 文档变化时清除缓存
        cachedDocumentText = null
        cachedDocumentLength = -1
        val data = extractEditorData(editor)
        scheduleInputMethodSwitch(data.fileName, data.documentText, data.caretOffset, 120L)
    }
    
    /**
     * 检查并显示注释场景提示
     */
    private fun checkAndShowCommentHint(editor: Editor, event: DocumentEvent) {
        try {
            val document = editor.document
            val offset = event.offset + event.newLength
            
            // 确保有足够的字符
            if (offset < 2) return
            
            val text = document.text
            if (offset > text.length) return
            
            // 检查光标前的两个字符
            val beforeCursor = text.substring(maxOf(0, offset - 2), offset)
            
            // 检查是否刚输入了 // 或 /*
            if (beforeCursor == "//" || beforeCursor == "/*") {
                ApplicationManager.getApplication().invokeLater {
                    ToastManager.showToast(editor, "注释场景", true, 2000)
                    logger.info("显示注释场景提示: $beforeCursor")
                }
            }
        } catch (e: Exception) {
            logger.error("显示注释场景提示失败", e)
        }
    }

    override fun beforeDocumentChange(event: DocumentEvent) {}

    private fun updateCaretRenderer(editor: Editor) {
        val renderer = CaretRendererManager.getOrCreate(editor)
        renderer.setEnabled(inputMethodService.isEnableCaretColor())
        renderer.refresh()
    }

    private fun scheduleInputMethodSwitch(
        fileName: String,
        documentText: String,
        caretOffset: Int,
        delayMs: Long = switchDelayMs
    ) {
        logger.info("scheduleInputMethodSwitch: file=$fileName, offset=$caretOffset")
        scheduledTask?.cancel()
        scheduledTask = null

        val task = object : TimerTask() {
            override fun run() {
                logger.info("scheduled task running...")
                detectAndSwitch(fileName, documentText, caretOffset)
            }
        }

        timer.schedule(task, delayMs)
        scheduledTask = task
        logger.info("task scheduled with delay=${delayMs}ms")
    }

    private fun detectAndSwitch(
        fileName: String,
        documentText: String,
        caretOffset: Int
    ) {
        val startTs = System.currentTimeMillis()
        if (!inputMethodService.isEnabled()) {
            logger.info("插件未启用")
            return
        }
        
        // 如果当前在特殊场景中（Git 提交、工具窗口等），不要干扰
        if (GitCommitSceneManager.isInAnySpecialScene()) {
            logger.info("当前在特殊场景中，跳过自动切换")
            return
        }

        logger.info("========== 开始检测上下文 ==========")
        logger.info("文件: $fileName, 光标位置: $caretOffset")

        val contextInfo = contextDetector.detectContext(documentText, caretOffset, fileName)
        logger.info("✅ 检测结果: 类型=${contextInfo.type}, 原因=${contextInfo.reason}")
        if (contextInfo.variableName != null) {
            logger.info("   变量名: ${contextInfo.variableName}, 语言: ${contextInfo.language}")
        }

        // 获取设置
        val settings = inputMethodService.getSettings()
        
        // 检查自定义规则（优先级最高）
        val customRuleMatch = checkCustomPatternRules(documentText, caretOffset, fileName, contextInfo.type, settings)
        if (customRuleMatch != null) {
            logger.info("🎯 匹配到自定义规则: ${customRuleMatch.name} -> ${customRuleMatch.targetInputMethod}")
            val targetMethod = customRuleMatch.targetInputMethod
            
            // 使用 InputMethodManager 的内部状态与冷却判定
            val (should, reason) = InputMethodManager.shouldSwitch(targetMethod)
            logger.info("🔄 是否需要切换: $should, 原因: $reason")
            
            if (should) {
                logger.info("⚡ 开始切换输入法...")
                val switchResult = InputMethodManager.switchTo(targetMethod, settings)
                val elapsed = System.currentTimeMillis() - startTs
                logger.info("✅ 切换结果: success=${switchResult.success}, 实际输入法=${switchResult.actualMethod}, 耗时=${elapsed}ms")
                
                // 更新光标颜色
                if (switchResult.success && settings.isEnableCaretColor) {
                    ApplicationManager.getApplication().invokeLater {
                        val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                        if (activeEditor != null) {
                            updateCursorColor(activeEditor, switchResult.actualMethod, settings)
                        }
                    }
                }
                
                // 显示 Toast 提示
                if (settings.isShowHints) {
                    ApplicationManager.getApplication().invokeLater {
                        val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                        if (activeEditor != null && switchResult.success) {
                            val toastMessage = "自定义规则: ${customRuleMatch.name}"
                            val isChinese = switchResult.actualMethod == InputMethodType.CHINESE
                            ToastManager.showToast(activeEditor, toastMessage, isChinese)
                        }
                    }
                }
            }
            
            lastContextInfo = contextInfo
            logger.info("========== 检测结束（自定义规则）==========\n")
            return
        }

        // 字符串场景特殊处理
        if (contextInfo.type == ContextType.STRING) {
            handleStringScene(contextInfo, fileName)
            lastContextInfo = contextInfo
            logger.info("========== 检测结束（字符串场景）==========\n")
            return
        } else {
            // 离开字符串场景，清除状态
            if (inStringScene) {
                logger.info("🚪 离开字符串场景")
                stopContinuousMonitoring()  // 停止监听
                inStringScene = false
                stringSceneVariableName = null
                stringSceneLanguage = null
                stringSceneInputMethod = null
                stringSceneSwitched = false  // 重置切换标记
            }
        }

        val targetMethod = determineInputMethod(contextInfo, fileName)
        logger.info("🎯 目标输入法: $targetMethod")

        // 获取当前实际输入法（用于补救功能）
        val currentMethod = InputMethodManager.getCurrentInputMethod()
        logger.info("📱 当前输入法: $currentMethod")

        // 使用 InputMethodManager 的内部状态与冷却判定
        val (should, reason) = InputMethodManager.shouldSwitch(targetMethod)
        logger.info("🔄 是否需要切换: $should, 原因: $reason")

        if (!should) {
            logger.info("========== 检测结束（无需切换）==========\n")
            lastContextInfo = contextInfo
            return
        }

        logger.info("⚡ 开始切换输入法...")
        val switchResult = InputMethodManager.switchTo(targetMethod, settings)
        val elapsed = System.currentTimeMillis() - startTs
        logger.info("✅ 切换结果: success=${switchResult.success}, 实际输入法=${switchResult.actualMethod}, 耗时=${elapsed}ms")
        
        // 更新光标颜色
        if (switchResult.success && settings.isEnableCaretColor) {
            ApplicationManager.getApplication().invokeLater {
                val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                if (activeEditor != null) {
                    updateCursorColor(activeEditor, switchResult.actualMethod, settings)
                }
            }
        }
        
        logger.info("========== 检测结束 ==========\n")

        // 补救功能：从英文切换到中文时
        if (switchResult.success && 
            settings.enableStringRescue && 
            contextInfo.type == ContextType.STRING &&
            currentMethod == InputMethodType.ENGLISH && 
            switchResult.actualMethod == InputMethodType.CHINESE) {
            
            val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
            if (activeEditor != null) {
                StringInputRescue.rescueInput(activeEditor, project, currentMethod, switchResult.actualMethod)
            }
        }

        // 显示 Toast 提示 - 根据实际切换结果显示
        logger.info("准备显示 Toast: isShowHints=${settings.isShowHints}")
        if (settings.isShowHints) {
            ApplicationManager.getApplication().invokeLater {
                val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                logger.info("activeEditor: $activeEditor")
                if (activeEditor != null) {
                    if (switchResult.success) {
                        val toastMessage = generateToastMessage(contextInfo, switchResult.actualMethod, fileName, switchResult.message)
                        val isChinese = switchResult.actualMethod == InputMethodType.CHINESE
                        logger.info("显示 Toast: $toastMessage, isChinese=$isChinese")
                        ToastManager.showToast(activeEditor, toastMessage, isChinese)
                    } else {
                        val failureMessage = when {
                            switchResult.message.contains("不支持") -> "输入法切换失败：不支持当前操作系统"
                            switchResult.message.contains("权限") -> "输入法切换失败：缺少系统权限"
                            switchResult.message.contains("冷却中") -> "输入法切换失败：切换过于频繁"
                            else -> "输入法切换失败：${switchResult.message}"
                        }
                        logger.info("显示失败 Toast: $failureMessage")
                        ToastManager.showToast(activeEditor, failureMessage, false)
                    }
                } else {
                    logger.warn("activeEditor 为 null，无法显示 Toast")
                }
            }
        } else {
            logger.info("isShowHints=false，跳过 Toast 显示")
        }

        lastContextInfo = contextInfo
    }
    
    /**
     * 处理字符串场景
     * 字符串场景有独立的状态管理，一旦进入就保持，直到离开
     */
    private fun handleStringScene(contextInfo: ContextInfo, fileName: String) {
        val settings = inputMethodService.getSettings()
        val variableName = contextInfo.variableName
        val language = contextInfo.language
        
        // 检查是否是新的字符串场景
        val isNewStringScene = !inStringScene || 
                                stringSceneVariableName != variableName || 
                                stringSceneLanguage != language
        
        if (isNewStringScene) {
            logger.info("🎯 进入新的字符串场景: $language.$variableName")
            inStringScene = true
            stringSceneVariableName = variableName
            stringSceneLanguage = language
            stringSceneSwitched = false  // 重置切换标记
            
            // 确定字符串场景的输入法
            val targetMethod = if (variableName != null && language != null) {
                // 查找配置的规则或习惯
                val configuredMethod = settings.getInputMethodForString(language, variableName)
                if (configuredMethod != null) {
                    logger.info("   找到配置: $language.$variableName -> $configuredMethod")
                    configuredMethod
                } else {
                    // 没有配置，使用字符串场景的默认输入法
                    logger.info("   没有配置，使用字符串场景默认输入法: ${settings.stringMethod}")
                    settings.stringMethod
                }
            } else {
                // 无法提取变量名，使用默认
                settings.stringMethod
            }
            
            // 记录是否使用了默认输入法（用于显示提示）
            val isUsingDefault = variableName != null && language != null && 
                                 settings.getInputMethodForString(language, variableName) == null
            
            logger.info("   是否使用默认输入法: $isUsingDefault")
            
            stringSceneInputMethod = targetMethod
            logger.info("   字符串场景输入法: $targetMethod")
            
            // 切换到字符串场景的输入法（只在第一次进入时切换）
            val currentMethod = InputMethodManager.getCurrentInputMethod()
            if (currentMethod != targetMethod && !stringSceneSwitched) {
                logger.info("   需要切换: $currentMethod -> $targetMethod")
                val switchResult = InputMethodManager.switchTo(targetMethod, settings)
                logger.info("   切换结果: ${switchResult.success}, 实际: ${switchResult.actualMethod}")
                stringSceneSwitched = true  // 标记已切换
                
                // 更新光标颜色
                if (switchResult.success && settings.isEnableCaretColor) {
                    ApplicationManager.getApplication().invokeLater {
                        val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                        if (activeEditor != null) {
                            updateCursorColor(activeEditor, switchResult.actualMethod, settings)
                        }
                    }
                }
                
                // 补救功能
                if (switchResult.success && 
                    settings.enableStringRescue && 
                    currentMethod == InputMethodType.ENGLISH && 
                    switchResult.actualMethod == InputMethodType.CHINESE) {
                    
                    val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                    if (activeEditor != null) {
                        StringInputRescue.rescueInput(activeEditor, project, currentMethod, switchResult.actualMethod)
                    }
                }
            } else {
                logger.info("   当前已是目标输入法，无需切换")
            }
            
            // 启动持续监听用户手动切换（无论是否有配置都监听）
            if (variableName != null && language != null) {
                logger.info("   💡 启动持续监听用户手动切换")
                startContinuousMonitoring(language, variableName)
            }
            
            // 显示提示（无论是否切换都显示）
            if (settings.isShowHints) {
                ApplicationManager.getApplication().invokeLater {
                    val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                    if (activeEditor != null) {
                        val message = if (isUsingDefault) {
                            // 使用默认输入法时显示特殊提示
                            logger.info("   📢 显示提示: 字符串区域默认输入法")
                            "字符串区域默认输入法"
                        } else if (variableName != null) {
                            logger.info("   📢 显示提示: 字符串场景: $variableName")
                            "字符串场景: $variableName → ${if (targetMethod == InputMethodType.CHINESE) "中文" else "英文"}"
                        } else {
                            logger.info("   📢 显示提示: 字符串区域")
                            "字符串区域 → ${if (targetMethod == InputMethodType.CHINESE) "中文" else "英文"}"
                        }
                        ToastManager.showToast(activeEditor, message, targetMethod == InputMethodType.CHINESE)
                    }
                }
            }
        } else {
            logger.info("📍 保持在字符串场景: $language.$variableName (输入法: $stringSceneInputMethod)")
            // 在同一个字符串场景内，不做任何自动切换
        }
    }
    
    /**
     * 开始持续监听用户在字符串场景中的手动切换
     * 使用定时器每 200ms 检查一次
     */
    private fun startContinuousMonitoring(language: String, variableName: String) {
        // 先停止之前的监听
        stopContinuousMonitoring()
        
        val expectedMethod = stringSceneInputMethod ?: return
        logger.info("🔍 开始持续监听用户手动切换: $language.$variableName, 预期输入法: $expectedMethod")
        
        userSwitchMonitorTimer = Timer("StringSceneMonitor", true)
        userSwitchMonitorTask = object : TimerTask() {
            override fun run() {
                // 检查是否还在字符串场景中
                if (!inStringScene || 
                    stringSceneLanguage != language || 
                    stringSceneVariableName != variableName) {
                    logger.info("⏸️ 已离开字符串场景，停止监听")
                    stopContinuousMonitoring()
                    return
                }
                
                val currentMethod = InputMethodManager.getCurrentInputMethod()
                val expectedNow = stringSceneInputMethod
                
                // 如果当前输入法与预期不同，说明用户手动切换了
                if (expectedNow != null && 
                    currentMethod != expectedNow && 
                    currentMethod != InputMethodType.UNKNOWN) {
                    
                    logger.info("🔧 检测到用户手动切换: $expectedNow -> $currentMethod")
                    
                    // 更新字符串场景的输入法
                    stringSceneInputMethod = currentMethod
                    
                    // 记录习惯
                    val settings = inputMethodService.getSettings()
                    val existingHabit = settings.stringSceneHabits.find {
                        it.language.equals(language, ignoreCase = true) && 
                        it.expression.equals(variableName, ignoreCase = true)
                    }
                    
                    if (existingHabit == null || existingHabit.preferredInputMethod != currentMethod) {
                        settings.recordStringSceneHabit(language, variableName, currentMethod)
                        logger.info("✅ 自动记录习惯: $language.$variableName -> $currentMethod")
                    }
                    
                    // 触发补救功能
                    if (settings.enableStringRescue && 
                        expectedNow == InputMethodType.ENGLISH && 
                        currentMethod == InputMethodType.CHINESE) {
                        
                        ApplicationManager.getApplication().invokeLater {
                            val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                            if (activeEditor != null) {
                                logger.info("🔧 触发补救功能: 删除英文拼音")
                                StringInputRescue.rescueInput(activeEditor, project, expectedNow, currentMethod)
                            }
                        }
                    }
                }
            }
        }
        
        // 每 200ms 检查一次
        userSwitchMonitorTimer?.schedule(userSwitchMonitorTask, 200L, 200L)
    }
    
    /**
     * 停止持续监听
     */
    private fun stopContinuousMonitoring() {
        userSwitchMonitorTask?.cancel()
        userSwitchMonitorTask = null
        userSwitchMonitorTimer?.cancel()
        userSwitchMonitorTimer = null
    }
    
    /**
     * 开始监听用户在字符串场景中的手动切换
     */
    private fun startMonitoringUserSwitch(language: String, variableName: String) {
        // 记录进入字符串时的输入法
        val initialMethod = stringSceneInputMethod ?: InputMethodManager.getCurrentInputMethod()
        logger.info("🔍 开始监听用户手动切换: $language.$variableName, 初始输入法: $initialMethod")
        
        // 使用更短的延迟，多次检查
        // 第一次检查：300ms
        Timer().schedule(object : TimerTask() {
            override fun run() {
                checkAndHandleUserSwitch(language, variableName, initialMethod)
            }
        }, 300L)
        
        // 第二次检查：800ms
        Timer().schedule(object : TimerTask() {
            override fun run() {
                checkAndHandleUserSwitch(language, variableName, initialMethod)
            }
        }, 800L)
        
        // 第三次检查：1500ms
        Timer().schedule(object : TimerTask() {
            override fun run() {
                checkAndHandleUserSwitch(language, variableName, initialMethod)
            }
        }, 1500L)
    }
    
    /**
     * 检查用户是否手动切换了输入法（在保持字符串场景时调用）
     */
    private fun checkUserManualSwitch(language: String, variableName: String) {
        val expectedMethod = stringSceneInputMethod ?: return
        val currentMethod = InputMethodManager.getCurrentInputMethod()
        
        // 如果当前输入法与预期不同，说明用户手动切换了
        if (currentMethod != expectedMethod && currentMethod != InputMethodType.UNKNOWN) {
            logger.info("🔧 检测到用户手动切换: $expectedMethod -> $currentMethod")
            
            // 更新字符串场景的输入法
            stringSceneInputMethod = currentMethod
            
            // 记录习惯
            val settings = inputMethodService.getSettings()
            val existingHabit = settings.stringSceneHabits.find {
                it.language.equals(language, ignoreCase = true) && 
                it.expression.equals(variableName, ignoreCase = true)
            }
            
            if (existingHabit == null || existingHabit.preferredInputMethod != currentMethod) {
                settings.recordStringSceneHabit(language, variableName, currentMethod)
                logger.info("✅ 自动记录习惯: $language.$variableName -> $currentMethod")
            }
            
            // 触发补救功能
            if (settings.enableStringRescue && expectedMethod == InputMethodType.ENGLISH && currentMethod == InputMethodType.CHINESE) {
                val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                if (activeEditor != null) {
                    logger.info("🔧 触发补救功能: 删除英文拼音")
                    StringInputRescue.rescueInput(activeEditor, project, expectedMethod, currentMethod)
                }
            }
        }
    }
    
    /**
     * 检查并处理用户的手动切换
     */
    private fun checkAndHandleUserSwitch(language: String, variableName: String, initialMethod: InputMethodType) {
        logger.info("🔍 检查用户手动切换: $language.$variableName, 初始输入法: $initialMethod")
        
        // 检查是否还在同一个字符串场景中
        if (inStringScene && 
            stringSceneLanguage == language && 
            stringSceneVariableName == variableName) {
            
            val currentMethod = InputMethodManager.getCurrentInputMethod()
            logger.info("   当前输入法: $currentMethod, 初始输入法: $initialMethod")
            
            if (currentMethod != initialMethod && currentMethod != InputMethodType.UNKNOWN) {
                // 用户手动切换了输入法
                logger.info("   ✅ 检测到用户手动切换: $initialMethod -> $currentMethod")
                
                // 检查是否已经记录过这个习惯
                val settings = inputMethodService.getSettings()
                val existingHabit = settings.stringSceneHabits.find {
                    it.language.equals(language, ignoreCase = true) && 
                    it.expression.equals(variableName, ignoreCase = true)
                }
                
                if (existingHabit == null || existingHabit.preferredInputMethod != currentMethod) {
                    // 记录习惯
                    settings.recordStringSceneHabit(language, variableName, currentMethod)
                    logger.info("✅ 自动记录习惯: $language.$variableName -> $currentMethod")
                    
                    // 更新当前字符串场景的输入法
                    stringSceneInputMethod = currentMethod
                }
                
                // 触发补救功能：清除不匹配的字符
                if (settings.enableStringRescue) {
                    val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                    if (activeEditor != null) {
                        logger.info("🔧 用户手动切换，触发补救功能: $initialMethod -> $currentMethod")
                        StringInputRescue.rescueInput(activeEditor, project, initialMethod, currentMethod)
                    }
                }
            } else {
                logger.info("   ⏸️ 输入法未变化或为 UNKNOWN，跳过")
            }
        } else {
            logger.info("   ⏸️ 已离开字符串场景，跳过检查")
        }
    }

    /**
     * 根据上下文和实际输入法生成提示消息
     */
    private fun generateToastMessage(
        contextInfo: ContextInfo,
        actualMethod: InputMethodType,
        fileName: String,
        switchMessage: String
    ): String {
        logger.info("=== generateToastMessage 被调用 ===")
        logger.info("actualMethod: $actualMethod")
        logger.info("contextInfo.type: ${contextInfo.type}")
        logger.info("switchMessage: $switchMessage")

        return when (actualMethod) {
            InputMethodType.CHINESE -> {
                when (contextInfo.type) {
                    ContextType.CODE -> {
                        // 如果切换消息表明已经是当前输入法，显示简单消息
                        if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                            "保持中文输入法"
                        } else {
                            "已切换为中文"
                        }
                    }
                    ContextType.COMMENT -> "注释区域自动切换为中文"
                    ContextType.STRING -> "字符串区域自动切换为中文"
                    ContextType.UNKNOWN -> "已切换为中文"
                }
            }
            InputMethodType.ENGLISH -> {
                // 根据文件类型生成消息
                val fileType = getFileType(fileName)
                 when (contextInfo.type) {
                     ContextType.CODE -> {
                         // 如果切换消息表明已经是当前输入法，显示简单消息
                         if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                             "保持英文输入法"
                         } else {
                             when (fileType) {
                                 FileType.JAVA -> "Java 文件已切换为英文"
                                 FileType.KOTLIN -> "Kotlin 文件已切换为英文"
                                 FileType.PYTHON -> "Python 文件已切换为英文"
                                 FileType.GO -> "Go 文件已切换为英文"
                                 FileType.JAVASCRIPT -> "JavaScript 文件已切换为英文"
                                 FileType.TYPESCRIPT -> "TypeScript 文件已切换为英文"
                                 FileType.C_CPP -> "C/C++ 文件已切换为英文"
                                 FileType.OTHER -> "代码区域已切换为英文"
                             }
                         }
                     }
                    ContextType.STRING -> {
                        // 英文字符串保持英文
                        if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                            "保持英文输入法"
                        } else {
                            "字符串区域已切换为英文"
                        }
                    }
                    ContextType.COMMENT -> {
                        if (switchMessage.contains("无需切换") || switchMessage.contains("冷却中")) {
                            "保持英文输入法"
                        } else {
                            "注释区域已切换为英文"
                        }
                    }
                    ContextType.UNKNOWN -> "已切换为英文"
                }
            }
            else -> ""
        }
    }

    /**
     * 获取文件类型
     */
    private fun getFileType(fileName: String): FileType {
        return when {
            fileName.endsWith(".java", ignoreCase = true) -> FileType.JAVA
            fileName.endsWith(".kt", ignoreCase = true) -> FileType.KOTLIN
            fileName.endsWith(".py", ignoreCase = true) -> FileType.PYTHON
            fileName.endsWith(".go", ignoreCase = true) -> FileType.GO
            fileName.endsWith(".js", ignoreCase = true) -> FileType.JAVASCRIPT
            fileName.endsWith(".ts", ignoreCase = true) -> FileType.TYPESCRIPT
            fileName.endsWith(".c", ignoreCase = true) ||
            fileName.endsWith(".cpp", ignoreCase = true) ||
            fileName.endsWith(".h", ignoreCase = true) ||
            fileName.endsWith(".hpp", ignoreCase = true) -> FileType.C_CPP
            else -> FileType.OTHER
        }
    }

    /**
     * 文件类型枚举
     */
    private enum class FileType {
        JAVA, KOTLIN, PYTHON, GO, JAVASCRIPT, TYPESCRIPT, C_CPP, OTHER
    }

    private fun determineInputMethod(contextInfo: ContextInfo, fileName: String): InputMethodType {
        val settings = inputMethodService.getSettings()
        return when (contextInfo.type) {
            ContextType.CODE -> {
                // 优先使用文件类型规则
                val fileExtension = fileName.substringAfterLast('.', "")
                settings.getInputMethodForFileType(fileExtension)
            }
            ContextType.COMMENT -> settings.commentMethod
            ContextType.STRING -> {
                // 字符串场景：优先使用习惯记录，其次使用规则，最后使用默认
                if (contextInfo.variableName != null && contextInfo.language != null) {
                    settings.getInputMethodForString(contextInfo.language, contextInfo.variableName) ?: settings.stringMethod
                } else {
                    settings.stringMethod
                }
            }
            ContextType.UNKNOWN -> settings.defaultMethod
        }
    }

    /**
     * 记录字符串场景的输入法习惯
     * 当用户进入字符串区域后主动切换输入法时，记录变量名和输入法的对应关系
     */
    private fun recordStringSceneHabit(contextInfo: ContextInfo, inputMethod: InputMethodType) {
        val variableName = contextInfo.variableName ?: return
        val language = contextInfo.language ?: return
        
        val settings = inputMethodService.getSettings()
        settings.recordStringSceneHabit(language, variableName, inputMethod)
        
        logger.info("记录字符串场景习惯: $language.$variableName -> $inputMethod")
    }

    /**
     * 监听字符串场景的输入法变化（用于记录用户主动切换的习惯）
     * 当用户在字符串区域主动切换输入法时，记录这个习惯
     */
    private fun monitorStringSceneInputMethod(contextInfo: ContextInfo) {
        // 获取当前实际的输入法
        val currentMethod = InputMethodManager.getCurrentInputMethod()
        if (currentMethod == InputMethodType.UNKNOWN) return
        
        val variableName = contextInfo.variableName ?: return
        val language = contextInfo.language ?: return
        
        val settings = inputMethodService.getSettings()
        val expectedMethod = settings.getInputMethodForString(language, variableName)
        
        // 如果当前输入法与预期不同，说明用户主动切换了，记录这个习惯
        if (currentMethod != expectedMethod) {
            settings.recordStringSceneHabit(language, variableName, currentMethod)
            logger.info("检测到用户主动切换: $language.$variableName -> $currentMethod")
        }
    }

    fun dispose() {
        stopContinuousMonitoring()  // 停止持续监听
        stopCapsLockMonitoring()  // 停止大写锁定监听
        stopInputMethodMonitoring()  // 停止输入法状态监听
        timer.cancel()
        CaretRendererManager.disposeAll()
        ToastManager.dismissAll()
    }
    
    /**
     * 更新光标颜色
     */
    private fun updateCursorColor(editor: Editor, inputMethod: InputMethodType, settings: SettingsState) {
        try {
            val chineseColor = CursorColorManager.parseColor(settings.chineseCaretColor) ?: Color(0xEF, 0x16, 0x16)
            val englishColor = CursorColorManager.parseColor(settings.englishCaretColor) ?: Color(0xDC, 0xDC, 0xD9)
            val capsLockColor = CursorColorManager.parseColor(settings.capsLockCaretColor) ?: Color(0xF6, 0xE3, 0x0E)
            
            CursorColorManager.setCursorColorByInputMethod(
                editor,
                inputMethod,
                chineseColor,
                englishColor,
                capsLockColor
            )
        } catch (e: Exception) {
            logger.error("更新光标颜色失败", e)
        }
    }
    
    /**
     * 启动输入法状态监听（检测用户手动切换）
     */
    private fun startInputMethodMonitoring() {
        val settings = inputMethodService.getSettings()
        if (!settings.isEnableCaretColor) {
            return
        }
        
        // 先停止之前的监听
        stopInputMethodMonitoring()
        
        logger.info("🔍 启动输入法状态监听")
        lastInputMethod = InputMethodManager.getCurrentInputMethod()
        
        inputMethodMonitorTimer = Timer("InputMethodMonitor", true)
        inputMethodMonitorTask = object : TimerTask() {
            override fun run() {
                val currentMethod = InputMethodManager.getCurrentInputMethod()
                
                // 如果输入法状态发生变化，更新光标颜色
                if (currentMethod != lastInputMethod && currentMethod != InputMethodType.UNKNOWN) {
                    logger.info("🔄 检测到输入法变化: $lastInputMethod -> $currentMethod")
                    lastInputMethod = currentMethod
                    
                    // 更新光标颜色
                    ApplicationManager.getApplication().invokeLater {
                        val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                        if (activeEditor != null) {
                            updateCursorColor(activeEditor, currentMethod, settings)
                        }
                    }
                }
            }
        }
        
        // 每 200ms 检查一次
        inputMethodMonitorTimer?.schedule(inputMethodMonitorTask, 200L, 200L)
    }
    
    /**
     * 停止输入法状态监听
     */
    private fun stopInputMethodMonitoring() {
        inputMethodMonitorTask?.cancel()
        inputMethodMonitorTask = null
        inputMethodMonitorTimer?.cancel()
        inputMethodMonitorTimer = null
    }
    
    /**
     * 启动大写锁定状态监听
     */
    private fun startCapsLockMonitoring() {
        val settings = inputMethodService.getSettings()
        if (!settings.isEnableCaretColor) {
            return
        }
        
        // 先停止之前的监听
        stopCapsLockMonitoring()
        
        logger.info("🔍 启动大写锁定状态监听")
        lastCapsLockState = isCapsLockOn()
        
        capsLockMonitorTimer = Timer("CapsLockMonitor", true)
        capsLockMonitorTask = object : TimerTask() {
            override fun run() {
                val currentCapsLockState = isCapsLockOn()
                
                // 如果大写锁定状态发生变化
                if (currentCapsLockState != lastCapsLockState) {
                    logger.info("🔄 大写锁定状态变化: $lastCapsLockState -> $currentCapsLockState")
                    lastCapsLockState = currentCapsLockState
                    
                    // 更新光标颜色
                    ApplicationManager.getApplication().invokeLater {
                        val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor
                        if (activeEditor != null) {
                            val currentMethod = InputMethodManager.getCurrentInputMethod()
                            updateCursorColor(activeEditor, currentMethod, settings)
                        }
                    }
                }
            }
        }
        
        // 每 300ms 检查一次
        capsLockMonitorTimer?.schedule(capsLockMonitorTask, 300L, 300L)
    }
    
    /**
     * 停止大写锁定状态监听
     */
    private fun stopCapsLockMonitoring() {
        capsLockMonitorTask?.cancel()
        capsLockMonitorTask = null
        capsLockMonitorTimer?.cancel()
        capsLockMonitorTimer = null
    }
    
    /**
     * 检查大写锁定是否开启
     */
    private fun isCapsLockOn(): Boolean {
        return try {
            java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查自定义规则
     * 返回匹配的规则，如果没有匹配则返回null
     */
    private fun checkCustomPatternRules(
        documentText: String,
        caretOffset: Int,
        fileName: String,
        contextType: ContextType,
        settings: SettingsState
    ): com.wonder.freemacinput.freemacinput.config.CustomPatternRule? {
        if (settings.customPatternRules.isEmpty()) {
            return null
        }
        
        // 获取光标左右两侧的文本
        val leftText = if (caretOffset > 0) {
            documentText.substring(0, caretOffset)
        } else {
            ""
        }
        
        val rightText = if (caretOffset < documentText.length) {
            documentText.substring(caretOffset)
        } else {
            ""
        }
        
        // 获取文件扩展名
        val fileExtension = fileName.substringAfterLast('.', "")
        
        // 遍历所有规则，找到第一个匹配的
        for (rule in settings.customPatternRules) {
            if (rule.matches(leftText, rightText, fileExtension, contextType)) {
                return rule
            }
        }
        
        return null
    }

    private data class EditorData(
        val fileName: String,
        val documentText: String,
        val caretOffset: Int
    )
}
