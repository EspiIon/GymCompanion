package com.gymcompanion.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.REMINDER_ENABLED_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.REMINDER_HOUR_PREF
import com.gymcompanion.app.viewmodel.SettingsViewModel.Companion.REMINDER_MIN_PREF
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reçoit les alarmes quotidiennes ET le BOOT_COMPLETED.
 *
 * - Alarme déclenchée → affiche la notification puis re-programme le lendemain.
 * - Boot → re-programme l'alarme (les alarmes sont perdues au redémarrage).
 */
class ReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderDeps {
        fun dataStore(): DataStore<Preferences>
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isBoot = intent.action == Intent.ACTION_BOOT_COMPLETED
        if (!isBoot) {
            NotificationHelper.showNutritionReminder(context)
        }
        // Re-programmation (lecture async des préférences via goAsync)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val deps = EntryPointAccessors.fromApplication(
                    context.applicationContext, ReminderDeps::class.java
                )
                val prefs = deps.dataStore().data.first()
                if (prefs[REMINDER_ENABLED_PREF] == true) {
                    NotificationHelper.scheduleDaily(
                        context,
                        prefs[REMINDER_HOUR_PREF] ?: 20,
                        prefs[REMINDER_MIN_PREF] ?: 0
                    )
                }
            } catch (_: Exception) {
                // Jamais de crash depuis un BroadcastReceiver
            } finally {
                pending.finish()
            }
        }
    }
}
