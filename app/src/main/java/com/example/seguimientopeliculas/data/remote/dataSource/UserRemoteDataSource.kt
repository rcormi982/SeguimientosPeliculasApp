package com.example.seguimientopeliculas.data.remote.dataSource
import android.net.Uri  // Cambiamos de coil3.Uri a android.net.Uri
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.data.remote.models.MoviesUser
import com.example.seguimientopeliculas.data.remote.models.PhotoUploadResult
import com.example.seguimientopeliculas.data.remote.models.RegisterResponse
import com.example.seguimientopeliculas.data.remote.models.UserResponse
import retrofit2.Response

interface UserRemoteDataSource {
    suspend fun registerUser(username: String, email: String, password: String): Response<RegisterResponse>
    suspend fun getMoviesForUser(userId: Int): List<Movie>
    suspend fun createMovieForUser(userId: Int, movie: Movie, jwt: String): Boolean
    suspend fun fetchMoviesUserId(userId: Int): Int?
    suspend fun getUserData(): UserResponse
    suspend fun updateUser(userId: Int, username: String, email: String): Response<UserResponse>
    suspend fun deleteUser(userId: Int): Response<Unit>
    suspend fun getMoviesUserData(): MoviesUser
    suspend fun uploadProfilePhoto(photoUri: Uri, moviesUserId: Int): PhotoUploadResult?
    suspend fun updateUserPhoto(moviesUserId: Int, photoResult: PhotoUploadResult): Boolean
    suspend fun updateMoviesUser(moviesUserId: Int, updatePayload: Map<String, Any>): Boolean
}