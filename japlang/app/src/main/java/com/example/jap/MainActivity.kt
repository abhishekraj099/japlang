package com.example.jap

import com.example.jap.data.Analytics
import com.example.jap.data.Guide
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.example.jap.audio.LocalSpeaker
import com.example.jap.audio.Chime
import com.example.jap.audio.LocalChime
import com.example.jap.audio.LocalVoice
import com.example.jap.audio.Voice
import com.example.jap.audio.Speaker
import com.example.jap.data.Progress
import com.example.jap.ink.Handwriting
import com.example.jap.ink.LocalHandwriting
import com.example.jap.ui.JapApp
import com.example.jap.ui.theme.JapTheme
import com.example.jap.ui.useGuideVoice
import com.example.jap.ui.useJapaneseVoice

class MainActivity : ComponentActivity() {

    private lateinit var speaker: Speaker
    private lateinit var handwriting: Handwriting
    private lateinit var chime: Chime
    private lateinit var voice: Voice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Not on a rotation or other re-creation, only when the app is opened.
        if (savedInstanceState == null) Analytics.log(Analytics.APP_OPEN)
        enableEdgeToEdge()
        speaker = Speaker(this)
        handwriting = Handwriting(this)
        chime = Chime()
        voice = Voice(this)
        val progress = Progress(this)
        Guide.select(progress.guideCharacter)
        speaker.useJapaneseVoice(progress)
        speaker.useRate(progress.speechRate)
        voice.useGuideVoice(progress)
        setContent {
            // The phone's setting by default; Settings can fix the app to light or dark.
            val dark = when (progress.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            JapTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalSpeaker provides speaker, LocalHandwriting provides handwriting, LocalChime provides chime, LocalVoice provides voice) {
                    JapApp(progress)
                }
            }
        }
    }

    override fun onDestroy() {
        speaker.shutdown()
        handwriting.close()
        chime.release()
        voice.shutdown()
        super.onDestroy()
    }
}
