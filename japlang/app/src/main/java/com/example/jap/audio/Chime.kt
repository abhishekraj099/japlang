package com.example.jap.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The short two-note chime played on a correct answer. It is synthesised once at start-up,
 * so there is no sound file to ship.
 */
class Chime {

    /** Turned off from the sound effects setting. */
    var enabled = true

    private val track: AudioTrack? = runCatching { build() }.getOrNull()

    fun play() {
        val t = track ?: return
        if (!enabled) return
        runCatching {
            t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    fun release() {
        track?.release()
    }

    private fun build(): AudioTrack {
        // C6 then G6, each fading out like a small bell.
        val notes = listOf(1046.5 to 0.09, 1568.0 to 0.22)
        val samples = notes.flatMap { (hz, seconds) ->
            val n = (RATE * seconds).toInt()
            List(n) { i ->
                val t = i / RATE.toDouble()
                val bell = sin(2 * PI * hz * t) + 0.3 * sin(4 * PI * hz * t)
                (bell * exp(-t * 14) * VOLUME * Short.MAX_VALUE).toInt().toShort()
            }
        }.toShortArray()
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }

    private companion object {
        const val RATE = 44_100
        const val VOLUME = 0.35
    }
}

val LocalChime = staticCompositionLocalOf<Chime> { error("Chime not provided") }
