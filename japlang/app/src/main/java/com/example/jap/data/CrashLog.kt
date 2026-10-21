package com.example.jap.data

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps a record of crashes on the phone, so a learner can choose to send it to us from Settings. Nothing is
 * uploaded automatically; an online crash service (for example Firebase Crashlytics) can replace this later.
 */
object CrashLog {

    private const val FILE = "crashes.log"
    private const val MAX_BYTES = 200_000L
    private var file: File? = null

    fun install(context: Context) {
        file = File(context.filesDir, FILE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record(context, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun record(context: Context, thread: Thread, error: Throwable) {
        val f = file ?: return
        if (f.length() > MAX_BYTES) f.writeText("")
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
        f.appendText(
            "=== $stamp · app $version · Android ${Build.VERSION.RELEASE} (${Build.MODEL}) · thread ${thread.name}\n" +
                error.stackTraceToString() + "\n"
        )
    }

    /** The saved crashes, or an empty string when there have been none. */
    fun read(): String = runCatching { file?.readText().orEmpty() }.getOrDefault("")

    fun clear() {
        runCatching { file?.delete() }
    }
}
