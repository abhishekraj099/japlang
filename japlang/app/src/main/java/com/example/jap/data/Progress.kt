package com.example.jap.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.jap.audio.NORMAL_RATE
import java.util.Calendar
import java.util.TimeZone

/** What kind of session was finished; drives the daily quests and coin rewards. */
enum class SessionKind { LESSON, PRACTICE, CHALLENGE, OTHER }

/** One day's study: XP earned and how many graded answers were right. */
data class DayStat(val xp: Int = 0, val answered: Int = 0, val correct: Int = 0) {
    operator fun plus(other: DayStat) = DayStat(xp + other.xp, answered + other.answered, correct + other.correct)
}

/** The learner's account, settings and progress, kept in SharedPreferences and exposed as Compose state. */
class Progress(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private val settings = context.applicationContext.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)

    // ---- account and settings ----

    var signedIn by mutableStateOf(settings.getBoolean("signedIn", false))
        private set
    var email by mutableStateOf(settings.getString("email", "").orEmpty())
        private set
    var name by mutableStateOf(settings.getString("name", "").orEmpty())
        private set
    var onboarded by mutableStateOf(settings.getBoolean("onboarded", false))
        private set
    var streakIntroSeen by mutableStateOf(settings.getBoolean("streakIntroSeen", false))
        private set
    var goal by mutableIntStateOf(settings.getInt("goal", Config.defaultGoal))
        private set
    var reminder by mutableStateOf(settings.getBoolean(KEY_REMINDER, false))
        private set
    var favouriteTips by mutableStateOf(settings.getStringSet("favouriteTips", emptySet()).orEmpty().toSet())
        private set

    /**
     * Local stand-in for real authentication: any well-formed email is accepted and the
     * password is neither checked nor stored. Replace with a server call when accounts go online.
     */
    fun signIn(address: String) {
        val clean = address.trim()
        if (!clean.equals(email, ignoreCase = true) || name.isEmpty()) {
            name = clean.substringBefore('@').split('.', '_', '-', '+').firstOrNull { it.isNotBlank() }
                ?.filter { it.isLetter() }
                ?.replaceFirstChar { it.uppercase() }
                .orEmpty()
                .ifEmpty { "Learner" }
        }
        email = clean
        signedIn = true
        settings.edit().putString("email", email).putString("name", name).putBoolean("signedIn", true).apply()
    }

    fun signOut() {
        signedIn = false
        settings.edit().putBoolean("signedIn", false).apply()
    }

    fun updateName(value: String) {
        name = value.trim().ifEmpty { name }
        settings.edit().putString("name", name).apply()
    }

    /** The course and level picked on the Learn tab, or empty until the learner picks one. */
    var trackId by mutableStateOf(settings.getString("track", "").orEmpty())
        private set
    var levelIndex by mutableIntStateOf(settings.getInt("level", -1))
        private set

    fun selectLevel(track: String, level: Int) {
        trackId = track
        levelIndex = level
        settings.edit().putString("track", track).putInt("level", level).apply()
    }

    fun updateGoal(value: Int) {
        goal = value
        settings.edit().putInt("goal", value).apply()
    }

    fun updateReminder(on: Boolean) {
        reminder = on
        settings.edit().putBoolean(KEY_REMINDER, on).apply()
    }

    /** "system" to follow the phone, or "light" / "dark" to always use that theme. */
    var theme by mutableStateOf(settings.getString("theme", "system").orEmpty())
        private set

    fun updateTheme(mode: String) {
        theme = mode
        settings.edit().putString("theme", mode).apply()
    }

    /** Picked "I'm new to Japanese" at setup; the guide's tours are for these learners. */
    var beginner by mutableStateOf(settings.getBoolean("beginner", false))
        private set

    fun updateBeginner(value: Boolean) {
        beginner = value
        settings.edit().putBoolean("beginner", value).apply()
    }

    /** The guide shows its tours on first visits; off, they only play from the "?" button. */
    var guideOn by mutableStateOf(settings.getBoolean("guideOn", true))
        private set

    fun updateGuide(on: Boolean) {
        guideOn = on
        settings.edit().putBoolean("guideOn", on).apply()
    }

    /** Which guide the learner picked, by id from assets/guide.json. */
    var guideCharacter by mutableStateOf(settings.getString("guideCharacter", "").orEmpty())
        private set

    fun updateGuideCharacter(id: String) {
        guideCharacter = id
        Guide.select(id)
        settings.edit().putString("guideCharacter", id).apply()
    }

    /** The guide reads their lines aloud. */
    var guideVoice by mutableStateOf(settings.getBoolean("guideVoice", true))
        private set

    fun updateGuideVoice(on: Boolean) {
        guideVoice = on
        settings.edit().putBoolean("guideVoice", on).apply()
    }

    /** The Japanese text-to-speech voice for lessons, by engine id; empty for the phone's default. */
    var japaneseVoice by mutableStateOf(settings.getString("japaneseVoice", "").orEmpty())
        private set

    /** How fast the Japanese voice speaks, 1.0 being the engine's normal speed. */
    var speechRate by mutableStateOf(settings.getFloat("speechRate", NORMAL_RATE))
        private set

    /** The guide's English text-to-speech voice, by engine id; empty for the phone's default. */
    var guideVoiceName by mutableStateOf(settings.getString("guideVoiceName", "").orEmpty())
        private set

    fun updateJapaneseVoice(name: String) {
        japaneseVoice = name
        settings.edit().putString("japaneseVoice", name).apply()
    }

    fun updateSpeechRate(rate: Float) {
        speechRate = rate
        settings.edit().putFloat("speechRate", rate).apply()
    }

    fun updateGuideVoiceName(name: String) {
        guideVoiceName = name
        settings.edit().putString("guideVoiceName", name).apply()
    }

    /** The welcome demo has been shown, on this device, before the first sign-in. */
    var demoSeen by mutableStateOf(settings.getBoolean("demoSeen", false))
        private set

    fun finishDemo() {
        demoSeen = true
        settings.edit().putBoolean("demoSeen", true).apply()
    }

    /** Screens whose tour the learner has seen or skipped. */
    var toursSeen by mutableStateOf(settings.getStringSet("toursSeen", emptySet()).orEmpty().toSet())
        private set

    fun markTourSeen(screen: String) {
        toursSeen = toursSeen + screen
        settings.edit().putStringSet("toursSeen", toursSeen).apply()
    }

    /** Whether the weekly ranking (sample learners for now) is shown at all. */
    var rankingOn by mutableStateOf(settings.getBoolean("rankingOn", true))
        private set

    fun updateRanking(on: Boolean) {
        rankingOn = on
        settings.edit().putBoolean("rankingOn", on).apply()
    }

    fun finishOnboarding() {
        onboarded = true
        settings.edit().putBoolean("onboarded", true).apply()
    }

    /** The name printed on certificates. Set once, then fixed. */
    var certName by mutableStateOf(settings.getString("certName", "").orEmpty())
        private set

    fun lockCertName(value: String) {
        if (certName.isNotEmpty() || value.isBlank()) return
        certName = value.trim()
        settings.edit().putString("certName", certName).apply()
    }

    /** When the certificate [key] was first issued; recorded the first time it is opened. */
    fun certificateDate(key: String): Long {
        val stored = settings.getLong("cert-$key", 0L)
        if (stored > 0) return stored
        val now = System.currentTimeMillis()
        settings.edit().putLong("cert-$key", now).apply()
        return now
    }

    fun finishStreakIntro() {
        streakIntroSeen = true
        settings.edit().putBoolean("streakIntroSeen", true).apply()
    }

    // ---- how lessons are shown ----

    var showKanji by mutableStateOf(settings.getBoolean("showKanji", true))
        private set
    var showReading by mutableStateOf(settings.getBoolean("showReading", true))
        private set
    var showRomaji by mutableStateOf(settings.getBoolean("showRomaji", true))
        private set
    var showTitle by mutableStateOf(settings.getBoolean("showTitle", true))
        private set
    var lessonAudio by mutableStateOf(settings.getBoolean("lessonAudio", true))
        private set

    /** The chime on a correct answer. */
    var soundEffects by mutableStateOf(settings.getBoolean("soundEffects", true))
        private set

    fun updateSoundEffects(on: Boolean) {
        soundEffects = on
        settings.edit().putBoolean("soundEffects", on).apply()
    }

    /** 0 (smallest) to 5; 2 is the default size. */
    var fontStep by mutableIntStateOf(settings.getInt("fontStep", 2))
        private set

    fun saveDisplay(kanji: Boolean, reading: Boolean, romaji: Boolean, title: Boolean, audio: Boolean, font: Int) {
        showKanji = kanji; showReading = reading; showRomaji = romaji; showTitle = title; lessonAudio = audio; fontStep = font
        settings.edit()
            .putBoolean("showKanji", kanji).putBoolean("showReading", reading).putBoolean("showRomaji", romaji)
            .putBoolean("showTitle", title).putBoolean("lessonAudio", audio).putInt("fontStep", font)
            .apply()
    }

    /**
     * Problems learners report in a lesson. Kept on the device until there is a server to send them to;
     * each entry is "<lesson id>|<time>|<text>".
     */
    fun report(lessonId: String, text: String) {
        val list = settings.getStringSet("reports", emptySet()).orEmpty() + "$lessonId|${System.currentTimeMillis()}|${text.trim()}"
        settings.edit().putStringSet("reports", list).apply()
    }

    fun toggleTip(title: String) {
        favouriteTips = if (title in favouriteTips) favouriteTips - title else favouriteTips + title
        settings.edit().putStringSet("favouriteTips", favouriteTips).apply()
    }

    // ---- learning progress ----

    var done by mutableStateOf(prefs.getStringSet("done", emptySet()).orEmpty().toSet())
        private set
    var xp by mutableIntStateOf(prefs.getInt("xp", 0))
        private set
    var srs by mutableStateOf(readSrs("srs"))
        private set

    /** Spaced repetition for writing, kept apart from [srs]: an item can be known for reading but not yet for writing. */
    var writeSrs by mutableStateOf(readSrs("writeSrs"))
        private set
    var bestGame by mutableIntStateOf(prefs.getInt("bestGame", 0))
        private set
    var bestExam by mutableIntStateOf(prefs.getInt("bestExam", -1))
        private set

    /** Longest run of correct answers inside one session. */
    var bestRun by mutableIntStateOf(prefs.getInt("bestRun", 0))
        private set

    /** Koban, the coin earned by studying and spent on omamori. */
    var coins by mutableIntStateOf(prefs.getInt("coins", 0))
        private set

    /** Omamori held. Each one covers a missed day so the streak survives it. */
    var omamori by mutableIntStateOf(prefs.getInt("omamori", 0))
        private set

    /** Streak length the learner committed to, or 0; [goalClaimed] once its reward was paid. */
    var streakGoal by mutableIntStateOf(prefs.getInt("streakGoal", 0))
        private set
    var goalClaimed by mutableStateOf(prefs.getBoolean("goalClaimed", false))
        private set

    var studiedDays by mutableStateOf(readDays("studiedDays"))
        private set
    /** Days the learner watered the tree. */
    var wateredDays by mutableStateOf(readDays("wateredDays"))
        private set

    val tree: Tree.State get() = Tree.state(day, studiedDays, protectedDays, wateredDays)

    /** Waters the tree; allowed once on each day the learner studied. Returns whether it watered. */
    fun waterTree(): Boolean {
        if (!tree.canWater) return false
        wateredDays = wateredDays + day
        save()
        return true
    }
    var protectedDays by mutableStateOf(readDays("protectedDays"))
        private set
    private var dayStats by mutableStateOf(readStats())

    private var streakCount by mutableIntStateOf(prefs.getInt("streak", 0))
    private var lastDay by mutableLongStateOf(prefs.getLong("lastDay", 0L))
    private var dayXp by mutableIntStateOf(prefs.getInt("dayXp", 0))
    private var dayLessons by mutableIntStateOf(prefs.getInt("dayLessons", 0))
    private var dayPerfect by mutableIntStateOf(prefs.getInt("dayPerfect", 0))
    private var dayPractice by mutableIntStateOf(prefs.getInt("dayPractice", 0))
    private var challengeDay by mutableLongStateOf(prefs.getLong("challengeDay", 0L))
    private var lastWeek by mutableLongStateOf(prefs.getLong("lastWeek", 0L))
    private var weekTotal by mutableIntStateOf(prefs.getInt("weekXp", 0))

    /** Local calendar day as a count of days; changes at the learner's midnight. */
    val day: Long get() = dayOf(System.currentTimeMillis())

    // Day 0 of the epoch was a Thursday, so shifting by three makes weeks run Monday to Sunday.
    val week: Long get() = (day + 3) / 7

    /** 0 on Monday through 6 on Sunday. */
    val weekDay: Int get() = ((day + 3) % 7).toInt()

    /** Days in a row with study or an omamori; drops to zero after an unprotected missed day. */
    val streak get() = if (day - lastDay <= 1) streakCount else 0

    val studiedToday get() = day in studiedDays
    val todayXp get() = if (lastDay == day) dayXp else 0
    val lessonsToday get() = if (lastDay == day) dayLessons else 0
    val perfectToday get() = if (lastDay == day) dayPerfect else 0
    val practiceToday get() = if (lastDay == day) dayPractice else 0
    val challengeDoneToday get() = challengeDay == day
    val weekXp get() = if (lastWeek == week) weekTotal else 0

    fun stats(day: Long): DayStat = dayStats[day] ?: DayStat()

    /**
     * Run at launch. If days were missed since the last study, spends one omamori per missed day
     * to keep the streak alive; without enough of them the streak is simply lost.
     */
    fun checkStreak() {
        val today = day
        if (lastDay == 0L || lastDay >= today - 1 || streakCount == 0) return
        val missed = (lastDay + 1 until today).toList()
        if (omamori < missed.size) return
        omamori -= missed.size
        protectedDays = protectedDays + missed
        lastDay = today - 1
        save()
    }

    /** Records a finished session. [results] maps item id to whether it was answered without a mistake. */
    fun finish(
        lessonId: String?,
        gained: Int,
        results: Map<String, Memory.Grade>,
        kind: SessionKind = SessionKind.OTHER,
        perfect: Boolean = false,
        answered: Int = 0,
        correct: Int = 0,
        run: Int = 0,
        writeResults: Map<String, Memory.Grade> = emptyMap()
    ) {
        val now = System.currentTimeMillis()
        val today = day
        if (lastDay != today) {
            streakCount = if (lastDay == today - 1) streakCount + 1 else 1
            dayXp = 0
            dayLessons = 0
            dayPerfect = 0
            dayPractice = 0
            lastDay = today
        }
        if (lastWeek != week) {
            weekTotal = 0
            lastWeek = week
        }
        if (lessonId != null) done = done + lessonId
        xp += gained
        dayXp += gained
        weekTotal += gained
        coins += coinsFor(kind, perfect)
        when (kind) {
            SessionKind.LESSON -> dayLessons++
            SessionKind.PRACTICE -> dayPractice++
            SessionKind.CHALLENGE -> challengeDay = today
            SessionKind.OTHER -> Unit
        }
        if (perfect && kind != SessionKind.OTHER) dayPerfect++
        if (run > bestRun) bestRun = run
        studiedDays = studiedDays + today
        dayStats = (dayStats + (today to stats(today) + DayStat(gained, answered, correct)))
            .filterKeys { it > today - KEEP_DAYS }
        if (streakGoal > 0 && !goalClaimed && streakCount >= streakGoal) {
            coins += Config.streakGoals.firstOrNull { it.days == streakGoal }?.reward ?: 0
            goalClaimed = true
        }
        srs = srs + results.map { (id, grade) -> id to Memory.next(srs[id], grade, now) }
        writeSrs = writeSrs + writeResults.map { (id, grade) -> id to Memory.next(writeSrs[id], grade, now) }
        save()
    }

    fun commitStreakGoal(days: Int) {
        streakGoal = days
        goalClaimed = false
        save()
    }

    fun canBuyOmamori() = coins >= Config.protectionCost && omamori < Config.maxProtection

    fun buyOmamori() {
        if (!canBuyOmamori()) return
        coins -= Config.protectionCost
        omamori++
        save()
    }

    /** Marks every lesson in the first [units] units as finished, after a placement test. */
    fun placeAt(units: Int) {
        done = done + Course.units.take(units).flatMap { it.lessons }.map { it.id }
        save()
    }

    /** Marks every lesson of [unit] as finished, after a passed test-out. */
    fun completeUnit(unit: CourseUnit) {
        done = done + unit.lessons.map { it.id }
        save()
    }

    fun recordGame(score: Int) {
        if (score > bestGame) bestGame = score
        save()
    }

    /** [percent] is the share of correct answers, 0 to 100. */
    fun recordExam(percent: Int) {
        if (percent > bestExam) bestExam = percent
        save()
    }

    // ---- videos ----

    /** Video id to when it was last opened, newest first in [history]. */
    var watched by mutableStateOf(readTimes("watched"))
        private set
    /** Best speaking score, 0 to 100, keyed "<video id>#<line index>". */
    var lineScores by mutableStateOf(readScores())
        private set

    val history: List<String> get() = watched.entries.sortedByDescending { it.value }.map { it.key }

    fun watch(videoId: String) {
        watched = watched + (videoId to System.currentTimeMillis())
        save()
    }

    fun bestScore(videoId: String, line: Int): Int? = lineScores["$videoId#$line"]

    /**
     * Keeps the best score for a line. Returns true the first time every line in [lines] has a score,
     * which is when the video's speaking practice counts as a finished session.
     */
    fun scoreLine(videoId: String, line: Int, score: Int, lines: List<Int>): Boolean {
        val before = lines.all { bestScore(videoId, it) != null }
        val key = "$videoId#$line"
        if (score > (lineScores[key] ?: -1)) lineScores = lineScores + (key to score)
        save()
        return !before && lines.all { bestScore(videoId, it) != null }
    }

    fun clearHistory() {
        watched = emptyMap()
        save()
    }

    fun due(now: Long): List<Item> = srs.filter { it.value.due <= now }.keys.mapNotNull(Course::item).filter(Course::quizzable)

    /** Due items with their schedule, for planning today's review. */
    fun dueSchedule(writing: Boolean, now: Long): List<Pair<Item, Srs>> =
        (if (writing) writeSrs else srs).filter { it.value.due <= now }
            .mapNotNull { (id, s) -> Course.item(id)?.takeIf(Course::quizzable)?.let { it to s } }

    /** Days between the last study day before today and today; 0 for a new learner. */
    val daysAway: Int get() = studiedDays.filter { it < day }.maxOrNull()?.let { (day - it).toInt() } ?: 0

    private var reviewedDay by mutableLongStateOf(prefs.getLong("reviewedDay", 0L))
    private var reviewedReading by mutableIntStateOf(prefs.getInt("reviewedReading", 0))
    private var reviewedWriting by mutableIntStateOf(prefs.getInt("reviewedWriting", 0))

    /** Items reviewed today in one skill, counted against the daily cap. */
    fun reviewedToday(writing: Boolean): Int = if (reviewedDay != day) 0 else if (writing) reviewedWriting else reviewedReading

    fun addReviewed(writing: Boolean, count: Int) {
        if (reviewedDay != day) {
            reviewedDay = day
            reviewedReading = 0
            reviewedWriting = 0
        }
        if (writing) reviewedWriting += count else reviewedReading += count
        prefs.edit().putLong("reviewedDay", reviewedDay).putInt("reviewedReading", reviewedReading).putInt("reviewedWriting", reviewedWriting).apply()
    }

    /** Items whose writing review is due. */
    fun writeDue(now: Long): List<Item> = writeSrs.filter { it.value.due <= now }.keys.mapNotNull(Course::item).filter(Course::quizzable)

    /** Items learned for reading that have never been written, so writing practice can bring them in. */
    fun writeNew(): List<Item> = srs.keys.filter { it !in writeSrs }.mapNotNull(Course::item).filter(Course::quizzable)

    /** Written correctly at least once; until then the item is traced over a guide. */
    fun knowsWriting(id: String): Boolean = (writeSrs[id]?.hits ?: 0) >= 1

    /** How far along an achievement is, from 0 to 1. */
    fun progressOf(rule: BadgeRule): Float {
        if (rule.metric == BadgeMetric.UNITS) {
            val lessons = Course.units.filter { it.id in rule.units }.flatMap { it.lessons }
            return if (lessons.isEmpty()) 0f else lessons.count { it.id in done } / lessons.size.toFloat()
        }
        val value = when (rule.metric) {
            BadgeMetric.XP -> xp
            BadgeMetric.STREAK -> streak
            BadgeMetric.LEARNED -> srs.size
            BadgeMetric.EXAM -> bestExam
            BadgeMetric.RUN -> bestRun
            BadgeMetric.PROTECTED -> protectedDays.size
            BadgeMetric.COINS -> coins
            BadgeMetric.UNITS -> 0
        }
        return if (rule.target <= 0) 1f else (value / rule.target.toFloat()).coerceIn(0f, 1f)
    }

    fun earned(rule: BadgeRule) = progressOf(rule) >= 1f

    /** Erases learning progress. The account and settings stay. */
    /**
     * Erases everything the app keeps: account, settings, progress, usage log and crash log. The caller restarts
     * the app afterwards, so it opens as if newly installed.
     */
    fun deleteEverything() {
        settings.edit().clear().commit()
        prefs.edit().clear().commit()
        Analytics.clear()
        CrashLog.clear()
    }

    fun reset() {
        done = emptySet()
        xp = 0
        srs = emptyMap()
        writeSrs = emptyMap()
        reviewedDay = 0L
        reviewedReading = 0
        reviewedWriting = 0
        streakCount = 0
        lastDay = 0L
        dayXp = 0
        dayLessons = 0
        dayPerfect = 0
        dayPractice = 0
        challengeDay = 0L
        lastWeek = 0L
        weekTotal = 0
        bestGame = 0
        bestExam = -1
        bestRun = 0
        coins = 0
        omamori = 0
        streakGoal = 0
        goalClaimed = false
        studiedDays = emptySet()
        protectedDays = emptySet()
        wateredDays = emptySet()
        dayStats = emptyMap()
        watched = emptyMap()
        lineScores = emptyMap()
        prefs.edit().clear().apply()
    }

    private fun save() {
        prefs.edit()
            .putStringSet("done", done)
            .putInt("xp", xp)
            .putInt("streak", streakCount)
            .putLong("lastDay", lastDay)
            .putInt("dayXp", dayXp)
            .putInt("dayLessons", dayLessons)
            .putInt("dayPerfect", dayPerfect)
            .putInt("dayPractice", dayPractice)
            .putLong("challengeDay", challengeDay)
            .putLong("lastWeek", lastWeek)
            .putInt("weekXp", weekTotal)
            .putInt("bestGame", bestGame)
            .putInt("bestExam", bestExam)
            .putInt("bestRun", bestRun)
            .putInt("coins", coins)
            .putInt("omamori", omamori)
            .putInt("streakGoal", streakGoal)
            .putBoolean("goalClaimed", goalClaimed)
            .putStringSet("studiedDays", studiedDays.map { it.toString() }.toSet())
            .putStringSet("protectedDays", protectedDays.map { it.toString() }.toSet())
            .putStringSet("wateredDays", wateredDays.map { it.toString() }.toSet())
            .putString("dayStats", dayStats.entries.joinToString("\n") { (d, s) -> "$d|${s.xp}|${s.answered}|${s.correct}" })
            .putString("srs", writeMemory(srs))
            .putString("writeSrs", writeMemory(writeSrs))
            .putString("watched", watched.entries.joinToString("\n") { "${it.key}|${it.value}" })
            .putString("lineScores", lineScores.entries.joinToString("\n") { "${it.key}|${it.value}" })
            .apply()
    }

    private fun readDays(key: String): Set<Long> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    private fun readStats(): Map<Long, DayStat> =
        prefs.getString("dayStats", "").orEmpty().lines().mapNotNull { line ->
            val parts = line.split("|").map { it.toLongOrNull() }
            if (parts.size < 4 || parts.any { it == null }) null
            else parts[0]!! to DayStat(parts[1]!!.toInt(), parts[2]!!.toInt(), parts[3]!!.toInt())
        }.toMap()

    private fun readTimes(key: String): Map<String, Long> =
        prefs.getString(key, "").orEmpty().lines().mapNotNull { line ->
            val at = line.substringAfterLast("|").toLongOrNull()
            if (at == null) null else line.substringBeforeLast("|") to at
        }.toMap()

    private fun readScores(): Map<String, Int> =
        prefs.getString("lineScores", "").orEmpty().lines().mapNotNull { line ->
            val score = line.substringAfterLast("|").toIntOrNull()
            if (score == null) null else line.substringBeforeLast("|") to score
        }.toMap()

    /** One line per item: id|level|due|stability|difficulty|last|hits|misses. */
    private fun writeMemory(map: Map<String, Srs>) = map.entries.joinToString("\n") { (id, m) ->
        "$id|${m.box}|${m.due}|${m.stability}|${m.difficulty}|${m.last}|${m.hits}|${m.misses}"
    }

    /** Reads saved memories; rows from the old box schedule (id|box|due) are converted to the memory model. */
    private fun readSrs(key: String): Map<String, Srs> =
        prefs.getString(key, "").orEmpty().lines().mapNotNull { line ->
            val parts = line.split("|")
            val box = parts.getOrNull(1)?.toIntOrNull()
            val due = parts.getOrNull(2)?.toLongOrNull()
            val stability = parts.getOrNull(3)?.toFloatOrNull()
            when {
                box == null || due == null -> null
                stability == null -> parts[0] to Memory.fromBox(box, due)
                else -> parts[0] to Srs(
                    box, due, stability, parts.getOrNull(4)?.toFloatOrNull() ?: 5f, parts.getOrNull(5)?.toLongOrNull() ?: 0L,
                    parts.getOrNull(6)?.toIntOrNull() ?: 0, parts.getOrNull(7)?.toIntOrNull() ?: 0
                )
            }
        }.toMap()

    companion object {
        const val SETTINGS = "settings"
        const val KEY_REMINDER = "reminder"
        private const val DAY = 86_400_000L
        private const val KEEP_DAYS = 400

        fun dayOf(millis: Long): Long = (millis + TimeZone.getDefault().getOffset(millis)) / DAY

        fun dayOf(calendar: Calendar): Long = dayOf(calendar.timeInMillis)

        /** Koban earned for finishing a session of this kind. */
        fun coinsFor(kind: SessionKind, perfect: Boolean): Int {
            val base = when (kind) {
                SessionKind.LESSON -> Config.lessonCoins
                SessionKind.PRACTICE -> Config.practiceCoins
                SessionKind.CHALLENGE -> Config.challengeCoins
                SessionKind.OTHER -> 0
            }
            return base + if (perfect && kind != SessionKind.OTHER) Config.perfectCoins else 0
        }
    }
}
