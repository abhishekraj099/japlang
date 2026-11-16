package com.example.jap.data

import org.json.JSONObject

/** One mock JLPT paper. */
class ExamPaper(val id: String, val title: String, val minutes: Int, val passPercent: Int, val questions: List<Question>)

/** The mock tests: "n5" from assets/exam.json and "n4" from assets/exam_n4.json, in load order. */
object Exams {

    var papers: List<ExamPaper> = emptyList()
        private set

    /** Loading an id again replaces that paper, so tests and a reload never duplicate it. */
    fun load(id: String, json: JSONObject) {
        val paper = ExamPaper(
            id = id,
            title = json.getString("title"),
            minutes = json.getInt("minutes"),
            passPercent = json.getInt("passPercent"),
            questions = json.getJSONArray("questions").objects().map {
                Question(
                    section = it.getString("section"),
                    prompt = it.getString("prompt"),
                    options = it.getJSONArray("options").strings(),
                    explain = it.getString("explain"),
                    passage = it.optString("passage")
                )
            }
        )
        papers = papers.filter { it.id != id } + paper
    }

    fun paper(id: String): ExamPaper? = papers.find { it.id == id }
}
