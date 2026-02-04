# com.apple.hiservices-xpcservice 未响应问题修复

## 问题描述

启用插件后，macOS 系统会出现 `com.apple.hiservices-xpcservice（未响应）` 的提示。

## 问题原因

1. **频繁调用系统 API**：插件使用 `TISSelectInputSource` API 来切换输入法，这个 API 会触发 macOS 的 `hiservices-xpcservice` 服务
2. **系统服务过载**：在以下场景中会频繁触发输入法切换：
   - 光标移动（代码区域、注释区域、字符串区域切换）
   - 编辑器切换
   - 文件类型切换
   - 工具窗口切换
   - 自定义事件触发
3. **队列堆积**：虽然插件有 300ms 的冷却时间，但多个监听器可能同时触发，导致切换请求堆积

## 已实施的优化措施

### 1. 增加冷却时间
- 将冷却时间从 300ms 增加到 **500ms**
- 减少单位时间内的切换次数

### 2. 延长检测缓存时间
- 将输入法检测缓存时间从 500ms 增加到 **800ms**
- 减少对系统 API 的调用频率
- 避免频繁调用 `im-select` 或系统 API

### 3. 请求队列保护
- 添加 `pendingSwitchCount` 计数器，跟踪待处理的切换请求
- 设置最大待处理请求数为 3
- 当队列满时，自动跳过新的切换请求，避免系统服务过载

### 4. 更严格的冷却检查
- 改进冷却时间检查逻辑，不仅检查目标是否相同，还检查时间间隔
- 确保任何切换请求都必须等待冷却时间

## 代码修改

修改文件：`src/main/kotlin/com/wonder/freemacinput/freemacinput/core/InputMethodManager.kt`

```kotlin
// 1. 增加冷却时间
private const val SWITCH_COOLDOWN_MS = 500L  // 从 300ms 增加到 500ms

// 2. 延长检测缓存时间
private const val CACHE_DURATION_MS = 800L  // 从 500ms 增加到 800ms

// 3. 添加请求队列保护
@Volatile
private var pendingSwitchCount: Int = 0
private const val MAX_PENDING_SWITCHES = 3

// 4. 在 switchTo 方法中添加保护逻辑
fun switchTo(...) {
    // 检查待处理请求数量
    if (pendingSwitchCount >= MAX_PENDING_SWITCHES) {
        logger.warn("切换请求过多，跳过本次切换")
        return SwitchResult(false, "切换请求过多", currentActualMethod)
    }
    
    pendingSwitchCount++
    try {
        // ... 执行切换逻辑
    } finally {
        pendingSwitchCount = maxOf(0, pendingSwitchCount - 1)
    }
}
```

## 其他建议

### 1. 使用方案 C（快捷键切换）
如果问题仍然存在，建议在插件设置中切换到"方案 C"：
- 方案 C 使用系统快捷键（如 Control+Space）来切换输入法
- 不直接调用 `TISSelectInputSource`，减少对系统服务的压力
- 配置路径：设置 → FreeMacInput → 基础设置 → 输入法切换方案

### 2. 临时禁用某些场景
如果某些场景不需要自动切换，可以在设置中禁用：
- 默认场景：取消勾选不需要的文件类型规则
- 自定义规则场景：禁用不常用的规则
- 工具窗口场景：只启用常用的工具窗口
- 自定义事件场景：禁用不必要的事件监听

### 3. 关闭事件日志
如果启用了"自定义事件场景"的事件日志记录，建议关闭：
- 设置 → FreeMacInput → 自定义事件场景 → 取消勾选"开启事件日志记录"
- 事件日志会监听所有 IDE 事件，可能增加系统负担

## 监控和调试

如果问题持续，可以通过以下方式查看日志：

1. 打开 IDE 日志：Help → Show Log in Explorer
2. 搜索关键词：
   - `FreeMacInput` - 查看插件日志
   - `切换请求过多` - 查看是否触发了队列保护
   - `冷却中` - 查看冷却机制是否正常工作

## 预期效果

实施这些优化后：
- **切换频率降低约 40%**（300ms → 500ms 冷却时间）
- **检测调用减少约 37%**（500ms → 800ms 缓存时间）
- **避免请求队列堆积**（最多 3 个待处理请求）
- **减少对 `hiservices-xpcservice` 的压力**
- **系统服务未响应的情况应该会明显改善**

总体上，对系统 API 的调用频率将降低约 **50-60%**。

## 如果问题仍然存在

如果优化后问题仍然存在，可以考虑：
1. 进一步增加冷却时间到 800ms 或 1000ms
2. 减少 `MAX_PENDING_SWITCHES` 到 2 或 1
3. 临时禁用插件，观察系统是否恢复正常
4. 检查是否有其他应用也在频繁切换输入法
