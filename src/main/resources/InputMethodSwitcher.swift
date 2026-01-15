#!/usr/bin/env swift
import Carbon
import Foundation
import CoreGraphics
import ScreenCaptureKit
import AppKit

// MARK: - 方案A：截图识别 + 快捷键切换

/// 使用 ScreenCaptureKit 截取状态栏区域
@available(macOS 12.3, *)
func captureStatusBarIcon() async throws -> NSImage? {
    // 获取所有可用的显示器
    let content = try await SCShareableContent.excludingDesktopWindows(false, onScreenWindowsOnly: true)
    
    guard let display = content.displays.first else {
        return nil
    }
    
    // 获取屏幕尺寸
    let displayWidth = display.width
    let displayHeight = display.height
    
    // 状态栏通常在右上角，高度约25-30像素
    // 输入法图标通常在右上角，距离右边缘约50-150像素
    let iconWidth = 60
    let iconHeight = 30
    let rightMargin = 80
    
    // 计算截图区域
    let captureRect = CGRect(
        x: displayWidth - rightMargin - iconWidth,
        y: 0,
        width: iconWidth,
        height: iconHeight
    )
    
    // 配置截图
    let config = SCStreamConfiguration()
    config.sourceRect = captureRect
    config.width = iconWidth
    config.height = iconHeight
    config.scalesToFit = false
    
    // 创建过滤器（截取整个显示器）
    let filter = SCContentFilter(display: display, excludingWindows: [])
    
    // 截图
    let image = try await SCScreenshotManager.captureImage(
        contentFilter: filter,
        configuration: config
    )
    
    return NSImage(cgImage: image, size: NSSize(width: iconWidth, height: iconHeight))
}

/// 分析图标判断是否为中文输入法
/// 通过分析图标的颜色分布和复杂度来判断
func analyzeInputMethodIcon(_ image: NSImage) -> Bool? {
    guard let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
        return nil
    }
    
    let width = cgImage.width
    let height = cgImage.height
    let bytesPerPixel = 4
    let bytesPerRow = bytesPerPixel * width
    let bitsPerComponent = 8
    
    var pixelData = [UInt8](repeating: 0, count: width * height * bytesPerPixel)
    
    guard let context = CGContext(
        data: &pixelData,
        width: width,
        height: height,
        bitsPerComponent: bitsPerComponent,
        bytesPerRow: bytesPerRow,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else {
        return nil
    }
    
    context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
    
    // 统计非透明像素的数量和平均亮度
    var nonTransparentPixels = 0
    var totalBrightness: Double = 0
    var darkPixels = 0  // 暗色像素（可能是文字）
    
    for y in 0..<height {
        for x in 0..<width {
            let offset = (y * width + x) * bytesPerPixel
            let r = Double(pixelData[offset])
            let g = Double(pixelData[offset + 1])
            let b = Double(pixelData[offset + 2])
            let a = Double(pixelData[offset + 3])
            
            // 只计算不透明的像素
            if a > 128 {
                nonTransparentPixels += 1
                let brightness = (r + g + b) / 3.0
                totalBrightness += brightness
                
                // 统计暗色像素（亮度 < 100）
                if brightness < 100 {
                    darkPixels += 1
                }
            }
        }
    }
    
    if nonTransparentPixels == 0 {
        return nil
    }
    
    let averageBrightness = totalBrightness / Double(nonTransparentPixels)
    let darkPixelRatio = Double(darkPixels) / Double(nonTransparentPixels)
    
    // 启发式判断：
    // 1. 中文输入法图标通常有更多的暗色像素（文字）
    // 2. 平均亮度较低
    // 3. 非透明像素较多（图标更复杂）
    
    let isChinese = darkPixelRatio > 0.15 || averageBrightness < 180
    
    // 调试信息
    print("DEBUG: nonTransparentPixels=\(nonTransparentPixels), avgBrightness=\(averageBrightness), darkRatio=\(darkPixelRatio), isChinese=\(isChinese)", to: &standardError)
    
    return isChinese
}

