import android.content.SharedPreferences
import android.util.Log
import com.example.seguimientopeliculas.data.remote.MovieListRaw
import com.example.seguimientopeliculas.data.remote.MoviePostRequest
import com.example.seguimientopeliculas.data.remote.MovieRaw
import com.example.seguimientopeliculas.data.remote.MovieRemoteDataSource
import com.example.seguimientopeliculas.data.remote.StrapiApi
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieNetworkDataSource @Inject constructor(
    private val strapiApi: StrapiApi,
    private val sharedPreferences: SharedPreferences,
) : MovieRemoteDataSource {

    override suspend fun getMovies(): Response<MovieListRaw> {
        val response = strapiApi.getMovies()
        if (response.isSuccessful) {
            return response
        } else {
            throw Exception("Error al obtener películas: ${response.message()}")
        }
    }


    override suspend fun createMovie(moviePayload: MoviePostRequest, jwt: String): Response<MovieRaw> {
        try {
            // Llamada al API utilizando el endpoint que soporta `@Query` para `populate`
            val response = strapiApi.createMovieForUser(
                moviePayload = moviePayload,
                populate = "movies_users,users_permissions_user"
            )
            // Verificar la respuesta del servidor
            if (response.isSuccessful) {
                return response
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Log.e(
                    "MovieNetworkDataSource",
                    "Error al crear película: Código ${response.code()}, Cuerpo: $errorBody"
                )
                throw Exception("Error en la creación de película: Código ${response.code()}, $errorBody")
            }
        } catch (e: Exception) {
            throw Exception("Error inesperado al crear la película: ${e.message}")
        }
    }


    override suspend fun getUserMovies(userId: Int): Response<MovieListRaw> {
        val token = obtenerToken()
        if (token.isEmpty()) throw Exception("Token JWT no encontrado")

        val response = strapiApi.getMoviesByUser(
            moviesUserId = userId,
            populate = "movies_users,users_permissions_user"
        )

        if (response.isSuccessful) {
            val movieListRaw = response.body() // Esto mapeará la respuesta a MovieListRaw
            return Response.success(movieListRaw)
        } else {
            val errorBody = response.errorBody()?.string()
            throw Exception("Error al obtener películas del usuario: $errorBody")
        }
    }

    override suspend fun updateMovie(movieId: Int, moviePayload: MoviePostRequest, jwt: String): Response<MovieRaw> {
        val response = strapiApi.updateMovie(movieId, moviePayload)
        if (!response.isSuccessful) {
            Log.d("MovieNetworkDataSource", "Película actualizada correctamente en el backend.")
        } else {
            Log.e(
                "MovieNetworkDataSource",
                "Error al actualizar película: Código ${response.code()} - ${response.errorBody()?.string()}"
            )
        }
        return response
    }

    override suspend fun deleteMovie(movieId: Int, jwt: String): Response<Unit> {
        try {
            // Llamada al API para eliminar la película
            val response = strapiApi.deleteMovie(movieId = movieId)

            if (response.isSuccessful) {
                return Response.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                throw Exception("Error al eliminar película: Código ${response.code()}, $errorBody")
            }
        } catch (e: Exception) {
            throw Exception("Error inesperado al eliminar la película: ${e.message}")
        }
    }


    private fun obtenerToken(): String {
        val token = sharedPreferences.getString("jwt_token", "").orEmpty()
        return token
    }

    override suspend fun getJwtToken(): String {
        return obtenerToken()
    }

    override suspend fun getGenresAndStatuses(): Pair<List<String>, List<String>> {
        val response = strapiApi.getMovies()
        if (response.isSuccessful) {
            val movies = response.body()?.data ?: emptyList()

            // Agregar géneros y estados únicos
            val genres = movies.mapNotNull { it.attributes?.Genre }.distinct()
            val statuses = movies.mapNotNull { it.attributes?.Status }.distinct()

            return Pair(genres, statuses)
        } else {
            val errorBody = response.errorBody()?.string() ?: "Error desconocido"
            throw Exception("Error al obtener géneros y estados: ${response.message()}")
        }
    }
}
