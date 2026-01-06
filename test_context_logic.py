#!/usr/bin/env python3
"""
FreeMacInput 上下文检测逻辑单元测试
测试 ContextDetector 的核心算法
"""

def get_current_line(text, offset):
    """获取当前行"""
    last_newline = text.rfind('\n', 0, offset)
    line_start = last_newline + 1 if last_newline >= 0 else 0
    next_newline = text.find('\n', offset)
    line_end = next_newline if next_newline >= 0 else len(text)
    return text[line_start:line_end]

def is_in_comment_line(line):
    """
    检测行是否包含 // 注释（忽略字符串和字符内部的 //）
    包括：行首注释、行内注释
    """
    in_string = False
    in_char = False
    i = 0
    while i < len(line):
        char = line[i]

        # 处理字符串
        if char == '"' and (i == 0 or line[i - 1] != '\\'):
            in_string = not in_string
        # 处理字符常量
        elif char == "'" and (i == 0 or line[i - 1] != '\\'):
            in_char = not in_char
        # 检测 //
        elif not in_string and not in_char and i + 1 < len(line) and line[i] == '/' and line[i + 1] == '/':
            return True

        i += 1
    return False

def is_inside_block_comment(text, offset):
    """检测光标是否在块注释内部"""
    safe_offset = min(offset, len(text))

    # 方法1: 检查光标之前的深度
    depth = 0
    i = 0
    while i < safe_offset:
        if i + 1 <= safe_offset:
            two_chars = text[i:min(i + 2, len(text))]
            if two_chars == "/*":
                if i < safe_offset:
                    depth += 1
                i += 2
            elif two_chars == "*/":
                if i < safe_offset:
                    if depth > 0:
                        depth -= 1
                i += 2
            else:
                i += 1
        else:
            i += 1

    if depth > 0:
        return True

    # 方法2: 检查当前行
    current_line = get_current_line(text, safe_offset)
    line_start_index = text.rfind('\n', 0, safe_offset) + 1
    line_offset = min(safe_offset - line_start_index, len(current_line))

    open_pos = current_line.find("/*")
    close_pos = current_line.find("*/")

    if open_pos < 0:
        return False
    if close_pos < 0:
        return True
    if close_pos <= open_pos:
        return False

    return line_offset > open_pos + 2 and line_offset < close_pos

def detect_context(text, offset):
    """检测上下文类型"""
    # 检查 // 注释
    current_line = get_current_line(text, offset)
    if is_in_comment_line(current_line):
        return "COMMENT"

    # 检查块注释
    if is_inside_block_comment(text, offset):
        return "COMMENT"

    # 默认
    return "DEFAULT"

# ==================== 测试用例 ====================

def test_case(name, text, offset, expected):
    """执行单个测试用例"""
    result = detect_context(text, offset)
    status = "✅ PASS" if result == expected else "❌ FAIL"
    print(f"{status}: {name}")
    if result != expected:
        print(f"   期望: {expected}, 结果: {result}")
        print(f"   文本: {repr(text)}")
        print(f"   偏移: {offset}")
    return result == expected

def main():
    print("=" * 60)
    print("FreeMacInput 上下文检测单元测试")
    print("=" * 60)
    print()

    all_passed = True

    # 测试 // 注释
    print("【// 注释测试】")
    all_passed &= test_case("单行注释", "code\n// comment\ncode", 7, "COMMENT")
    all_passed &= test_case("行首注释", "// comment", 2, "COMMENT")
    all_passed &= test_case("注释后代码", "code // comment", 16, "COMMENT")
    print()

    # 测试块注释
    print("【块注释测试】")
    all_passed &= test_case("简单块注释", "/* comment */", 3, "COMMENT")
    all_passed &= test_case("块注释内部", "code\n/* comment */\ncode", 7, "COMMENT")
    all_passed &= test_case("块注释外", "code\n/* comment */\ncode", 20, "DEFAULT")
    all_passed &= test_case("多行块注释", "code\n/* line1\nline2 */", 12, "COMMENT")
    print()

    # 测试代码行
    print("【代码行测试】")
    all_passed &= test_case("普通代码", "int x = 1;", 3, "DEFAULT")
    all_passed &= test_case("字符串代码", "String s = \"hello\";", 10, "DEFAULT")
    all_passed &= test_case("方法定义", "public void test() {}", 10, "DEFAULT")
    print()

    # 测试空行
    print("【空行测试】")
    all_passed &= test_case("空行", "code\n\ncode", 6, "DEFAULT")
    print()

    print("=" * 60)
    if all_passed:
        print("✅ 所有测试通过!")
    else:
        print("❌ 有测试失败")
    print("=" * 60)

    return 0 if all_passed else 1

if __name__ == "__main__":
    exit(main())
