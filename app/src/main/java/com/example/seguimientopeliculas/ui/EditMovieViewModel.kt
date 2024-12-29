package com.example.seguimientopeliculas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditMovieViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditMovieUiState>(EditMovieUiState.Idle)
    val uiState: StateFlow<EditMovieUiState> get() = _uiState

    fun updateMovie(movie: Movie) {
        viewModelScope.launch {
            try {
                _uiState.value = EditMovieUiState.Loading
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

    sealed class EditMovieUiState {
        object Idle : EditMovieUiState()
        object Loading : EditMovieUiState()
        object SuccessUpdate : EditMovieUiState()
        object SuccessDelete : EditMovieUiState()
        data class Error(val message: String) : EditMovieUiState()
    }
}
