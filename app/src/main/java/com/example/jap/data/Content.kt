package com.example.jap.data

import android.content.Context
import org.json.JSONObject

/**
 * Loads everything the app teaches and every tuning value from JSON.
 * Today the files ship in assets; pointing [read] at a server is the only change
 * needed to deliver content over the network.
 */
object Content {

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        Config.load(read(context, "config.json"))
        Course.load(read(context, "course.json"))
        Exam.load(read(context, "exam.json"))
        Scenarios.load(read(context, "scenarios.json"))
        Videos.load(read(context, "videos.json"))
        loaded = true
    }

    private fun read(context: Context, name: String): JSONObject =
        JSONObject(context.assets.open(name).bufferedReader().use { it.readText() })
}
