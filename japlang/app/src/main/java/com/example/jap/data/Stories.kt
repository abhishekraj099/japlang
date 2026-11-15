package com.example.jap.data

import org.json.JSONObject

/** One line of a story, written as tokens like a course sentence. */
data class StoryLine(val speaker: String, val tokens: List<String>, val romaji: String, val en: String)

/** A short graded reading, using only what the Basic course has taught by [unlockAfter]. */
data class Story(val id: String, val title: String, val unlockAfter: String, val lines: List<StoryLine>, val questions: List<Question>)

/** The graded reading stories, read from assets/stories.json. */
object Stories {

    var all: List<Story> = emptyList()
        private set

    fun load(json: JSONObject) {
        all = json.getJSONArray("stories").objects().map { s ->
            Story(
                s.getString("id"), s.getString("title"), s.getString("unlockAfter"),
                s.getJSONArray("lines").objects().map {
                    StoryLine(it.getString("speaker"), it.getJSONArray("tokens").strings(), it.getString("romaji"), it.getString("en"))
                },
                s.getJSONArray("questions").objects().map {
                    Question("story", it.getString("prompt"), it.getJSONArray("options").strings(), it.getString("explain"))
                }
            )
        }
    }

    /** A unit's review is done both by finishing it and by testing out of the unit, so either unlocks its story. */
    fun unlocked(done: Set<String>): List<Story> = all.filter { "${it.unlockAfter}-check" in done }
}
