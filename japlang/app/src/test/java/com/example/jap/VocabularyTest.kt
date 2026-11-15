package com.example.jap

import com.example.jap.data.Course
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** The Basic course teaches every word of a standard JLPT N5 list (there is no official one). */
class VocabularyTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()
    }

    /**
     * Words are compared on their kana reading, so a kana and a kanji spelling of one word both count; a noun also
     * counts as taught by its する verb (べんきょう by べんきょうします).
     */
    @Test
    fun n5ListIsCovered() {
        val basic = Course.courses.first().unitIds.toSet()
        val taught = Course.units.filter { it.id in basic }.flatMap { it.lessons }.flatMap { it.items }
            .flatMap { listOf(it.reading, it.jp) }.map { it.replace(" ", "") }.toSet()
        val list = javaClass.getResource("/n5_words.txt")!!.readText().lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }.map { it.substringBefore("|").replace(" ", "") }
        val missing = list.filterNot { it in taught || "${it}します" in taught }
        assertTrue("${missing.size} N5 words are not taught: $missing", missing.isEmpty())
    }
}
