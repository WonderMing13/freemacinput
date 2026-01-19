fun main() {
    val leftPattern = """.*[\u4e00-\u9fa5]$"""
    val rightPattern = """^[\u4e00-\u9fa5].*"""
    
    val leftText = """String text = "我可以帮助您提"""
    val rightText = """高效率";List<AppNavigate"""
    
    println("左侧文本: '$leftText'")
    println("左侧最后一个字符: '${leftText.last()}' (${leftText.last().code})")
    println("左侧匹配: ${leftPattern.toRegex().matches(leftText)}")
    
    println()
    
    println("右侧文本: '$rightText'")
    println("右侧第一个字符: '${rightText.first()}' (${rightText.first().code})")
    println("右侧匹配: ${rightPattern.toRegex().matches(rightText)}")
    
    // 测试简单情况
    println()
    println("简单测试:")
    println("'提' 匹配 [\\u4e00-\\u9fa5]: ${"提".matches("""[\u4e00-\u9fa5]""".toRegex())}")
    println("'高' 匹配 [\\u4e00-\\u9fa5]: ${"高".matches("""[\u4e00-\u9fa5]""".toRegex())}")
}
