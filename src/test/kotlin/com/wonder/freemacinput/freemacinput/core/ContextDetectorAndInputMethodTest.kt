package com.wonder.freemacinput.freemacinput.core

import org.junit.Assert.*
import org.junit.Test

/**
 * 注释识别和输入法切换测试
 *
 * 测试场景：
 * 1. 识别注释，自动切换到中文
 * 2. 识别代码，自动切换到英文
 * 3. 边界条件和异常健壮性
 */
class ContextDetectorAndInputMethodTest {

    private val detector = ContextDetector()

    // ==================== 注释识别测试 ====================

    @Test
    fun testLineComment_Chinese() {
        println("\n=== 测试1: 行注释识别 (应返回中文) ===")
        val code = """
            // 这是一个注释
            fun test() {
                val a = 1
            }
        """.trimIndent()

        // 找到注释行的偏移量
        val commentOffset = code.indexOf("//")
        val context = detector.detectContext(code, false, commentOffset)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是注释类型", ContextType.COMMENT, context.type)
        assertTrue("原因应该包含注释", context.reason.contains("注释"))
    }

    @Test
    fun testBlockComment_Chinese() {
        println("\n=== 测试2: 块注释识别 (应返回中文) ===")
        val code = """
            /* 这是一个块注释 */
            fun test() {
                val a = 1
            }
        """.trimIndent()

        // 找到块注释内部的偏移量
        val blockStart = code.indexOf("/*")
        val context = detector.detectContext(code, false, blockStart + 5)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是注释类型", ContextType.COMMENT, context.type)
    }

    @Test
    fun testMultiLineBlockComment_Chinese() {
        println("\n=== 测试3: 多行块注释识别 (应返回中文) ===")
        val code = """
            /*
             * 多行注释
             * 第二行
             */
            fun test() {
                val a = 1
            }
        """.trimIndent()

        // 在多行注释中间
        val context = detector.detectContext(code, false, 30)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是注释类型", ContextType.COMMENT, context.type)
    }

    @Test
    fun testInlineComment_Chinese() {
        println("\n=== 测试4: 行内注释识别 (应返回中文) ===")
        val code = """
            val a = 1 // 这是行内注释
            val b = 2
        """.trimIndent()

        // 行内注释位置
        val inlineOffset = code.indexOf("//") + 5
        val context = detector.detectContext(code, false, inlineOffset)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是注释类型", ContextType.COMMENT, context.type)
    }

    // ==================== 代码区识别测试 ====================

    @Test
    fun testCodeArea_English() {
        println("\n=== 测试5: 代码区识别 (应返回英文) ===")
        val code = """
            fun test() {
                val a = 1
                val b = "string"
            }
        """.trimIndent()

        // 在代码区域（非注释）
        val codeOffset = code.indexOf("val a")
        val context = detector.detectContext(code, false, codeOffset)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是代码区域类型", ContextType.DEFAULT, context.type)
    }

