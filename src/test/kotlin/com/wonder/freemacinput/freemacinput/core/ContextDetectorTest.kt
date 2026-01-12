package com.wonder.freemacinput.freemacinput.core

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * 上下文检测测试 - 验证检测准确性
 */
class ContextDetectorTest {

    private val detector = ContextDetector()

    // ==================== 字符串检测测试 ====================

    @Test
    fun `检测双引号字符串内的光标`() {
        val text = """var message = "hello world";"""
        val result = detector.detectContext(text, text.indexOf("hello"))
        assertEquals(ContextType.STRING, result.type)
        assertEquals("字符串区域", result.reason)
    }

    @Test
    fun `检测单引号字符串内的光标`() {
        val text = """var message = 'hello world';"""
        val result = detector.detectContext(text, text.indexOf("hello"))
        assertEquals(ContextType.STRING, result.type)
        assertEquals("字符串区域", result.reason)
    }

    @Test
    fun `检测字符串外的光标`() {
        val text = """var message = "hello";"""
        val result = detector.detectContext(text, 0) // 在 var 之前
        assertEquals(ContextType.CODE, result.type)
        assertEquals("代码区域", result.reason)
    }

    @Test
    fun `检测转义字符后的字符串`() {
        val text = """var message = "hel\"lo";"""
        val result = detector.detectContext(text, text.indexOf("lo"))
        assertEquals(ContextType.STRING, result.type)
        assertEquals("字符串区域", result.reason)
    }

    @Test
    fun `检测空字符串内的光标`() {
        val text = """var message = "";"""
        val result = detector.detectContext(text, text.indexOf("\"") + 1)
        assertEquals(ContextType.STRING, result.type)
        assertEquals("字符串区域", result.reason)
    }

    @Test
    fun `检测多个字符串中的位置`() {
        val text = """var a = "first" + "second" + "third";"""
        val firstPos = text.indexOf("first")
        val secondPos = text.indexOf("second")
        val thirdPos = text.indexOf("third")
        val outsidePos = text.indexOf(" + ")

        assertEquals(ContextType.STRING, detector.detectContext(text, firstPos).type)
        assertEquals(ContextType.STRING, detector.detectContext(text, secondPos).type)
        assertEquals(ContextType.STRING, detector.detectContext(text, thirdPos).type)
        assertEquals(ContextType.CODE, detector.detectContext(text, outsidePos).type)
    }

    // ==================== 行注释检测测试 ====================

