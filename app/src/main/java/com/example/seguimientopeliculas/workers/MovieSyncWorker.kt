package com.example.seguimientopeliculas.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.seguimientopeliculas.MoviesApplication
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.data.repository.MovieRepository
import com.example.seguimientopeliculas.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking

@HiltWorker
class MovieSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val movieRepository: MovieRepository
) : Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "MovieSyncWorker"
        private const val MOVIE_SYNC_NOTIFICATION_ID = 1
        private const val MOVIE_PREFS = "movie_sync_prefs"
        private const val LAST_MOVIE_COUNT = "last_movie_count"
    }

    override fun doWork(): Result {
        return runBlocking {
            Log.d(TAG, "Iniciando trabajo de sincronización de películas")

            try {
                // Obtener el ID del usuario
                val sharedPreferences = appContext.getSharedPreferences(
                    "app_preferences", Context.MODE_PRIVATE
                )
                val userId = sharedPreferences.getInt("moviesUserId", -1)

                if (userId == -1) {
                    Log.e(TAG, "No se encontró ID del usuario")
                    return@runBlocking Result.failure()
                }

                // Guardar el recuento anterior de películas
                val syncPrefs = appContext.getSharedPreferences(MOVIE_PREFS, Context.MODE_PRIVATE)
                val previousMovieCount = syncPrefs.getInt(LAST_MOVIE_COUNT, 0)

                // Sincronizar películas con runBlocking para manejar la función suspend
                val movies = movieRepository.getUserMovies(userId)
                Log.d(TAG, "Se han sincronizado ${movies.size} películas (recuento anterior: $previousMovieCount)")

                // Guardar el nuevo recuento
                syncPrefs.edit().putInt(LAST_MOVIE_COUNT, movies.size).apply()

                // Comprobar si hay películas nuevas
                if (previousMovieCount > 0 && movies.size > previousMovieCount) {
                    val newMovies = movies.size - previousMovieCount
                    val recentMovies = movies.takeLast(newMovies)
                    Log.d(TAG, "Detectadas $newMovies películas nuevas: ${recentMovies.map { it.title }}")
                    showNewMoviesNotification(newMovies, recentMovies)
                }

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error en la sincronización: ${e.message}")
                Result.retry()
            }
        }
    }

    private fun showNewMoviesNotification(count: Int, newMovies: List<Movie>) {
        try {
            val contentTitle = if (count == 1) {
                "Nueva película añadida"
            } else {
                "$count nuevas películas añadidas"
            }

            val contentText = if (count == 1) {
                "Se ha añadido: ${newMovies.first().title}"
            } else {
                "Se han añadido nuevas películas a tu colección"
            }

            // Intent para abrir la actividad principal
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(appContext, MoviesApplication.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.claqueta3)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            with(NotificationManagerCompat.from(appContext)) {
                // Verificar si tenemos permiso para mostrar notificaciones
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notify(MOVIE_SYNC_NOTIFICATION_ID, notification)
                        Log.d(TAG, "Notificación mostrada correctamente")
                    } else {
                        Log.w(TAG, "No se tienen permisos para mostrar notificaciones")
                    }
                } else {
                    notify(MOVIE_SYNC_NOTIFICATION_ID, notification)
                    Log.d(TAG, "Notificación mostrada correctamente")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar notificación: ${e.message}")
        }
    }
}