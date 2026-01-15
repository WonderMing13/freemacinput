package com.wonder.freemacinput.freemacinput.core

/**
 * 特殊场景管理器
 * 用于标记当前是否在特殊场景中（Git 提交、工具窗口等）
 * 防止代码区域的监听器覆盖这些场景的输入法设置
 */
object SpecialSceneManager {
    
    @Volatile
    private var inGitCommitScene = false
    
    @Volatile
    private var inToolWindowScene = false
    
    /**
     * 设置是否在 Git 提交场景中
     */
    fun setInGitCommitScene(value: Boolean) {
        inGitCommitScene = value
    }
    
    /**
     * 检查是否在 Git 提交场景中
     */
    fun isInGitCommitScene(): Boolean {
        return inGitCommitScene
    }
    
    /**
     * 设置是否在工具窗口场景中
     */
    fun setInToolWindowScene(value: Boolean) {
        inToolWindowScene = value
    }
    
    /**
     * 检查是否在工具窗口场景中
     */
    fun isInToolWindowScene(): Boolean {
        return inToolWindowScene
    }
    
    /**
     * 检查是否在任何特殊场景中
     */
    fun isInAnySpecialScene(): Boolean {
        return inGitCommitScene || inToolWindowScene
    }
}

// 保持向后兼容的别名
typealias GitCommitSceneManager = SpecialSceneManager
