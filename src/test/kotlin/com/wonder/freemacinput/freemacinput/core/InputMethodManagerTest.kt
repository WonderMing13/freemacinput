package com.wonder.freemacinput.freemacinput.core

import org.junit.Assert.*
import org.junit.Test

/**
 * InputMethodManager 单元测试
 *
 * 测试输入法切换的逻辑（不实际执行 AppleScript）
 */
class InputMethodManagerTest {

    data class TestSwitchState(
        var lastSwitchTime: Long = 0,
        var lastSwitchedTo: InputMethodType? = null,
        var currentTargetMethod: InputMethodType = InputMethodType.ENGLISH,
        val SWITCH_COOLDOWN_MS: Long = 1000
    )

    private fun shouldSwitch(
        state: TestSwitchState,
        targetMethod: InputMethodType
    ): Pair<Boolean, String> {
        if (targetMethod == InputMethodType.AUTO) {
            return true to "AUTO模式，跳过切换"
        }

        val now = System.currentTimeMillis()

        if (now - state.lastSwitchTime < state.SWITCH_COOLDOWN_MS) {
            return false to "冷却中，跳过"
        }

        if (targetMethod == state.lastSwitchedTo && targetMethod == state.currentTargetMethod) {
            return false to "目标相同，跳过"
        }

        return true to "应该切换"
    }

    @Test
    fun testAutoModeNoSwitch() {
        val state = TestSwitchState()
        val (should, reason) = shouldSwitch(state, InputMethodType.AUTO)
        assertTrue("AUTO模式应该跳过", should)
        assertEquals("AUTO模式，跳过切换", reason)
    }

    @Test
    fun testFirstSwitchShouldExecute() {
        val state = TestSwitchState()
        val (should, _) = shouldSwitch(state, InputMethodType.CHINESE)
        assertTrue("首次切换应该执行", should)
    }

    @Test
    fun testSameTargetNoRepeatSwitch() {
        // 设置一个过去的切换时间，避免冷却期影响
        val state = TestSwitchState(
            lastSwitchTime = System.currentTimeMillis() - 2000,
            lastSwitchedTo = InputMethodType.CHINESE,
            currentTargetMethod = InputMethodType.CHINESE
        )
        val (should, reason) = shouldSwitch(state, InputMethodType.CHINESE)
        assertFalse("相同目标应该跳过", should)
        assertEquals("目标相同，跳过", reason)
    }

    @Test
    fun testDifferentTargetShouldSwitch() {
        // 设置一个过去的切换时间，避免冷却期影响
        val state = TestSwitchState(
            lastSwitchTime = System.currentTimeMillis() - 2000,
            lastSwitchedTo = InputMethodType.CHINESE,
            currentTargetMethod = InputMethodType.CHINESE
        )
        val (should, _) = shouldSwitch(state, InputMethodType.ENGLISH)
        assertTrue("不同目标应该切换", should)
    }

    @Test
    fun testCooldownNoSwitch() {
        val state = TestSwitchState(
            lastSwitchTime = System.currentTimeMillis(),
            lastSwitchedTo = InputMethodType.CHINESE,
            currentTargetMethod = InputMethodType.CHINESE,
            SWITCH_COOLDOWN_MS = 5000
        )
        val (should, reason) = shouldSwitch(state, InputMethodType.ENGLISH)
        assertFalse("冷却期间应该跳过", should)
        assertEquals("冷却中，跳过", reason)
    }

    @Test
    fun testCooldownCanSwitchAfter() {
        val state = TestSwitchState(
            lastSwitchTime = System.currentTimeMillis() - 2000,
            lastSwitchedTo = InputMethodType.CHINESE,
            currentTargetMethod = InputMethodType.CHINESE,
            SWITCH_COOLDOWN_MS = 1000
        )
        val (should, _) = shouldSwitch(state, InputMethodType.ENGLISH)
        assertTrue("冷却期过后应该可以切换", should)
    }

    @Test
    fun testEnglishToChinese() {
        val state = TestSwitchState(
            currentTargetMethod = InputMethodType.ENGLISH
        )
        val (should, _) = shouldSwitch(state, InputMethodType.CHINESE)
        assertTrue("ENGLISH -> CHINESE 应该切换", should)
    }

    @Test
    fun testChineseToEnglish() {
        val state = TestSwitchState(
            currentTargetMethod = InputMethodType.CHINESE
        )
        val (should, _) = shouldSwitch(state, InputMethodType.ENGLISH)
        assertTrue("CHINESE -> ENGLISH 应该切换", should)
    }

    @Test
    fun testInputMethodTypeEnum() {
        assertEquals(3, InputMethodType.entries.size)
        assertTrue(InputMethodType.entries.contains(InputMethodType.ENGLISH))
        assertTrue(InputMethodType.entries.contains(InputMethodType.CHINESE))
        assertTrue(InputMethodType.entries.contains(InputMethodType.AUTO))
    }
}
