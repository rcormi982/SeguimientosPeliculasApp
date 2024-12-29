package com.example.seguimientopeliculas.data.remote

import MovieNetworkDataSource
import android.content.SharedPreferences
import android.util.Log
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.login.RegisterResponse
import com.example.seguimientopeliculas.login.UserResponse
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserNetworkDataSource @Inject constructor(
    private val strapiApi: StrapiApi,
    private val sharedPreferences: SharedPreferences,
    //private val movieNetworkDataSource: MovieNetworkDataSource
) : UserRemoteDataSource {

    override suspend fun registerUser(
        username: String,
        email: String,
        password: String
    ): Response<RegisterResponse> {
        val registerRequest = RegisterPostRequest(
            username = username,
            email = email,
            password = password
        )

        val registerResponse = strapiApi.registerUser(registerRequest)

        if (registerResponse.isSuccessful) {
            val userId = registerResponse.body()?.user?.id
            if (userId != null) {
                try {
                    // Crea el registro correspondiente en movies_user
                    val moviesUserPayload = MoviesUserRequest(
                        data = RegisterUserData(
                            users_permissions_user = userId,
                            username = username,
                            email = email
                        )
                    )
                    val moviesUserResponse = strapiApi.createMoviesUser(moviesUserPayload)

                    if (!moviesUserResponse.isSuccessful) {
                        Log.e(
                            "RegisterUser",
                            "Error al registrar en movies_user: ${moviesUserResponse.errorBody()?.string()}"
                        )
                        // No lanzamos excepción, solo registramos el error para no interrumpir el flujo
                    }
                } catch (e: Exception) {
                    Log.e("RegisterUser", "Excepción al crear movies_user: ${e.message}")
                    // Evitamos que la excepción interrumpa el flujo
                }
            }
        } else {
            // Manejo de error explícito del registro en `users`
            val errorBody = registerResponse.errorBody()?.string()
            Log.e("RegisterUser", "Error al registrar usuario: $errorBody")
        }

        return registerResponse
    }


    override suspend fun fetchMoviesUserId(userId: Int): Int? {
        val response = strapiApi.getMoviesUserByUserId(userId)

        return if (response.isSuccessful) {
            val moviesUserLoginData = response.body()?.data?.firstOrNull()
            moviesUserLoginData?.id // Devuelve el ID de MoviesUser si existe
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e("UserNetworkDataSource", "Error al obtener MoviesUserId: $errorBody")
            null
        }
    }

    override suspend fun getUserData(): UserResponse {
        val jwt = getJwtToken()
        if (jwt.isEmpty()) throw Exception("Token JWT no encontrado")

        val response = strapiApi.getUserData("Bearer $jwt")
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("No se pudo obtener la información del usuario")
        } else {
            throw Exception("Error al obtener datos del usuario: ${response.message()}")
        }
    }

    private fun getJwtToken(): String {
        val token = sharedPreferences.getString("jwt_token", "").orEmpty()
        Log.d("MovieNetworkDataSource", "Token recuperado: $token")
        return token
    }

    override suspend fun updateUser(userId: Int, username: String, email: String): Response<UserResponse> {
        val updatePayload = mapOf(
            "username" to username,
            "email" to email
        )
        return strapiApi.updateUser(userId, updatePayload)
    }

    override suspend fun deleteUser(userId: Int): Response<Unit> {
        val response = strapiApi.deleteUser(userId)
        return response
    }

    suspend fun deleteMoviesUser(moviesUserId: Int): Response<Unit> {
        val response = strapiApi.deleteMoviesUser(moviesUserId)
        return response
    }


    override suspend fun getMoviesForUser(userId: Int): List<Movie> {
        // Llamada al endpoint con el token y filtros
        val response = strapiApi.getMoviesByUser(
            moviesUserId = userId,
            populate = "movies_users,users_permissions_user"
        )

        if (response.isSuccessful) {
            // Obtener el objeto MovieListRaw
            val movieListRaw: MovieListRaw? = response.body() // Mapear la respuesta a MovieListRaw
            if (movieListRaw == null || movieListRaw.data.isNullOrEmpty()) {
                // Si la respuesta está vacía o no tiene películas
                Log.e("UserNetworkDataSource", "No se encontraron películas en la respuesta.")
                return emptyList()
            }

            // Procesar los datos obtenidos para convertirlos en objetos Movie
            return movieListRaw.data.mapNotNull { movieRaw ->
                movieRaw.toMovie() // Convertimos cada MovieRaw a Movie
            }
        } else {
            // Manejar el caso de error
            val errorBody = response.errorBody()?.string()
            Log.e("UserNetworkDataSource", "Error en la respuesta: $errorBody")
            throw Exception("Error al obtener películas del usuario: $errorBody")
        }
    }

    override suspend fun createMovieForUser(userId: Int, movie: Movie, jwt: String): Boolean {
        val movieAttributes = MovieAttributes(
            Title = movie.title,
            Genre = movie.genre,
            Rating = movie.rating,
            Status = movie.status,
            Premiere = movie.premiere,
            Comments = movie.comments,
            movies_users = listOf(movie.moviesUserId ?: 0), // Asegurar que el backend recibe un Int
            //users_permissions_user = movie.moviesUserId ?: 0 // Usar un valor predeterminado si es nulo
        )

        val moviePayload = MoviePostRequest(
            data = movieAttributes
        )

        // Pasa el token JWT en la llamada a strapiApi
        val response = strapiApi.createMovieForUser(moviePayload, jwt)

        if (response.isSuccessful) {
            return true
        } else {
            val errorBody = response.errorBody()?.string()
            throw Exception("Error en la creación de película: $errorBody")
        }
    }

    private fun MovieRaw.toMovie(): Movie {
        return Movie(
            id = this.id,
            title = this.attributes?.Title ?: "Título no disponible",
            genre = this.attributes?.Genre ?: "Género desconocido",
            rating = this.attributes?.Rating ?: 0,
            premiere = this.attributes?.Premiere ?: false,
            status = this.attributes?.Status ?: "Desconocido",
            comments = this.attributes?.Comments ?: "",
        )
    }
}

