package com.example.seguimientopeliculas.data.remote.api

import com.example.seguimientopeliculas.data.remote.models.UpdateMoviesUserPayload
import com.example.seguimientopeliculas.data.remote.models.MovieListRaw
import com.example.seguimientopeliculas.data.remote.models.MoviePostRequest
import com.example.seguimientopeliculas.data.remote.models.MovieRaw
import com.example.seguimientopeliculas.data.remote.models.MoviesUserRequest
import com.example.seguimientopeliculas.data.remote.models.MoviesUserResponse
import com.example.seguimientopeliculas.data.remote.models.RegisterPostRequest
import com.example.seguimientopeliculas.data.remote.models.UploadResponse
import com.example.seguimientopeliculas.data.remote.models.LoginResponse
import com.example.seguimientopeliculas.data.remote.models.LoginRequestApi
import com.example.seguimientopeliculas.data.remote.models.RegisterResponse
import com.example.seguimientopeliculas.data.remote.models.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface StrapiApi {

    // Obtener todas las películas
    @GET("films")
    suspend fun getMovies(): Response<MovieListRaw>

    // Crear una nueva película
    @POST("films")
    suspend fun createMovieForUser(
        @Body moviePayload: MoviePostRequest,
        @Query("populate") populate: String = "movies_users,users_permissions_user"
    ): Response<MovieRaw>

    // Obtener películas filtradas por usuario
    @GET("films")
    suspend fun getMoviesByUser(
        @Query("filters[movies_users][id][\$eq]") moviesUserId: Int,
        @Query("populate") populate: String = "movies_users,users_permissions_user"
    ): Response<MovieListRaw>

    // Crear una relación en movies_users
    @POST("movies-users")
    suspend fun createMoviesUser(
        @Body relationshipPayload: MoviesUserRequest
    ): Response<MoviesUserResponse>

    // Registrar un nuevo usuario
    @POST("auth/local/register")
    suspend fun registerUser(@Body userPostRequest: RegisterPostRequest): Response<RegisterResponse>

    // Login de usuario
    @POST("auth/local")
    suspend fun loginUser(@Body loginRequest: LoginRequestApi): Response<LoginResponse>

    @GET("movies-users")
    suspend fun getMoviesUserByUserId(
        @Query("filters[users_permissions_user][id][\$eq]") userId: Int,
        @Query("populate") populate: String = "*"
    ): Response<MoviesUserResponse>

    @PUT("films/{id}")
    suspend fun updateMovie(
        @Path("id") movieId: Int,
        @Body moviePayload: MoviePostRequest
    ): Response<MovieRaw>

    @DELETE("films/{id}")
    suspend fun deleteMovie(
        @Path("id") movieId: Int
    ): Response<Unit>

    @GET("users/me")
    suspend fun getUserData(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body updatePayload: Map<String, String>
    ): Response<UserResponse>

    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Path("id") id: Int
    ): Response<Unit>

    @DELETE("movies-users/{id}")
    suspend fun deleteMoviesUser(
        @Path("id") id: Int
    ): Response<Unit>

    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("refId") refId: RequestBody,
        @Part("ref") ref: RequestBody,
        @Part("field") field: RequestBody,
        @Header("Authorization") token: String
    ): Response<List<UploadResponse>>

    @PUT("movies-users/{id}")
    suspend fun updateMoviesUser(
        @Path("id") id: Int,
        @Body payload: UpdateMoviesUserPayload,
        @Header("Authorization") token: String
    ): Response<MoviesUserResponse>
}