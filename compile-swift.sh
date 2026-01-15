#!/bin/bash

# 编译 Swift 输入法切换器
# 只在 macOS 上编译

if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "正在编译 Swift 输入法切换器..."
    
    SWIFT_SOURCE="src/main/resources/InputMethodSwitcher.swift"
    OUTPUT_DIR="src/main/resources/bin"
    OUTPUT_FILE="$OUTPUT_DIR/InputMethodSwitcher"
    
    # 创建输出目录
    mkdir -p "$OUTPUT_DIR"
    
    # 编译 Swift 代码，链接必要的框架
    swiftc -o "$OUTPUT_FILE" "$SWIFT_SOURCE" \
        -framework Carbon \
        -framework CoreGraphics \
        -framework ScreenCaptureKit \
        -framework AppKit \
        -O
    
    if [ $? -eq 0 ]; then
        echo "✅ Swift 编译成功: $OUTPUT_FILE"
        # 设置可执行权限
        chmod +x "$OUTPUT_FILE"
        # 显示文件信息
        ls -lh "$OUTPUT_FILE"
    else
        echo "❌ Swift 编译失败"
        exit 1
    fi
else
    echo "⚠️  非 macOS 系统，跳过 Swift 编译"
fi
