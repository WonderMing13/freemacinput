#!/bin/bash

echo "=========================================="
echo "   FreeMacInput 输入法诊断工具"
echo "=========================================="
echo ""

# 检查系统输入法
echo "1. 检查已安装的输入法："
osascript -e 'tell application "System Events" to get name of every input source' 2>/dev/null

echo ""
echo "2. 检查系统设置中的输入法（打开设置查看）："
defaults read com.apple.HIToolbox AppleEnabledInputSources 2>/dev/null

echo ""
echo "3. 检查 im-select 状态："
which im-select && im-select || echo "   im-select 未安装或不可用"

echo ""
echo "=========================================="
echo "   诊断结果"
echo "=========================================="
echo ""

# 检查是否有英文输入法
ENGLISH_IM=$(defaults read com.apple.HIToolbox 2>/dev/null | grep -E "ABC|U\.S\.|English|en-")
if [ -z "$ENGLISH_IM" ]; then
    echo "⚠️  问题发现：系统可能没有英文输入法"
    echo ""
    echo "💡 解决方案："
    echo "   1. 打开系统设置："
    echo "      → 键盘 → 输入法"
    echo ""
    echo "   2. 点击 '+' 添加英文输入法"
    echo "   3. 选择 'ABC' 或 'U.S.'"
    echo "   4. 点击添加"
    echo ""
    echo "   5. 确保英文输入法已添加"
    echo ""
    echo "   6. 重新测试插件"
else
    echo "✅ 检测到英文输入法"
    echo "$ENGLISH_IM"
fi

echo ""
echo "=========================================="
echo "   完成诊断，按回车打开系统设置"
read
open "/System/Library/PreferencePanes/Keyboard.prefPane"
