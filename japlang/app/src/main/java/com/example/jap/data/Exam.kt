package com.example.jap.data

import org.json.JSONObject

/** The practice test, read from assets/exam.json. */
object Exam {

    var title = ""
        private set
    var minutes = 0
        private set
    var passPercent = 0
        private set
    var questions: List<Question> = emptyList()
        private set

    fun load(json: JSONObject) {
        title = json.getString("title")
        minutes = json.getInt("minutes")
        passPercent = json.getInt("passPercent")
        questions = json.getJSONArray("questions").objects().map {
            Question(
                section = it.getString("section"),
                prompt = it.getString("prompt"),
                options = it.getJSONArray("options").strings(),
                explain = it.getString("explain"),
                passage = it.optString("passage")
            )
        }
    }
}
