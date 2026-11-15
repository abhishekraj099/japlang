package com.example.jap.audio

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONObject
import java.util.Locale

/**
 * Says Japanese out loud. A native speaker's recording is played when one ships in assets/audio (listed in
 * assets/audio/index.json, see docs/AUDIO.md); anything without a recording is read by the device's text-to-speech.
 */
class Speaker(context: Context) {

    private val assets: AssetManager = context.applicationContext.assets

    /** Recorded text (spaces removed) to its file in assets/audio. Empty when no recordings ship. */
    private val recordings: Map<String, String> = runCatching {
        val json = JSONObject(assets.open("audio/index.json").bufferedReader().use { it.readText() })
        json.keys().asSequence().associate { key(it) to json.getString(it) }
    }.getOrDefault(emptyMap())

    private var player: MediaPlayer? = null

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
                voices = tts.choices("ja")
                tts.use(voiceNames, Locale.JAPANESE)
                tts.setSpeechRate(rate)
                tts.setPitch(pitch)
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

    /** The Japanese voices installed on this phone, filled in once the engine has started. */
    var voices by mutableStateOf(emptyList<VoiceChoice>())
        private set

    /** The voices picked in Settings, best first; empty for the engine's default Japanese voice. */
    private var voiceNames = emptyList<String>()
    private var pitch = 1f

    /** How fast lessons are read: 1.0 is the engine's normal speed. Learners usually want it a little slower. */
    private var rate = NORMAL_RATE

    /** Speaks with the first of [names] installed on the phone, at [pitch]: a character's voice or one picked by name. */
    fun useVoice(names: List<String>, pitch: Float) {
        voiceNames = names
        this.pitch = pitch
        if (ready) {
            tts.use(names, Locale.JAPANESE)
            tts.setPitch(pitch)
        }
    }

    fun useRate(value: Float) {
        rate = value
        if (ready) tts.setSpeechRate(value)
    }

    /** Says [text] with text-to-speech even when a recording exists, so Settings can let the learner hear a voice. */
    fun preview(text: String) {
        if (ready && available) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
    }

    fun speak(text: String) {
        if (text.isBlank() || muted) return
        recordings[key(text)]?.let { file -> if (play(file)) return }
        if (!ready) {
            pending = text
            return
        }
        if (available) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jap")
    }

    /** Silences whatever is playing, so the microphone does not pick up the app's own voice. */
    fun stop() {
        player?.release()
        player = null
        if (ready) tts.stop()
    }

    /** True when there is a recording of [text], so the voice is a person rather than the device's engine. */
    fun recorded(text: String) = key(text) in recordings

    /** Plays assets/audio/[file]; false if it cannot be played, so text-to-speech is used instead. */
    private fun play(file: String): Boolean = runCatching {
        if (ready) tts.stop()
        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            assets.openFd("audio/$file").use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            setOnCompletionListener { it.release(); if (player === it) player = null }
            prepare()
            start()
        }
        true
    }.getOrDefault(false)

    private fun key(text: String) = text.filterNot { it.isWhitespace() }

    fun shutdown() {
        player?.release()
        player = null
        tts.stop()
        tts.shutdown()
    }
}

/** Japanese speeds offered in Settings. */
const val SLOW_RATE = 0.6f
const val NORMAL_RATE = 0.85f
const val FAST_RATE = 1.05f

val LocalSpeaker = staticCompositionLocalOf<Speaker> { error("Speaker not provided") }
