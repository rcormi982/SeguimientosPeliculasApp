package com.example.seguimientopeliculas.data.remote
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.login.RegisterResponse
import com.example.seguimientopeliculas.login.UserResponse
import retrofit2.Response

interface UserRemoteDataSource {
    suspend fun registerUser(username: String, email: String, password: String): Response<RegisterResponse>
    suspend fun getMoviesForUser(userId: Int): List<Movie>
    suspend fun createMovieForUser(userId: Int, movie: Movie, jwt: String): Boolean
    suspend fun fetchMoviesUserId(userId: Int): Int?
    suspend fun getUserData(): UserResponse
    suspend fun updateUser(userId: Int, username: String, email: String): Response<UserResponse>
    suspend fun deleteUser(userId: Int): Response<Unit>
}
