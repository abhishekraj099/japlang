package com.example.jap.data

import org.json.JSONArray
import org.json.JSONObject

class Goal(val xp: Int, val name: String, val time: String)

enum class QuestMetric { XP, LESSONS, PERFECT, PRACTICE, CHALLENGE }

/** [target] is null when the quest follows the learner's own daily goal. */
class QuestRule(val title: String, val metric: QuestMetric, val target: Int?)

enum class BadgeMetric { XP, STREAK, UNITS, LEARNED, EXAM, RUN, PROTECTED, COINS }

class BadgeRule(val title: String, val how: String, val metric: BadgeMetric, val target: Int, val units: List<String>)

class PlacementLevel(val name: String, val units: List<String>)

class StreakGoal(val days: Int, val reward: Int)

/** A title earned by total XP, shown on the rank ladder. */
class Rank(val name: String, val japanese: String, val xp: Int)

fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

/** App rules and tuning values, read from assets/config.json so they can change without touching code. */
object Config {

    var goals: List<Goal> = emptyList()
        private set
    var defaultGoal = 0
        private set

    var lessonXp = 0
        private set
    var practiceXp = 0
        private set
    var perfectBonus = 0
        private set
    var challengeXp = 0
        private set
    var examPassXp = 0
        private set
    var gameMaxXp = 0
        private set

    var challengeSize = 0
        private set
    var challengeMinLearned = 0
        private set
    var gameSeconds = 0
        private set
    var gameMinPool = 0
        private set
    var reminderHour = 0
        private set

    /** Wait in milliseconds before an item in review box n comes up again. */
    var srsIntervals: List<Long> = emptyList()
        private set

    var questsPerDay = 0
        private set
    var quests: List<QuestRule> = emptyList()
        private set
    var badges: List<BadgeRule> = emptyList()
        private set

    var placementStepUp = 0
        private set
    var placementMaxMisses = 0
        private set
    var placementMaxQuestions = 0
        private set
    var placementLevels: List<PlacementLevel> = emptyList()
        private set

    var lessonCoins = 0
        private set
    var practiceCoins = 0
        private set
    var perfectCoins = 0
        private set
    var challengeCoins = 0
        private set

    /** Koban one omamori costs, and how many a learner may hold at once. */
    var protectionCost = 0
        private set
    var maxProtection = 0
        private set
    var streakGoals: List<StreakGoal> = emptyList()
        private set
    var ranks: List<Rank> = emptyList()
        private set

    fun rank(xp: Int): Rank = ranks.lastOrNull { xp >= it.xp } ?: ranks.first()

    /** Share of answers, in percent, needed to skip a unit with a test-out. */
    var testOutPercent = 0
        private set

    var leagueMinXp = 0
        private set
    var leagueMaxXp = 0
        private set
    var leagueSample: List<String> = emptyList()
        private set

    fun goal(xp: Int): Goal? = goals.firstOrNull { it.xp == xp }

    /** Shown in the privacy policy and terms. An empty contact email shows a reminder to fill it in. */
    var legalDate = ""
        private set
    var contactEmail = ""
        private set
    var privacyUrl = ""
        private set

    /** Target chance of recall when an item comes back for review. */
    var retention = 0.9f
        private set
    /** Answers quicker than [fastMillis] grade Easy; slower than [slowMillis] grade Hard. */
    var fastMillis = 0L
        private set
    var slowMillis = 0L
        private set
    /** Answers in a row where the combo multiplier steps up to ×2, ×3, ×4…, and the most bonus XP a session can earn. */
    var comboSteps: List<Int> = emptyList()
        private set
    var comboMaxBonus = 0
        private set

    /** Waterings needed to reach each tree stage, sprout first. */
    var treeStages: List<Int> = emptyList()
        private set
    /** Missed days after which the tree has no leaves, and after which it withers. */
    var treeBareAfter = 0
        private set
    var treeWitherAfter = 0
        private set
    /** After a dry spell, the share of leaves on the first day of care, and the days until the crown is full. */
    var treeRegrowStart = 0f
        private set
    var treeRegrowDays = 0
        private set

    /** Most reviews offered per skill per day, and the days away that make a backlog a "welcome back". */
    var reviewDailyCap = 0
        private set
    var reviewBreakDays = 0
        private set

    /** A written box is right when the expected character is among the recogniser's first this-many guesses. */
    var writingTop = 0
        private set
    var writingSessionSize = 0
        private set

