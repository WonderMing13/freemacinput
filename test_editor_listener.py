#!/usr/bin/env python3
"""
测试 FreeMacInput 编辑器事件监听逻辑
移植自 Kotlin 代码，完全匹配
"""

import re
from typing import Optional, Tuple, List

# ==================== ContextType 常量 ====================

class ContextType:
    DEFAULT = "DEFAULT"
    COMMENT = "COMMENT"
    STRING = "STRING"
    GIT_COMMIT = "GIT_COMMIT"
    TOOL_WINDOW = "TOOL_WINDOW"
    CUSTOM_EVENT = "CUSTOM_EVENT"
    CUSTOM_RULE = "CUSTOM_RULE"
    UNKNOWN = "UNKNOWN"

class ContextInfo:
    def __init__(self, type: str, reason: str = ""):
        self.type = type
        self.reason = reason

    def __eq__(self, other):
        return self.type == other.type

    def __repr__(self):
        return f"ContextInfo(type={self.type}, reason={self.reason})"

# ==================== 从 Kotlin 移植的核心逻辑 ====================

def is_chinese_char(char: str) -> bool:
    """检查是否是中文字符"""
    code = ord(char)
    return 0x4E00 <= code <= 0x9FFF

def is_chinese_content(content: str) -> bool:
    """检查内容是否为中文（30%阈值）"""
    if not content:
        return False

    chinese_punctuation = set([
        '\uFF0C', '\u3002', '\uFF01', '\uFF1F', '\uFF1B', '\uFF1A',  # ，。！？；：
        '\u201C', '\u201D', '\u2018', '\u2019', '\uFF08', '\uFF09',  # ""''
        '\u3010', '\u3011', '\u300A', '\u300B', '\u300E', '\u300F',  # 【】《》
        '\u3014', '\u3015', '\u300C', '\u300D'                       # 『』「」
    ])

    chinese_count = 0
    total_count = 0

    for char in content:
        # 统计中文字符
        if '\u4E00' <= char <= '\u9FFF':
            chinese_count += 1
        elif char in chinese_punctuation:
            chinese_count += 1
        elif '\uFF00' <= char <= '\uFFEF':  # 全角符号
            chinese_count += 1
        elif '\u3000' <= char <= '\u303F':  # CJK符号
            chinese_count += 1

        # 只统计有效字符
        if not char.isspace() and char != '\\':
            total_count += 1

    # 如果中文字符占比超过30%
    return total_count > 0 and (chinese_count / total_count) > 0.3

def is_git_commit_file(fileName: str) -> bool:
    """检查是否是 Git 提交文件"""
    return "COMMIT_EDITMSG" in fileName or fileName.endswith(".tmp")

def get_current_line(text: str, offset: int) -> str:
    """获取光标所在的当前行"""
    safe_offset = min(offset, len(text))

    last_newline = text.rfind('\n', 0, safe_offset)
    line_start = last_newline + 1 if last_newline >= 0 else 0

    next_newline = text.find('\n', safe_offset)
    line_end = next_newline if next_newline >= 0 else len(text)

    return text[line_start:line_end]

def is_in_comment_line(line: str) -> bool:
    """检测当前行是否是注释行"""
    trimmed = line.strip()

    # 整行注释
    if trimmed.startswith("//"):
        return True

    # 行内注释检测
    in_string = False
    for i, char in enumerate(line):
        if char == '"' and (i == 0 or line[i - 1] != '\\'):
            in_string = not in_string
        if not in_string and i + 1 < len(line) and line[i] == '/' and line[i + 1] == '/':
            return True

    return False

def is_inside_block_comment(text: str, offset: int) -> bool:
    """检测光标是否在块注释内部"""
    safe_offset = min(offset, len(text))
    text_before = text[:safe_offset]

    open_count = text_before.count("/*")
    close_count = text_before.count("*/")

    return open_count > close_count

def detect_string_context(line: str) -> Tuple[bool, Optional[str], bool]:
    """检测当前行是否在字符串中"""
    quotes = []
    for i, char in enumerate(line):
        if char == '"' and (i == 0 or line[i - 1] != '\\'):
            quotes.append(i)

    if len(quotes) < 2:
        if len(quotes) == 1:
            before_quote = line[:quotes[0]].strip()
            var_name = extract_variable_name(before_quote)
            string_content = line[quotes[0]+1:]
            is_chinese = is_chinese_content(string_content)
            return True, var_name, is_chinese
        return False, None, False

    # 光标在行末
    cursor_pos = len(line) - 1

    for i in range(0, len(quotes) - 1, 2):
        open_quote = quotes[i]
        close_quote = quotes[i + 1]

        if open_quote <= cursor_pos <= close_quote:
            before_quote = line[:open_quote].strip()
            var_name = extract_variable_name(before_quote)
            string_content = line[open_quote+1:close_quote]
            is_chinese = is_chinese_content(string_content)
            return True, var_name, is_chinese

    return False, None, False

def extract_variable_name(text: str) -> Optional[str]:
    """从文本中提取变量名"""
    words = re.split(r'\s+', text.strip())
    for word in reversed(words):
        if word and all(c.isalnum() or c == '_' for c in word):
            if len(word) >= 2:
                return word
    return None

# ==================== 核心 detectContext 函数（完全匹配 Kotlin） ====================

