package com.example.jap

import android.app.Application
import com.example.jap.data.Content
import com.example.jap.data.Analytics
import com.example.jap.data.CrashLog

class JapApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Here rather than in the activity so the reminder receiver, which can start the
        // process on its own, also sees the loaded configuration.
        Content.load(this)
        Analytics.init(this)
        CrashLog.install(this)
    }
}
