package com.example.jap.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Product analytics, kept on the device for now: each event is one JSON line in files/analytics.jsonl.
 * Swap [write] for a call to an analytics service when there is one; the events stay the same.
 */
object Analytics {

    const val APP_OPEN = "app_open"
    const val LESSON_START = "lesson_start"
    const val LESSON_FINISH = "lesson_finish"
    const val REVIEW_DONE = "review_done"
    const val WRITING_CHECK = "writing_check"
    const val PLACEMENT_RESULT = "placement_result"

    private const val FILE = "analytics.jsonl"
    /** The log is cut back to its newer half once it passes this size. */
    private const val MAX_BYTES = 1_000_000L

    private val io = Executors.newSingleThreadExecutor()
    private var file: File? = null

    /** Ties together the events of one launch of the app. */
    private val session = UUID.randomUUID().toString().take(8)

    fun init(context: Context) {
        file = File(context.filesDir, FILE)
    }

    /** Deletes the usage log, for "Delete all my data". Done straight away: the app restarts right after. */
    fun clear() {
        runCatching { file?.delete() }
    }

    fun log(event: String, vararg props: Pair<String, Any?>) {
        val line = JSONObject().apply {
            put("event", event)
            put("at", System.currentTimeMillis())
            put("session", session)
            props.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) }
        }.toString()
        io.execute { write(line) }
    }

    private fun write(line: String) {
        val f = file ?: return
        runCatching {
            if (f.length() > MAX_BYTES) {
                val lines = f.readLines()
                f.writeText(lines.drop(lines.size / 2).joinToString("\n", postfix = "\n"))
            }
            f.appendText(line + "\n")
        }
    }
}
