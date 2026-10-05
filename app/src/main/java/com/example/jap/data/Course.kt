package com.example.jap.data

import org.json.JSONObject

/** The course, read from assets/course.json. Nothing about what is taught lives in code. */
object Course {

    var units: List<CourseUnit> = emptyList()
        private set
    var lessons: List<Lesson> = emptyList()
        private set

    /** Every item that can be quizzed: lesson items plus the extra kana that only appear in the chart and drills. */
    var allItems: List<Item> = emptyList()
        private set
    var phrases: List<Phrase> = emptyList()
        private set

    var courses: List<Track> = emptyList()
        private set
    var curated: List<Curated> = emptyList()
        private set
    var topics: List<String> = emptyList()
        private set
    var tips: List<Tip> = emptyList()
        private set

    /** Each unit's conversation, kept off the learning path and played on the Talk tab. */
    var conversations: List<Lesson> = emptyList()
        private set

    private var byId: Map<String, Item> = emptyMap()
    private var unitById: Map<String, CourseUnit> = emptyMap()
    private val talks = mutableListOf<Pair<String, Lesson>>()
    private var chart: List<Pair<String, List<String>>> = emptyList()

    fun item(id: String): Item? = byId[id]

    fun unitsOf(level: Level): List<CourseUnit> = level.units.mapNotNull { id -> units.firstOrNull { it.id == id } }

    fun lessonCount(track: Track): Int = track.unitIds.sumOf { id -> units.firstOrNull { it.id == id }?.lessons?.size ?: 0 }

    fun unitOf(lesson: Lesson): CourseUnit = unitById.getValue(lesson.id)

    /** The first lesson of every unit is open so learners who already know kana can jump ahead. */
    fun unlocked(lesson: Lesson, done: Set<String>): Boolean {
        val i = lessons.indexOf(lesson)
        return i == 0 || lesson.id in done || lessons[i - 1].id in done || units.any { it.lessons.first() == lesson }
    }

    /** Sections of the kana table, each a list of rows; null marks an empty cell. */
    fun kanaChart(kind: Kind): List<Pair<String, List<List<Item?>>>> {
        val prefix = prefix(kind)
        return chart.map { (title, rows) ->
            title to rows.map { row -> row.split(" ").map { byId["$prefix-$it"] } }
        }
    }

    fun load(json: JSONObject) {
        val checkpoint = json.getJSONObject("checkpoint")
        val icons = json.getJSONObject("icons")
        talks.clear()
        units = json.getJSONArray("units").objects().map { unit ->
            val id = unit.getString("id")
            val tags = unit.optJSONArray("tags")?.strings().orEmpty()
            val taught = unit.optJSONArray("lessons")?.objects()?.map { lesson(it).copy(tags = tags) }
                ?: topicLessons(id, unit, tags, icons)
            // Each unit ends with a review of everything in it.
            val review = Lesson(
                id = "$id-check",
                title = unit.optString("reviewTitle").ifEmpty { checkpoint.getString("title") },
                type = LessonType.CHECKPOINT,
                icon = checkpoint.getString("icon"),
                items = taught.flatMap { it.items }.distinctBy { it.id },
                sentences = taught.flatMap { it.sentences },
                tags = tags
            )
            CourseUnit(
                id, unit.getString("title"), unit.getString("subtitle"),
                colour(unit.getString("color")), colour(unit.getString("dark")), taught + review,
                alphabet = unit.optBoolean("alphabet")
            )
        }
        lessons = units.flatMap { it.lessons }
        conversations = talks.map { it.second }
        unitById = units.flatMap { unit -> unit.lessons.map { it.id to unit } }.toMap() +
            talks.associate { (unitId, talk) -> talk.id to units.first { it.id == unitId } }

        val extra = json.getJSONArray("extraKana").objects().flatMap(::kana)
        allItems = (lessons.flatMap { it.items } + extra).distinctBy { it.id }

        // Characters that sound the same as another one: shown in the chart, never quizzed.
        val chartOnly = json.getJSONArray("chartOnly").objects().map {
            Item(it.getString("id"), it.getString("jp"), "", it.getString("romaji"), "", kind = Kind.valueOf(it.getString("script")))
        }
        byId = (allItems + chartOnly).associateBy { it.id }
        chart = json.getJSONArray("chart").objects().map { it.getString("title") to it.getJSONArray("rows").strings() }
        phrases = lessons.flatMap { it.phrases } + json.getJSONArray("phrases").objects().map(::phrase)

        courses = json.getJSONArray("courses").objects().map { c ->
            Track(
                c.getString("id"), c.getString("title"), c.getString("icon"),
                colour(c.getString("color")), colour(c.getString("dark")),
                c.getJSONArray("levels").objects().map { Level(it.getString("name"), it.getJSONArray("units").strings(), it.optInt("rank")) }
            )
        }
        curated = json.getJSONArray("curated").objects().mapNotNull { entry ->
            (lessons + conversations).firstOrNull { it.id == entry.getString("lesson") }?.let { Curated(it, entry.getString("level")) }
        }
        topics = json.getJSONArray("topics").strings()
        tips = json.getJSONArray("tips").objects().map { Tip(it.getString("title"), it.getString("body")) }
    }

    /** Lessons about [topic], matched on lesson titles and tags and on word meanings. */
    fun lessonsAbout(topic: String): List<Lesson> {
        val words = topic.lowercase().split(" ", ",").filter { it.length > 1 }
        if (words.isEmpty()) return emptyList()
        return lessons.filter { lesson ->
            lesson.type != LessonType.CHECKPOINT && words.any { word ->
                lesson.title.lowercase().contains(word) ||
                    lesson.tags.any { it.contains(word) || word.contains(it) } ||
                    lesson.items.any { it.meaning.lowercase().contains(word) }
            }
        }
    }

