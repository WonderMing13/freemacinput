#!/usr/bin/env swift
import Carbon

let inputSources = TISCreateInputSourceList(nil, false).takeRetainedValue() as! [TISInputSource]

for source in inputSources {
    if let sourceID = TISGetInputSourceProperty(source, kTISPropertyInputSourceID) {
        let id = Unmanaged<CFString>.fromOpaque(sourceID).takeUnretainedValue() as String
        
        // 只输出键盘输入法和输入模式
        if let category = TISGetInputSourceProperty(source, kTISPropertyInputSourceCategory) {
            let cat = Unmanaged<CFString>.fromOpaque(category).takeUnretainedValue() as String
            if cat == "TISCategoryKeyboardInputSource" || cat == "TISCategoryInputMode" {
                if let sourceName = TISGetInputSourceProperty(source, kTISPropertyLocalizedName) {
                    let name = Unmanaged<CFString>.fromOpaque(sourceName).takeUnretainedValue() as String
                    print("\(id)|\(name)")
                } else {
                    print("\(id)|")
                }
            }
        }
    }
}
