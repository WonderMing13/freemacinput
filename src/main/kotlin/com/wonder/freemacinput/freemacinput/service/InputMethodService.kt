package com.wonder.freemacinput.freemacinput.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.wonder.freemacinput.freemacinput.config.SettingsState
import com.wonder.freemacinput.freemacinput.core.InputMethodManager
import com.wonder.freemacinput.freemacinput.core.InputMethodType


/**
 * 输入法服务
 *
 * 提供全局的输入法切换控制和设置访问。
 * 本服务作为插件的核心接口，封装了所有与输入法相关的操作。
 *
 * 主要功能：
 * 1. 插件开关控制
 * 2. 各场景的输入法配置管理
 */
@Service(Service.Level.PROJECT)
class InputMethodService(private val project: Project) {

    // 应用级设置状态（通过服务获取）
    private val settingsState: SettingsState by lazy {
        ApplicationManager.getApplication().getService(SettingsState::class.java)
    }



    // ==================== 插件开关 ====================

    /**
     * 是否启用插件
     */
    fun isEnabled(): Boolean = settingsState.isEnabled

    /**
     * 是否显示切换提示
     */
    fun isShowHints(): Boolean = settingsState.isShowHints

    /**
     * 是否启用光标颜色
     */
    fun isEnableCaretColor(): Boolean = settingsState.isEnableCaretColor

    /**
     * 获取当前设置状态
     */
    fun getSettings(): SettingsState = settingsState



    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目实例
         * @return InputMethodService实例
         */
        fun getInstance(project: Project): InputMethodService {
            return project.getService(InputMethodService::class.java)
        }
    }
}
