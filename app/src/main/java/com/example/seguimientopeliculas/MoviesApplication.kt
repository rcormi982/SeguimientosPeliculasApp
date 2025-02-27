package com.example.seguimientopeliculas

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MoviesApplication : Application(), Configuration.Provider {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "movie_sync_channel"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Sincronización de Películas"
        val descriptionText = "Notificaciones sobre nuevas películas y sincronización"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        Log.d("MoviesApplication", "Canal de notificaciones creado")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}