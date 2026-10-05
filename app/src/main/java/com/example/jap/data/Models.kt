package com.example.jap.data

enum class Kind { HIRAGANA, KATAKANA, WORD, KANJI }

data class Item(
    val id: String,
    val jp: String,
    val reading: String,
    val romaji: String,
    val meaning: String,
    val emoji: String = "",
    val kind: Kind,
    val say: String = "",
    val note: String = ""
) {
    val isKana get() = kind == Kind.HIRAGANA || kind == Kind.KATAKANA

    /** Text handed to the speech engine. */
    val speak get() = say.ifEmpty { reading.ifEmpty { jp } }

    /** What the learner answers with: the sound for kana, the meaning for everything else. */
    val label get() = if (isKana) romaji else meaning
}

data class Sentence(
    val tokens: List<String>,
    val romaji: String,
    val en: String,
    val blank: Int = -1,
    val distract: List<String> = emptyList()
) {
    val text get() = tokens.joinToString("")
}

/** One line of a dialogue. [heard] lists other spellings a speech recogniser may return for it. */
data class Line(val speaker: String, val jp: String, val romaji: String, val en: String, val heard: List<String> = emptyList())

data class Note(val title: String, val pattern: String, val body: String)

/** A line to say aloud. [heard] lists other spellings a speech recogniser may return for it. */
data class Phrase(val jp: String, val romaji: String, val en: String, val heard: List<String> = emptyList())

/** Mock-exam question; the first option is the correct one and the screen shuffles them. */
data class Question(
    val section: String,
    val prompt: String,
    val options: List<String>,
    val explain: String,
    val passage: String = ""
)

enum class LessonType { KANA, VOCAB, GRAMMAR, KANJI, DIALOGUE, CHECKPOINT, THEORY, PRACTICE, SPEAKING, WRITING }

/** A grammar or usage explanation with the example sentences shown under it. */
data class TheoryNote(val note: Note, val examples: List<Sentence>)

data class Lesson(
    val id: String,
    val title: String,
    val type: LessonType,
    val icon: String,
    val items: List<Item> = emptyList(),
    val sentences: List<Sentence> = emptyList(),
    val note: Note? = null,
    val dialogue: List<Line> = emptyList(),
    val phrases: List<Phrase> = emptyList(),
    /** Topics the lesson covers, used to build lessons on request. */
    val tags: List<String> = emptyList(),
    val theory: List<TheoryNote> = emptyList()
) {
    /** What the lesson covers in one or two lines: its words, then its grammar pattern. */
    val summary: String
        get() = listOfNotNull(
            items.takeIf { it.isNotEmpty() }?.joinToString("、") { it.jp },
            note?.pattern ?: theory.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.note.title }
        ).joinToString("\n")
}

/** A group of units inside a course, like a volume of a textbook. [rank] (0-3) drives the difficulty bars. */
data class Level(val name: String, val units: List<String>, val rank: Int)

/** A course the learner can switch to from the Learn tab, made of one or more levels. */
data class Track(val id: String, val title: String, val icon: String, val color: Long, val dark: Long, val levels: List<Level>) {
    val unitIds: List<String> get() = levels.flatMap { it.units }
}

data class Tip(val title: String, val body: String)

data class Curated(val lesson: Lesson, val level: String)

data class CourseUnit(
    val id: String,
    val title: String,
    val subtitle: String,
    val color: Long,
    val dark: Long,
    val lessons: List<Lesson>,
    /** True for the hiragana and katakana units, which sit in their own section of the Learn tab. */
    val alphabet: Boolean = false
)

data class Srs(val box: Int, val due: Long)

sealed interface Exercise {
    val itemId: String? get() = null
}

data class Teach(val item: Item) : Exercise

data class NoteCard(val note: Note, val examples: List<Sentence>) : Exercise

data class DialogueCard(val title: String, val lines: List<Line>) : Exercise

data class Choice(
    val instruction: String,
    val prompt: String,
    val sub: String = "",
    val romaji: String = "",
    val speak: String? = null,
    val after: String? = null,
    val listen: Boolean = false,
    val options: List<String>,
    val answer: Int,
    val explain: String,
    val bigOptions: Boolean = false,
    override val itemId: String? = null,
    /** A longer note on the word, shown when the learner taps the light bulb. */
    val hint: String = ""
) : Exercise

data class Match(val pairs: List<Pair<String, String>>) : Exercise

data class Build(
    val en: String,
    val tokens: List<String>,
    val pool: List<String>,
    val romaji: String
) : Exercise

data class TypeIn(
    val prompt: String,
    val answers: List<String>,
    val speak: String,
    override val itemId: String?
) : Exercise

data class Speak(val phrase: Phrase) : Exercise

/** Hear the item and write it from memory; the handwriting is checked. */
data class Dictate(val item: Item) : Exercise {
    override val itemId: String get() = item.id
}

data class Write(val item: Item) : Exercise {
    override val itemId: String get() = item.id
}
