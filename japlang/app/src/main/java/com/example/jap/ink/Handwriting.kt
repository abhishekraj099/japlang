package com.example.jap.ink

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea

/**
 * Reads Japanese handwriting on the device with ML Kit Digital Ink and its "ja" model. The model downloads by
 * itself in the background when the app starts; without a connection it waits and starts as soon as the device
 * is back online. Once downloaded it works offline.
 */
class Handwriting(context: Context) {

    enum class State {
        /** Finding out whether the model is on the device, or downloading it. */
        PREPARING,
        /** No connection, so the download is waiting for one. */
        OFFLINE,
        READY,
        /** This device cannot recognise Japanese handwriting; writing is skipped. */
        UNSUPPORTED
    }

    private val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val main = Handler(Looper.getMainLooper())

    var state by mutableStateOf(State.PREPARING)
        private set

    private val manager = RemoteModelManager.getInstance()
    // Null if the Japanese model is unknown on this device; writing exercises are then skipped instead of crashing.
    private val model = runCatching { DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja") }.getOrNull()
        ?.let { DigitalInkRecognitionModel.builder(it).build() }
    private val recognizer = model?.let { DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(it).build()) }
    private var downloading = false

    /** Starts the download again whenever a connection comes back while the model is still missing. */
    private val onNetwork = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            main.post { if (state != State.READY) prepare() }
        }
    }

    init {
        if (model == null) {
            state = State.UNSUPPORTED
        } else {
            runCatching { connectivity?.registerDefaultNetworkCallback(onNetwork) }
            prepare()
        }
    }

    private fun online(): Boolean {
        val caps = connectivity?.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Uses the model if it is already on the device, otherwise downloads it when there is a connection. */
    private fun prepare() {
        val model = model ?: return
        if (downloading) return
        manager.isModelDownloaded(model).addOnCompleteListener { task ->
            when {
                task.isSuccessful && task.result == true -> ready()
                !online() -> state = State.OFFLINE
                else -> download()
            }
        }
    }

    private fun download() {
        val model = model ?: return
        downloading = true
        state = State.PREPARING
        manager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { ready() }
            .addOnFailureListener { state = if (online()) State.PREPARING else State.OFFLINE }
            .addOnCompleteListener {
                downloading = false
                // A failure while still online is retried once a little later.
                if (state == State.PREPARING) main.postDelayed({ prepare() }, RETRY_MILLIS)
            }
    }

    private fun ready() {
        state = State.READY
        runCatching { connectivity?.unregisterNetworkCallback(onNetwork) }
    }

    /** Calls [onResult] with the recogniser's guesses, best first, or null if it could not read the strokes. */
    fun read(strokes: List<List<Offset>>, width: Float, height: Float, onResult: (List<String>?) -> Unit) {
        if (strokes.isEmpty()) {
            onResult(null)
            return
        }
        val ink = Ink.builder()
        for (points in strokes) {
            val stroke = Ink.Stroke.builder()
            for (p in points) stroke.addPoint(Ink.Point.create(p.x, p.y))
            ink.addStroke(stroke.build())
        }
        val context = RecognitionContext.builder()
            .setPreContext("")
            .setWritingArea(WritingArea(width, height))
            .build()
        val recognizer = recognizer ?: return onResult(null)
        recognizer.recognize(ink.build(), context)
            .addOnSuccessListener { result -> onResult(result.candidates.map { it.text }) }
            .addOnFailureListener { onResult(null) }
    }

    /** Reads every box on its own, one after another; an empty box gives null. */
    fun readBoxes(boxes: List<List<List<Offset>>>, width: Float, height: Float, onResult: (List<List<String>?>) -> Unit) {
        val out = mutableListOf<List<String>?>()
        fun next(i: Int) {
            if (i == boxes.size) return onResult(out)
            if (boxes[i].isEmpty()) {
                out += null
                next(i + 1)
            } else {
                read(boxes[i], width, height) { guesses ->
                    // A box with ink that could not be read still counts as written, so it shows as "?".
                    out += guesses ?: emptyList()
                    next(i + 1)
                }
            }
        }
        next(0)
    }

    fun close() {
        runCatching { connectivity?.unregisterNetworkCallback(onNetwork) }
        main.removeCallbacksAndMessages(null)
        recognizer?.close()
    }

    companion object {
        private const val RETRY_MILLIS = 30_000L

        /** Characters that are drawn the same way in both kana scripts, so either reading counts. */
        private val lookalikes = listOf("へヘ", "りリ")

        fun matches(target: String, guesses: List<String>): Boolean {
            val accepted = lookalikes.fold(setOf(target)) { set, pair ->
                if (target.length == 1 && target in pair) set + pair.map { it.toString() } else set
            }
            return guesses.any { it.trim() in accepted }
        }
    }
}

val LocalHandwriting = staticCompositionLocalOf<Handwriting> { error("Handwriting not provided") }
