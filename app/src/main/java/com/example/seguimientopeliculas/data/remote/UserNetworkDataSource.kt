package com.example.seguimientopeliculas.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.net.Uri
import android.provider.OpenableColumns
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.UpdateMoviesUserPayload
import com.example.seguimientopeliculas.data.ImageAttributes
import com.example.seguimientopeliculas.data.ImageUrlData
import com.example.seguimientopeliculas.data.UpdateData
import com.example.seguimientopeliculas.data.UpdateImageUrl
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
                    }
                } catch (e: Exception) {
                    Log.e("RegisterUser", "Excepción al crear movies_user: ${e.message}")
                }
            }
        } else {
            val errorBody = registerResponse.errorBody()?.string()
            Log.e("RegisterUser", "Error al registrar usuario: $errorBody")
        }

        return registerResponse
    }

    override suspend fun fetchMoviesUserId(userId: Int): Int? {
        val response = strapiApi.getMoviesUserByUserId(userId)

        return if (response.isSuccessful) {
            val moviesUserLoginData = response.body()?.data?.firstOrNull()
            moviesUserLoginData?.id
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

        // Extraer la URL de la imagen correctamente siguiendo la estructura anidada
        val imageUrl = moviesUserData.attributes.imageUrl?.data?.attributes?.url

        // Agregar registro de depuración para verificar la URL
        Log.d("UserNetworkDataSource", "URL de foto de perfil: $imageUrl")

        return MoviesUser(
            id = moviesUserData.id,
            username = moviesUserData.attributes.username,
            email = moviesUserData.attributes.email,
            password = "",
            userId = user.id,
            imageUrl = imageUrl,
            image = null
        )
    }

    override suspend fun uploadProfilePhoto(photoUri: Uri, moviesUserId: Int): String? {
        var fileName = "profile_${System.currentTimeMillis()}.jpg"

        try {
            val jwt = getJwtToken()
            Log.d("UserNetworkDataSource", "Token para subida de foto: $jwt")
            Log.d("UserNetworkDataSource", "Token length: ${jwt.length}")
            Log.d("UserNetworkDataSource", "MoviesUserId: $moviesUserId")

            if (jwt.isEmpty()) {
                Log.e("UserNetworkDataSource", "Token JWT no encontrado")
                throw Exception("Token JWT no encontrado")
            }

            // 1. Obtener el ContentResolver
            val contentResolver = context.contentResolver

            // 2. Obtener el nombre real del archivo
            val cursor = contentResolver.query(photoUri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor != null && cursor.moveToFirst() && nameIndex != null) {
                fileName = cursor.getString(nameIndex)
            }
            cursor?.close()

            // 3. Crear el archivo temporal y copiar el contenido
            val inputStream = contentResolver.openInputStream(photoUri)
            val file = File(context.cacheDir, fileName)

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 4. Crear MultipartBody.Part
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "files",
                fileName,
                requestFile
            )

            Log.d("UserNetworkDataSource", "Enviando archivo: $fileName")
            Log.d("UserNetworkDataSource", "Tamaño del archivo: ${file.length()} bytes")

            // 5. Realizar la petición de subida de archivo
            val response = strapiApi.uploadFile(filePart, "Bearer $jwt")

            // 6. Manejar la respuesta
            return if (response.isSuccessful) {
                val uploadResponse = response.body()?.firstOrNull()
                Log.d("UserNetworkDataSource", "Subida exitosa. Respuesta: $uploadResponse")
                uploadResponse?.url
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserNetworkDataSource", "Error en la subida de foto: $errorBody")
                null
            }

        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Excepción en la subida de foto", e)
            return null
        }
    }

    override suspend fun updateUserPhoto(moviesUserId: Int, photoUrl: String): Boolean {
        try {
            val token = getJwtToken()
            Log.d("UserNetworkDataSource", "Token para actualización de foto: $token")
            Log.d("UserNetworkDataSource", "URL de foto a actualizar: $photoUrl")
            Log.d("UserNetworkDataSource", "MoviesUserId: $moviesUserId")

            if (token.isEmpty()) {
                throw Exception("Token JWT no encontrado")
            }

            val updateData = UpdateData(
                imageUrl = UpdateImageUrl(
                    data = ImageUrlData(
                        attributes = ImageAttributes(
                            url = photoUrl
                        )
                    )
                )
            )

            val payload = UpdateMoviesUserPayload(data = updateData)

            val response = strapiApi.updateMoviesUser(
                moviesUserId,
                payload,
                "Bearer $token"
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e("UserNetworkDataSource", "Error al actualizar foto: $errorBody")
                Log.e("UserNetworkDataSource", "Payload enviado: $payload")
                return false
            }

            return true
        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Excepción al actualizar foto: ${e.message}")
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

            // Preparar el payload de actualización
            val updateData = UpdateData(
                username = updatePayload["username"] as? String,
                email = updatePayload["email"] as? String,
                imageUrl = updatePayload["imageUrl"]?.let { url ->
                    UpdateImageUrl(
                        data = ImageUrlData(
                            attributes = ImageAttributes(
                                url = url as String
                            )
                        )
                    )
                }
            )

            val payload = UpdateMoviesUserPayload(data = updateData)

            val response = strapiApi.updateMoviesUser(
                moviesUserId,
                payload,
                "Bearer $token"
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e("UserNetworkDataSource", "Error en actualización: $errorBody")
                return false
            }

            return true
        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Error en actualización: ${e.message}")
            return false
        }
    }
    override suspend fun deleteUser(userId: Int): Response<Unit> {
        return strapiApi.deleteUser(userId)
    }

    suspend fun deleteMoviesUser(moviesUserId: Int): Response<Unit> {
        return strapiApi.deleteMoviesUser(moviesUserId)
    }

    override suspend fun getMoviesForUser(userId: Int): List<Movie> {
        val response = strapiApi.getMoviesByUser(
            moviesUserId = userId,
            populate = "movies_users,users_permissions_user"
        )

        if (response.isSuccessful) {
            val movieListRaw: MovieListRaw? = response.body()
            if (movieListRaw == null || movieListRaw.data.isNullOrEmpty()) {
                Log.e("UserNetworkDataSource", "No se encontraron películas en la respuesta.")
                return emptyList()
            }

            return movieListRaw.data.mapNotNull { movieRaw ->
                movieRaw.toMovie()
            }
        } else {
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
            movies_users = listOf(movie.moviesUserId ?: 0)
        )

        val moviePayload = MoviePostRequest(
            data = movieAttributes
        )

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