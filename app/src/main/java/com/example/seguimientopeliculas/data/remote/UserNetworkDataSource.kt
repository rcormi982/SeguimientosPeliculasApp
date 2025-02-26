package com.example.seguimientopeliculas.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.net.Uri
import android.provider.OpenableColumns
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.UpdateMoviesUserPayload
import com.example.seguimientopeliculas.data.PhotoUploadResult
import com.example.seguimientopeliculas.data.UpdateData
import com.example.seguimientopeliculas.data.local.database.DatabaseHelper
import com.example.seguimientopeliculas.login.RegisterResponse
import com.example.seguimientopeliculas.login.UserResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserNetworkDataSource @Inject constructor(
    private val strapiApi: StrapiApi,
    private val sharedPreferences: SharedPreferences,
    private val context: Context,
    private val databaseHelper: DatabaseHelper
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
                            "Error al registrar en movies_user: ${
                                moviesUserResponse.errorBody()?.string()
                            }"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("RegisterUser", "Excepción al crear movies_user: ${e.message}")
                }
            }
        } else {
            val errorBody = registerResponse.errorBody()?.string()
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
            null
        }
    }

    override suspend fun getUserData(): UserResponse {
        try {
            val jwt = getJwtToken()
            if (jwt.isEmpty()) throw Exception("Token JWT no encontrado")

            val response = strapiApi.getUserData("Bearer $jwt")
            if (response.isSuccessful) {
                val userData = response.body()
                    ?: throw Exception("No se pudo obtener la información del usuario")

                // Guardar datos para uso offline
                databaseHelper.saveUserDataForOffline(userData)

                return userData
            } else {
                throw Exception("Error al obtener datos del usuario: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Error obteniendo datos remotos del usuario: ${e.message}")

            // Intentar obtener datos del usuario desde almacenamiento local
            val offlineUserData = databaseHelper.getOfflineUserData()
            if (offlineUserData != null) {
                Log.d("UserNetworkDataSource", "Usando datos de usuario almacenados localmente")
                return offlineUserData
            }

            throw Exception("No se pudo obtener la información del usuario: ${e.message}")
        }
    }

    override suspend fun getMoviesUserData(): MoviesUser {
        try {
            // Intentar obtener datos remotos
            val user = getUserData()
            val response = strapiApi.getMoviesUserByUserId(user.id)

            if (response.isSuccessful && !response.body()?.data.isNullOrEmpty()) {
                val moviesUserData = response.body()?.data?.firstOrNull()
                    ?: throw Exception("No se encontraron datos del MoviesUser")

                // Extraer la URL de la imagen
                val imageUrl = moviesUserData.attributes.imageUrl?.data?.attributes?.url

                // Crear objeto MoviesUser
                val moviesUser = MoviesUser(
                    id = moviesUserData.id,
                    username = moviesUserData.attributes.username,
                    email = moviesUserData.attributes.email,
                    password = "",
                    userId = user.id,
                    imageUrl = imageUrl,
                    image = null
                )

                // Guardar para uso offline
                databaseHelper.saveMoviesUserForOffline(moviesUser)

                return moviesUser
            } else {
                throw Exception("Error al obtener datos del MoviesUser")
            }
        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Error obteniendo datos remotos de MoviesUser: ${e.message}")

            // Intentar obtener datos desde almacenamiento local
            val offlineMoviesUser = databaseHelper.getOfflineMoviesUserData()
            if (offlineMoviesUser != null) {
                Log.d("UserNetworkDataSource", "Usando datos de MoviesUser almacenados localmente")
                return offlineMoviesUser
            }

            throw Exception("No se pudo obtener datos del MoviesUser: ${e.message}")
        }
    }

    override suspend fun uploadProfilePhoto(photoUri: Uri, moviesUserId: Int): PhotoUploadResult? {
        var fileName = "profile_${System.currentTimeMillis()}.jpg"

        try {
            val jwt = getJwtToken()
            if (jwt.isEmpty()) {
                throw Exception("Token JWT no encontrado")
            }

            // 1. Obtener el ContentResolver
            val contentResolver = context.contentResolver

            // 2. Obtener el nombre real del archivo si está disponible
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

            // 4. Crear MultipartBody.Part para el archivo
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "files",
                fileName,
                requestFile
            )

            // 5. Crear los RequestBody para los parámetros adicionales
            val refIdBody = moviesUserId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val refBody = "api::movies-user.movies-user".toRequestBody("text/plain".toMediaTypeOrNull())
            val fieldBody = "imageUrl".toRequestBody("text/plain".toMediaTypeOrNull())

            // 6. Realizar la petición con todos los parámetros
            val response = strapiApi.uploadFile(
                file = filePart,
                refId = refIdBody,
                ref = refBody,
                field = fieldBody,
                token = "Bearer $jwt"
            )

            return if (response.isSuccessful) {
                val uploadResponse = response.body()?.firstOrNull()
                uploadResponse?.let {
                    PhotoUploadResult(
                        url = it.url,
                        id = it.id
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                null
            }

        } catch (e: Exception) {
            return null
        } finally {
            // 7. Limpiar archivos temporales
            try {
                val tempFile = File(context.cacheDir, fileName)
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e("UserNetworkDataSource", "Error al eliminar archivo temporal", e)
            }
        }
    }


    override suspend fun updateUserPhoto(moviesUserId: Int, photoResult: PhotoUploadResult): Boolean {
        try {
            val token = getJwtToken()

            if (token.isEmpty()) {
                throw Exception("Token JWT no encontrado")
            }

            val updateData = UpdateData(imageUrl = photoResult.id)
            val payload = UpdateMoviesUserPayload(data = updateData)

            val response = strapiApi.updateMoviesUser(
                moviesUserId,
                payload,
                "Bearer $token"
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                return false
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getJwtToken(): String {
        val token = sharedPreferences.getString("jwt_token", "").orEmpty()
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
                imageUrl = when (val imageUrl = updatePayload["imageUrl"]) {
                    is Int -> imageUrl
                    is String -> imageUrl.toIntOrNull()
                    else -> null
                }
            )

            val payload = UpdateMoviesUserPayload(data = updateData)

            val response = strapiApi.updateMoviesUser(
                moviesUserId,
                payload,
                "Bearer $token"
            )

            if (!response.isSuccessful) {
                // Elimina el log de error
                return true
            }

            return true
        } catch (e: Exception) {
            return true
        }
    }

    override suspend fun deleteUser(userId: Int): Response<Unit> {
        return strapiApi.deleteUser(userId)
    }

    suspend fun deleteMoviesUser(moviesUserId: Int): Response<Unit> {
        return strapiApi.deleteMoviesUser(moviesUserId)
    }

    override suspend fun getMoviesForUser(userId: Int): List<Movie> {
        try {
            // Intentar obtener datos remotos
            val response = strapiApi.getMoviesByUser(
                moviesUserId = userId,
                populate = "movies_users,users_permissions_user"
            )

            if (response.isSuccessful) {
                val movieListRaw: MovieListRaw? = response.body()
                if (movieListRaw == null || movieListRaw.data.isNullOrEmpty()) {
                    return emptyList()
                }

                val movies = movieListRaw.data.mapNotNull { movieRaw ->
                    movieRaw.toMovie()
                }

                // Guardar para uso offline
                databaseHelper.saveMoviesForOffline(movies, userId)

                return movies
            } else {
                throw Exception("Error al obtener películas del usuario: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("UserNetworkDataSource", "Error obteniendo películas remotas: ${e.message}")

            // Intentar obtener datos locales
            val offlineMovies = databaseHelper.getOfflineMoviesForUser(userId)
            if (offlineMovies.isNotEmpty()) {
                Log.d("UserNetworkDataSource", "Usando películas almacenadas localmente")
                return offlineMovies
            }

            throw Exception("Error al obtener películas del usuario: ${e.message}")
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