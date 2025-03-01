package com.example.seguimientopeliculas.ui.movies.add

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seguimientopeliculas.MoviesApplication
import com.example.seguimientopeliculas.R
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.data.repository.MovieRepository
import com.example.seguimientopeliculas.data.remote.models.MovieAttributes
import com.example.seguimientopeliculas.data.remote.models.MoviePostRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.example.seguimientopeliculas.ui.MainActivity

@HiltViewModel
class AddMovieViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _genres = MutableStateFlow<List<String>>(emptyList())

    private val _statuses = MutableStateFlow<List<String>>(emptyList())

    private val _uiState = MutableStateFlow<AddMovieUiState>(AddMovieUiState.Idle)
    val uiState: StateFlow<AddMovieUiState> = _uiState.asStateFlow()

    init {
        fetchGenresAndStatuses() // Cargar géneros y estados al iniciar
    }

    private fun fetchGenresAndStatuses() {
        // Evita múltiples cargas si los datos ya están disponibles
        if (_genres.value.isNotEmpty() && _statuses.value.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (genres, statuses) = movieRepository.getGenresAndStatuses()
                _genres.value = genres
                _statuses.value = statuses
            } catch (e: Exception) {
                _uiState.value =
                    AddMovieUiState.Error("Error al cargar géneros y estados: ${e.message}")
            }
        }
    }

    fun addMovie(movie: Movie, jwt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = AddMovieUiState.Loading

                // Verificar conexión
                if (!isNetworkAvailable()) {
                    _uiState.value =
                        AddMovieUiState.Error("No hay conexión a Internet. No se pueden crear películas sin conexión.")
                    return@launch
                }

                // Construir los atributos de la película
                val movieAttributes = MovieAttributes(
                    Title = movie.title,
                    Genre = movie.genre,
                    Rating = movie.rating,
                    Premiere = movie.premiere,
                    Status = movie.status,
                    Comments = movie.comments,
                    movies_users = listOf(movie.moviesUserId ?: 0)
                )

                // Crear el objeto de la solicitud
                val moviePostRequest = MoviePostRequest(data = movieAttributes)

                // Llamar al repositorio para crear la película
                val result = movieRepository.createMovie(moviePostRequest, jwt)

                // Forzar sincronización para asegurar que la nueva película se guarde localmente
                val userId = movie.moviesUserId ?: return@launch
                movieRepository.getUserMovies(userId)

                // Mostrar notificación
                showMovieAddedNotification(movie)

                // Manejar el resultado
                _uiState.value = AddMovieUiState.Success(result)
            } catch (e: Exception) {
                // Manejo de errores generales
                _uiState.value = AddMovieUiState.Error("Error al guardar película: ${e.message}")
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showMovieAddedNotification(movie: Movie) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el intent para abrir la app cuando se toque la notificación
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, MoviesApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Película Agregada")
            .setContentText("Has añadido '${movie.title}' a tu lista")
            .setSmallIcon(R.drawable.claqueta3) // Usa tu ícono de la claqueta
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Mostrar la notificación
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

sealed class AddMovieUiState {
    object Idle : AddMovieUiState()
    object Loading : AddMovieUiState()
    data class Success(val movie: Movie) : AddMovieUiState()
    data class Error(val message: String) : AddMovieUiState()
}