    @Test
    fun `检测行注释内的光标`() {
        val text = """// This is a comment"""
        val result = detector.detectContext(text, text.indexOf("is"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    @Test
    fun `检测缩进行注释内的光标`() {
        val text = """    // This is a comment"""
        val result = detector.detectContext(text, text.indexOf("is"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    @Test
    fun `检测代码后行注释内的光标`() {
        val text = """var x = 10; // This is a comment"""
        val result = detector.detectContext(text, text.indexOf("comment"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    @Test
    fun `检测代码后在注释前的光标`() {
        val text = """var x = 10; // This is a comment"""
        val result = detector.detectContext(text, text.indexOf(";"))
        assertEquals(ContextType.CODE, result.type)
        assertEquals("代码区域", result.reason)
    }

    @Test
    fun `检测行注释中的字符串内`() {
        val text = """// This is a "string" in comment"""
        val result = detector.detectContext(text, text.indexOf("string"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    // ==================== 块注释检测测试 ====================

    @Test
    fun `检测块注释内的光标`() {
        val text = """/* This is a block comment */"""
        val result = detector.detectContext(text, text.indexOf("is"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    @Test
    fun `检测跨行块注释内的光标`() {
        val text = """/* This is a
block comment */"""
        val result = detector.detectContext(text, text.indexOf("block"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    @Test
    fun `检测块注释外的光标`() {
        val text = """var x = 10; /* comment */"""
        val result = detector.detectContext(text, text.indexOf("var"))
        assertEquals(ContextType.CODE, result.type)
        assertEquals("代码区域", result.reason)
    }

    @Test
    fun `检测块注释后的光标`() {
        val text = """/* comment */ var x = 10;"""
        val result = detector.detectContext(text, text.indexOf("var"))
        assertEquals(ContextType.CODE, result.type)
        assertEquals("代码区域", result.reason)
    }

    @Test
    fun `检测嵌套的块注释`() {
        val text = """/* outer /* inner */ comment */"""
        val result1 = detector.detectContext(text, text.indexOf("inner"))
        // 注意：这是非嵌套的，所以 "inner" 实际上不在注释内
        // 这种情况下检测器应该能正确处理
        val result2 = detector.detectContext(text, text.indexOf("outer"))
        assertEquals(ContextType.COMMENT, result2.type)
    }

    @Test
    fun `检测块注释中的字符串内`() {
        val text = """/* This is a "string" in comment */"""
        val result = detector.detectContext(text, text.indexOf("string"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    // ==================== 复杂场景测试 ====================

    @Test
    fun `检测代码区域`() {
        val text = """function test() {
    var x = 10;
    return x;
}"""
        val result = detector.detectContext(text, text.indexOf("function"))
        assertEquals(ContextType.CODE, result.type)
        assertEquals("代码区域", result.reason)
    }

    @Test
    fun `检测if语句中的代码区域`() {
        val text = """if (condition) {
    doSomething();
}"""
        val result = detector.detectContext(text, text.indexOf("doSomething"))
        assertEquals(ContextType.CODE, result.type)
        assertEquals("代码区域", result.reason)
    }

    @Test
    fun `检测参数列表中的代码区域`() {
        val text = """function test(param1, param2) {}"""
        val result = detector.detectContext(text, text.indexOf("param1"))
        assertEquals(ContextType.CODE, result.type)
        assertEquals("代码区域", result.reason)
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `检测空文档`() {
        val text = ""
        val result = detector.detectContext(text, 0)
        assertEquals(ContextType.UNKNOWN, result.type)
    }

    @Test
    fun `检测只有一个字符的文档`() {
        val text = "a"
        val result = detector.detectContext(text, 0)
        assertEquals(ContextType.CODE, result.type)
    }

    @Test
    fun `检测未闭合的字符串`() {
        val text = """var x = "unclosed string"""
        val result = detector.detectContext(text, text.indexOf("unclosed"))
        assertEquals(ContextType.STRING, result.type)
        assertEquals("字符串区域", result.reason)
    }

    @Test
    fun `检测未闭合的块注释`() {
        val text = """/* unclosed comment
var x = 10;"""
        val result = detector.detectContext(text, text.indexOf("var"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    @Test
    fun `检测多个连续的注释标记`() {
        val text = """/// This is a triple comment"""
        val result = detector.detectContext(text, text.indexOf("This"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    // ==================== 混合场景测试 ====================

    @Test
    fun `检测字符串包含注释标记`() {
        val text = """var x = "This is not // a comment";"""
        val result = detector.detectContext(text, text.indexOf("not"))
        assertEquals(ContextType.STRING, result.type)
        assertEquals("字符串区域", result.reason)
    }

    @Test
    fun `检测注释包含字符串标记`() {
        val text = """// This is "not" a string"""
        val result = detector.detectContext(text, text.indexOf("not"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    @Test
    fun `检测块注释包含行注释标记`() {
        val text = """/* This is // not a line comment */"""
        val result = detector.detectContext(text, text.indexOf("not"))
        assertEquals(ContextType.COMMENT, result.type)
        assertEquals("注释区域", result.reason)
    }

    // ==================== 特殊语言模式测试 ====================

    @Test
    fun `检测Java风格的字符串`() {
        val text = """String s = "hello";"""
        val result = detector.detectContext(text, text.indexOf("hello"))
        assertEquals(ContextType.STRING, result.type)
    }

    @Test
    fun `检测Python风格的注释`() {
        val text = """# This is a Python comment"""
        // Python 的 # 注释不会被检测到，所以应该是代码区域
        val result = detector.detectContext(text, text.indexOf("This"))
        assertEquals(ContextType.CODE, result.type)
    }

    @Test
    fun `检测HTML风格的注释`() {
        val text = """<!-- HTML comment -->"""
        // HTML 的 <!-- --> 注释不会被检测到
        val result = detector.detectContext(text, text.indexOf("HTML"))
        assertEquals(ContextType.CODE, result.type)
    }

    // ==================== 实际代码片段测试 ====================

    @Test
    fun `检测实际Java代码`() {
        val text = """
public class Test {
    /**
     * This is a Javadoc comment
     */
    public void method() {
        String message = "Hello World";
        // This is a line comment
        int x = 10;
    }
}
        """.trimIndent()

        // 测试各个位置
        val javadocPos = text.indexOf("Javadoc")
        val stringPos = text.indexOf("Hello")
        val lineCommentPos = text.indexOf("line comment")
        val codePos = text.indexOf("int x")

        assertEquals("Javadoc 应该被检测为注释", ContextType.COMMENT, detector.detectContext(text, javadocPos).type)
        assertEquals("字符串内的位置应该被检测为字符串", ContextType.STRING, detector.detectContext(text, stringPos).type)
        assertEquals("行注释应该被检测为注释", ContextType.COMMENT, detector.detectContext(text, lineCommentPos).type)
        assertEquals("代码区域应该被检测为代码", ContextType.CODE, detector.detectContext(text, codePos).type)
    }

    @Test
    fun `检测实际Kotlin代码`() {
        val text = """
class Example {
    fun greet(name: String) {
        val message = "Hello, World!"
        // Print greeting
        println(message)
    }
}
        """.trimIndent()

        val stringTemplatePos = text.indexOf("Hello")
        val lineCommentPos = text.indexOf("Print")
        val codePos = text.indexOf("println")

        assertEquals(ContextType.STRING, detector.detectContext(text, stringTemplatePos).type)
        assertEquals(ContextType.COMMENT, detector.detectContext(text, lineCommentPos).type)
        assertEquals(ContextType.CODE, detector.detectContext(text, codePos).type)
    }

    @Test
    fun `检测JavaScript代码`() {
        val text = """
const x = 10; // Line comment
function test() {
    return "value";
}
/* Block comment */
var y = 20;
        """.trimIndent()

        val lineCommentPos = text.indexOf("Line")
        val stringPos = text.indexOf("value")
        val blockCommentPos = text.indexOf("Block")
        val codePos = text.indexOf("var y")

        assertEquals(ContextType.COMMENT, detector.detectContext(text, lineCommentPos).type)
        assertEquals(ContextType.STRING, detector.detectContext(text, stringPos).type)
        assertEquals(ContextType.COMMENT, detector.detectContext(text, blockCommentPos).type)
        assertEquals(ContextType.CODE, detector.detectContext(text, codePos).type)
    }

    // ==================== 性能测试 ====================

    @Test
    fun `大文档检测性能`() {
        val sb = StringBuilder()
        repeat(10000) {
            sb.append("var x$it = $it; // comment\n")
        }
        sb.append("/* large block comment */")
        val text = sb.toString()

        val start = System.currentTimeMillis()
        val result = detector.detectContext(text, text.length - 10)
        val elapsed = System.currentTimeMillis() - start

        assertEquals(ContextType.COMMENT, result.type)
        assertTrue("检测大文档应该在100ms内完成，实际耗时: ${elapsed}ms", elapsed < 100)
    }
}
