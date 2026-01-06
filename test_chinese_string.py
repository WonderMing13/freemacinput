#!/usr/bin/env python3
"""
FreeMacInput 字符串中文检测测试

测试场景：
1. 中文字符串 → 切换到中文
2. 英文字符串 → 保持英文
3. 混合字符串 → 根据比例判断
"""

import re
from enum import Enum
from dataclasses import dataclass
from typing import Optional, Tuple, List

class ContextType(Enum):
    DEFAULT = "DEFAULT"
    COMMENT = "COMMENT"
    STRING = "STRING"
    GIT_COMMIT = "GIT_COMMIT"
    TOOL_WINDOW = "TOOL_WINDOW"
    CUSTOM_EVENT = "CUSTOM_EVENT"
    CUSTOM_RULE = "CUSTOM_RULE"
    UNKNOWN = "UNKNOWN"

@dataclass
class ContextInfo:
    type: ContextType
    tool_window_type: Optional[str]
    reason: str
    string_name: Optional[str] = None
    custom_rule_name: Optional[str] = None

class ContextDetector:
    """上下文检测服务（Python实现，用于测试核心逻辑）"""

    def detect_context(self, document_text: str, is_git_commit: bool, caret_offset: int) -> ContextInfo:
        try:
            print(f"[检测] textLength={len(document_text)}, offset={caret_offset}")

            if is_git_commit:
                return ContextInfo(
                    type=ContextType.GIT_COMMIT,
                    tool_window_type=None,
                    reason="Git提交信息输入"
                )

            current_line = self.get_current_line(document_text, caret_offset)
            print(f"[检测] 当前行: '{current_line}'")

            # 检测字符串
            in_string, string_name, is_chinese = self.detect_string_context(current_line)
            if in_string:
                if is_chinese:
                    print(f"[检测] 检测到中文字符串: {string_name}")
                    return ContextInfo(
                        type=ContextType.STRING,
                        tool_window_type=None,
                        string_name=string_name,
                        reason=f"中文字符串: {string_name}"
                    )
                else:
                    print(f"[检测] 检测到英文字符串: {string_name}")
                    return ContextInfo(
                        type=ContextType.DEFAULT,
                        tool_window_type=None,
                        string_name=string_name,
                        reason=f"英文字符串: {string_name}"
                    )
            elif self.is_in_comment_line(current_line):
                print("[检测] 检测到注释区域")
                return ContextInfo(
                    type=ContextType.COMMENT,
                    tool_window_type=None,
                    reason="注释区域"
                )
            else:
                if self.is_inside_block_comment(document_text, caret_offset):
                    print("[检测] 检测到块注释区域")
                    return ContextInfo(
                        type=ContextType.COMMENT,
                        tool_window_type=None,
                        reason="块注释区域"
                    )
                else:
                    print("[检测] 默认代码区域")
                    return ContextInfo(
                        type=ContextType.DEFAULT,
                        tool_window_type=None,
                        reason="代码区域"
                    )

        except Exception as e:
            print(f"[检测] 异常: {type(e).__name__}: {e}")
            return ContextInfo(
                type=ContextType.UNKNOWN,
                tool_window_type=None,
                reason=f"检测异常: {e}"
            )

    def get_current_line(self, text: str, offset: int) -> str:
        safe_offset = min(offset, len(text))
        last_newline = text.rfind('\n', 0, safe_offset)
        line_start = last_newline + 1 if last_newline >= 0 else 0
        next_newline = text.find('\n', safe_offset)
        line_end = next_newline if next_newline >= 0 else len(text)
        return text[line_start:line_end]

    def detect_string_context(self, line: str) -> Tuple[bool, Optional[str], bool]:
        """检测当前行是否在字符串中，返回 (是否在字符串, 变量名, 是否为中文字符串)"""
        quotes = []
        for i, char in enumerate(line):
            if char == '"' and (i == 0 or line[i-1] != '\\'):
                quotes.append(i)

        if len(quotes) < 2:
            if len(quotes) == 1:
                before_quote = line[:quotes[0]].strip()
                var_name = self.extract_variable_name(before_quote)
                string_content = self.extract_string_content(line, quotes[0], len(line))
                is_chinese = self.is_chinese_content(string_content)
                return (True, var_name, is_chinese)
            return (False, None, False)

        cursor_pos = len(line) - 1

        for i in range(0, len(quotes) - 1, 2):
            open_quote = quotes[i]
            close_quote = quotes[i + 1]

            if cursor_pos >= open_quote and cursor_pos <= close_quote:
                before_quote = line[:open_quote].strip()
                var_name = self.extract_variable_name(before_quote)
                string_content = self.extract_string_content(line, open_quote, close_quote)
                is_chinese = self.is_chinese_content(string_content)
                return (True, var_name, is_chinese)

        return (False, None, False)

    def extract_string_content(self, line: str, start_quote: int, end_quote: int) -> str:
        if start_quote + 1 >= end_quote:
            return ""
        return line[start_quote + 1:min(end_quote, len(line))]

    def is_chinese_content(self, content: str) -> bool:
        """判断字符串是否为中文内容"""
        if not content:
            return False

        chinese_count = 0
        total_count = 0

        # 中文字符范围
        chinese_chars = set()
        for c in range(0x4E00, 0x9FFF + 1):
            chinese_chars.add(chr(c))

        # 中文标点
        chinese_punctuation = set("，。！？；：\"''（）【】《》『』「」")

        for char in content:
            if char in chinese_chars or char in chinese_punctuation:
                chinese_count += 1
            if not char.isspace() and char != '\\':
                total_count += 1

        # 如果中文字符占比超过30%，认为是中文内容
        if total_count > 0:
            return chinese_count / total_count > 0.3
        return False

    def extract_variable_name(self, text: str) -> Optional[str]:
        words = text.split()
        for word in reversed(words):
            if word and all(c.isalnum() or c == '_' for c in word):
                if len(word) >= 2:
                    return word
        return None

    def is_in_comment_line(self, line: str) -> bool:
        trimmed = line.strip()
        if trimmed.startswith("//"):
            return True

        in_string = False
        for i, char in enumerate(line):
            if char == '"' and (i == 0 or line[i-1] != '\\'):
                in_string = not in_string
            if not in_string and i + 1 < len(line) and line[i] == '/' and line[i+1] == '/':
                return True
        return False

    def is_inside_block_comment(self, text: str, offset: int) -> bool:
        safe_offset = min(offset, len(text))
        text_before = text[:safe_offset]
        open_count = text_before.count("/*")
        close_count = text_before.count("*/")
        return open_count > close_count


