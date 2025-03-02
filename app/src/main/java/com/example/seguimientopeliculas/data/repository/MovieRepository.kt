package com.example.seguimientopeliculas.data.repository

import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.data.remote.models.MoviePostRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MovieRepository {
    val setStream: StateFlow<List<Movie>>
    suspend fun getMovies(): List<Movie>
    suspend fun createMovie(movie: MoviePostRequest, jwt: String): Movie
    fun observeAll(): Flow<List<Movie>>
    suspend fun getGenresAndStatuses(): Pair<List<String>, List<String>>
    suspend fun getUserMovies(userId: Int): List<Movie>
    suspend fun updateMovie(movie: Movie): Boolean
    suspend fun deleteMovie(movieId: Int): Boolean

}