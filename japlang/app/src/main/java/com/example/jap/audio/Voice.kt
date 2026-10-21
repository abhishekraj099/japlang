package com.example.jap.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * The guide's speaking voice: English text-to-speech, kept apart from the Japanese [Speaker] so lesson audio
 * and the guide never change each other's language. Japanese words in a line are read by the same voice, so
 * lines keep their romaji in brackets for her to say.
 */
class Voice(context: Context) {

    /** Turned off from the guide voice setting. */
    var enabled = true

    /** Higher for a girl guide, lower for a boy; applied on the next line spoken. */
    private var pitch = 1.2f

    /** The English voices installed on this phone, filled in once the engine has started. */
    var voices by mutableStateOf(emptyList<VoiceChoice>())
        private set

    /** The voices to speak with, best first; empty for the engine's default US English voice. */
    private var voiceNames = emptyList<String>()

    /** Speaks with the first of [names] installed on the phone, at [pitch]. */
    fun useVoice(names: List<String>, pitch: Float) {
        this.pitch = pitch
        if (names == voiceNames) return
        voiceNames = names
        if (ready) tts.use(names, Locale.US)
    }

    private var ready = false
    private var pending: String? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                voices = tts.choices("en")
                tts.use(voiceNames, Locale.US)
                tts.setSpeechRate(1.0f)
                ready = true
                pending?.let(::say)
                pending = null
            }
        }
    }

    fun say(text: String) {
        if (!enabled || text.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        tts.setPitch(pitch)
        tts.speak(spoken(text), TextToSpeech.QUEUE_FLUSH, null, "guide")
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        /** Japanese script is dropped from what is said aloud; the romaji next to it is read instead. */
        fun spoken(text: String): String =
            text.replace(Regex("[\\u3040-\\u30ff\\u4e00-\\u9fff々ー]+\\s*"), "").replace(Regex("\\(\\s*"), "(").replace(Regex("\\s{2,}"), " ").trim()
    }
}

val LocalVoice = staticCompositionLocalOf<Voice> { error("Voice not provided") }
