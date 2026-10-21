package com.example.jap.data

import org.json.JSONObject

/** One stage of a scenario, e.g. "Asking someone's name", with its words and sentence patterns. */
data class ScenarioPart(val title: String, val vocab: List<Item>, val patterns: List<Sentence>)

/** An everyday situation such as a self-introduction or ordering at a restaurant. */
data class Scenario(
    val id: String,
    val title: String,
    val level: String,
    val icon: String,
    val color: Long,
    val dark: Long,
    val summary: String,
    val tags: List<String>,
    val parts: List<ScenarioPart>,
    /** Where the person's face sits in the picture, 0 (top) to 1 (bottom), so headers crop around it. */
    val imageFocus: Float = 0.3f
) {
    /** Lesson id for part [index], stored in the finished set. */
    fun partId(index: Int) = "scn-$id-${index + 1}"
}

/**
 * Where scenarios come from. Today this is assets/scenarios.json; a server-backed source only
 * needs to implement this interface and be assigned to [Scenarios.source].
 */
interface ScenarioSource {
    fun all(): List<Scenario>
    fun curated(): List<Scenario>
    fun search(query: String): List<Scenario>
}

object Scenarios {

    lateinit var source: ScenarioSource
        private set

    fun load(json: JSONObject) {
        source = LocalScenarios(json)
    }

    /** A playable lesson for one part: its words first, then its sentences. */
    fun lesson(scenario: Scenario, index: Int): Lesson {
        val part = scenario.parts[index]
        return Lesson(
            id = scenario.partId(index), title = part.title, type = LessonType.PRACTICE, icon = scenario.icon,
            items = part.vocab, sentences = part.patterns, tags = scenario.tags
        )
    }
}

private class LocalScenarios(json: JSONObject) : ScenarioSource {

    private val scenarios = json.getJSONArray("scenarios").objects().map { s ->
        val id = s.getString("id")
        Scenario(
            id = id,
            title = s.getString("title"),
            level = s.getString("level"),
            icon = s.getString("icon"),
            color = 0xFF000000 or s.getString("color").removePrefix("#").toLong(16),
            dark = 0xFF000000 or s.getString("dark").removePrefix("#").toLong(16),
            summary = s.getString("summary"),
            tags = s.getJSONArray("tags").strings(),
            parts = s.getJSONArray("parts").objects().map { p ->
                ScenarioPart(
                    title = p.getString("title"),
                    vocab = p.getJSONArray("vocab").objects().map { v ->
                        Item(
                            id = "sc-$id-${v.getString("romaji")}",
                            jp = v.getString("jp"),
                            reading = v.getString("reading").takeIf { it != v.getString("jp") }.orEmpty(),
                            romaji = v.getString("romaji"),
                            meaning = v.getString("meaning"),
                            kind = Kind.WORD,
                            say = v.getString("reading"),
                            note = v.optString("note")
                        )
                    },
                    patterns = p.getJSONArray("patterns").objects().map {
                        Sentence(it.getJSONArray("tokens").strings(), it.getString("romaji"), it.getString("en"))
                    }
                )
            },
            imageFocus = s.optDouble("imageFocus", 0.3).toFloat()
        )
    }
    private val curatedIds = json.getJSONArray("curated").strings()

    override fun all() = scenarios

    override fun curated() = curatedIds.mapNotNull { id -> scenarios.firstOrNull { it.id == id } }

    /** Best matches first: title, then tags, then what the scenario teaches. */
    override fun search(query: String): List<Scenario> {
        val words = query.lowercase().split(" ", ",", "'").filter { it.length > 1 }
        if (words.isEmpty()) return emptyList()
        return scenarios.map { s ->
            val score = words.sumOf { w ->
                (if (s.title.lowercase().contains(w)) 5 else 0) +
                    (if (s.tags.any { it.contains(w) || w.contains(it) }) 3 else 0) +
                    (if (s.summary.lowercase().contains(w)) 2 else 0) +
                    s.parts.sumOf { p ->
                        (if (p.title.lowercase().contains(w)) 2 else 0) +
                            p.vocab.count { it.meaning.lowercase().contains(w) || it.romaji.contains(w) }
                    }
            }
            s to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
    }
}
