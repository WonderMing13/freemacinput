package com.wonder.freemacinput.freemacinput.config

/**
 * 离开IDE时的输入法切换策略
 */
enum class LeaveIDEStrategy {
    /**
     * 切换为进入IDE前的状态
     */
    RESTORE_PREVIOUS,
    
    /**
     * 切换为英文
     */
    ENGLISH,
    
    /**
     * 切换为母语（中文）
     */
    NATIVE_LANGUAGE,
    
    /**
     * 不切换
     */
    NO_CHANGE;
    
    fun getDisplayName(): String {
        return when (this) {
            RESTORE_PREVIOUS -> "切换为进入IDE前的状态"
            ENGLISH -> "英文"
            NATIVE_LANGUAGE -> "母语（中文）"
            NO_CHANGE -> "不切换"
        }
    }
    
    companion object {
        fun fromDisplayName(name: String): LeaveIDEStrategy {
            return values().find { it.getDisplayName() == name } ?: RESTORE_PREVIOUS
        }
    }
}
