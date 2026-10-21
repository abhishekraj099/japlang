package com.example.jap.audio

import android.speech.tts.TextToSpeech
import java.util.Locale

/** One text-to-speech voice the learner can pick in Settings. [name] is the engine's id; empty means its default. */
data class VoiceChoice(val name: String, val label: String, val online: Boolean)

/**
 * The installed voices of [language], offline ones first, with short labels ("Voice 1", "British 2"…): engines
 * name voices with codes like "ja-jp-x-htm-local", which mean nothing to a learner. Voices whose data is not
 * downloaded are left out.
 */
internal fun TextToSpeech.choices(language: String): List<VoiceChoice> {
    val found = runCatching { voices.orEmpty() }.getOrDefault(emptySet())
        .filter { it.locale.language == language && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
        .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.locale.country != "US" && language == "en" }, { it.locale.toString() }, { it.name }))
    val counts = mutableMapOf<String, Int>()
    return found.map { v ->
        val group = if (language == "en") accent(v.locale) else "Voice"
        val n = counts.merge(group, 1, Int::plus)!!
        VoiceChoice(v.name, "$group $n", v.isNetworkConnectionRequired)
    }
}

private fun accent(locale: Locale) = when (locale.country) {
    "US" -> "American"
    "GB" -> "British"
    "AU" -> "Australian"
    "IN" -> "Indian"
    "NG" -> "Nigerian"
    else -> locale.getDisplayCountry(Locale.ENGLISH).ifEmpty { "English" }
}

/** Switches [tts] to the first of [names] installed on the phone, or to [fallback]'s default voice. */
internal fun TextToSpeech.use(names: List<String>, fallback: Locale) {
    val installed = runCatching { voices.orEmpty() }.getOrDefault(emptySet())
        .filter { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }.associateBy { it.name }
    val voice = names.firstNotNullOfOrNull { installed[it] }
    if (voice != null) this.voice = voice else language = fallback
}
