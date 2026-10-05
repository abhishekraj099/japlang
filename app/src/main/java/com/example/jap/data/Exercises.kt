package com.example.jap.data

import kotlin.random.Random

object Exercises {

    fun normalise(s: String) = s.lowercase().filter { it.isLetter() }

    fun forLesson(lesson: Lesson): List<Exercise> = when (lesson.type) {
        LessonType.KANA -> kana(lesson.items)
        LessonType.VOCAB -> vocab(lesson)
        LessonType.KANJI -> kanji(lesson.items)
        LessonType.GRAMMAR -> grammar(lesson)
        LessonType.DIALOGUE -> dialogue(lesson)
        LessonType.CHECKPOINT -> checkpoint(lesson)
        LessonType.THEORY -> theory(lesson)
        LessonType.PRACTICE -> practice(lesson)
        LessonType.SPEAKING -> lesson.phrases.map { Speak(it) }
        LessonType.WRITING -> writing(lesson)
    }

    fun forReview(items: List<Item>): List<Exercise> {
        val out = items.shuffled().map<Item, Exercise> {
            when (Random.nextInt(4)) {
                0 -> recognise(it, items)
                1 -> recall(it, items)
                2 -> listen(it, items)
                else -> typeIn(it)
            }
        }.toMutableList()
        if (items.size >= 4) out.add(out.size / 2, match(items))
        out += dictations(items, 2)
        return out
    }

    fun forSpeaking(phrases: List<Phrase>): List<Exercise> = phrases.shuffled().take(8).map { Speak(it) }

    fun forDictation(items: List<Item>): List<Exercise> = dictations(items, 8)

    /** A short lesson on one character: meet it, trace it, then recognise, hear and write it. */
    fun forCharacter(item: Item, neighbours: List<Item>): List<Exercise> {
        val out = mutableListOf<Exercise>(Teach(item), Write(item), recognise(item, neighbours), listen(item, neighbours))
        if (item.romaji != "wo") out += Dictate(item)
        out += recall(item, neighbours)
        return out
    }

    /** A lesson put together on request from every lesson matching a topic. */
    fun forTopic(lessons: List<Lesson>): List<Exercise> {
        val items = lessons.flatMap { it.items }.distinctBy { it.id }.shuffled().take(10)
        val out = forReview(items).toMutableList()
        lessons.flatMap { it.sentences }.shuffled().take(3).forEach { out += build(it) }
        lessons.flatMap { it.phrases }.shuffled().take(2).forEach { out += Speak(it) }
        return out
    }

    /** Meaning or sound shown, learner picks the Japanese. */
    fun reverse(item: Item, pool: List<Item>): Choice = recall(item, pool)

    fun blank(sentence: Sentence): Choice = fillBlank(sentence)

    /** One quick multiple-choice question for [item], used by the speed game. */
    fun quick(item: Item, pool: List<Item>): Choice = recognise(item, pool)

    private fun kana(items: List<Item>): List<Exercise> {
        val out = mutableListOf<Exercise>()
        items.chunked(3).forEach { group ->
            group.forEach { out += Teach(it) }
            group.shuffled().forEach { out += recognise(it, items) }
        }
        out += match(items)
        items.shuffled().take(6).forEach { out += if (Random.nextBoolean()) recall(it, items) else listen(it, items) }
        items.shuffled().take(2).forEach { out += Write(it) }
        dictations(items, 2).forEach { out += it }
        items.shuffled().take(2).forEach { out += typeIn(it) }
        return out
    }

    private fun vocab(lesson: Lesson): List<Exercise> {
        val items = lesson.items
        val out = mutableListOf<Exercise>()
        items.chunked(4).forEach { group ->
            group.forEach { out += Teach(it) }
            group.shuffled().forEach { out += recognise(it, items) }
        }
        out += match(items)
        items.shuffled().take(6).forEach { out += if (Random.nextBoolean()) recall(it, items) else listen(it, items) }
        items.shuffled().take(2).forEach { out += typeIn(it) }
        lesson.sentences.forEach { out += build(it) }
        return out
    }

    private fun kanji(items: List<Item>): List<Exercise> {
        val out = mutableListOf<Exercise>()
        items.chunked(3).forEach { group ->
            group.forEach { out += Teach(it) }
            group.shuffled().forEach { out += recognise(it, items) }
        }
        out += match(items)
        items.shuffled().forEach { out += reading(it, items) }
        items.shuffled().take(3).forEach { out += Write(it) }
        dictations(items, 2).forEach { out += it }
        items.shuffled().take(3).forEach { out += recall(it, items) }
        return out
    }

