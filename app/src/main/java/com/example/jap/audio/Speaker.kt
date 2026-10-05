package com.example.jap.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/** Japanese text-to-speech using the engine installed on the device. */
class Speaker(context: Context) {

    /** False when the device has no Japanese voice. */
    var available by mutableStateOf(true)
        private set

    private var ready = false
    private var pending: String? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.JAPANESE)
                available = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                tts.setSpeechRate(0.85f)
                ready = true
                pending?.let(::speak)
                pending = null
            } else {
                available = false
            }
        }
    }

    /** Lesson audio switched off in the lesson settings. */
    var muted by mutableStateOf(false)

    fun speak(text: String) {
        if (text.isBlank() || muted) return
        if (!ready) {
            pending = text
            return
        }
        if (available) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jap")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

val LocalSpeaker = staticCompositionLocalOf<Speaker> { error("Speaker not provided") }
