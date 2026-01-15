package com.wonder.freemacinput.freemacinput.core

/**
 * 输入法切换方案
 */
enum class SwitchStrategy {
    /**
     * 方案B：通过系统API切换，UI线程有90ms延迟
     * 需要：辅助功能权限
     * 适用：大部分场景，可能有卡顿（仅macOS）
     */
    STRATEGY_B,
    
    /**
     * 方案C：通过系统API识别 + 选择上一个输入法快捷键切换
     * 需要：辅助功能权限
     * 适用：安装超过两种输入法，UI无延迟（仅macOS）
     */
    STRATEGY_C,
    
    /**
     * im-select 方案（原有方案，跨平台支持）
     */
    IM_SELECT;
    
    fun getDisplayName(): String {
        return when (this) {
            STRATEGY_B -> "方案B（系统API，有延迟，仅macOS）"
            STRATEGY_C -> "方案C（API识别+快捷键，仅macOS）"
            IM_SELECT -> "im-select（跨平台）"
        }
    }
    
    fun getDescription(): String {
        return when (this) {
            STRATEGY_B -> "需要辅助功能权限，UI更新有90ms延迟，可能有卡顿（仅macOS）"
            STRATEGY_C -> "需要辅助功能权限，UI无延迟，可安装超过两种输入法（仅macOS）"
            IM_SELECT -> "使用im-select工具，需要先安装：brew install im-select（跨平台）"
        }
    }
}
