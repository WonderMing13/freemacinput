# FreeMacInput 开发进度

## 当前状态
- 基本功能已完成
- 输入法自动检测已实现
- 配置界面已优化

## 已完成功能

### 1. 输入法检测
- 使用 Swift API 检测系统中所有已安装的输入法
- 自动分类中文/英文输入法
- 过滤无效的输入法ID
- 支持第三方输入法（搜狗、百度等）

### 2. 基本设置页面
- 插件开关
- 显示切换提示开关
- 选择中文输入法（下拉框，自动检测）
- 选择英文输入法（下拉框，自动检测）
- 选择切换方案（A/B/C/im-select）
- 测试切换按钮（切换为英文/中文）
- 排查输入法切换问题链接
- 离开IDE场景配置

### 3. 场景管理
- 代码场景
- 注释场景
- 字符串场景（支持习惯记录）
- Git 提交场景
- 工具窗口场景
- 特殊场景管理（防止冲突）

### 4. 补救功能
- 字符串场景输入补救
- 只删除光标前的连续英文字符

## 待实现功能

### 方案A：图标识别 + 快捷键切换
- [ ] 截取状态栏输入法图标
- [ ] 识别当前输入法（中文/英文）
- [ ] 使用快捷键切换输入法
- [ ] 权限检查（辅助功能 + 录屏）

### 方案B：系统API + 延迟
- [ ] 使用 macOS 系统API切换
- [ ] 处理90ms UI延迟
- [ ] 权限检查（辅助功能）

### 方案C：API识别 + 快捷键
- [ ] 使用系统API识别当前输入法
- [ ] 使用快捷键切换
- [ ] 权限检查（辅助功能）

## 技术栈
- Kotlin
- IntelliJ Platform SDK
- Swift (用于 macOS API 调用)
- im-select (命令行工具)

## 文件结构
```
src/main/kotlin/com/wonder/freemacinput/freemacinput/
├── config/                    # 配置页面
│   ├── BasicSettingsConfigurable.kt
│   ├── RootConfigurable.kt
│   └── ...
├── core/                      # 核心功能
│   ├── InputMethodManager.kt
│   ├── InputMethodDetector.kt
│   ├── SwitchStrategy.kt
│   ├── SpecialSceneManager.kt
│   └── StringInputRescue.kt
├── listener/                  # 监听器
│   ├── EditorEventListener.kt
│   ├── GitCommitListener.kt
│   ├── ToolWindowFocusListener.kt
│   └── IDEFocusListener.kt
└── ui/                        # UI组件
    └── ToastManager.kt
```