    /**
     * The fixed shape of a topic unit: Theory, one lesson per practice set, Speaking and Writing.
     * The unit's review is added by the caller; its conversation goes to [conversations].
     */
    private fun topicLessons(id: String, unit: JSONObject, tags: List<String>, icons: JSONObject): List<Lesson> {
        val practices = unit.getJSONArray("practices").objects().mapIndexed { i, p ->
            Lesson(
                id = "$id-p${i + 1}",
                title = p.optString("title").ifEmpty { "Practice ${i + 1}" },
                type = LessonType.PRACTICE,
                icon = icons.getString("PRACTICE"),
                items = p.optJSONArray("items")?.objects().orEmpty().map(::item),
                sentences = sentences(p),
                note = p.optJSONObject("note")?.let(::note),
                tags = tags
            )
        }
        val theory = unit.optJSONArray("theory")?.objects().orEmpty().map { TheoryNote(note(it), sentences(it, "examples")) } +
            practices.mapNotNull { p -> p.note?.let { TheoryNote(it, p.sentences.take(2)) } }
        val talk = unit.optJSONObject("dialogue")?.let { d ->
            Lesson(
                id = "$id-talk", title = d.getString("title"), type = LessonType.DIALOGUE, icon = icons.getString("DIALOGUE"),
                dialogue = d.getJSONArray("lines").objects().map(::line), sentences = sentences(d), tags = tags
            ).also { talks += id to it }
        }
        // Speaking covers the unit's phrases plus every line of its conversation.
        val spoken = unit.optJSONArray("phrases")?.objects().orEmpty().map(::phrase) +
            talk?.dialogue.orEmpty().map { Phrase(it.jp, it.romaji, it.en, it.heard) }
        return listOfNotNull(
            Lesson("$id-theory", "Theory", LessonType.THEORY, icons.getString("THEORY"), tags = tags, theory = theory)
                .takeIf { theory.isNotEmpty() }
        ) + practices + listOfNotNull(
            Lesson("$id-speak", "Speaking", LessonType.SPEAKING, icons.getString("SPEAKING"), phrases = spoken, tags = tags)
                .takeIf { spoken.isNotEmpty() },
            Lesson(
                "$id-write", "Writing", LessonType.WRITING, icons.getString("WRITING"),
                items = practices.flatMap { it.items }.distinctBy { it.id }, sentences = practices.flatMap { it.sentences }, tags = tags
            )
        )
    }

    private fun note(json: JSONObject) = Note(json.getString("title"), json.getString("pattern"), json.getString("body"))

    private fun sentences(json: JSONObject, key: String = "sentences") = json.optJSONArray(key)?.objects().orEmpty().map {
        Sentence(
            tokens = it.getJSONArray("tokens").strings(),
            romaji = it.getString("romaji"),
            en = it.getString("en"),
            blank = it.optInt("blank", -1),
            distract = it.optJSONArray("distract")?.strings().orEmpty()
        )
    }

    private fun line(json: JSONObject) = Line(
        json.getString("speaker"), json.getString("jp"), json.getString("romaji"), json.getString("en"),
        json.optJSONArray("heard")?.strings().orEmpty()
    )

    private fun prefix(kind: Kind) = when (kind) {
        Kind.HIRAGANA -> "h"
        Kind.KATAKANA -> "k"
        Kind.KANJI -> "j"
        Kind.WORD -> "w"
    }

    /** "#RRGGBB" as an opaque ARGB value. */
    private fun colour(hex: String): Long = 0xFF000000 or hex.removePrefix("#").toLong(16)

    /** Kana are written as "character romaji" pairs in one string, e.g. "あ a い i". */
    private fun kana(json: JSONObject): List<Item> {
        val kind = Kind.valueOf(json.getString("script"))
        return json.getString("chars").trim().split(" ").chunked(2).map { (jp, romaji) ->
            Item(id = "${prefix(kind)}-$romaji", jp = jp, reading = "", romaji = romaji, meaning = "", kind = kind)
        }
    }

    private fun item(json: JSONObject): Item {
        val kind = Kind.valueOf(json.getString("kind"))
        val romaji = json.getString("romaji")
        return Item(
            id = json.optString("id").ifEmpty { "${prefix(kind)}-$romaji" },
            jp = json.getString("jp"),
            reading = json.optString("reading"),
            romaji = romaji,
            meaning = json.optString("meaning"),
            emoji = json.optString("emoji"),
            kind = kind,
            say = json.optString("say"),
            note = json.optString("note")
        )
    }

    private fun phrase(json: JSONObject) = Phrase(
        json.getString("jp"), json.getString("romaji"), json.getString("en"),
        json.optJSONArray("heard")?.strings().orEmpty()
    )

    private fun lesson(json: JSONObject): Lesson {
        val items = json.optJSONObject("kana")?.let(::kana)
            ?: json.optJSONArray("items")?.objects()?.map(::item).orEmpty()
        return Lesson(
            id = json.getString("id"),
            title = json.getString("title"),
            type = LessonType.valueOf(json.getString("type")),
            icon = json.getString("icon"),
            items = items,
            sentences = sentences(json),
            note = json.optJSONObject("note")?.let(::note),
            dialogue = json.optJSONArray("dialogue")?.objects().orEmpty().map(::line),
            theory = json.optJSONArray("theory")?.objects().orEmpty().map { TheoryNote(note(it), sentences(it, "examples")) },
            phrases = json.optJSONArray("phrases")?.objects().orEmpty().map(::phrase),
            tags = json.optJSONArray("tags")?.strings().orEmpty()
        )
    }
}
