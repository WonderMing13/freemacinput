package com.wonder.freemacinput.freemacinput.core

/**
 * ContextDetector 纯逻辑测试
 *
 * 直接运行此脚本测试核心检测逻辑：
 * kotlinc -script ContextDetectorTestScript.kts
 */
object ContextDetectorTestScript {

    private var passed = 0
    private var failed = 0
    private val failures = mutableListOf<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(60))
        println("ContextDetector 核心逻辑测试")
        println("=".repeat(60))

        // 字符串检测测试
        testStringDetection()

        // 标识符提取测试
        testIdentifierExtraction()

        // 标识符字符测试
        testIdentifierChar()

        // 注释检测测试
        testCommentDetection()

        // 块注释检测测试
        testBlockCommentDetection()

        // 出现次数计算测试
        testCountOccurrences()

        // 边界条件测试
        testBoundary()

        // 输出结果
        println("\n" + "=".repeat(60))
        println("测试结果: $passed 通过, $failed 失败")
        println("=".repeat(60))

        if (failed > 0) {
            println("\n失败的测试:")
            failures.forEach { println("  - $it") }
            System.exit(1)
        } else {
            println("\n所有测试通过!")
        }
    }

    // ============ 字符串检测测试 ============

    private fun testStringDetection() {
        println("\n--- 字符串检测测试 ---")

        test("双引号字符串检测") {
            val text = "val str = \"hello world\""
            val offset = text.indexOf("hello") + 3
            assertTrue(isInStringLiteral(text, offset))
        }

        test("单引号字符串检测") {
            val text = "val char = 'a'"
            val offset = text.indexOf("a") + 1
            assertTrue(isInStringLiteral(text, offset))
        }

        test("反引号字符串检测") {
            val text = "val template = `hello world`"
            val offset = text.indexOf("hello") + 3
            assertTrue(isInStringLiteral(text, offset))
        }

        test("不在字符串中") {
            val text = "val x = 123"
            val offset = 8
            assertFalse(isInStringLiteral(text, offset))
        }

        test("字符串结束后") {
            val text = "val str = \"hello\""
            val offset = text.length
            assertFalse(isInStringLiteral(text, offset))
        }

        test("空字符串") {
            assertFalse(isInStringLiteral("", 0))
        }

        test("offset超出范围") {
            val text = "val x = 1"
            assertFalse(isInStringLiteral(text, 100))
        }

        test("转义引号") {
            val text = "val str = \"he said \\\"hello\\\"\""
            val offset = text.indexOf("hello") + 3
            assertTrue(isInStringLiteral(text, offset))
        }

        test("连续字符串") {
            val text = "val a = \"first\" + \"second\""
            val offset = text.indexOf("second") + 3
            assertTrue(isInStringLiteral(text, offset))
        }

        test("字符串中的引号") {
            val text = "val str = \"it's a test\""
            val offset = text.indexOf("test") + 2
            assertTrue(isInStringLiteral(text, offset))
        }
    }

    // ============ 标识符提取测试 ============

    private fun testIdentifierExtraction() {
        println("\n--- 标识符提取测试 ---")

        test("普通变量名") {
            val text = "val myVariable = 1"
            val offset = text.indexOf("myVariable") + 5
            assertEquals("myVariable", extractStringIdentifier(text, offset))
        }

        test("下划线变量名") {
            val text = "val privateVar = 1"
            val offset = text.indexOf("privateVar") + 8
            assertEquals("privateVar", extractStringIdentifier(text, offset))
        }

        test("太短的标识符返回null") {
            val text = "val x = 1"
            val offset = text.indexOf("x") + 1
            assertNull(extractStringIdentifier(text, offset))
        }

        test("offset超出范围") {
            val text = "val x = 1"
            assertNull(extractStringIdentifier(text, 100))
        }

        test("类名提取") {
            val text = "class MyClass"
            val offset = text.indexOf("MyClass") + 4
            assertEquals("MyClass", extractStringIdentifier(text, offset))
        }

        test("函数名提取") {
            val text = "fun doSomething()"
            val offset = text.indexOf("doSomething") + 6
            assertEquals("doSomething", extractStringIdentifier(text, offset))
        }
    }

    // ============ 标识符字符测试 ============

    private fun testIdentifierChar() {
        println("\n--- 标识符字符测试 ---")

        test("字母") {
            assertTrue(isIdentifierChar('a'))
            assertTrue(isIdentifierChar('Z'))
            assertTrue(isIdentifierChar('m'))
            assertTrue(isIdentifierChar('M'))
        }

        test("数字") {
            assertTrue(isIdentifierChar('0'))
            assertTrue(isIdentifierChar('9'))
        }

        test("下划线") {
            assertTrue(isIdentifierChar('_'))
        }

        test("非标识符字符") {
            assertFalse(isIdentifierChar('@'))
            assertFalse(isIdentifierChar('#'))
            assertFalse(isIdentifierChar(' '))
            assertFalse(isIdentifierChar('.'))
            assertFalse(isIdentifierChar(','))
            assertFalse(isIdentifierChar('('))
            assertFalse(isIdentifierChar(')'))
            assertFalse(isIdentifierChar('"'))
            assertFalse(isIdentifierChar('\''))
        }
    }

    // ============ 注释检测测试 ============

    private fun testCommentDetection() {
        println("\n--- 注释检测测试 ---")

        test("在行注释中") {
            val text = "fun test() {\n// 这是一行注释\nval x = 1\n}"
            val textBefore = text.substring(0, text.indexOf("这是") + 2)
            assertTrue(isInCommentByText(textBefore))
        }

        test("在代码行中不在注释中") {
            val text = "fun test() {\nval x = 1 // 这是注释\nval y = 2\n}"
            val textBefore = text.substring(0, text.indexOf("val y = 2") + 8)
            assertFalse(isInCommentByText(textBefore))
        }

        test("在块注释内部") {
            val text = "/*\n * 这是一个块注释\n */\nval x = 1"
            val textBefore = text.substring(0, text.indexOf("这是一个块注释") + 5)
            assertTrue(isInCommentByText(textBefore))
        }

        test("在块注释外部") {
            val text = "/*\n * 这是一个块注释\n */\nval x = 1"
            val textBefore = text.substring(0, text.indexOf("val x = 1") + 8)
            assertFalse(isInCommentByText(textBefore))
        }

        test("空字符串") {
            assertFalse(isInCommentByText(""))
        }

        test("无注释的代码") {
            val text = "fun test() {\nval x = 1\nval y = 2\n}"
            val textBefore = text.substring(0, text.indexOf("val y = 2") + 8)
            assertFalse(isInCommentByText(textBefore))
        }

        test("字符串中的注释不被识别") {
            val text = "val str = \"// 这不是注释\"\nval x = 1"
            val textBefore = text.substring(0, text.indexOf("val x = 1") + 8)
            assertFalse(isInCommentByText(textBefore))
        }

        test("多行块注释") {
            val text = "/*\n * 第一行\n * 第二行\n * 第三行\n */\nval x = 1"
            val textBefore = text.substring(0, text.indexOf("第三行") + 3)
            assertTrue(isInCommentByText(textBefore))
        }

        test("嵌套的块注释") {
            val text = "/* 外层 /* 内层 */ 闭合 */\nval x = 1"
            val textBefore = text.substring(0, text.indexOf("闭合") + 2)
            assertFalse(isInCommentByText(textBefore))
        }
    }

    // ============ 块注释检测测试 ============

    private fun testBlockCommentDetection() {
        println("\n--- 块注释检测测试 ---")

        test("在注释内部") {
            val text = "/*\n * comment\n */"
            val textBefore = text.substring(0, text.indexOf("comment") + 3)
            assertTrue(isInsideBlockComment(textBefore))
        }

        test("注释结束后") {
            val text = "/*\n * comment\n */\nval x = 1"
            val textBefore = text.substring(0, text.indexOf("val x = 1") + 8)
            assertFalse(isInsideBlockComment(textBefore))
        }

        test("未闭合的注释") {
            val text = "/* unclosed comment"
            assertTrue(isInsideBlockComment(text))
        }

        test("完整成对的注释") {
            val text = "/* comment */"
            val textBefore = text.substring(0, text.indexOf("comment") + 3)
            assertTrue(isInsideBlockComment(textBefore))
        }
    }

    // ============ 出现次数计算测试 ============

    private fun testCountOccurrences() {
        println("\n--- 出现次数计算测试 ---")

        test("单次出现") {
            assertEquals(1, countOccurrences("hello world", "world"))
        }

        test("多次出现") {
            assertEquals(3, countOccurrences("aaa", "a"))
        }

        test("不存在") {
            assertEquals(0, countOccurrences("hello world", "xyz"))
        }

        test("空字符串查找") {
            assertEquals(4, countOccurrences("aaaa", ""))
        }

        test("重叠查找") {
            assertEquals(2, countOccurrences("aaa", "aa"))
        }

        test("注释标记") {
            assertEquals(2, countOccurrences("/* comment */", "/*"))
            assertEquals(2, countOccurrences("/* comment */", "*/"))
        }

        test("多行文本") {
            val text = "/* line 1\n * line 2\n */"
            assertEquals(1, countOccurrences(text, "/*"))
            assertEquals(1, countOccurrences(text, "*/"))
        }
    }

    // ============ 边界条件测试 ============

    private fun testBoundary() {
        println("\n--- 边界条件测试 ---")

        test("offset为0") {
            val text = "val x = 1"
            assertFalse(isInStringLiteral(text, 0))
            assertFalse(isInCommentByText(""))
        }

        test("在行首") {
            val text = "// comment\ncode"
            val textBefore = text.substring(0, text.indexOf("/"))
            assertFalse(isInCommentByText(textBefore))
        }

        test("只有行注释符") {
            val text = "//"
            val textBefore = text.substring(0, 2)
            assertTrue(isInCommentByText(textBefore))
        }

        test("只有块注释符") {
            val text = "/*"
            assertTrue(isInsideBlockComment(text))
        }

        test("注释符在行尾") {
            val text = "code //"
            val textBefore = text
            assertTrue(isInCommentByText(textBefore))
        }
    }

    // ============ 辅助函数 ============

    private fun isInStringLiteral(text: String, offset: Int): Boolean {
        if (offset >= text.length) return false
        var inSingleQuote = false
        var inDoubleQuote = false
        var inBacktick = false
        for (i in 0 until offset) {
            val char = text[i]
            when {
                char == '\'' && !inDoubleQuote && !inBacktick -> inSingleQuote = !inSingleQuote
                char == '"' && !inSingleQuote && !inBacktick -> inDoubleQuote = !inDoubleQuote
                char == '`' && !inSingleQuote && !inDoubleQuote -> inBacktick = !inBacktick
            }
        }
        return inSingleQuote || inDoubleQuote || inBacktick
    }

    private fun extractStringIdentifier(text: String, offset: Int): String? {
        if (offset >= text.length) return null
        var end = offset - 1
        while (end >= 0 && isIdentifierChar(text[end])) {
            end--
        }
        val identifier = text.substring(end + 1, offset)
        return if (identifier.length >= 2) identifier else null
    }

    private fun isIdentifierChar(char: Char): Boolean {
        return char.isLetterOrDigit() || char == '_'
    }

    private fun isInCommentByText(textBeforeOffset: String): Boolean {
        if (textBeforeOffset.isEmpty()) return false

        val lastNewline = textBeforeOffset.lastIndexOf('\n')
        val lineStart = if (lastNewline >= 0) lastNewline + 1 else 0
        val currentLine = textBeforeOffset.substring(lineStart)
        val lineLength = currentLine.length

        val lineCommentPos = currentLine.indexOf("//")
        if (lineCommentPos >= 0 && lineCommentPos < lineLength) {
            val cursorPosInLine = textBeforeOffset.length - lineStart
            if (cursorPosInLine > lineCommentPos + 2) {
                return true
            }
        }

        return isInsideBlockComment(textBeforeOffset)
    }

    private fun isInsideBlockComment(text: String): Boolean {
        val startMarkers = countOccurrences(text, "/*")
        val endMarkers = countOccurrences(text, "*/")
        return startMarkers > endMarkers
    }

    private fun countOccurrences(text: String, sub: String): Int {
        if (sub.isEmpty()) return text.length + 1
        var count = 0
        var index = 0
        while (true) {
            index = text.indexOf(sub, index)
            if (index < 0) break
            count++
            index += sub.length
        }
        return count
    }

    // ============ 测试框架 ============

    private fun test(name: String, block: () -> Unit) {
        try {
            block()
            passed++
            println("  [PASS] $name")
        } catch (e: AssertionError) {
            failed++
            failures.add(name)
            println("  [FAIL] $name: ${e.message}")
        } catch (e: Exception) {
            failed++
            failures.add("$name (Exception: ${e.message})")
            println("  [ERROR] $name: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun assertTrue(value: Boolean) {
        if (!value) throw AssertionError("期望 true，但实际是 false")
    }

    private fun assertFalse(value: Boolean) {
        if (value) throw AssertionError("期望 false，但实际是 true")
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        if (expected != actual) {
            throw AssertionError("期望 $expected，但实际是 $actual")
        }
    }

    private fun assertNull(value: Any?) {
        if (value != null) {
            throw AssertionError("期望 null，但实际是 $value")
        }
    }
}
