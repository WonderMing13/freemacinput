# FreeMacInput 使用指南

## 功能说明

FreeMacInput 是一个智能输入法切换插件，可以根据编辑上下文自动切换输入法：

- **代码区域**：自动切换为英文输入法
- **注释区域**：自动切换为中文输入法（可配置）
- **字符串区域**：自动切换为中文输入法（可配置）

## 系统要求

### macOS

1. **安装 im-select 工具**（推荐）：
   ```bash
   brew install im-select
   ```

2. **授予权限**：
   - 打开 `系统设置` → `隐私与安全性` → `辅助功能`
   - 添加 IntelliJ IDEA 并授予权限
   - 打开 `系统设置` → `隐私与安全性` → `自动化`
   - 添加 IntelliJ IDEA 并授予权限

3. **配置输入法**（可选）：
   - 如果安装了 im-select，可以在插件设置中配置输入法 ID
   - 获取输入法 ID：在终端运行 `im-select`
   - 示例：
     - 英文：`com.apple.keylayout.ABC`
     - 中文拼音：`com.apple.inputmethod.SCIM.Pinyin`

### Windows

1. **配置输入法 Locale ID**（可选）：
   - 常用 Locale ID：
     - 英文（美国）：`0409`
     - 中文（简体）：`0804`
     - 中文（繁体）：`0404`

2. **确保已安装所需的输入语言**：
   - 打开 `设置` → `时间和语言` → `语言和区域`
   - 添加所需的输入语言

## 使用方法

### 1. 启用插件

- 打开 `Preferences/Settings` → `FreeMacInput`
- 勾选 `启用插件`

### 2. 配置输入法策略

- **代码区域默认输入法**：选择在代码区域使用的输入法（默认：英文）
- **注释区域默认输入法**：选择在注释区域使用的输入法（默认：中文）

### 3. 配置提示选项

- **显示切换提示**：切换输入法时显示 Toast 提示
- **启用光标颜色提示**：根据输入法改变光标颜色

### 4. 测试功能

创建一个测试文件，例如 `Test.java`：

```java
public class Test {
    // 这是中文注释，光标移到这里会自动切换为中文输入法
    
    public void method() {
        // 在注释中输入中文
        String message = "字符串中也可以输入中文";
        
        int count = 10; // 代码区域会自动切换为英文
    }
    
    /*
     * 块注释也支持
     * 自动切换为中文
     */
}
```

## 支持的语言

### 完全支持
- Java
- Kotlin
- JavaScript/TypeScript
- C/C++
- Go

### 部分支持
- Python（`#` 注释暂不支持，将在后续版本添加）
- HTML/XML（`<!-- -->` 注释暂不支持）

## 故障排除

### macOS 输入法无法切换

1. **检查 im-select 是否安装**：
   ```bash
   which im-select
   im-select
   ```

2. **检查系统权限**：
   - 确保 IntelliJ IDEA 在 `辅助功能` 和 `自动化` 中有权限

3. **查看日志**：
   - 打开 `Help` → `Show Log in Finder`
   - 搜索 `InputMethodManager` 查看详细日志

### Windows 输入法无法切换

1. **检查 PowerShell 版本**：
   ```powershell
   $PSVersionTable.PSVersion
   ```
   确保版本 ≥ 3.0

2. **检查输入语言**：
   - 确保已安装所需的输入语言

### 切换不准确

1. **配置精确的输入法 ID**（macOS）：
   - 运行 `im-select` 获取当前输入法 ID
   - 在插件设置中配置

2. **配置精确的 Locale ID**（Windows）：
   - 在插件设置中配置

## 性能优化

- 插件内置了 300ms 的冷却时间，避免频繁切换
- 输入法检测结果有 500ms 缓存，减少系统调用
- 文档文本缓存，避免重复读取

## 反馈与支持

如果遇到问题或有建议，请：
1. 查看日志文件
2. 检查系统权限和配置
3. 提交 Issue 到项目仓库

## 更新日志

### v1.0.0
- 初始版本
- 支持 macOS 和 Windows
- 支持代码、注释、字符串区域检测
- 支持 im-select 工具（macOS）
- 支持 PowerShell 切换（Windows）
- 支持 AppleScript 自动检测切换（macOS）
