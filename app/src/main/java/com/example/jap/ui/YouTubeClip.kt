package com.example.jap.ui

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.jap.data.Video
import com.example.jap.data.VideoLine
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/** Removes YouTube's caption module; `player` is the page's IFrame API player. */
private const val HIDE_CAPTIONS = "try { player.unloadModule('captions'); player.unloadModule('cc'); } catch (e) {}"

private fun webViewIn(view: View): WebView? = when (view) {
    is WebView -> view
    is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { webViewIn(view.getChildAt(it)) }
    else -> null
}

/** Small grace period so a line's last sound isn't cut off. */
private const val TAIL = 0.25f

/**
 * Controls one clip of a YouTube video: where playback is, and playing the whole clip or a single line.
 * Playback stops on its own at the end of the clip or line.
 */
@Stable
class ClipPlayer(val video: Video) {
    internal var player: YouTubePlayer? = null
    var second by mutableFloatStateOf(video.start.toFloat())
        private set
    var playing by mutableStateOf(false)
        internal set
    /** True once the clip has played to its end. */
    var ended by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        internal set
    private var stopAt: Float? = null

    fun seek(seconds: Double, play: Boolean = true) {
        ended = false
        second = seconds.toFloat()
        player?.seekTo(seconds.toFloat())
        if (play) player?.play()
    }

    /** Plays just [line], then pauses. */
    fun playLine(line: VideoLine) {
        stopAt = line.end.toFloat() + TAIL
        seek(line.start)
    }

    /** Tap on the video: play, pause, or start the clip again once it has ended. */
    fun toggle() {
        when {
            ended -> replay()
            playing -> pause()
            else -> player?.play()
        }
    }

    fun replay() {
        stopAt = null
        seek(video.start)
    }

    fun pause() {
        stopAt = null
        player?.pause()
    }

    internal fun tick(now: Float) {
        val before = second
        second = now
        stopAt?.let { if (now >= it) pause() }
        if (before < video.end && now >= video.end) {
            player?.pause()
            ended = true
        }
    }
}

@Composable
fun rememberClipPlayer(video: Video) = remember(video.id) { ClipPlayer(video) }

/**
 * The official YouTube embedded player, starting at the clip and reporting progress to [state].
 * YouTube's own controls are off; callers cover it with [ClipControls] so taps never leave the app.
 */
@Composable
fun YouTubeClip(state: ClipPlayer, modifier: Modifier = Modifier) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        modifier = modifier,
        factory = { context ->
            YouTubePlayerView(context).apply {
                enableAutomaticInitialization = false
                lifecycle.addObserver(this)
                val options = IFramePlayerOptions.Builder(context).controls(0).rel(0).ivLoadPolicy(3).ccLoadPolicy(0).build()
                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        state.player = youTubePlayer
                        youTubePlayer.loadVideo(state.video.youtube, state.video.start.toFloat())
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) = state.tick(second)

                    override fun onStateChange(youTubePlayer: YouTubePlayer, playerState: PlayerConstants.PlayerState) {
                        state.playing = playerState == PlayerConstants.PlayerState.PLAYING
                        // The app shows its own subtitles, so turn off any caption track YouTube switches on.
                        if (state.playing) webViewIn(this@apply)?.evaluateJavascript(HIDE_CAPTIONS, null)
                    }

                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        state.error = error.name
                    }
                }, true, options)
            }
        },
        onRelease = { view ->
            lifecycle.removeObserver(view)
            state.player = null
            view.release()
        }
    )
}
