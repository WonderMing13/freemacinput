#!/bin/bash

# FreeMacInput 插件验证脚本
# 验证 JAR 包是否包含必要的组件

JAR_FILE="${1:-build/libs/FreeMacInput-1.0.0.jar}"

echo "=== FreeMacInput 插件验证 ==="
echo "JAR 文件: $JAR_FILE"
echo ""

if [ ! -f "$JAR_FILE" ]; then
    echo "❌ 错误: JAR 文件不存在"
    exit 1
fi

echo "📦 JAR 文件信息:"
unzip -l "$JAR_FILE" | head -5
echo ""

echo "🔍 检查必需组件:"
echo ""

# 检查 plugin.xml
if unzip -l "$JAR_FILE" | grep -q "META-INF/plugin.xml"; then
    echo "✅ plugin.xml 存在"
    echo "   内容预览:"
    unzip -p "$JAR_FILE" META-INF/plugin.xml | head -15
else
    echo "❌ plugin.xml 不存在"
fi
echo ""

# 检查 StartupActivity
if unzip -l "$JAR_FILE" | grep -q "StartupActivity.class"; then
    echo "✅ StartupActivity.class 存在"
else
    echo "❌ StartupActivity.class 不存在"
fi

# 检查 EditorEventListener
if unzip -l "$JAR_FILE" | grep -q "EditorEventListener.class"; then
    echo "✅ EditorEventListener.class 存在"
else
    echo "❌ EditorEventListener.class 不存在"
fi

# 检查 ContextDetector
if unzip -l "$JAR_FILE" | grep -q "ContextDetector.class"; then
    echo "✅ ContextDetector.class 存在"
else
    echo "❌ ContextDetector.class 不存在"
fi

# 检查 InputMethodManager
if unzip -l "$JAR_FILE" | grep -q "InputMethodManager.class"; then
    echo "✅ InputMethodManager.class 存在"
else
    echo "❌ InputMethodManager.class 不存在"
fi

# 检查 SettingsConfigurable
if unzip -l "$JAR_FILE" | grep -q "SettingsConfigurable.class"; then
    echo "✅ SettingsConfigurable.class 存在"
else
    echo "❌ SettingsConfigurable.class 不存在"
fi

echo ""
echo "=== 验证完成 ==="
