package com.example.seguimientopeliculas.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val movieRepository: MovieRepository
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

                val movies = movieRepository.getUserMovies(userId)
                _uiState.value = if (movies.isEmpty()) {
                    Log.e(
                        "MovieListViewModel",
                        "No se encontraron películas para el usuario $userId."
                    )
                    MovieListUiState.Error("No se encontraron películas para este usuario.")
                } else {
                    Log.d("MovieListViewModel", "Películas encontradas: ${movies.size}")
                    MovieListUiState.Success(movies)
                }
            } catch (e: Exception) {
                Log.e("MovieListViewModel", "Error al cargar películas: ${e.message}", e)
                _uiState.value = MovieListUiState.Error("Error al cargar películas del usuario.")
            }
        }
    }
}

    sealed class MovieListUiState {
    object Loading : MovieListUiState()
    data class Success(val movieList: List<Movie>) : MovieListUiState()
    data class Error(val message: String) : MovieListUiState()
}
