package com.gymcompanion.app.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.gymcompanion.app.MainActivity
import com.gymcompanion.app.R
import java.util.Calendar

object NotificationHelper {
    const val CHANNEL_ID = "gymcompanion_reminders"
    private const val NOTIF_ID = 1001
    private const val REQUEST_CODE = 1001

    fun createChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Rappels GymCompanion",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Rappels nutrition et séances" }
                context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
        } catch (_: Exception) { /* non bloquant */ }
    }

    fun showNutritionReminder(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("GymCompanion")
            .setContentText("N'oublie pas de logger ta nutrition d'aujourd'hui")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notif)
    }

    /**
     * Programme le prochain rappel (one-shot exact ; [ReminderReceiver] re-programme
     * après chaque déclenchement et au BOOT_COMPLETED).
     * Repli sur alarme inexacte si l'utilisateur a refusé SCHEDULE_EXACT_ALARM (Android 12+).
     */
    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        if (hour !in 0..23 || minute !in 0..59) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = buildPendingIntent(context)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            // Fenêtre ±10 min : compromis sans permission exacte
            am.setWindow(AlarmManager.RTC_WAKEUP, cal.timeInMillis, 10 * 60 * 1000L, pi)
        }
    }

    fun cancelReminder(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
