package com.example.seguimientopeliculas.data

import android.content.SharedPreferences
import android.util.Log
import com.example.seguimientopeliculas.data.remote.MovieAttributes
import com.example.seguimientopeliculas.data.remote.MoviePostRequest
import com.example.seguimientopeliculas.data.remote.MovieRemoteDataSource
import com.example.seguimientopeliculas.data.remote.MovieRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMovieRepository @Inject constructor(
    private val movieRemoteDataSource: MovieRemoteDataSource,
    private val sharedPreferences: SharedPreferences
) : MovieRepository {

    private val _moviesStateFlow = MutableStateFlow<List<Movie>>(emptyList())
    override val setStream: StateFlow<List<Movie>>
        get() = _moviesStateFlow

    override fun observeAll(): Flow<List<Movie>> = _moviesStateFlow

    override suspend fun getMovies(): List<Movie> {
        val response = movieRemoteDataSource.getMovies()
        return if (response.isSuccessful) {
            val movies = response.body()?.data?.mapNotNull { it.toMovie() } ?: emptyList()
            _moviesStateFlow.emit(movies)
            movies
        } else {
            Log.e("MovieRepository", "Error al obtener películas: ${response.message()}")
            emptyList()
        }
    }

    override suspend fun getUserMovies(userId: Int): List<Movie> {
        val response = movieRemoteDataSource.getUserMovies(userId)
        return if (response.isSuccessful) {
            val movies = response.body()?.data?.mapNotNull { it.toMovie() } ?: emptyList()
            Log.d("MovieRepository", "Películas del usuario $userId procesadas: $movies")
            _moviesStateFlow.emit(movies)
            movies
        } else {
            Log.e(
                "MovieRepository",
                "Error al obtener películas del usuario $userId: ${response.message()}"
            )
            emptyList()
        }
    }

    override suspend fun createMovie(movie: MoviePostRequest, jwt: String): Movie {
        val response = movieRemoteDataSource.createMovie(movie, jwt)

        return if (response.isSuccessful) {
            response.body()?.toMovie() ?: throw Exception("La película creada es nula.")
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
        return try {
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
                    movies_users = listOf(movieUserId) // Usar el ID dinámico
                )
            )

            Log.d("DefaultMovieRepository", "Payload enviado: $moviePayload con movieUserId: $movieUserId")

            val response = movieRemoteDataSource.updateMovie(movie.id, moviePayload, jwt)

            if (response.isSuccessful) {
                Log.d("DefaultMovieRepository", "Película actualizada correctamente.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DefaultMovieRepository", "Error al actualizar película: ${e.message}")
            false
        }
    }
    private fun obtenerMovieUserId(): Int {
        val movieUserId = sharedPreferences.getInt("moviesUserId", -1)
        Log.d("DefaultMovieRepository", "Obteniendo moviesUserId desde SharedPreferences: $movieUserId")
        if (movieUserId == -1) {
            throw Exception("MoviesUserId no encontrado en SharedPreferences")
        }
        return movieUserId
    }


    override suspend fun deleteMovie(movieId: Int): Boolean {
        return try {
            val jwt = movieRemoteDataSource.getJwtToken() // Método para obtener el token JWT
            if (jwt.isEmpty()) throw Exception("Token JWT no encontrado")

            val response = movieRemoteDataSource.deleteMovie(movieId, jwt)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error al eliminar película: ${e.message}")
            false
        }
    }
}
