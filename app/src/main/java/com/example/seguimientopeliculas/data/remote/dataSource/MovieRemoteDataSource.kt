package com.example.seguimientopeliculas.data.remote.dataSource

import com.example.seguimientopeliculas.data.remote.models.MovieListRaw
import com.example.seguimientopeliculas.data.remote.models.MoviePostRequest
import com.example.seguimientopeliculas.data.remote.models.MovieRaw
import retrofit2.Response

interface MovieRemoteDataSource {
    suspend fun getMovies(): Response<MovieListRaw>
    suspend fun createMovie(moviePayload: MoviePostRequest, jwt: String): Response<MovieRaw>
    suspend fun getGenresAndStatuses(): Pair<List<String>, List<String>>
    suspend fun getUserMovies(userId: Int): Response<MovieListRaw>
    suspend fun updateMovie(movieId: Int, moviePayload: MoviePostRequest, jwt: String): Response<MovieRaw>
    suspend fun deleteMovie(movieId: Int, jwt: String): Response<Unit>
    suspend fun getJwtToken(): String
}
