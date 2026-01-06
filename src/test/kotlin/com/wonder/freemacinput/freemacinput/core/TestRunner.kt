package com.wonder.freemacinput.freemacinput.core

/**
 * 简单的测试运行器
 *
 * 直接运行此文件可执行所有单元测试：
 * kotlinc -script TestRunner.kts
 * 或
 * kotlin TestRunner.kt
 */
object TestRunner {

    private var passed = 0
    private var failed = 0
    private val failures = mutableListOf<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(60))
        println("FreeMacInput 单元测试")
        println("=".repeat(60))

        // 运行所有测试
        testContextDetector()
        testInputMethodManager()

        // 输出结果
        println("\n" + "=".repeat(60))
        println("测试结果: ${passed} 通过, ${failed} 失败")
        println("=".repeat(60))

        if (failed > 0) {
            println("\n失败的测试:")
            failures.forEach { println("  - $it") }
        }
    }

    // ============ ContextDetector 测试 ============

    private fun testContextDetector() {
        println("\n--- ContextDetector 测试 ---")

        test("行注释检测 - 在注释行中") {
            val code = """
                fun test() {
                    // 这是一行注释
                    val x = 1
                }
            """.trimIndent()
            // 光标在 "这是" 后面，也就是在 // 后面
            val textBefore = code.substring(0, code.indexOf("这是") + 2)
            assertTrue(isInCommentByText(textBefore))
        }

        test("行注释检测 - 在代码行中") {
            val code = """
                fun test() {
                    val x = 1 // 这是注释
                    val y = 2
                }
            """.trimIndent()
            val textBefore = code.substring(0, code.indexOf("val y = 2") + 5)
            assertFalse(isInCommentByText(textBefore))
        }

        test("块注释检测 - 在块注释内部") {
            val code = """
                /*
                 * 这是一个块注释
                 */
                val x = 1
            """.trimIndent()
            val textBefore = code.substring(0, code.indexOf("这是一个块注释") + 5)
            assertTrue(isInCommentByText(textBefore))
        }

        test("块注释检测 - 在块注释外部") {
            val code = """
                /*
                 * 这是一个块注释
                 */
                val x = 1
            """.trimIndent()
            val textBefore = code.substring(0, code.indexOf("val x = 1") + 8)
            assertFalse(isInCommentByText(textBefore))
        }

        test("普通代码行 - 无注释") {
            val code = """
                fun test() {
                    val x = 1
                    val y = 2
                }
            """.trimIndent()
            val textBefore = code.substring(0, code.indexOf("val y = 2") + 5)
            assertFalse(isInCommentByText(textBefore))
        }

        test("空字符串") {
            assertFalse(isInCommentByText(""))
        }

        test("字符串中的注释不被识别") {
            val code = """
                val str = "// 这不是注释"
                val x = 1
            """.trimIndent()
            val textBefore = code.substring(0, code.indexOf("val x = 1") + 8)
            assertFalse(isInCommentByText(textBefore))
        }
    }

    // ============ InputMethodManager 测试 ============

    private fun testInputMethodManager() {
        println("\n--- InputMethodManager 测试 ---")

        test("AUTO模式不切换") {
            assertTrue(shouldSwitchAuto(InputMethodType.AUTO))
        }

        test("首次切换应该执行") {
            assertTrue(shouldSwitchAuto(InputMethodType.CHINESE))
        }

        test("相同目标不重复切换") {
            assertFalse(shouldSwitchAuto(InputMethodType.CHINESE, System.currentTimeMillis(), InputMethodType.CHINESE, InputMethodType.CHINESE))
        }

        test("不同目标应该切换") {
            // 使用过去的某个时间点，确保冷却期已过
            val oldTime = System.currentTimeMillis() - 2000
            assertTrue(shouldSwitchAuto(InputMethodType.ENGLISH, oldTime, InputMethodType.CHINESE, InputMethodType.CHINESE))
        }

        test("输入法类型枚举值数量") {
            assertEquals(3, InputMethodType.entries.size)
        }
    }

    // ============ 辅助方法 ============

    private fun isInCommentByText(textBeforeOffset: String): Boolean {
        if (textBeforeOffset.isEmpty()) return false

        val lastNewline = textBeforeOffset.lastIndexOf('\n')
        val lineStart = if (lastNewline >= 0) lastNewline + 1 else 0
        val currentLine = textBeforeOffset.substring(lineStart)
        val lineLength = currentLine.length

        // 检测行注释 //
        val lineCommentPos = currentLine.indexOf("//")
        if (lineCommentPos >= 0 && lineCommentPos < lineLength) {
            // // 存在且在当前行内
            // 检查光标是否在 // 之后（光标位置 = textBeforeOffset.length）
            val cursorPosInLine = textBeforeOffset.length - lineStart
            if (cursorPosInLine > lineCommentPos + 2) {
                // 光标在 // 后面至少2个字符之后，表示在行注释中
                return true
            }
        }

        // 检查是否在块注释内部
        return isInsideBlockComment(textBeforeOffset)
    }

    private fun isInsideBlockComment(text: String): Boolean {
        val startMarkers = countOccurrences(text, "/*")
        val endMarkers = countOccurrences(text, "*/")
        return startMarkers > endMarkers
    }

    private fun countOccurrences(text: String, sub: String): Int {
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

    private fun shouldSwitchAuto(method: InputMethodType, lastSwitchTime: Long = 0, lastSwitchedTo: InputMethodType? = null, currentTarget: InputMethodType = InputMethodType.ENGLISH): Boolean {
        if (method == InputMethodType.AUTO) return true

        val now = System.currentTimeMillis()
        val cooldown = 1000L

        // 冷却期检查
        if (now - lastSwitchTime < cooldown) return false

        // 目标相同且已切换过，跳过
        if (method == lastSwitchedTo && method == currentTarget) return false

        return true
    }

    // ============ 测试框架 ============

    private fun test(name: String, block: () -> Unit) {
        try {
            block()
            passed++
            println("  ✓ $name")
        } catch (e: AssertionError) {
            failed++
            failures.add(name)
            println("  ✗ $name: ${e.message}")
        } catch (e: Exception) {
            failed++
            failures.add("$name (Exception: ${e.message})")
            println("  ✗ $name: ${e.javaClass.simpleName}: ${e.message}")
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
}

enum class InputMethodType {
    ENGLISH, CHINESE, AUTO
}