    @Test
    fun testFunctionDeclaration_English() {
        println("\n=== 测试6: 函数声明识别 (应返回英文) ===")
        val code = """
            fun calculate(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()

        val context = detector.detectContext(code, false, 10)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是代码区域类型", ContextType.DEFAULT, context.type)
    }

    // ==================== 边界健壮性测试 ====================

    @Test
    fun testEmptyText() {
        println("\n=== 测试7: 空文本健壮性 ===")
        val context = detector.detectContext("", false, 0)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        // 不应该崩溃，应该返回UNKNOWN或DEFAULT
        assertNotNull(context)
    }

    @Test
    fun testNullLikeText() {
        println("\n=== 测试8: 空白文本健壮性 ===")
        val context = detector.detectContext("   \n   ", false, 2)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        // 不应该崩溃
        assertNotNull(context)
    }

    @Test
    fun testOffsetAtEnd() {
        println("\n=== 测试9: 偏移量在文本末尾 ===")
        val code = "val a = 1"
        val context = detector.detectContext(code, false, code.length)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertNotNull(context)
    }

    @Test
    fun testOffsetBeyondEnd() {
        println("\n=== 测试10: 偏移量超出文本长度 ===")
        val code = "val a = 1"
        val context = detector.detectContext(code, false, 100)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        // 应该能处理，不崩溃
        assertNotNull(context)
    }

    @Test
    fun testStringWithSlashes() {
        println("\n=== 测试11: 字符串中的斜杠不应被误判为注释 ===")
        val code = """val url = "http://example.com""""

        // 在URL中的 // 位置
        val urlOffset = code.indexOf("http")
        val context = detector.detectContext(code, false, urlOffset)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        // 应该在字符串中，不是注释
        assertEquals("应该是字符串类型", ContextType.STRING, context.type)
    }

    @Test
    fun testCodeAfterComment() {
        println("\n=== 测试12: 注释后的代码 ===")
        val code = """
            // 注释
            val a = 1
            // 另一个注释
            val b = 2
        """.trimIndent()

        // 在注释后的代码位置
        val codeOffset = code.indexOf("val a")
        val context = detector.detectContext(code, false, codeOffset)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是代码区域类型", ContextType.DEFAULT, context.type)
    }

    @Test
    fun testUnclosedBlockComment() {
        println("\n=== 测试13: 未闭合的块注释 ===")
        val code = """
            /* 未闭合的注释
            val a = 1
        """.trimIndent()

        val context = detector.detectContext(code, false, 30)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是注释类型（未闭合）", ContextType.COMMENT, context.type)
    }

    @Test
    fun testMultipleBlockComments() {
        println("\n=== 测试14: 多个块注释 ===")
        val code = """
            /* 第一个注释 */
            val a = 1
            /* 第二个注释 */
            val b = 2
        """.trimIndent()

        val context = detector.detectContext(code, false, 40)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是代码区域类型", ContextType.DEFAULT, context.type)
    }

    @Test
    fun testNestedSlashesInCode() {
        println("\n=== 测试15: 代码中的斜杠操作符 ===")
        val code = """
            val result = a / b
            val regex = Pattern.compile("\\d+")
        """.trimIndent()

        val context = detector.detectContext(code, false, 15)

        println("检测结果: type=${context.type}, reason=${context.reason}")
        assertEquals("应该是代码区域类型", ContextType.DEFAULT, context.type)
    }

    // ==================== 输入法切换逻辑测试 ====================

    @Test
    fun testDetermineInputMethod_CommentToChinese() {
        println("\n=== 测试16: 注释→中文切换逻辑 ===")

        val commentContext = ContextInfo(
            type = ContextType.COMMENT,
            toolWindowType = null,
            reason = "测试注释"
        )

        // 模拟SettingsState
        val mockSettings = createMockSettings()

        val targetMethod = determineMethodForContext(commentContext, mockSettings)
        println("注释目标输入法: $targetMethod")
        assertEquals("注释应该用中文", InputMethodType.CHINESE, targetMethod)
    }

    @Test
    fun testDetermineInputMethod_CodeToEnglish() {
        println("\n=== 测试17: 代码→英文切换逻辑 ===")

        val codeContext = ContextInfo(
            type = ContextType.DEFAULT,
            toolWindowType = null,
            reason = "测试代码"
        )

        val mockSettings = createMockSettings()

        val targetMethod = determineMethodForContext(codeContext, mockSettings)
        println("代码目标输入法: $targetMethod")
        assertEquals("代码应该用英文", InputMethodType.ENGLISH, targetMethod)
    }

    @Test
    fun testDetermineInputMethod_StringToChinese() {
        println("\n=== 测试18: 字符串→中文切换逻辑 ===")

        val stringContext = ContextInfo(
            type = ContextType.STRING,
            toolWindowType = null,
            stringName = "test",
            reason = "测试字符串"
        )

        val mockSettings = createMockSettings()

        val targetMethod = determineMethodForContext(stringContext, mockSettings)
        println("字符串目标输入法: $targetMethod")
        assertEquals("字符串应该用中文", InputMethodType.CHINESE, targetMethod)
    }

    // ==================== 辅助方法 ====================

    private fun createMockSettings(): MockSettingsState {
        return MockSettingsState(
            defaultMethod = InputMethodType.ENGLISH,
            commentMethod = InputMethodType.CHINESE,
            stringMethod = InputMethodType.CHINESE,
            gitCommitMethod = InputMethodType.CHINESE
        )
    }

    private fun determineMethodForContext(context: ContextInfo, settings: MockSettingsState): InputMethodType {
        return when (context.type) {
            ContextType.DEFAULT -> settings.defaultMethod
            ContextType.COMMENT -> settings.commentMethod
            ContextType.STRING -> settings.stringMethod
            ContextType.GIT_COMMIT -> settings.gitCommitMethod
            else -> settings.defaultMethod
        }
    }

    /**
     * 模拟SettingsState用于测试
     */
    private data class MockSettingsState(
        val defaultMethod: InputMethodType,
        val commentMethod: InputMethodType,
        val stringMethod: InputMethodType,
        val gitCommitMethod: InputMethodType
    )
}
