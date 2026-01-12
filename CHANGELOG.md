# 输入法切换重构 - 更新说明

## 概述

重构了 macOS 和 Windows 的输入法切换方法，使其更可靠、更精确。

## 主要变更

### 1. 新增配置项（SettingsState.kt）

添加了输入源配置字段：
- `macOSChineseInputSource`: macOS 中文输入源 ID
- `macOSEnglishInputSource`: macOS 英文输入源 ID
- `windowsChineseInputLocale`: Windows 中文 Locale ID（默认 0804）
- `windowsEnglishInputLocale`: Windows 英文 Locale ID（默认 0409）

### 2. 重构切换逻辑（InputMethodManager.kt）

#### macOS 切换方法（优先级从高到低）

1. **im-select 工具切换**（最可靠）
   - 精确切换到指定的输入源 ID
   - 需要用户安装 `im-select` 工具：`brew install install-im-select`
   - 支持配置具体的输入源 ID

2. **AppleScript 自动检测切换**
   - 自动检测并切换到匹配的输入法
   - 通过输入法名称匹配
   - 中文：包含 "中文"、"拼音"、"简体"、"Pinyin"、"Simplified"
   - 英文：包含 "ABC"、"U.S."、"English"、"英文"

3. **快捷键切换**（最后的备用方法）
   - 使用 Ctrl+Space 快捷键
   - 依赖系统快捷键设置

#### Windows 切换方法（优先级从高到低）

1. **PowerShell InputLanguage 切换**
   - 使用 `[System.Windows.Forms.InputLanguage]::CurrentInputLanguage` 直接设置
   - 通过 Locale ID 精确切换
   - 支持配置具体的 Locale ID

2. **LoadKeyboardLayout API**
   - 使用 Windows API `LoadKeyboardLayout` 和 `ActivateKeyboardLayout`
   - 备用方案

### 3. 新增工具类（InputSourceHelper.kt）

提供了输入源检测和配置辅助功能：
- `getAllInputSources()`: 获取所有可用输入法
- `getCurrentInputSourceID()`: 获取当前输入源 ID（macOS）
- `isIMSelectInstalled()`: 检查 im-select 工具是否已安装
- `getIMSelectInstallGuide()`: 获取 im-select 安装指南

### 4. 改进配置界面（SettingsConfigurable.kt）

新增输入源配置区域：
- macOS 输入源 ID 配置
- Windows 输入 Locale 配置
- "查看当前输入源" 按钮：
  - 如果安装了 im-select，显示当前输入源 ID
  - 如果未安装，显示所有可用输入法列表
  - 包含 im-select 安装指南

## 使用指南

### macOS 用户

#### 方法 1：使用 im-select（推荐）

1. 安装工具：
   ```bash
   brew install install-im-select
   ```

2. 获取输入源 ID：
   ```bash
   im-select
   ```
   示例输出：
   ```
   com.apple.inputmethod.SCIM.ITABC  # 五笔
   com.apple.keylayout.US  # 英文
   ```

3. 在插件设置中配置：
   - 打开 `Preferences` → `FreeMacInput`
   - 在 "输入源配置" 部分填入输入源 ID
   - 或点击 "查看当前输入源" 获取

#### 方法 2：自动检测

不填写输入源 ID，插件会自动检测。

#### 权限要求

在 macOS 系统设置中：
- 隐私与安全性 → 辅助功能：添加 IntelliJ IDEA
- 隐私与安全性 → 自动化：添加 IntelliJ IDEA

### Windows 用户

#### 配置 Locale ID

1. 打开插件设置 `Preferences` → `FreeMacInput`
2. 在 "输入源配置" 部分：
   - 中文输入 Locale：默认 `0804`（简体中文）
   - 英文输入 Locale：默认 `0409`（美国英语）
3. 可点击 "查看当前输入源" 查看所有可用输入法的 LCID

#### 常用 Locale ID

```
0409  # 英语（美国）
0804  # 中文（简体，中国）
0404  # 中文（繁体，台湾）
0C04  # 中文（繁体，香港）
1004  # 中文（简体，新加坡）
```

## 技术改进

### 1. 切换精确度
- macOS：使用 im-select 可精确到具体的输入源 ID
- Windows：使用 Locale ID 可精确到具体语言

### 2. 容错性
- 多种切换方法按优先级尝试
- 前一个方法失败自动尝试下一个
- 保留快捷键作为最后的备用方案

### 3. 性能优化
- 冷却时间：100ms 避免频繁切换
- 检测缓存：500ms 减少系统调用

### 4. 用户体验
- 配置界面直观
- "查看当前输入源" 按钮方便配置
- 详细的错误提示和安装指南

## 故障排除

### macOS

**问题：输入法无法切换**
1. 检查系统权限（辅助功能、自动化）
2. 安装 im-select 工具
3. 检查输入法菜单是否启用

**问题：切换不准确**
1. 使用 im-select 精确配置输入源 ID
2. 点击 "查看当前输入源" 获取准确 ID

### Windows

**问题：输入法无法切换**
1. 检查 PowerShell 版本（≥ 3.0）
2. 检查执行策略
3. 确认已安装所需输入语言

**问题：切换不准确**
1. 使用精确的 Locale ID
2. 点击 "查看当前输入源" 查看可用 LCID

## 文件变更

- ✏️ `SettingsState.kt`: 添加输入源配置
- ✏️ `SettingsConfigurable.kt`: 添加输入源配置 UI
- ✏️ `InputMethodManager.kt`: 重构切换逻辑
- ✏️ `InputSourceHelper.kt`: 新增工具类
- 📄 `INPUT_METHOD_GUIDE.md`: 新增配置指南

## 下一步

1. 测试新的切换方法
2. 根据用户反馈进一步优化
3. 考虑添加更多平台的支持（Linux）