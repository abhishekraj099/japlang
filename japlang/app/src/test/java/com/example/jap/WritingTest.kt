package com.example.jap

import com.example.jap.data.Config
import com.example.jap.data.Course
import com.example.jap.data.Exercises
import com.example.jap.data.Item
import com.example.jap.data.Kind
import com.example.jap.data.LessonType
import com.example.jap.data.Memory
import com.example.jap.data.Write
import com.example.jap.data.Strokes
import com.example.jap.data.Writing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class WritingTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun load() = ContentTest.load()
    }

    private fun word(jp: String, reading: String = "") = Item("w", jp, reading, "x", "meaning", kind = Kind.WORD)

    /** Each box read perfectly: the character itself is the best guess. */
    private fun boxes(text: String) = text.map { listOf(it.toString()) }

    /** Item ids that get a Write exercise somewhere on the Learn path. */
    private val written: Set<String> by lazy {
        Course.lessons.flatMap { Exercises.forLesson(it) }.filterIsInstance<Write>().map { it.item.id }.toSet()
    }

    @Test
    fun everyItemOnThePathIsWritten() {
        val all = Course.lessons.flatMap { it.items }.distinctBy { it.id }
        val byKind = all.groupBy { it.kind }.mapValues { (_, v) -> v.map { it.id }.toSet() }
        assertEquals(918, byKind.getValue(Kind.WORD).size)
        assertEquals(46, byKind.getValue(Kind.HIRAGANA).size)
        assertEquals(46, byKind.getValue(Kind.KATAKANA).size)
        assertEquals(270, byKind.getValue(Kind.KANJI).size)
        byKind.values.forEach { ids -> assertTrue((ids - written).toString(), written.containsAll(ids)) }
        assertEquals(1280, written.size)
    }

    @Test
    fun wordsSplitIntoKanjiAndKanaOnly() {
        // 19 words are taught in kana first and with kanji later under the same id (がくせい, then 学生).
        val versions = Course.lessons.flatMap { it.items }.filter { it.kind == Kind.WORD }.groupBy { it.id }
        assertEquals(918, versions.size)
        assertEquals(19, versions.count { (_, v) -> v.map { it.jp }.distinct().size > 1 })
        assertEquals(421, versions.count { (_, v) -> v.any(Writing::hasKanji) })
        assertEquals(497, versions.count { (_, v) -> v.none(Writing::hasKanji) })
        // Every kanji spelling can also be written in kana.
        assertTrue(versions.values.flatten().filter(Writing::hasKanji).all { Writing.spellings(it).size == 2 })
    }

    @Test
    fun kanjiOrKanaSpellingIsAccepted() {
        val student = word("学生", "がくせい")
        assertEquals(listOf("学生", "がくせい"), Writing.spellings(student))
        assertEquals(4, Writing.boxes(student))
        assertEquals("学生", Writing.match(student, boxes("学生") + listOf(null, null), Config.writingTop))
        assertEquals("がくせい", Writing.match(student, boxes("がくせい"), Config.writingTop))
        assertNull(Writing.match(student, boxes("学土"), Config.writingTop))
        assertNull(Writing.match(student, boxes("がくせ"), Config.writingTop))
    }

    @Test
    fun targetMustBeInTheTopCandidates() {
        val cat = word("ねこ")
        val second = listOf(listOf("ね", "れ"), listOf("て", "こ"))
        assertEquals("ねこ", Writing.match(cat, second, Config.writingTop))
        val tooFar = listOf(listOf("ね"), listOf("a", "b", "c", "こ"))
        assertNull(Writing.match(cat, tooFar, 3))
    }

    @Test
    fun smallKanaAndLookalikesMatch() {
        assertEquals("きって", Writing.match(word("きって"), boxes("きつて"), 3))
        assertEquals("コーヒー", Writing.match(word("コーヒー"), boxes("コ一ヒ一"), 3))
        assertEquals("ロ", Writing.match(Item("k-ro", "ロ", "", "ro", "", kind = Kind.KATAKANA), boxes("口"), 3))
    }

    @Test
    fun tildeIsPrintedNotWritten() {
        val way = word("～方", "かた")
        assertEquals("～", Writing.prefix(way))
        assertEquals(listOf("方", "かた"), Writing.spellings(way))
        assertEquals("方", Writing.match(way, boxes("方"), 3))
    }

    @Test
    fun writingIsTracedUntilWrittenRightOnce() {
        val now = 1_000L
        // Wrong the first time: still traced next time.
        val missed = Memory.next(null, Memory.Grade.AGAIN, now)
        assertEquals(0, missed.hits)
        assertEquals(Memory.Level.NEW, Memory.level(missed))
        // Right once: free writing from then on, and it comes back later.
        val written = Memory.next(missed, Memory.Grade.GOOD, now + 600_000L)
        assertEquals(1, written.hits)
        assertTrue(written.due > now + 600_000L)
    }

    @Test
    fun writingLessonsWriteEveryItemOfTheirUnit() {
        // Kana and kanji units write every character inside its own lesson instead.
        Course.units.filterNot { unit -> unit.alphabet || unit.lessons.any { it.type == LessonType.KANJI } }.forEach { unit ->
            val lesson = unit.lessons.first { it.type == LessonType.WRITING }
            val writes = Exercises.forLesson(lesson).filterIsInstance<Write>().map { it.item.id }
            assertEquals(lesson.items.map { it.id }, writes)
        }
    }

    @Test
    fun kanjiN4IsComplete() {
        val track = Course.courses.first { it.id == "kanji" }
        val kanjiOf = { name: String ->
            Course.unitsOf(track.levels.first { it.name == name }).map { unit ->
                unit.lessons.filter { it.type == LessonType.KANJI }.flatMap { it.items }
            }
        }
        val n5 = kanjiOf("Kanji N5").flatten().map { it.jp }.toSet()
        val units = kanjiOf("Kanji N4")
        assertTrue(units.dropLast(1).all { it.size == 10 } && units.last().size in 1..10)
        val n4 = units.flatten()
        assertTrue(n4.size.toString(), n4.size >= 160)
        assertEquals(n4.size, n4.map { it.jp }.distinct().size)
        assertTrue((n4.map { it.jp }.toSet() intersect n5).toString(), n4.none { it.jp in n5 })
        n4.forEach { kanji ->
            assertTrue("No meaning: ${kanji.jp}", kanji.meaning.isNotBlank())
            assertTrue("No readings: ${kanji.jp}", kanji.on.isNotEmpty() || kanji.kun.isNotEmpty())
            assertTrue("No words: ${kanji.jp}", kanji.words.size >= 2)
            kanji.words.forEach { w -> assertTrue("Bad word: $w", listOf(w.jp, w.reading, w.romaji, w.en).all { it.isNotBlank() }) }
            assertTrue("No strokes: ${kanji.jp}", Strokes.of(kanji.jp).isNotEmpty())
        }
    }

    @Test
    fun kanjiLessonsWriteEveryKanjiAndHaveStrokes() {
        val lessons = Course.lessons.filter { it.type == LessonType.KANJI }
        assertEquals(54, lessons.size)
        lessons.forEach { lesson ->
            val writes = Exercises.forLesson(lesson).filterIsInstance<Write>().map { it.item.id }
            assertEquals(lesson.items.map { it.id }, writes)
            lesson.items.forEach { kanji ->
                assertTrue("No readings: ${kanji.jp}", kanji.on.isNotEmpty() || kanji.kun.isNotEmpty())
                assertTrue("No words: ${kanji.jp}", kanji.words.size >= 2)
                assertTrue("No strokes: ${kanji.jp}", Strokes.of(kanji.jp).isNotEmpty())
            }
        }
    }
}