def detect_context(documentText: str, isGitCommit: bool, caretOffset: int) -> ContextInfo:
    """检测当前编辑上下文场景"""
    # 空文本处理
    if not documentText or caretOffset <= 0:
        return ContextInfo(ContextType.DEFAULT, "空文本或无效偏移")

    # 边界检查
    safeOffset = min(caretOffset, len(documentText))

    # Git 提交消息特殊处理
    if isGitCommit:
        return ContextInfo(ContextType.GIT_COMMIT, "Git提交信息输入")

    # 2. 检测光标所在的当前行
    currentLine = get_current_line(documentText, safeOffset)

    # 检测字符串
    in_string, stringName, isChineseString = detect_string_context(currentLine)
    if in_string:
        if isChineseString:
            return ContextInfo(ContextType.STRING, f"中文字符串: {stringName}")
        else:
            return ContextInfo(ContextType.DEFAULT, f"英文字符串: {stringName}")

    # 检测注释区域（当前行是注释）
    if is_in_comment_line(currentLine):
        return ContextInfo(ContextType.COMMENT, "注释区域")

    # 检测块注释
    if is_inside_block_comment(documentText, safeOffset):
        return ContextInfo(ContextType.COMMENT, "块注释区域")

    # 默认返回代码区域
    return ContextInfo(ContextType.DEFAULT, "代码区域")

# ==================== 测试用例 ====================

TEST_CASES = [
    # (测试名称, 文件名, 文档内容, 光标偏移, 期望的上下文类型)
    # 核心功能测试

    ("普通代码区域", "Test.java", "public void test() {\n    int x = 1;\n}", 30, ContextType.DEFAULT),

    ("单行注释内-开头", "Test.java", "// 这是一个测试文件\nint x = 1;", 5, ContextType.COMMENT),
    ("单行注释内-中间", "Test.java", "// 这是一个测试文件\nint x = 1;", 10, ContextType.COMMENT),

    ("多行注释内", "Test.java", "/* 多行\n注释 */", 8, ContextType.COMMENT),

    ("代码区域-无注释", "Test.java", "int x = 100;\nString s = \"hello\";", 10, ContextType.DEFAULT),
    ("Git提交消息", "COMMIT_EDITMSG", "修复bug", 2, ContextType.GIT_COMMIT),
    ("行内注释-代码后", "Test.java", "int x = 1; // comment", 15, ContextType.COMMENT),
    ("空文本", "Test.java", "", 0, ContextType.DEFAULT),

    # 边界测试
    ("光标在文本开头", "Test.java", "int x = 1;", 0, ContextType.DEFAULT),
    ("光标在文本中间", "Test.java", "int x = 1;", 3, ContextType.DEFAULT),
]

def run_tests():
    """运行所有测试"""
    print("=" * 60)
    print("FreeMacInput 编辑器事件监听逻辑测试")
    print("=" * 60)

    passed = 0
    failed = 0

    for name, fileName, documentText, caretOffset, expectedType in TEST_CASES:
        isGitCommit = is_git_commit_file(fileName)
        result = detect_context(documentText, isGitCommit, caretOffset)

        status = "✓ PASS" if result.type == expectedType else "✗ FAIL"
        if result.type == expectedType:
            passed += 1
        else:
            failed += 1

        print(f"\n[{status}] {name}")
        print(f"  文件: {fileName}")
        print(f"  光标偏移: {caretOffset}")
        print(f"  检测结果: {result}")
        print(f"  期望类型: {expectedType}")

    print("\n" + "=" * 60)
    print(f"测试结果: {passed} 通过, {failed} 失败")
    print("=" * 60)

    return failed == 0

# ==================== 模拟编辑器事件测试 ====================

class MockEditor:
    """模拟编辑器用于测试"""
    def __init__(self, fileName: str, content: str):
        self.fileName = fileName
        self.content = content
        self.caretOffset = len(content)

    def move_caret(self, offset: int) -> Tuple[int, int]:
        old_offset = self.caretOffset
        self.caretOffset = offset
        return old_offset, offset

class EditorEventListener:
    """模拟的事件监听器"""
    def __init__(self):
        self.events = []
        self.context_history = []

    def on_caret_position_changed(self, editor: MockEditor):
        """光标位置变化回调"""
        isGitCommit = is_git_commit_file(editor.fileName)
        context = detect_context(editor.content, isGitCommit, editor.caretOffset)

        self.events.append({
            "type": "caret",
            "file": editor.fileName,
            "offset": editor.caretOffset,
            "context": context
        })
        self.context_history.append(context.type)

def simulate_editor_events():
    """模拟编辑器事件"""
    print("\n" + "=" * 60)
    print("模拟编辑器事件测试")
    print("=" * 60)

    # 创建模拟编辑器 - 一个真实的 Java 文件
    content = """// 这是一个测试文件
public class Test {
    // 方法注释
    public void test() {
        String msg = "Hello World";
        int 中文变量 = 100;
    }
}
""".strip()

    editor = MockEditor("Test.java", content)
    listener = EditorEventListener()

    # 模拟光标移动场景
    test_moves = [
        (3, "在行首注释标记旁"),
        (10, "在注释中文本中"),
        (50, "在类定义代码中"),
        (100, "在方法注释中"),
        (145, "在字符串 'Hello World' 行末(中文字符串)"),
        (200, "在中文变量旁"),
    ]

    for offset, description in test_moves:
        old_offset, new_offset = editor.move_caret(offset)
        listener.on_caret_position_changed(editor)
        print(f"\n[{description}]")
        print(f"  光标: {old_offset} -> {new_offset}")
        print(f"  检测到: {listener.events[-1]['context']}")

    print("\n" + "-" * 60)
    print("上下文变化历史:")
    for i, ctx_type in enumerate(listener.context_history):
        print(f"  {i+1}. {ctx_type}")
    print("=" * 60)

if __name__ == "__main__":
    success = run_tests()
    simulate_editor_events()

    if not success:
        print("\n测试失败！")
        exit(1)
    else:
        print("\n所有测试通过！")
