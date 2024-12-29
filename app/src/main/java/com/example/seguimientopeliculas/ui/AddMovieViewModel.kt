package com.example.seguimientopeliculas.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MovieRepository
import com.example.seguimientopeliculas.data.remote.MovieAttributes
import com.example.seguimientopeliculas.data.remote.MoviePostRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMovieViewModel @Inject constructor(
    private val movieRepository: MovieRepository
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
                _uiState.value = AddMovieUiState.Error("Error al cargar géneros y estados: ${e.message}")
                Log.e("AddMovieViewModel", "Error al cargar géneros y estados", e)
            }
        }
    }

    fun addMovie(movie: Movie, jwt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = AddMovieUiState.Loading

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

                // Manejar el resultado
                _uiState.value = AddMovieUiState.Success(result)
            } catch (e: Exception) {
                // Manejo de errores generales
                _uiState.value = AddMovieUiState.Error("Error al guardar película: ${e.message}")
            }
        }
    }
}

sealed class AddMovieUiState {
    object Idle : AddMovieUiState()
    object Loading : AddMovieUiState()
    data class Success(val movie: Movie) : AddMovieUiState()
    data class Error(val message: String) : AddMovieUiState()
}
