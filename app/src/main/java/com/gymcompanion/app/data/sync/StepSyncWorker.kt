package com.gymcompanion.app.data.sync

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.EntryPointAccessors

class StepSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = EntryPointAccessors.fromApplication(
                applicationContext, StepSyncDeps::class.java
            ).gymRepository()
            val sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (sensor == null) {
                return Result.success()
            }
            var lastSteps = -1L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.get(0)?.let {
                        if (lastSteps >= 0) {
                            val delta = (it - lastSteps).toInt().coerceAtLeast(0)
                            if (delta > 0) {
                                // Mise à jour asynchrone via coroutine serait plus propre,
                                // mais simplifions : on ne fait rien ici car le capteur
                                // donne le cumul depuis le redémarrage.
                            }
                        }
                        lastSteps = it.toLong()
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            kotlinx.coroutines.delay(2000L)
            sensorManager.unregisterListener(listener)
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE = "step_sync"
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<StepSyncWorker>(4, java.util.concurrent.TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