    private fun grammar(lesson: Lesson): List<Exercise> {
        val out = mutableListOf<Exercise>()
        lesson.items.forEach { out += Teach(it) }
        lesson.items.forEach { out += recognise(it, lesson.items) }
        lesson.note?.let { out += NoteCard(it, lesson.sentences.take(2)) }
        lesson.sentences.filter { it.blank >= 0 }.forEach { out += fillBlank(it) }
        lesson.sentences.shuffled().take(2).forEach { s -> out += translate(s.tokens.joinToString(" "), s.romaji, s.en, lesson.sentences.map { it.en }) }
        lesson.sentences.shuffled().forEach { out += build(it) }
        return out
    }

    private fun dialogue(lesson: Lesson): List<Exercise> {
        val out = mutableListOf<Exercise>()
        out += DialogueCard(lesson.title, lesson.dialogue)
        val meanings = lesson.dialogue.map { it.en }
        lesson.dialogue.shuffled().take(4).forEach { out += translate(it.jp, it.romaji, it.en, meanings) }
        lesson.sentences.forEach { out += build(it) }
        lesson.phrases.forEach { out += Speak(it) }
        return out
    }

    /** The explanations, then a few quick checks on their examples. */
    private fun theory(lesson: Lesson): List<Exercise> {
        val out = lesson.theory.map<TheoryNote, Exercise> { NoteCard(it.note, it.examples) }.toMutableList()
        val examples = lesson.theory.flatMap { it.examples }
        val meanings = examples.map { it.en }.distinct()
        if (meanings.size >= 3) {
            examples.distinctBy { it.en }.shuffled().take(3).forEach { s ->
                out += translate(s.tokens.joinToString(" "), s.romaji, s.en, meanings)
            }
        }
        return out
    }

    /** New words, then the grammar point, then sentences that use both. */
    private fun practice(lesson: Lesson): List<Exercise> {
        val items = lesson.items
        val out = mutableListOf<Exercise>()
        items.chunked(3).forEach { group ->
            group.forEach { out += Teach(it) }
            group.shuffled().forEach { out += recognise(it, items) }
        }
        if (items.size >= 3) out += match(items)
        items.shuffled().take(4).forEach { out += if (Random.nextBoolean()) recall(it, items) else listen(it, items) }
        lesson.note?.let { out += NoteCard(it, lesson.sentences.take(2)) }
        lesson.sentences.filter { it.blank >= 0 }.forEach { out += fillBlank(it) }
        lesson.sentences.shuffled().forEach { out += build(it) }
        items.shuffled().take(1).forEach { out += typeIn(it) }
        return out
    }

    /** Characters are traced and written from dictation; words are typed; sentences are built. */
    private fun writing(lesson: Lesson): List<Exercise> {
        val out = mutableListOf<Exercise>()
        // Single characters and two-character words fit the drawing pad; longer words are typed.
        val characters = lesson.items.filter { it.kind != Kind.WORD || it.jp.length <= 2 }
        characters.forEach { out += Write(it) }
        out += dictations(characters, characters.size)
        lesson.items.filter { it.kind == Kind.WORD && it.jp.length > 2 }.shuffled().take(5).forEach { out += typeIn(it) }
        lesson.sentences.shuffled().take(3).forEach { out += build(it) }
        return out
    }

    private fun checkpoint(lesson: Lesson): List<Exercise> {
        val picked = lesson.items.shuffled().take(10)
        val out = mutableListOf<Exercise>()
        picked.forEachIndexed { i, item ->
            out += when (i % 3) {
                0 -> recognise(item, lesson.items)
                1 -> recall(item, lesson.items)
                else -> listen(item, lesson.items)
            }
        }
        out.add(out.size / 2, match(lesson.items))
        lesson.sentences.shuffled().take(3).forEach { out += build(it) }
        picked.take(2).forEach { out += typeIn(it) }
        return out
    }

    // ---- single exercises ----

    /** Up to three items of the same kind whose [key] differs from [item]'s and from each other. */
    private fun others(item: Item, local: List<Item>, key: (Item) -> String): List<Item> {
        val seen = mutableSetOf(key(item))
        val sameKind = Course.allItems.filter { it.kind == item.kind }
        return (local.filter { it.kind == item.kind }.shuffled() + sameKind.shuffled())
            .filter { seen.add(key(it)) }
            .take(3)
    }

    private fun explain(item: Item) =
        if (item.isKana) "${item.jp} = ${item.romaji}" else "${item.jp} (${item.romaji}) = ${item.meaning}"

