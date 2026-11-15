package com.example.jap

import com.example.jap.data.Course
import org.json.JSONArray
import org.json.JSONObject

/** What the Basic course has taught by a unit, for checking that stories and talks use only known words. */
object KnownWords {

    private val course: JSONObject by lazy { ContentTest.asset("course.json") }

    /** Basic topic units in course order, without the alphabet. */
    val basicUnits: List<String> by lazy {
        ContentTest.load()
        Course.courses.first().unitIds.filterNot { id -> Course.units.first { it.id == id }.alphabet }
    }

    private fun JSONArray.objects() = List(length()) { getJSONObject(it) }

    /** Words of a token or line: punctuation splits words and punctuation alone is not a word. */
    fun words(text: String) = text.split(Regex("[、。！？「」『』…（）()?!,.・〜～\\s]+")).filter { it.isNotEmpty() }

    /** Sentence tokens, dialogue words and item spellings and readings anywhere in [node]. */
    private fun collect(node: Any?, out: MutableSet<String>) {
        when (node) {
            is JSONObject -> {
                node.optJSONArray("tokens")?.let { t -> (0 until t.length()).forEach { out += words(t.getString(it)) } }
                if (node.has("speaker") && node.has("jp")) out += words(node.getString("jp"))
                if (node.has("kind") && node.has("jp")) {
                    out += words(node.getString("jp"))
                    out += words(node.optString("reading"))
                }
                node.keys().forEach { collect(node.get(it), out) }
            }
            is JSONArray -> (0 until node.length()).forEach { collect(node.get(it), out) }
        }
    }

    /** Everything taught in the Basic course up to and including [unitId], alphabet first. */
    fun known(unitId: String): Set<String> {
        ContentTest.load()
        val order = Course.courses.first().unitIds
        val byId = course.getJSONArray("units").objects().associateBy { it.getString("id") }
        return mutableSetOf<String>().also { out -> order.take(order.indexOf(unitId) + 1).forEach { collect(byId[it], out) } }
    }
}
