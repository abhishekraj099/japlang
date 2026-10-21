package com.example.jap.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Listens for one Japanese sentence. [level] is the live loudness, 0 to 1, for the mic animation. */
@Stable
class SpeechInput internal constructor() {
    var listening by mutableStateOf(false)
        internal set
    var level by mutableFloatStateOf(0f)
        internal set
    internal var begin: (String) -> Unit = {}
    internal var end: () -> Unit = {}

    fun start(prompt: String) = begin(prompt)
    fun stop() = end()
}

/**
 * Uses the on-device recogniser so listening happens inline; falls back to the system speech
 * dialog when none is available or the microphone permission is refused.
 */
@Composable
fun rememberSpeechInput(onResult: (List<String>) -> Unit, onError: (String) -> Unit): SpeechInput {
    val context = LocalContext.current
    val result by rememberUpdatedState(onResult)
    val error by rememberUpdatedState(onError)
    val input = remember { SpeechInput() }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    var pending by remember { mutableStateOf<String?>(null) }

    fun intent(prompt: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
        .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

    val dialog = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val texts = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (texts.isNotEmpty()) result(texts)
    }

    fun openDialog(prompt: String) {
        runCatching { dialog.launch(intent(prompt)) }.onFailure { error("Speech recognition isn't available on this device.") }
    }

    fun listen(prompt: String) {
        val r = recognizer ?: return openDialog(prompt)
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { input.listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { input.level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { input.level = 0f }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                input.listening = false
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (texts.isEmpty()) error("I didn't catch that. Try again a little louder.") else result(texts)
            }

            override fun onError(code: Int) {
                input.listening = false
                input.level = 0f
                when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> error("I didn't catch that. Try again a little louder.")
                    SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Unit
                    else -> openDialog(prompt)
                }
            }
        })
        input.listening = true
        r.startListening(intent(prompt).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val prompt = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (granted) listen(prompt) else openDialog(prompt)
    }

    input.begin = { prompt ->
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        when {
            recognizer == null -> openDialog(prompt)
            granted -> listen(prompt)
            else -> { pending = prompt; permission.launch(Manifest.permission.RECORD_AUDIO) }
        }
    }
    input.end = { recognizer?.stopListening() }

    DisposableEffect(recognizer) { onDispose { recognizer?.destroy() } }
    return input
}