    /** Japanese shown, learner picks the sound or meaning. */
    private fun recognise(item: Item, local: List<Item>): Choice {
        val options = (others(item, local) { it.label } + item).shuffled()
        return Choice(
            instruction = if (item.isKana) "Which sound is this?" else "What does this mean?",
            prompt = item.jp,
            sub = item.reading,
            romaji = if (item.isKana) "" else item.romaji,
            // Hearing a kana before answering would give the sound away.
            speak = if (item.isKana) null else item.speak,
            after = item.speak,
            options = options.map { it.label },
            answer = options.indexOf(item),
            explain = explain(item),
            itemId = item.id,
            hint = item.note
        )
    }

    /** Sound or meaning shown, learner picks the Japanese. */
    private fun recall(item: Item, local: List<Item>): Choice {
        val options = (others(item, local) { it.jp } + item).shuffled()
        return Choice(
            instruction = if (item.isKana) "Pick the right character" else "How do you say this?",
            prompt = if (item.isKana) item.romaji else "${item.emoji} ${item.meaning}".trim(),
            after = item.speak,
            options = options.map { it.jp },
            answer = options.indexOf(item),
            explain = explain(item),
            bigOptions = item.kind != Kind.WORD,
            itemId = item.id,
            hint = item.note
        )
    }

    private fun listen(item: Item, local: List<Item>): Choice {
        // を and お sound the same, so neither can be told apart by ear.
        val sound = { it: Item -> if (it.romaji == "wo") "o" else it.romaji }
        if (item.romaji == "wo") return recognise(item, local)
        val options = (others(item, local, sound) + item).shuffled()
        val byMeaning = item.kind == Kind.WORD
        return Choice(
            instruction = "What do you hear?",
            prompt = "",
            speak = item.speak,
            listen = true,
            options = options.map { if (byMeaning) it.label else it.jp },
            answer = options.indexOf(item),
            explain = explain(item),
            bigOptions = !byMeaning,
            itemId = item.id,
            hint = item.note
        )
    }

    private fun reading(item: Item, local: List<Item>): Choice {
        val options = (others(item, local) { it.reading } + item).shuffled()
        return Choice(
            instruction = "How is this read?",
            prompt = item.jp,
            after = item.speak,
            options = options.map { "${it.reading}  ·  ${it.romaji}" },
            answer = options.indexOf(item),
            explain = explain(item),
            itemId = item.id,
            hint = item.note
        )
    }

    /**
     * Listen-and-write tasks for characters only: a whole word does not fit the pad,
     * and を cannot be told from お by ear.
     */
    private fun dictations(items: List<Item>, count: Int): List<Dictate> =
        items.filter { it.kind != Kind.WORD && it.romaji != "wo" }.shuffled().take(count).map { Dictate(it) }

    private fun match(items: List<Item>) =
        Match(items.shuffled().distinctBy { it.label }.distinctBy { it.jp }.take(5).map { it.jp to it.label })

    private fun typeIn(item: Item): TypeIn {
        val base = normalise(item.romaji)
        val short = base.replace("ou", "o").replace("oo", "o").replace("uu", "u").replace("ii", "i")
        val kunrei = base.replace("shi", "si").replace("chi", "ti").replace("tsu", "tu").replace("fu", "hu")
        val answers = listOf(base, short, kunrei, normalise(item.jp), normalise(item.reading))
            .filter { it.isNotEmpty() }
            .distinct()
        return TypeIn(prompt = item.jp, answers = answers, speak = item.speak, itemId = item.id)
    }

    private fun fillBlank(s: Sentence): Choice {
        val answer = s.tokens[s.blank]
        val options = (s.distract + answer).distinct().shuffled()
        return Choice(
            instruction = "Fill in the blank",
            prompt = s.tokens.mapIndexed { i, t -> if (i == s.blank) "＿＿" else t }.joinToString(" "),
            sub = s.en,
            after = s.text,
            options = options,
            answer = options.indexOf(answer),
            explain = "${s.tokens.joinToString(" ")}\n${s.romaji}"
        )
    }

    private fun translate(jp: String, romaji: String, en: String, meanings: List<String>): Choice {
        val options = (meanings.filter { it != en }.distinct().shuffled().take(3) + en).shuffled()
        return Choice(
            instruction = "What does this mean?",
            prompt = jp,
            romaji = romaji,
            speak = jp.replace(" ", ""),
            options = options,
            answer = options.indexOf(en),
            explain = "$jp\n$en"
        )
    }

    private fun build(s: Sentence) =
        Build(en = s.en, tokens = s.tokens, pool = (s.tokens + s.distract).shuffled(), romaji = s.romaji)
}
