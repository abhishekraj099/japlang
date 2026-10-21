package com.example.jap.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * Everything the guide says, read from assets/guide.json: the guides a learner can choose from, tours for screens
 * and first taps, answers to common questions, and the welcome demo. A step can point at an element the screen has
 * tagged with that id. Lines say "{name}" where the guide gives their own name, so they fit whichever guide is chosen.
 */
object Guide {

    /**
     * A guide the learner can pick: [pitch] sets their speaking voice (1 is the engine's normal pitch), [voice]
     * describes it, and [voices] lists the engine voices they prefer for each language ("ja", "en"), best first.
     */
    class Character(
        val id: String,
        val name: String,
        val pitch: Float,
        val about: String,
        val voice: String = "",
        val voices: Map<String, List<String>> = emptyMap()
    )

    /** A text-to-speech voice to use: the first of [names] installed on the phone (or its default), at [pitch]. */
    data class VoiceStyle(val names: List<String>, val pitch: Float)

    /** How a voice setting names a character's voice rather than one engine voice. */
    const val CHARACTER_VOICE = "character:"

    /**
     * Turns a voice setting into the voice to use for [language]. The setting is empty for the default, an engine
     * voice id, or [CHARACTER_VOICE] plus a character id. An empty setting means [default]'s voice when there is
     * one (the guide speaks in their own voice) and the phone's default voice otherwise (Japanese lessons).
     */
    fun voiceStyle(setting: String, language: String, default: Character?): VoiceStyle {
        val character = if (setting.startsWith(CHARACTER_VOICE)) {
            characters.firstOrNull { it.id == setting.removePrefix(CHARACTER_VOICE) }
        } else if (setting.isEmpty()) default else null
        return when {
            character != null -> VoiceStyle(character.voices[language].orEmpty(), character.pitch)
            // Another voice picked for the guide keeps the guide's own pitch.
            else -> VoiceStyle(listOfNotNull(setting.ifEmpty { null }), default?.pitch ?: 1f)
        }
    }

    /**
     * [target] is the tagged element to spotlight, or null for a step without one. With [tap] the learner moves on
     * by tapping the target itself instead of "Next". [side] is "left" or "right" to place the guide, or null to
     * let them stand on the side away from the target.
     */
    class Step(val target: String?, val pose: String, private val line: String, val tap: Boolean, val side: String? = null) {
        val text: String get() = named(line)
    }

    class Tour(val screen: String, val beginnersOnly: Boolean, val steps: List<Step>)

    /** A question learners often have, answered by the guide. */
    class Faq(private val q: String, private val a: String, val pose: String) {
        val question: String get() = named(q)
        val answer: String get() = named(a)
    }

    /** One page of the welcome demo shown before sign-in. */
    class DemoPage(private val heading: String, private val body: String, val pose: String) {
        val title: String get() = named(heading)
        val text: String get() = named(body)
    }

    var characters: List<Character> = emptyList()
        private set

    /** The guide the learner chose; screens redraw when it changes. */
    var current: Character by mutableStateOf(Character("hana", "Hana", 1.2f, ""))
        private set

    val name: String get() = current.name

    var faq: List<Faq> = emptyList()
        private set
    var demo: List<DemoPage> = emptyList()
        private set
    private var tours: Map<String, Tour> = emptyMap()

    fun load(json: JSONObject) {
        characters = json.getJSONArray("characters").objects().map {
            val voices = it.optJSONObject("voices")
            Character(
                it.getString("id"), it.getString("name"), it.optDouble("pitch", 1.0).toFloat(), it.optString("about"),
                it.optString("voice"),
                voices?.keys()?.asSequence()?.associateWith { lang -> voices.getJSONArray(lang).let { a -> List(a.length()) { i -> a.getString(i) } } }.orEmpty()
            )
        }
        current = characters.first()
        tours = json.getJSONArray("tours").objects().map { t ->
            Tour(
                t.getString("screen"),
                t.optBoolean("beginnersOnly"),
                t.getJSONArray("steps").objects().map {
                    Step(
                        it.optString("target").ifEmpty { null }, it.optString("pose", "wave"), it.getString("text"),
                        it.optBoolean("tap"), it.optString("side").ifEmpty { null }
                    )
                }
            )
        }.associateBy { it.screen }
        faq = json.optJSONArray("faq")?.objects().orEmpty().map { Faq(it.getString("q"), it.getString("a"), it.optString("pose", "think")) }
        demo = json.optJSONArray("demo")?.objects().orEmpty().map { DemoPage(it.getString("title"), it.getString("text"), it.optString("pose", "wave")) }
    }

    /** Makes [id] the guide; an unknown id keeps the current one. */
    fun select(id: String) {
        characters.firstOrNull { it.id == id }?.let { current = it }
    }

    fun tour(screen: String): Tour? = tours[screen]

    private fun named(text: String) = text.replace("{name}", name)
}
