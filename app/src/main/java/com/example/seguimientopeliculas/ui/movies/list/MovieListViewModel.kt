package com.example.seguimientopeliculas.ui.movies.list

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieListUiState>(MovieListUiState.Loading)
    val uiState: StateFlow<MovieListUiState> = _uiState.asStateFlow()

    fun loadMovies(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sharedPreferences =
                    context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                val userId = sharedPreferences.getInt("moviesUserId", -1)
                if (userId == -1) throw Exception("Usuario no logueado. No se encontró userId.")

                val isNetworkAvailable = isNetworkAvailable(context)
                val movies = movieRepository.getUserMovies(userId)

                if (movies.isEmpty()) {
                    if (!isNetworkAvailable) {
                        _uiState.value =
                            MovieListUiState.Error("Sin conexión a Internet. No hay películas guardadas localmente.")
                    } else {
                        _uiState.value =
                            MovieListUiState.Error("No se encontraron películas para este usuario.")
                    }
                } else {
                    _uiState.value = MovieListUiState.Success(movies)
                }
            } catch (e: Exception) {
                _uiState.value = MovieListUiState.Error("Error al cargar películas: ${e.message}")
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
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
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}

    sealed class MovieListUiState {
    object Loading : MovieListUiState()
    data class Success(val movieList: List<Movie>) : MovieListUiState()
    data class Error(val message: String) : MovieListUiState()
}
