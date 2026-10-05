package com.example.jap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.example.jap.audio.LocalSpeaker
import com.example.jap.audio.Speaker
import com.example.jap.data.Progress
import com.example.jap.ink.Handwriting
import com.example.jap.ink.LocalHandwriting
import com.example.jap.ui.JapApp
import com.example.jap.ui.theme.JapTheme

class MainActivity : ComponentActivity() {

    private lateinit var speaker: Speaker
    private lateinit var handwriting: Handwriting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        speaker = Speaker(this)
        handwriting = Handwriting()
        val progress = Progress(this)
        setContent {
            JapTheme {
                CompositionLocalProvider(LocalSpeaker provides speaker, LocalHandwriting provides handwriting) {
                    JapApp(progress)
                }
            }
        }
    }

    override fun onDestroy() {
        speaker.shutdown()
        handwriting.close()
        super.onDestroy()
    }
}
