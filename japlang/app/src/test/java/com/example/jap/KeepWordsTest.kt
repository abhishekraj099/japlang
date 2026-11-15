package com.example.jap

import com.example.jap.ui.theme.keepWords
import org.junit.Assert.assertEquals
import org.junit.Test

class KeepWordsTest {
    @Test
    fun onlySpacesRemainBreakable() {
        val shown = keepWords("ちち と いもうと です。")
        assertEquals("ちち と いもうと です。", shown.replace("⁠", ""))
        assertEquals(listOf("ち⁠ち", "と", "い⁠も⁠う⁠と", "で⁠す⁠。"), shown.split(" "))
    }
}
