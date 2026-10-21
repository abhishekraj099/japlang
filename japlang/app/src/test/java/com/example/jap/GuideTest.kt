package com.example.jap

import com.example.jap.data.Guide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.example.jap.audio.Voice
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Keeps assets/guide.json and the code in step: every tour can start, and every step can find its target. */
class GuideTest {

    companion object {
        private lateinit var code: String
        private lateinit var screens: List<String>

        @BeforeClass
        @JvmStatic
        fun load() {
            val json = ContentTest.asset("guide.json")
            Guide.load(json)
            screens = json.getJSONArray("tours").let { a -> List(a.length()) { a.getJSONObject(it).getString("screen") } }
            code = File("src/main/java").walk().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        }
    }

    @Test
    fun everyTourHasATriggerInTheCode() {
        screens.forEach { screen ->
            // Screen tours start from GuideTour; first-tap explanations are named where the tap is handled.
            assertTrue("Nothing starts \"$screen\"", code.contains("GuideTour(\"$screen\")") || code.contains("\"$screen\""))
        }
    }

    @Test
    fun everyTargetIsTaggedOnAnElement() {
        screens.flatMap { Guide.tour(it)!!.steps }.mapNotNull { it.target }.distinct().forEach { target ->
            assertTrue("Nothing has guideTarget(\"$target\")", code.contains("guideTarget(\"$target\")"))
        }
    }

    @Test
    fun posesAreOnesTheGuideCanDraw() {
        val poses = setOf("wave", "point", "cheer", "think")
        screens.flatMap { Guide.tour(it)!!.steps }.forEach { assertTrue(it.pose, it.pose in poses) }
    }

    @Test
    fun helperQuestionsAndDemoAreThere() {
        assertEquals(5, Guide.faq.size)
        assertEquals(3, Guide.demo.size)
        val poses = setOf("wave", "point", "cheer", "think")
        (Guide.faq.map { it.pose } + Guide.demo.map { it.pose }).forEach { assertTrue(it, it in poses) }
    }

    @Test
    fun voiceSaysTheRomajiNotTheJapaneseScript() {
        assertEquals("like (koohii, coffee) and (terebi, TV).", Voice.spoken("like コーヒー (koohii, coffee) and テレビ (terebi, TV)."))
        assertEquals("Hello", Voice.spoken("Hello"))
    }

    @Test
    fun learnersCanChooseAGirlOrABoyGuide() {
        assertEquals(listOf("hana", "ren"), Guide.characters.map { it.id })
        assertTrue("A boy guide speaks lower", Guide.characters[1].pitch < Guide.characters[0].pitch)
        try {
            Guide.select("ren")
            assertEquals("Ren", Guide.name)
            assertTrue(Guide.tour("learn")!!.steps.first().text.startsWith("Hi, I'm Ren!"))
            assertEquals("Hi, I'm Ren!", Guide.demo.first().title)
            // No line is left with the placeholder or the other guide's name.
            val lines = Guide.characters.indices.let {
                listOf("learn", "alphabet", "streak", "practice", "me", "lessonplus", "video", "tap-katakana")
                    .flatMap { s -> Guide.tour(s)!!.steps.map { it.text } } +
                    Guide.faq.flatMap { listOf(it.question, it.answer) } + Guide.demo.flatMap { listOf(it.title, it.text) }
            }
            lines.forEach { assertTrue(it, "{name}" !in it && "Hana" !in it) }
            Guide.select("nobody")
            assertEquals("Ren", Guide.name)
        } finally {
            Guide.select("hana")
        }
    }
}
