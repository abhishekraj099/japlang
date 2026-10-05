package com.example.jap.ink

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
 * Reads Japanese handwriting on the device. The language model is downloaded once,
 * which needs a connection; after that it works offline.
 */
class Handwriting {

    enum class State { LOADING, READY, FAILED }

    var state by mutableStateOf(State.LOADING)
        private set

    private val model = DigitalInkRecognitionModel
        .builder(requireNotNull(DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")))
        .build()
    private val recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())

    init {
        prepare()
    }

    /** Downloads the model if it is not on the device yet. Safe to call again after a failure. */
    fun prepare() {
        state = State.LOADING
        RemoteModelManager.getInstance()
            .download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { state = State.READY }
            .addOnFailureListener { state = State.FAILED }
    }

    /** Calls [onResult] with the recogniser's guesses, best first, or null if it could not read the strokes. */
    fun read(strokes: List<List<Offset>>, width: Float, height: Float, onResult: (List<String>?) -> Unit) {
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
        recognizer.recognize(ink.build(), context)
            .addOnSuccessListener { result -> onResult(result.candidates.map { it.text }) }
            .addOnFailureListener { onResult(null) }
    }

    fun close() = recognizer.close()

    companion object {
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
