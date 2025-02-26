package com.example.seguimientopeliculas.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.seguimientopeliculas.data.local.database.AppDatabase
import com.example.seguimientopeliculas.data.local.database.DatabaseHelper
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserFilmEntity
import com.example.seguimientopeliculas.data.local.iLocalDataSource.IMoviesUserFilmLocalDataSource
import com.example.seguimientopeliculas.data.remote.MovieAttributes
import com.example.seguimientopeliculas.data.remote.MoviePostRequest
import com.example.seguimientopeliculas.data.remote.MovieRemoteDataSource
import com.example.seguimientopeliculas.data.remote.MovieRaw
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import android.net.ConnectivityManager

@Singleton
class DefaultMovieRepository @Inject constructor(
    private val movieRemoteDataSource: MovieRemoteDataSource,
    private val moviesUserFilmLocalDataSource: IMoviesUserFilmLocalDataSource,
    private val databaseHelper: DatabaseHelper,
    private val appDatabase: AppDatabase,
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

            if (isConnected) {
                // Intentar obtener datos remotos primero
                val response = movieRemoteDataSource.getUserMovies(userId)
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

                    // Guardar para uso offline
                    databaseHelper.saveMoviesForOffline(remoteMovies, userId)

                    _moviesStateFlow.emit(remoteMovies)
                    return remoteMovies
                }
            }

            Log.e("Repository", "Error obteniendo películas remotas: No hay conexión o respuesta fallida")
        } catch (e: Exception) {
            Log.e("Repository", "Error obteniendo películas remotas: ${e.message}")
        }

        // Cargar datos offline
        Log.d("Repository", "Intentando cargar películas desde la base de datos local")
        val offlineMovies = databaseHelper.getOfflineMoviesForUser(userId)
        if (offlineMovies.isNotEmpty()) {
            Log.d("Repository", "Cargadas ${offlineMovies.size} películas locales")
            _moviesStateFlow.emit(offlineMovies)
            return offlineMovies
        }

        Log.d("Repository", "No se encontraron películas locales para usuario $userId")
        return emptyList()
    }

    override suspend fun createMovie(movie: MoviePostRequest, jwt: String): Movie {
        val response = movieRemoteDataSource.createMovie(movie, jwt)
        if (response.isSuccessful) {
            return response.body()?.toMovie() ?: throw Exception("La película creada es nula.")
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

    // Añadir al DefaultMovieRepository
    suspend fun debugDatabaseState(userId: Int): String {
        return withContext(Dispatchers.IO) {
            val result = StringBuilder()
            try {
                // Verificar usuarios
                val userCount = appDatabase.userDao().getUserCount()
                result.appendLine("Total usuarios: $userCount")

                // Verificar películas
                val allMovies = appDatabase.movieDao().getAllFilms()
                result.appendLine("Total películas en BD: ${allMovies.size}")
                if (allMovies.isNotEmpty()) {
                    result.appendLine("Primera película: ${allMovies[0].title}")
                }

                // Verificar relaciones
                val relationCount = appDatabase.moviesUserFilmDao().countRelationsForUser(userId)
                result.appendLine("Relaciones para usuario $userId: $relationCount")

                // Verificar películas del usuario
                val userMovies = appDatabase.moviesUserFilmDao().getFilmsForUser(userId)
                result.appendLine("Películas del usuario $userId: ${userMovies.size}")

            } catch (e: Exception) {
                result.appendLine("Error en diagnóstico: ${e.message}")
            }
            result.toString()
        }
    }
}