/// 方案A：通过截图识别当前输入法类型
@available(macOS 12.3, *)
func detectInputMethodByScreenCapture() async -> String? {
    do {
        guard let image = try await captureStatusBarIcon() else {
            return nil
        }
        
        guard let isChinese = analyzeInputMethodIcon(image) else {
            return nil
        }
        
        return isChinese ? "chinese" : "english"
    } catch {
        print("ERROR: Failed to capture screen: \(error)", to: &standardError)
        return nil
    }
}

/// 方案A：检测并使用快捷键切换
@available(macOS 12.3, *)
func detectByScreenCaptureAndSwitch(targetType: String, modifiers: [String], keyCode: Int) async -> Bool {
    // 1. 截图识别当前输入法
    guard let currentType = await detectInputMethodByScreenCapture() else {
        return false
    }
    
    print("DEBUG: Current type detected: \(currentType)", to: &standardError)
    
    // 2. 如果已经是目标类型，不需要切换
    if currentType == targetType {
        return true
    }
    
    // 3. 使用快捷键切换
    return switchWithShortcut(modifiers: modifiers, keyCode: keyCode)
}

// 标准错误输出
var standardError = FileHandle.standardError

extension FileHandle: TextOutputStream {
    public func write(_ string: String) {
        guard let data = string.data(using: .utf8) else { return }
        self.write(data)
    }
}

// MARK: - 方案C：使用系统API识别 + 快捷键切换

/// 获取当前输入法ID
func getCurrentInputMethodId() -> String? {
    guard let currentSource = TISCopyCurrentKeyboardInputSource()?.takeRetainedValue() else {
        return nil
    }
    
    guard let sourceID = TISGetInputSourceProperty(currentSource, kTISPropertyInputSourceID) else {
        return nil
    }
    
    let id = Unmanaged<CFString>.fromOpaque(sourceID).takeUnretainedValue() as String
    return id
}

/// 判断输入法是否为中文
func isChineseInputMethod(_ id: String) -> Bool {
    let chineseKeywords = ["SCIM", "TCIM", "Pinyin", "Wubi", "Cangjie", "Zhuyin", "Shuangpin", "ITABC",
                          "sogou", "baidu", "QQInput", "iFlyIME", "Chinese"]
    return chineseKeywords.contains { id.localizedCaseInsensitiveContains($0) }
}

/// 使用快捷键切换输入法（模拟 Control+Space 或自定义快捷键）
func switchWithShortcut(modifiers: [String], keyCode: Int) -> Bool {
    // 将修饰键字符串转换为 CGEventFlags
    var flags: CGEventFlags = []
    for modifier in modifiers {
        switch modifier.lowercased() {
        case "control", "ctrl":
            flags.insert(.maskControl)
        case "option", "alt":
            flags.insert(.maskAlternate)
        case "command", "cmd":
            flags.insert(.maskCommand)
        case "shift":
            flags.insert(.maskShift)
        default:
            break
        }
    }
    
    // 创建按键按下事件
    guard let keyDownEvent = CGEvent(keyboardEventSource: nil, virtualKey: CGKeyCode(keyCode), keyDown: true) else {
        return false
    }
    keyDownEvent.flags = flags
    
    // 创建按键释放事件
    guard let keyUpEvent = CGEvent(keyboardEventSource: nil, virtualKey: CGKeyCode(keyCode), keyDown: false) else {
        return false
    }
    keyUpEvent.flags = flags
    
    // 发送事件
    keyDownEvent.post(tap: .cghidEventTap)
    Thread.sleep(forTimeInterval: 0.05) // 50ms 延迟
    keyUpEvent.post(tap: .cghidEventTap)
    
    return true
}

// MARK: - 方案B：使用系统API直接切换

