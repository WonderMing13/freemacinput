fun main() {
    val testChar = "高"
    
    println("测试字符: $testChar")
    println("Unicode: ${testChar[0].code} (0x${testChar[0].code.toString(16)})")
    println()
    
    // 测试不同的正则写法
    val patterns = listOf(
        "[\\u4e00-\\u9fa5]" to "双反斜杠转义",
        "[\u4e00-\u9fa5]" to "单反斜杠转义",
        "[一-龥]" to "直接使用汉字",
        "\\p{IsHan}" to "Unicode属性",
        "[\\x{4e00}-\\x{9fa5}]" to "十六进制表示"
    )
    
    for ((pattern, desc) in patterns) {
        try {
            val matches = testChar.matches(pattern.toRegex())
            println("$desc: $pattern")
            println("  结果: $matches")
        } catch (e: Exception) {
            println("$desc: $pattern")
            println("  错误: ${e.message}")
        }
        println()
    }
    
    // 测试完整的左侧匹配
    val leftText = "我可以帮助您提高"
    println("测试完整匹配:")
    println("文本: $leftText")
    
    val fullPatterns = listOf(
        ".*[\\u4e00-\\u9fa5]$",
        ".*[\u4e00-\u9fa5]$",
        ".*[一-龥]$"
    )
    
    for (pattern in fullPatterns) {
        try {
            val matches = leftText.matches(pattern.toRegex())
            println("  $pattern -> $matches")
        } catch (e: Exception) {
            println("  $pattern -> 错误: ${e.message}")
        }
    }
}
