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
        placementMaxQuestions = placement.getInt("maxQuestions")
        placementLevels = placement.getJSONArray("levels").objects().map {
            PlacementLevel(it.getString("name"), it.getJSONArray("units").strings())
        }

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

        val league = json.getJSONObject("league")
        leagueMinXp = league.getInt("minWeeklyXp")
        leagueMaxXp = league.getInt("maxWeeklyXp")
        leagueSample = league.getJSONArray("sample").strings()
    }
}
