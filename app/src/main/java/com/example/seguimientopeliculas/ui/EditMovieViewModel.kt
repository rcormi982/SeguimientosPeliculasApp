package com.example.seguimientopeliculas.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditMovieViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditMovieUiState>(EditMovieUiState.Idle)
    val uiState: StateFlow<EditMovieUiState> get() = _uiState

    fun updateMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                _uiState.value = EditMovieUiState.Loading

                // Verificar conexión
                if (!isNetworkAvailable()) {
                    _uiState.value = EditMovieUiState.Error("No hay conexión a Internet. No se pueden actualizar películas sin conexión.")
                    return@launch
                }

                val isSuccess = movieRepository.updateMovie(movie)
                if (isSuccess) {
                    _uiState.value = EditMovieUiState.SuccessUpdate
                } else {
                    _uiState.value = EditMovieUiState.Error("No se pudo actualizar la película.")
                }
            } catch (e: Exception) {
                _uiState.value = EditMovieUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun deleteMovie(movieId: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = EditMovieUiState.Loading

                // Verificar conexión
                if (!isNetworkAvailable()) {
                    _uiState.value = EditMovieUiState.Error("No hay conexión a Internet. No se pueden eliminar películas sin conexión.")
                    return@launch
                }

                val isSuccess = movieRepository.deleteMovie(movieId)
                if (isSuccess) {
                    _uiState.value = EditMovieUiState.SuccessDelete
                } else {
                    _uiState.value = EditMovieUiState.Error("No se pudo eliminar la película.")
                }
            } catch (e: Exception) {
                _uiState.value = EditMovieUiState.Error("Error: ${e.message}")
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

    sealed class EditMovieUiState {
        object Idle : EditMovieUiState()
        object Loading : EditMovieUiState()
        object SuccessUpdate : EditMovieUiState()
        object SuccessDelete : EditMovieUiState()
        data class Error(val message: String) : EditMovieUiState()
    }
}