package com.example.jap

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.jap.data.Config
import com.example.jap.data.Progress
import java.util.Calendar

/** Daily study reminder, delivered as a notification at about the hour set in assets/config.json. */
object Reminder {

    const val CHANNEL = "daily-reminder"

    fun schedule(context: Context) {
        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Config.reminderHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarms(context).setInexactRepeating(
            AlarmManager.RTC_WAKEUP, first.timeInMillis, AlarmManager.INTERVAL_DAY, pending(context)
        )
    }

    fun cancel(context: Context) = alarms(context).cancel(pending(context))

    private fun alarms(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val on = context.getSharedPreferences(Progress.SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(Progress.KEY_REMINDER, false)
        if (!on) return
        // Alarms do not survive a reboot, so set it again.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminder.schedule(context)
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(Reminder.CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, Reminder.CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Time for Japanese")
            .setContentText("A few minutes today keeps your streak going.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(1, notification)
    }
}
