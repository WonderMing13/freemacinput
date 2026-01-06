#!/usr/bin/env python3
"""
完整测试上下文检测逻辑 - 与 Kotlin 代码保持一致
"""


def get_current_line(text: str, offset: int) -> str:
    safe_offset = min(offset, len(text))
    last_newline = text.rfind('\n', 0, safe_offset)
    line_start = last_newline + 1 if last_newline >= 0 else 0
    next_newline = text.find('\n', safe_offset)
    line_end = next_newline if next_newline >= 0 else len(text)
    return text[line_start:line_end]


def is_in_comment_line(line: str) -> bool:
    trimmed = line.strip()
    if trimmed.startswith("//"):
        return True

    in_string = False
    in_char = False
    i = 0
    while i < len(line):
        char = line[i]
        if char == '"' and (i == 0 or line[i - 1] != '\\'):
            in_string = not in_string
        elif char == '\'' and (i == 0 or line[i - 1] != '\\'):
            in_char = not in_char
        elif not in_string and not in_char and i + 1 < len(line) and line[i] == '/' and line[i + 1] == '/':
            return True
        i += 1

    return False


def is_inside_block_comment(text: str, offset: int) -> bool:
    """检测光标是否在块注释内部 - 与 Kotlin 代码一致"""
    safe_offset = min(offset, len(text))

    # 方法1: 检查光标之前的深度（处理跨行注释）
    depth = 0
    i = 0
    while i < safe_offset:
        if i + 1 <= safe_offset:
            two_chars = text[i:min(i + 2, len(text))]
            if two_chars == "/*":
                # 计数：如果 /* 开始位置 < 光标位置
                if i < safe_offset:
                    depth += 1
                i += 2
            elif two_chars == "*/":
                # 计数：如果 */ 开始位置 < 光标位置
                if i < safe_offset:
                    if depth > 0:
                        depth -= 1
                i += 2
            else:
                i += 1
        else:
            i += 1

    # 如果深度 > 0，光标在注释内
    if depth > 0:
        return True

    # 方法2: 检查当前行内是否有 /* 和 */
    # 获取当前行
    last_newline = text.rfind('\n', 0, safe_offset)
    line_start = last_newline + 1 if last_newline >= 0 else 0
    next_newline = text.find('\n', safe_offset)
    line_end = next_newline if next_newline >= 0 else len(text)
    current_line = text[line_start:line_end]

    # 关键：lineOffset 不能超过当前行长度（光标可能在换行符上）
    line_start_index = text.rfind('\n', 0, safe_offset) + 1
    line_offset = min(safe_offset - line_start_index, len(current_line))

    open_pos = current_line.find("/*")
    close_pos = current_line.find("*/")

    if open_pos < 0:
        # 当前行没有 /*，不在块注释内
        return False

    if close_pos < 0:
        # 有 /* 没有 */，在注释内
        return True

    if close_pos <= open_pos:
        # */ 在 /* 之前或同一位置，不在注释内
        return False

    # 光标在 /* 之后、*/ 之前
    return line_offset > open_pos + 2 and line_offset < close_pos


def detect_context(text: str, is_git_commit: bool, offset: int) -> str:
    if is_git_commit:
        return "GIT_COMMIT"

    current_line = get_current_line(text, offset)
    print(f"    当前行: '{current_line}'")

    if is_in_comment_line(current_line):
        print("    检测到 // 注释行")
        return "COMMENT"

    if is_inside_block_comment(text, offset):
        print("    检测到块注释区域")
        return "COMMENT"

    return "DEFAULT"


def test_case(name: str, text: str, offset: int, expected: str):
    print(f"\n[TEST] {name}")
    print(f"  文本: {repr(text)[:80]}...")
    print(f"  光标偏移: {offset}")

    result = detect_context(text, False, offset)

    status = "PASS" if expected in result or result in expected else "FAIL"
    print(f"  期望: {expected}")
    print(f"  结果: {result}")
    print(f"  [{status}]")

    return expected in result or result in expected


def main():
    print("=" * 60)
    print("上下文检测逻辑完整测试 (与 Kotlin 一致)")
    print("=" * 60)

    all_passed = True

    print("\n" + "=" * 60)
    print("测试 // 注释")
    print("=" * 60)

    all_passed &= test_case(
        "整行注释",
        "/* comment */\n// 这是一行注释\nint x = 1;",
        20, "COMMENT"
    )

    all_passed &= test_case(
        "行内注释",
        "int x = 1; // comment",
        10, "COMMENT"
    )

    all_passed &= test_case(
        "字符串内的 //",
        'String url = "http://";',
        12, "DEFAULT"
    )

    print("\n" + "=" * 60)
    print("测试块注释")
    print("=" * 60)

    all_passed &= test_case(
        "块注释内部",
        "/* comment */",
        5, "COMMENT"
    )

    all_passed &= test_case(
        "块注释外部",
        "/* comment */ code",
        15, "DEFAULT"
    )

    all_passed &= test_case(
        "未闭合注释",
        "/* comment",
        5, "COMMENT"
    )

    all_passed &= test_case(
        "闭合注释边界",
        "/**/",
        1, "COMMENT"
    )

    print("\n" + "=" * 60)
    print("真实场景测试")
    print("=" * 60)

    real_code = """/**
 * 这是一个方法
 */
public void test() {
    // 单行注释
    String msg = "hello";
    /* 块注释 */
    int x = 1;
}
"""

    all_passed &= test_case(
        "真实 - 块注释内",
        real_code, 2, "COMMENT"
    )

    all_passed &= test_case(
        "真实 - 块注释后",
        real_code, 20, "DEFAULT"
    )

    all_passed &= test_case(
        "真实 - 行内注释",
        real_code, 50, "COMMENT"
    )

    all_passed &= test_case(
        "真实 - 块注释行",
        real_code, 87, "COMMENT"
    )

    all_passed &= test_case(
        "真实 - 普通代码",
        real_code, 110, "DEFAULT"
    )

    print("\n" + "=" * 60)
    if all_passed:
        print("所有测试通过!")
    else:
        print("有测试失败!")
    print("=" * 60)

    return all_passed


if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
