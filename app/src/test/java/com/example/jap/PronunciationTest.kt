package com.example.jap

import com.example.jap.data.Pronunciation
import com.example.jap.data.Ruby
import com.example.jap.data.VideoLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationTest {

    private fun line(vararg words: List<Ruby>) = VideoLine(0, 0.0, 1.0, words.toList(), "", "", "")

    private val welcome = line(listOf(Ruby("いらっしゃいませ", "")))

    @Test
    fun exactMatchScoresFull() {
        assertEquals(100, Pronunciation.assess(welcome, listOf("いらっしゃいませ")).score)
    }

    @Test
    fun extraSoundsAreReported() {
        val a = Pronunciation.assess(welcome, listOf("いらっしゃっしゃいませ"))
        assertEquals(77, a.score)
        assertTrue(a.issues.single().notes.single().contains("extra sound"))
    }

    @Test
    fun kanjiFromTheRecogniserIsReadBack() {
        val eat = line(listOf(Ruby("朝", "あさ"), Ruby("ご", ""), Ruby("飯", "はん"), Ruby("を", "")), listOf(Ruby("食", "た"), Ruby("べます", "")))
        val a = Pronunciation.assess(eat, listOf("朝ご飯を食べます"))
        assertEquals(100, a.score)
        assertEquals(listOf(true, true), a.wordOk)
    }

    @Test
    fun missingSoundMarksTheWord() {
        val eat = line(listOf(Ruby("ごはん", "")), listOf(Ruby("たべます", "")))
        val a = Pronunciation.assess(eat, listOf("ごはんたべす"))
        assertEquals(listOf(true, false), a.wordOk)
        assertTrue(a.issues.single().notes.single().contains("「ま」 was missing"))
    }
}
