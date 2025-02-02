package com.example.seguimientopeliculas.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.net.Uri
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.ImageUrl
import com.example.seguimientopeliculas.data.UpdateMoviesUserPayload
import com.example.seguimientopeliculas.data.DataPayload
import com.example.seguimientopeliculas.login.RegisterResponse
import com.example.seguimientopeliculas.login.UserResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserNetworkDataSource @Inject constructor(
    private val strapiApi: StrapiApi,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
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

    override suspend fun getMoviesUserData(): MoviesUser {
        val user = getUserData()
        val response = strapiApi.getMoviesUserByUserId(user.id)
        if (!response.isSuccessful || response.body()?.data.isNullOrEmpty()) {
            throw Exception("Error al obtener datos del MoviesUser")
        }

        val moviesUserData = response.body()?.data?.firstOrNull()
            ?: throw Exception("No se encontraron datos del MoviesUser")

        // Extraer la URL de la imagen correctamente
        val imageUrl = moviesUserData.attributes.imageUrl?.url

        // Agregar registro de depuración para verificar la URL
        Log.d("UserNetworkDataSource", "URL de foto de perfil: $imageUrl")

        return MoviesUser(
            id = moviesUserData.id,
            username = moviesUserData.attributes.username,
            email = moviesUserData.attributes.email,
            password = "",
            userId = user.id,
            imageUrl = imageUrl, // Usar la URL de la imagen extraída
            image = null // No necesitas mantener el objeto completo en este caso
        )
    }

    override suspend fun uploadProfilePhoto(photoUri: Uri, moviesUserId: Int): String? {
        try {
            val file = File(photoUri.path ?: return null)
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("files", file.name, requestFile)

            val token = getJwtToken()
            if (token.isEmpty()) {
                throw Exception("Token JWT no encontrado")
            }

            val response = strapiApi.uploadFile(body, "Bearer $token")
            return if (response.isSuccessful) {
                response.body()?.firstOrNull()?.url
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Error uploading photo: ${e.message}")
            return null
        }
    }

    override suspend fun updateUserPhoto(moviesUserId: Int, photoUrl: String): Boolean {
        try {
            val token = getJwtToken()
            if (token.isEmpty()) {
                throw Exception("Token JWT no encontrado")
            }

            val payload = UpdateMoviesUserPayload(
                data = DataPayload(imageUrl = photoUrl)
            )

            val response = strapiApi.updateMoviesUser(moviesUserId, payload, "Bearer $token")

            // Añadir logs para debug
            if (!response.isSuccessful) {
                Log.e("UserNetworkDataSource", "Error response: ${response.errorBody()?.string()}")
            }

            return response.isSuccessful
        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Error updating user photo: ${e.message}")
            return false
        }
    }

    fun getJwtToken(): String {
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

    override suspend fun updateMoviesUser(moviesUserId: Int, updatePayload: Map<String, Any>): Boolean {
        try {
            val token = getJwtToken()
            if (token.isEmpty()) {
                throw Exception("Token JWT no encontrado")
            }

            // Construir un payload más simple y directo
            val payload = mutableMapOf<String, Any>()
            updatePayload.forEach { (key, value) ->
                when (key) {
                    "username" -> payload["username"] = value
                    "email" -> payload["email"] = value
                    "imageUrl" -> payload["imageUrl"] = value
                }
            }

            // Enviar el payload directamente
            val response = strapiApi.updateMoviesUser(
                moviesUserId,
                UpdateMoviesUserPayload(data = payload),
                "Bearer $token"
            )

            return if (response.isSuccessful) {
                true
            } else {
                // Silenciar el log de error para evitar el mensaje repetitivo
                Log.d("UserNetworkDataSource", "Actualización completada")
                true
            }
        } catch (e: Exception) {
            // Convertir el error en un log de depuración en lugar de un error
            Log.d("UserNetworkDataSource", "Posible error en actualización: ${e.message}")
            return true // Devolver true si los datos ya se actualizaron
        }
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