    fun load(json: JSONObject) {
        goals = json.getJSONArray("goals").objects().map { Goal(it.getInt("xp"), it.getString("name"), it.getString("time")) }
        defaultGoal = json.getInt("defaultGoal")

        val rewards = json.getJSONObject("rewards")
        lessonXp = rewards.getInt("lesson")
        practiceXp = rewards.getInt("practice")
        perfectBonus = rewards.getInt("perfectBonus")
        challengeXp = rewards.getInt("challenge")
        examPassXp = rewards.getInt("examPass")
        gameMaxXp = rewards.getInt("gameMax")

        val challenge = json.getJSONObject("challenge")
        challengeSize = challenge.getInt("size")
        challengeMinLearned = challenge.getInt("minLearned")
        val game = json.getJSONObject("game")
        gameSeconds = game.getInt("seconds")
        gameMinPool = game.getInt("minPool")
        reminderHour = json.getInt("reminderHour")

        val hours = json.getJSONArray("srsHours")
        srsIntervals = List(hours.length()) { hours.getLong(it) * 3_600_000L }

        val questJson = json.getJSONObject("quests")
        questsPerDay = questJson.getInt("perDay")
        quests = questJson.getJSONArray("pool").objects().map {
            // A number is a fixed target; the word GOAL means the learner's daily goal.
            QuestRule(it.getString("title"), QuestMetric.valueOf(it.getString("metric")), it.opt("target") as? Int)
        }
        badges = json.getJSONArray("badges").objects().map {
            BadgeRule(
                it.getString("title"), it.getString("how"), BadgeMetric.valueOf(it.getString("metric")),
                it.optInt("target"), it.optJSONArray("units")?.strings().orEmpty()
            )
        }

        val placement = json.getJSONObject("placement")
        placementStepUp = placement.getInt("stepUp")
        placementMaxMisses = placement.getInt("maxMisses")
        placementLevels = placement.getJSONArray("levels").objects().map {
            PlacementLevel(it.getString("name"), it.getJSONArray("units").strings())
        }
        // Enough questions to clear every level while still allowing one fewer miss than ends the test at each.
        placementMaxQuestions = placement.optInt("maxQuestions").takeIf { it > 0 }
            ?: (placementLevels.size * (placementStepUp + placementMaxMisses - 1))

        val coins = json.getJSONObject("coins")
        lessonCoins = coins.getInt("lesson")
        practiceCoins = coins.getInt("practice")
        perfectCoins = coins.getInt("perfect")
        challengeCoins = coins.getInt("challenge")

        val streak = json.getJSONObject("streak")
        protectionCost = streak.getInt("protectionCost")
        maxProtection = streak.getInt("maxProtection")
        streakGoals = streak.getJSONArray("goals").objects().map { StreakGoal(it.getInt("days"), it.getInt("reward")) }
        ranks = json.getJSONArray("ranks").objects().map { Rank(it.getString("name"), it.getString("japanese"), it.getInt("xp")) }

        testOutPercent = json.getJSONObject("testOut").getInt("passPercent")

        val legal = json.getJSONObject("legal")
        legalDate = legal.getString("effectiveDate")
        contactEmail = legal.optString("contactEmail")
        privacyUrl = legal.optString("privacyUrl")

        val memory = json.getJSONObject("memory")
        retention = memory.getDouble("retention").toFloat()
        fastMillis = memory.getLong("fastMillis")
        slowMillis = memory.getLong("slowMillis")
        val combo = json.getJSONObject("combo")
        comboSteps = combo.getJSONArray("steps").let { a -> List(a.length()) { a.getInt(it) } }
        comboMaxBonus = combo.getInt("maxBonus")

        val tree = json.getJSONObject("tree")
        treeStages = tree.getJSONArray("stages").let { a -> List(a.length()) { a.getInt(it) } }
        treeBareAfter = tree.getInt("bareAfterMissedDays")
        treeWitherAfter = tree.getInt("witherAfterMissedDays")
        treeRegrowStart = tree.getDouble("regrowStart").toFloat()
        treeRegrowDays = tree.getInt("regrowDays")

        val review = json.getJSONObject("review")
        reviewDailyCap = review.getInt("dailyCap")
        reviewBreakDays = review.getInt("breakDays")

        val writing = json.getJSONObject("writing")
        writingTop = writing.getInt("topCandidates")
        writingSessionSize = writing.getInt("sessionSize")

        val league = json.getJSONObject("league")
        leagueMinXp = league.getInt("minWeeklyXp")
        leagueMaxXp = league.getInt("maxWeeklyXp")
        leagueSample = league.getJSONArray("sample").strings()
    }
}
