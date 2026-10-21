package com.example.jap.data

import org.json.JSONObject

/**
 * Stroke order for kanji, from assets/strokes.json: each character's strokes in writing order, as SVG path data in
 * a 109 x 109 box. The paths come from KanjiVG (CC BY-SA 3.0, see the Licenses screen).
 */
object Strokes {

    /** The side of the square the paths are drawn in. */
    const val BOX = 109f

    private var paths: Map<String, List<String>> = emptyMap()

    fun load(json: JSONObject) {
        paths = json.keys().asSequence().associateWith { char ->
            json.getJSONArray(char).let { a -> List(a.length()) { a.getString(it) } }
        }
    }

    /** The strokes of [char] in order, or an empty list when there is no stroke data for it. */
    fun of(char: String): List<String> = paths[char].orEmpty()
}