/// 使用系统API切换到指定输入法
func switchToInputMethod(_ targetId: String) -> Bool {
    // 获取所有输入法
    guard let inputSources = TISCreateInputSourceList(nil, false)?.takeRetainedValue() as? [TISInputSource] else {
        return false
    }
    
    // 查找目标输入法
    for source in inputSources {
        guard let sourceID = TISGetInputSourceProperty(source, kTISPropertyInputSourceID) else {
            continue
        }
        
        let id = Unmanaged<CFString>.fromOpaque(sourceID).takeUnretainedValue() as String
        
        if id == targetId {
            // 切换到该输入法
            let result = TISSelectInputSource(source)
            return result == noErr
        }
    }
    
    return false
}

// MARK: - 主程序

let args = CommandLine.arguments

if args.count < 2 {
    print("Usage:")
    print("  \(args[0]) current              - Get current input method ID")
    print("  \(args[0]) isChinese <id>       - Check if input method is Chinese")
    print("  \(args[0]) switch <id>          - Switch to input method (Strategy B)")
    print("  \(args[0]) shortcut <mods> <key> - Switch using shortcut (Strategy C)")
    print("                                    Example: shortcut control,space 49")
    print("  \(args[0]) detectByScreenCapture <type> <mods> <key> - Screen capture detect (Strategy A)")
    print("                                    Example: detectByScreenCapture chinese control,space 49")
    exit(1)
}

let command = args[1]

// 对于需要异步的命令，使用 Task
if #available(macOS 12.3, *) {
    if command == "detectByScreenCapture" {
        if args.count < 5 {
            print("ERROR: Missing target type, modifiers and key code")
            print("Example: detectByScreenCapture chinese control,space 49")
            exit(1)
        }
        
        let targetType = args[2]
        let modifiersStr = args[3]
        let keyCodeStr = args[4]
        
        guard let keyCode = Int(keyCodeStr) else {
            print("ERROR: Invalid key code: \(keyCodeStr)")
            exit(1)
        }
        
        let modifiers = modifiersStr.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }
        
        // 使用 Task 运行异步代码
        let task = Task {
            let success = await detectByScreenCaptureAndSwitch(targetType: targetType, modifiers: modifiers, keyCode: keyCode)
            if success {
                print("SUCCESS")
                exit(0)
            } else {
                print("ERROR: Failed to detect and switch")
                exit(1)
            }
        }
        
        // 等待任务完成
        RunLoop.main.run()
        exit(0)
    }
}

// 同步命令
switch command {
case "current":
    if let id = getCurrentInputMethodId() {
        print(id)
        exit(0)
    } else {
        print("ERROR: Failed to get current input method")
        exit(1)
    }
    
case "isChinese":
    if args.count < 3 {
        print("ERROR: Missing input method ID")
        exit(1)
    }
    let id = args[2]
    let result = isChineseInputMethod(id)
    print(result ? "true" : "false")
    exit(0)
    
case "switch":
    if args.count < 3 {
        print("ERROR: Missing target input method ID")
        exit(1)
    }
    let targetId = args[2]
    let success = switchToInputMethod(targetId)
    if success {
        print("SUCCESS")
        exit(0)
    } else {
        print("ERROR: Failed to switch to \(targetId)")
        exit(1)
    }
    
case "shortcut":
    if args.count < 4 {
        print("ERROR: Missing modifiers and key code")
        print("Example: shortcut control,space 49")
        exit(1)
    }
    let modifiersStr = args[2]
    let keyCodeStr = args[3]
    
    guard let keyCode = Int(keyCodeStr) else {
        print("ERROR: Invalid key code: \(keyCodeStr)")
        exit(1)
    }
    
    let modifiers = modifiersStr.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }
    
    let success = switchWithShortcut(modifiers: modifiers, keyCode: keyCode)
    if success {
        print("SUCCESS")
        exit(0)
    } else {
        print("ERROR: Failed to send shortcut")
        exit(1)
    }
    
default:
    print("ERROR: Unknown command: \(command)")
    exit(1)
}
