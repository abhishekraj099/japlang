package com.example.jap.data

import org.json.JSONObject

/** A run of text with the furigana shown above it; [reading] is empty for kana. */
data class Ruby(val text: String, val reading: String)

/** One subtitle line. [words] are display groups that never wrap mid-word; [kana] is what scoring compares against. */
data class VideoLine(
    val index: Int,
    val start: Double,
    val duration: Double,
    val words: List<List<Ruby>>,
    val kana: String,
    val romaji: String,
    val en: String
) {
    val text: String get() = words.joinToString("") { w -> w.joinToString("") { it.text } }
    val end: Double get() = start + duration
}

data class VideoTopic(val id: String, val name: String, val icon: String)

/** A home-screen row: videos of one [level], one [topic], or both; [short] rows hold vertical Shorts. */
data class VideoSection(val title: String, val level: String?, val topic: String?, val short: Boolean = false, val jlpt: String? = null)

/** A short clip of a YouTube video, played between [start] and [end] seconds. */
data class Video(
    val id: String,
    val youtube: String,
    val title: String,
    val channel: String,
    val source: String,
    val topic: String,
    val level: String,
    val start: Double,
    val end: Double,
    /** A vertical YouTube Short rather than a landscape video. */
    val short: Boolean,
    /** JLPT level the language suits, N5 (easiest) to N1. */
    val jlpt: String,
    val lines: List<VideoLine>
) {
    val seconds: Int get() = (end - start).toInt()
    val thumbnail: String get() = "https://i.ytimg.com/vi/$youtube/hqdefault.jpg"

    /** Lines worth saying aloud: no repeats and more than a grunt. */
    val speakable: List<VideoLine>
        get() = lines.filter { it.kana.length >= 3 }.distinctBy { it.kana }
}

/**
 * Where videos come from. Today this is assets/videos.json; a server-backed source only needs to
 * implement this interface and be assigned to [Videos.source].
 */
interface VideoSource {
    fun all(): List<Video>
    fun topics(): List<VideoTopic>
    fun levels(): List<String>
    fun jlptLevels(): List<String>
    fun sections(): List<VideoSection>
    fun recommended(): List<Video>
    fun reportReasons(): List<String>
}

object Videos {

    lateinit var source: VideoSource
        private set

    fun load(json: JSONObject) {
        source = LocalVideos(json)
    }

    fun byId(id: String): Video? = source.all().firstOrNull { it.id == id }

    fun topic(id: String): VideoTopic? = source.topics().firstOrNull { it.id == id }

    fun filter(level: String? = null, topic: String? = null, short: Boolean? = null, jlpt: String? = null): List<Video> =
        source.all().filter {
            (level == null || it.level == level) && (topic == null || it.topic == topic) &&
                (short == null || it.short == short) && (jlpt == null || it.jlpt == jlpt)
        }

    /** Shorts rows hold only Shorts; every other row holds only landscape videos. */
    fun inSection(section: VideoSection) = filter(section.level, section.topic, section.short, section.jlpt)

    /** Best matches first: title, then topic, then anything said in the video. */
    fun search(query: String): List<Video> {
        val words = query.lowercase().split(" ", ",").filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return source.all().map { v ->
            val topic = topic(v.topic)?.name.orEmpty().lowercase()
            v to words.sumOf { w ->
                (if (v.title.lowercase().contains(w)) 5 else 0) +
                    (if (topic.contains(w)) 3 else 0) +
                    (if (v.level.lowercase() == w || v.jlpt.lowercase() == w) 2 else 0) +
                    v.lines.count { it.en.lowercase().contains(w) || it.text.contains(w) || it.romaji.contains(w) }
            }
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
    }
}

private class LocalVideos(json: JSONObject) : VideoSource {

    private val topics = json.getJSONArray("topics").objects().map {
        VideoTopic(it.getString("id"), it.getString("name"), it.getString("icon"))
    }
    private val levels = json.getJSONArray("levels").strings()
    private val jlpt = json.getJSONArray("jlpt").strings()
    private val sections = json.getJSONArray("sections").objects().map {
        VideoSection(
            it.getString("title"), it.optString("level").ifEmpty { null }, it.optString("topic").ifEmpty { null }, it.optBoolean("short"),
            it.optString("jlpt").ifEmpty { null }
        )
    }
    private val reasons = json.getJSONArray("reportReasons").strings()
    private val videos = json.getJSONArray("videos").objects().map { v ->
        val lines = v.getJSONArray("lines").objects()
        Video(
            id = v.getString("id"),
            youtube = v.getString("youtube"),
            title = v.getString("title"),
            channel = v.getString("channel"),
            source = v.getString("source"),
            topic = v.getString("topic"),
            level = v.getString("level"),
            start = v.getDouble("start"),
            end = v.getDouble("end"),
            short = v.optBoolean("short"),
            jlpt = v.getString("jlpt"),
            lines = lines.mapIndexed { i, l ->
                val words = l.getJSONArray("w")
                VideoLine(
                    index = i,
                    start = l.getDouble("t"),
                    duration = l.getDouble("d"),
                    words = List(words.length()) { w ->
                        val pieces = words.getJSONArray(w)
                        List(pieces.length()) { p -> pieces.getJSONArray(p).let { Ruby(it.getString(0), it.getString(1)) } }
                    },
                    kana = l.getString("kana"),
                    romaji = l.getString("romaji"),
                    en = l.getString("en")
                )
            }
        )
    }
    private val recommendedIds = json.getJSONArray("recommended").strings()

    override fun all() = videos
    override fun topics() = topics
    override fun levels() = levels
    override fun jlptLevels() = jlpt
    override fun sections() = sections
    override fun recommended() = recommendedIds.mapNotNull { id -> videos.firstOrNull { it.id == id } }
    override fun reportReasons() = reasons
}
