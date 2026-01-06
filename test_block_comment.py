#!/usr/bin/env python3
"""
测试块注释检测逻辑
"""

def isInsideBlockComment(text: str, offset: int) -> bool:
    """检测光标是否在块注释内部 - 与修复后的 Kotlin 代码一致"""
    safeOffset = min(offset, len(text))

    # 方法1: 检查光标之前的深度（处理跨行注释）
    depth = 0
    i = 0
    while i < safeOffset:
        if i + 1 <= safeOffset:
            twoChars = text[i:i+2]
            if twoChars == "/*":
                # 计数：如果 /* 开始位置 < 光标位置
                if i < safeOffset:
                    depth += 1
                i += 2
            elif twoChars == "*/":
                # 计数：如果 */ 开始位置 < 光标位置
                if i < safeOffset:
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
    last_newline = text.rfind('\n', 0, safeOffset)
    line_start = last_newline + 1 if last_newline >= 0 else 0
    next_newline = text.find('\n', safeOffset)
    line_end = next_newline if next_newline >= 0 else len(text)
    current_line = text[line_start:line_end]

    line_offset = safeOffset - line_start

    # 查找 /* 和 */ 在当前行的位置
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


def test_case(description: str, text: str, offset: int, expected: bool):
    result = isInsideBlockComment(text, offset)
    status = "✓ PASS" if result == expected else "✗ FAIL"
    print(f"[{status}] {description}")
    print(f"  文本: {repr(text)}")
    print(f"  光标偏移: {offset}")
    print(f"  期望: {expected}, 实际: {result}")
    print()
    return result == expected


def main():
    print("=" * 60)
    print("块注释检测逻辑测试")
    print("=" * 60)
    print()

    all_passed = True

    # 测试用例1: 简单块注释内部
    all_passed &= test_case(
        "简单块注释内部",
        "/* comment */",
        5,  # 光标在注释内部
        True
    )

    # 测试用例2: 块注释外部（前面）
    all_passed &= test_case(
        "块注释外部（前面）",
        "/* comment */ code",
        0,  # 光标在注释前面
        False
    )

    # 测试用例3: 块注释外部（后面）
    all_passed &= test_case(
        "块注释外部（后面）",
        "/* comment */ code",
        15,  # 光标在注释后面
        False
    )

    # 测试用例4: 嵌套块注释 - 在外层内部
    all_passed &= test_case(
        "嵌套块注释 - 在外层内部",
        "/* outer /* inner */ */",
        5,  # 光标在外层内部
        True
    )

    # 测试用例5: 嵌套块注释 - 在内层内部
    all_passed &= test_case(
        "嵌套块注释 - 在内层内部",
        "/* outer /* inner */ */",
        12,  # 光标在内层内部
        True
    )

    # 测试用例6: 嵌套块注释 - 在内层后面但外层闭合前
    all_passed &= test_case(
        "嵌套块注释 - 在内层后面",
        "/* outer /* inner */ */",
        20,  # 光标在内层后面，外层闭合前
        True
    )

    # 测试用例7: 嵌套块注释 - 完全外部
    all_passed &= test_case(
        "嵌套块注释 - 完全外部",
        "/* outer /* inner */ */",
        25,  # 光标在所有注释后面
        False
    )

    # 测试用例8: 多行块注释 - 在注释内部
    all_passed &= test_case(
        "多行块注释 - 在注释内部",
        "/* line1\nline2\nline3 */",
        10,  # 光标在第二行
        True
    )

    # 测试用例9: 多行块注释 - 在注释后面
    all_passed &= test_case(
        "多行块注释 - 在注释后面",
        "/* line1\nline2\nline3 */ code",
        25,  # 光标在注释后面
        False
    )

    # 测试用例10: 空文本
    all_passed &= test_case(
        "空文本",
        "",
        0,
        False
    )

    # 测试用例11: 只有开始标记
    all_passed &= test_case(
        "只有开始标记",
        "/* comment",
        5,  # 光标在未闭合注释内
        True
    )

    # 测试用例12: 开始和结束在一起 - 光标在 */ 上也应该算作注释内
    all_passed &= test_case(
        "开始和结束在一起",
        "/**/",
        2,  # 光标在 */ 上
        True  # 修正：光标在 */ 上仍在注释内
    )

    # 测试用例13: 代码中的注释 - 光标在代码行
    # 注意：偏移16是 */ 的 * 字符位置，不是第三行的代码
    all_passed &= test_case(
        "代码中的注释 - 光标在代码行",
        "code\n/* comment */\ncode",
        18,  # 光标在第三行代码 "code" 的 c 位置
        False
    )

    # 测试用例14: 代码中的注释 - 光标在注释行
    all_passed &= test_case(
        "代码中的注释 - 光标在注释行",
        "code\n/* comment */\ncode",
        7,  # 光标在注释行
        True
    )

    # 测试用例15: 注释中有星号
    all_passed &= test_case(
        "注释中有星号",
        "/* hello * world */",
        10,  # 光标在注释内部
        True
    )

    # 测试用例16: 多个块注释
    all_passed &= test_case(
        "多个块注释 - 在第一个注释内",
        "/* first */ code /* second */",
        3,  # 光标在第一个注释内
        True
    )

    # 测试用例17: 多个块注释 - 在两个注释之间
    all_passed &= test_case(
        "多个块注释 - 在两个注释之间",
        "/* first */ code /* second */",
        13,  # 光标在两个注释之间
        False
    )

    # 测试用例18: 块注释边界 - 光标在 /* 后面
    all_passed &= test_case(
        "块注释边界 - 光标在 /* 后面",
        "/* comment */",
        2,  # 光标在 /* 后面
        True
    )

    # 测试用例19: 块注释边界 - 光标在 */ 前面
    all_passed &= test_case(
        "块注释边界 - 光标在 */ 前面",
        "/* comment */",
        10,  # 光标在 */ 前面
        True
    )

    # 测试用例20: 光标正好在 */ 上 - 应该在注释内
    all_passed &= test_case(
        "块注释边界 - 光标正好在 */",
        "/* comment */",
        11,  # 光标在 */ 位置
        True  # 修正：光标在 */ 上仍在注释内
    )

    print("=" * 60)
    if all_passed:
        print("所有测试通过！")
    else:
        print("有测试失败！")
    print("=" * 60)

    return all_passed


if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
