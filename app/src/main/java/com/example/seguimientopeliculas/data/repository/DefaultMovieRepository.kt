package com.example.seguimientopeliculas.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.seguimientopeliculas.data.local.database.LocalDatabase
import com.example.seguimientopeliculas.data.local.iLocalDataSource.IMoviesUserFilmLocalDataSource
import com.example.seguimientopeliculas.data.remote.models.MovieAttributes
import com.example.seguimientopeliculas.data.remote.models.MoviePostRequest
import com.example.seguimientopeliculas.data.remote.dataSource.MovieRemoteDataSource
import com.example.seguimientopeliculas.data.remote.models.MovieRaw
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.net.ConnectivityManager
import com.example.seguimientopeliculas.data.remote.models.Movie

@Singleton
class DefaultMovieRepository @Inject constructor(
    private val movieRemoteDataSource: MovieRemoteDataSource,
    private val moviesUserFilmLocalDataSource: IMoviesUserFilmLocalDataSource,
    private val localDatabase: LocalDatabase,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: Context
) : MovieRepository {

    private val _moviesStateFlow = MutableStateFlow<List<Movie>>(emptyList())
    override val setStream: StateFlow<List<Movie>>
        get() = _moviesStateFlow

    override fun observeAll(): Flow<List<Movie>> = _moviesStateFlow

    override suspend fun getMovies(): List<Movie> {
        try {
            // Intentar obtener datos del servidor
            val response = movieRemoteDataSource.getMovies()
            if (response.isSuccessful) {
                val movies = response.body()?.data?.mapNotNull { it.toMovie() } ?: emptyList()
                _moviesStateFlow.emit(movies)
                return movies
            }
        } catch (e: Exception) {
            // Error de red, intentar recuperar desde local
            Log.e("DefaultMovieRepository", "Error obteniendo películas del servidor: ${e.message}")
        }

        // Recuperar datos locales como fallback
        val userId = sharedPreferences.getInt("moviesUserId", -1)
        if (userId != -1) {
            return moviesUserFilmLocalDataSource.readAllRelationsForUser(userId)
        }

        return emptyList()
    }

    override suspend fun getUserMovies(userId: Int): List<Movie> {
        try {
            // Verificar conexión a internet
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo != null && networkInfo.isConnected

            Log.d("Repository", "Conexión a internet: $isConnected")

            if (isConnected) {
                // Intentar obtener datos remotos primero
                val response = movieRemoteDataSource.getUserMovies(userId)

                Log.d("Repository", "Respuesta del servidor: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val remoteMovies = response.body()?.data?.map {
                        Movie(
                            id = it.id,
                            title = it.attributes?.Title ?: "",
                            genre = it.attributes?.Genre ?: "",
                            rating = it.attributes?.Rating ?: 0,
                            status = it.attributes?.Status ?: "",
                            premiere = it.attributes?.Premiere ?: false,
                            comments = it.attributes?.Comments ?: "",
                            moviesUserId = userId
                        )
                    } ?: emptyList()

                    Log.d("Repository", "Películas remotas recibidas: ${remoteMovies.size}")

                    // Usar la nueva función de reset y sincronización
                    localDatabase.resetAndSyncMovies(userId, remoteMovies)

                    _moviesStateFlow.emit(remoteMovies)
                    return remoteMovies
                }
            }

            // Cargar datos offline
            Log.d("Repository", "Sin conexión o respuesta fallida. Cargando películas locales")

            val offlineMovies = localDatabase.getOfflineMoviesForUser(userId)

            Log.d("Repository", "Películas locales cargadas: ${offlineMovies.size}")

            if (offlineMovies.isNotEmpty()) {
                _moviesStateFlow.emit(offlineMovies)
                return offlineMovies
            }

            return emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "Error general: ${e.message}")
            return emptyList()
        }
    }

    override suspend fun createMovie(movie: MoviePostRequest, jwt: String): Movie {
        val response = movieRemoteDataSource.createMovie(movie, jwt)
        if (response.isSuccessful) {
            val createdMovie = response.body()?.toMovie()
                ?: throw Exception("La película creada es nula.")

            // Obtener el ID de usuario de las preferencias compartidas
            val userId = sharedPreferences.getInt("moviesUserId", -1)

            // Forzar sincronización si hay un usuario válido
            if (userId != -1) {
                getUserMovies(userId)
            }

            return createdMovie
        } else {
            throw Exception("Error al crear película: ${response.message()}")
        }
    }

    override suspend fun getGenresAndStatuses(): Pair<List<String>, List<String>> {
        val movies = getMovies()
        val genres = movies.mapNotNull { it.genre }.distinct() // Elimina valores nulos
        val statuses = movies.mapNotNull { it.status?.takeIf { it != "Desconocido" } }.distinct()
        return Pair(genres, statuses)
    }

    private fun MovieRaw.toMovie(): Movie {
        return Movie(
            id = this.id,
            title = this.attributes?.Title ?: "Título no disponible",
            genre = this.attributes?.Genre ?: "Género desconocido",
            rating = this.attributes?.Rating ?: 0,
            premiere = this.attributes?.Premiere ?: false,
            status = this.attributes?.Status ?: "Desconocido",
            comments = this.attributes?.Comments ?: ""
        )
    }

    override suspend fun updateMovie(movie: Movie): Boolean {
        try {
            val jwt = movieRemoteDataSource.getJwtToken()
            if (jwt.isEmpty()) throw Exception("Token JWT no encontrado")

            val movieUserId = obtenerMovieUserId()
            val moviePayload = MoviePostRequest(
                data = MovieAttributes(
                    Title = movie.title,
                    Genre = movie.genre,
                    Rating = movie.rating,
                    Premiere = movie.premiere,
                    Status = movie.status,
                    Comments = movie.comments,
                    movies_users = listOf(movieUserId)
                )
            )

            val response = movieRemoteDataSource.updateMovie(movie.id, moviePayload, jwt)
            return response.isSuccessful
        } catch (e: Exception) {
            Log.e("DefaultMovieRepository", "Error actualizando película: ${e.message}")
            return false
        }
    }

    private fun obtenerMovieUserId(): Int {
        val movieUserId = sharedPreferences.getInt("moviesUserId", -1)
        if (movieUserId == -1) {
            throw Exception("MoviesUserId no encontrado en SharedPreferences")
        }
        return movieUserId
    }


    override suspend fun deleteMovie(movieId: Int): Boolean {
        try {
            val jwt = movieRemoteDataSource.getJwtToken()
            if (jwt.isEmpty()) throw Exception("Token JWT no encontrado")

            val response = movieRemoteDataSource.deleteMovie(movieId, jwt)
            return response.isSuccessful
        } catch (e: Exception) {
            Log.e("DefaultMovieRepository", "Error eliminando película: ${e.message}")
            return false
        }
    }
}