class TestResult:
    def __init__(self, name: str, passed: bool, message: str = ""):
        self.name = name
        self.passed = passed
        self.message = message

    def __str__(self):
        status = "✓ PASS" if self.passed else "✗ FAIL"
        return f"{status}: {self.name} {self.message}"


def run_tests():
    detector = ContextDetector()
    results = []

    print("\n" + "="*60)
    print("FreeMacInput 中文字符串检测测试")
    print("="*60)

    # ==================== 中文字符串测试 ====================

    def test(name, code, offset, expected_type):
        context = detector.detect_context(code, False, offset)
        passed = context.type == expected_type
        message = f"expected={expected_type.value}, got={context.type.value}, reason={context.reason}"
        results.append(TestResult(name, passed, message))
        return passed

    # 测试1: 纯中文句子
    print("\n--- 测试1: 纯中文字符串 ---")
    code1 = 'val msg = "你好世界"'
    test("中文字符串识别", code1, code1.find("你好"), ContextType.STRING)

    # 测试2: 中文+标点
    print("\n--- 测试2: 中文+标点字符串 ---")
    code2 = 'val tips = "请输入正确的格式！"'
    test("中文标点识别", code2, code2.find("请"), ContextType.STRING)

    # 测试3: 中文+英文混合
    print("\n--- 测试3: 中文+英文混合字符串 ---")
    code3 = 'val mixed = "Error: 文件不存在"'
    test("混合字符串识别", code3, code3.find("Error"), ContextType.STRING)  # 30%中文阈值

    # 测试4: 纯英文URL
    print("\n--- 测试4: 纯英文URL字符串 ---")
    code4 = 'val url = "http://example.com"'
    test("URL字符串(英文)", code4, code4.find("http"), ContextType.DEFAULT)

    # 测试5: 纯英文代码
    print("\n--- 测试5: 纯英文代码字符串 ---")
    code5 = 'val code = "console.log(test)"'
    test("代码字符串(英文)", code5, code5.find("console"), ContextType.DEFAULT)

    # 测试6: 纯数字
    print("\n--- 测试6: 纯数字字符串 ---")
    code6 = 'val num = "12345"'
    test("数字字符串", code6, code4.find("1"), ContextType.DEFAULT)

    # 测试7: 特殊符号
    print("\n--- 测试7: 特殊符号字符串 ---")
    code7 = 'val symbol = "!@#$%"'
    test("特殊符号字符串", code7, code7.find("!"), ContextType.DEFAULT)

    # 测试8: 空字符串
    print("\n--- 测试8: 空字符串 ---")
    code8 = 'val empty = ""'
    test("空字符串", code8, code8.find('"') + 1, ContextType.DEFAULT)

    # 测试9: 中文注释
    print("\n--- 测试9: 中文注释 ---")
    code9 = """// 这是中文注释
val a = 1"""
    test("中文注释识别", code9, code9.find("这是"), ContextType.COMMENT)

    # 测试10: 英文注释
    print("\n--- 测试10: 英文注释 ---")
    code10 = """// This is English comment
val a = 1"""
    test("英文注释识别", code10, code10.find("This"), ContextType.COMMENT)

    # 测试11: 短中文字符串
    print("\n--- 测试11: 短中文字符串 ---")
    code11 = 'val short = "你好"'
    test("短中文字符串", code11, code11.find("你好"), ContextType.STRING)

    # 测试12: 混合标点
    print("\n--- 测试12: 中文+英文混合(高英文比例) ---")
    code12 = 'val msg = "Error: file not found, please check"'
    test("高英文比例字符串", code12, code12.find("Error"), ContextType.DEFAULT)

    # ==================== 输入法切换逻辑测试 ====================

    print("\n--- 输入法切换逻辑测试 ---")

    def test_method(name, context, expected_method):
        method = "中文" if context.type == ContextType.STRING else "英文"
        passed = method == expected_method
        results.append(TestResult(name, passed, f"expected={expected_method}, got={method}"))
        return passed

    # 中文字符串 → 中文
    ctx1 = ContextInfo(
        type=ContextType.STRING,
        tool_window_type=None,
        string_name="test",
        reason="测试"
    )
    test_method("中文字符串→中文", ctx1, "中文")

    # 英文字符串 → 英文
    ctx2 = ContextInfo(
        type=ContextType.DEFAULT,
        tool_window_type=None,
        string_name="test",
        reason="测试"
    )
    test_method("英文字符串→英文", ctx2, "英文")

    # 注释 → 中文 (注释类型返回中文字符串类型)
    ctx3 = ContextInfo(
        type=ContextType.COMMENT,
        tool_window_type=None,
        reason="测试"
    )
    # 注释目前返回 COMMENT 类型，切换逻辑在 EditorEventListener 中决定使用 commentMethod

    # 代码 → 英文
    ctx4 = ContextInfo(
        type=ContextType.DEFAULT,
        tool_window_type=None,
        reason="测试"
    )
    test_method("代码→英文", ctx4, "英文")

    # ==================== 输出结果 ====================

    print("\n" + "="*60)
    print("测试结果汇总")
    print("="*60)

    passed = sum(1 for r in results if r.passed)
    failed = len(results) - passed

    for r in results:
        print(r)

    print(f"\n总计: {len(results)} 个测试, {passed} 通过, {failed} 失败")

    if failed > 0:
        print("\n⚠️  有测试失败，请检查上方详情")
    else:
        print("\n✓ 所有测试通过!")

    return failed == 0


if __name__ == "__main__":
    success = run_tests()
    exit(0 if success else 1)
