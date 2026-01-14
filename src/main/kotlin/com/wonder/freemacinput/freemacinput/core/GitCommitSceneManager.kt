package com.wonder.freemacinput.freemacinput.core

/**
 * Git 提交场景管理器
 * 用于标记当前是否在 Git 提交场景中
 */
object GitCommitSceneManager {
    
    @Volatile
    private var inGitCommitScene = false
    
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
}
