package com.example.seguimientopeliculas.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerHelper @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager
) {
    companion object {
        private const val TAG = "WorkManagerHelper"
        private const val MOVIE_SYNC_WORK_NAME = "movie_sync_work"

        // Configuración del trabajo periódico inicial
        private const val INITIAL_SYNC_INTERVAL_HOURS = 1L

        // Configuración de backoff exponencial
        private const val BACKOFF_DELAY_MINUTES = 15L
    }

    fun schedulePeriodicalSync() {
        Log.d(TAG, "Programando sincronización periódica con backoff exponencial")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Construir la solicitud de trabajo periódico
        val syncRequest = PeriodicWorkRequest.Builder(
            MovieSyncWorker::class.java,
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS
        ).setInitialDelay(INITIAL_SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_MINUTES,
                TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            MOVIE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )

        Log.d(TAG, "Sincronización programada correctamente")
    }

    fun cancelAllWork() {
        Log.d(TAG, "Cancelando todos los trabajos")
        workManager.cancelAllWork()
    }

    fun cancelSyncWork() {
        Log.d(TAG, "Cancelando el trabajo de sincronización")
        workManager.cancelUniqueWork(MOVIE_SYNC_WORK_NAME)
    }
}