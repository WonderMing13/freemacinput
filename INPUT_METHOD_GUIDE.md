# FreeMacInput 输入法切换配置指南

## 概述

FreeMacInput 支持多种输入法切换方法，优先使用最可靠的方法，并提供备用方案。

## macOS 输入法切换

### 切换方法优先级

1. **im-select 工具切换**（最可靠）
   - 精确切换到指定的输入源 ID
   - 需要安装 `im-select` 工具

2. **AppleScript 自动检测切换**
   - 自动检测并切换到匹配的输入法
   - 不需要额外工具

3. **快捷键切换**（备用）
   - 使用系统快捷键（默认 Ctrl+Space）
   - 依赖系统设置

### 配置方法

#### 方法 1：使用 im-select（推荐）

1. 安装 im-select 工具：
   ```bash
   brew install install-im-select
   ```

2. 获取当前输入源 ID：
   ```bash
   im-select
   ```
   输出示例：
   ```
   com.apple.inputmethod.SCIM.ITABC  # 五笔
   com.apple.inputmethod.TCIM.Cangjie  # 仓颉
   com.apple.inputmethod.SCIM.Pinyin  # 拼音
   com.apple.keylayout.US  # 英文
   ```

3. 在插件设置中配置：
   - 打开 `Preferences` → `FreeMacInput`
   - 在 "输入源配置" 部分：
     - 将中文输入源 ID 填入 "中文输入源 ID"
     - 将英文输入源 ID 填入 "英文输入源 ID"
   - 点击 "查看当前输入源" 按钮可快速获取

#### 方法 2：自动检测（无需配置）

不填写输入源 ID，插件会自动检测并切换：
- 中文输入法：名称包含 "中文"、"拼音"、"简体"、"Pinyin"、"Simplified"
- 英文输入法：名称包含 "ABC"、"U.S."、"English"、"英文"

### 权限要求

在 macOS 上，请确保：
1. 系统设置 → 隐私与安全性 → 辅助功能
   - 添加 IntelliJ IDEA
2. 系统设置 → 隐私与安全性 → 自动化
   - 添加 IntelliJ IDEA

## Windows 输入法切换

### 切换方法优先级

1. **PowerShell InputLanguage 切换**（推荐）
   - 直接设置当前输入语言
   - 使用 Locale ID 精确切换

2. **LoadKeyboardLayout API**（备用）
   - 使用 Windows API 加载键盘布局

### 配置方法

1. 获取系统输入语言 Locale ID：
   - 打开插件设置
   - 点击 "查看当前输入源" 按钮查看可用输入法

2. 常用 Locale ID：
   ```
   0409  # 英语（美国）
   0804  # 中文（简体，中国）
   0404  # 中文（繁体，台湾）
   0C04  # 中文（繁体，香港特别行政区）
   1404  # 中文（繁体，澳门特别行政区）
   1004  # 中文（简体，新加坡）
   ```

3. 在插件设置中配置：
   - 将中文 Locale ID 填入 "中文输入 Locale"
   - 将英文 Locale ID 填入 "英文输入 Locale"

### 权限要求

在 Windows 上，请确保：
- Windows 设置 → 选项 → 设备 → 输入
- 已安装所需的输入语言

## 故障排除

### macOS

**问题：输入法无法切换**

1. 检查系统权限：
   - 打开 `系统设置` → `隐私与安全性`
   - 确保 IntelliJ IDEA 在 `辅助功能` 和 `自动化` 中有权限

2. 检查快捷键设置：
   - 打开 `系统设置` → `键盘` → `键盘快捷键`
   - 确认输入法切换快捷键（通常为 `Control + Space`）

3. 检查输入法菜单：
   - 打开 `系统设置` → `键盘`
   - 确保在菜单栏中显示了输入法菜单

4. 安装 im-select 工具：
   ```bash
   brew install install-im-select
   ```
   然后配置输入源 ID

**问题：切换不准确**

1. 使用 im-select 工具精确配置
2. 点击 "查看当前输入源" 按钮获取准确的 ID

### Windows

**问题：输入法无法切换**

1. 检查 PowerShell 版本：
   ```powershell
   $PSVersionTable.PSVersion
   ```
   确保版本 ≥ 3.0

2. 检查执行策略：
   ```powershell
   Get-ExecutionPolicy
   ```
   应该返回 `RemoteSigned` 或 `Unrestricted`

3. 检查输入语言安装：
   - 打开 `设置` → `时间和语言` → `语言和区域`
   - 确保已安装所需的输入语言

**问题：切换不准确**

1. 使用精确的 Locale ID
2. 点击 "查看当前输入源" 查看可用输入法的 LCID

## 性能优化

### 冷却时间

插件内置了 100ms 的冷却时间，避免频繁切换：
- 同一目标输入法在 100ms 内不会重复切换
- 减少不必要的系统调用

### 检测缓存

当前输入法检测结果有 500ms 缓存：
- 减少系统调用
- 提高响应速度

## 高级配置

### 自定义快捷键

默认使用 `Ctrl + Space` 切换。如果需要自定义：

1. 修改系统快捷键设置
2. 插件会自动使用系统默认的切换快捷键

### 禁用视觉提示

如果不需要提示，可以在插件设置中关闭：
- 取消勾选 "显示切换提示"
- 取消勾选 "启用光标颜色提示"

## 调试

### 查看日志

在 IntelliJ IDEA 中：
1. 打开 `Help` → `Show Log in Finder` (macOS) 或 `Show Log in Explorer` (Windows)
2. 查找包含 `InputMethodManager` 的日志

### 测试输入法切换

使用终端命令测试：

**macOS:**
```bash
# 测试 im-select
im-select

# 测试切换
im-select com.apple.inputmethod.SCIM.Pinyin
```

**Windows:**
```powershell
# 测试 PowerShell
Add-Type -AssemblyName System.Windows.Forms
[System.Windows.Forms.InputLanguage]::CurrentInputLanguage
```

## 获取帮助

如果遇到问题：
1. 查看 IntelliJ IDEA 日志
2. 检查系统权限和设置
3. 尝试安装 im-select 工具（macOS）
4. 提交 Issue 到项目仓库