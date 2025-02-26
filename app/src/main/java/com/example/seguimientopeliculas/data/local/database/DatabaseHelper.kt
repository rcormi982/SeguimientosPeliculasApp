package com.example.seguimientopeliculas.data.local.database

import android.content.Context
import android.util.Log
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DatabaseHelper @Inject constructor(
    private val appDatabase: AppDatabase,
    private val context: Context
) {
    // Esta función intentará guardar películas sin depender de las relaciones
    suspend fun saveMoviesForOffline(movies: List<Movie>, userId: Int) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Guardar todas las películas
                for (movie in movies) {
                    val movieEntity = MovieEntity(
                        id = movie.id,
                        title = movie.title,
                        genre = movie.genre,
                        rating = movie.rating,
                        status = movie.status,
                        premiere = movie.premiere,
                        comments = movie.comments ?: ""
                    )

                    try {
                        appDatabase.movieDao().insertFilm(movieEntity)
                    } catch (e: Exception) {
                        // Intenta actualizar si ya existe
                        try {
                            appDatabase.movieDao().updateFilm(movieEntity)
                        } catch (e2: Exception) {
                            Log.e("DatabaseHelper", "Error guardando película: ${e2.message}")
                        }
                    }
                }

                // Guardar userId en preferencias para usar como referencia
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    // Guardar IDs de películas para este usuario
                    val movieIds = movies.map { it.id.toString() }.toSet()
                    putStringSet("user_${userId}_movies", movieIds)
                    apply()
                }

                Log.d("DatabaseHelper", "Guardadas ${movies.size} películas para modo offline")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error en saveMoviesForOffline: ${e.message}")
            }
        }
    }

    suspend fun getOfflineMoviesForUser(userId: Int): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                // Obtener IDs de películas para este usuario
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                val movieIds = sharedPrefs.getStringSet("user_${userId}_movies", emptySet()) ?: emptySet()

                if (movieIds.isEmpty()) {
                    // Si no hay IDs guardados, intentar obtener todas las películas como fallback
                    val allMovies = appDatabase.movieDao().getAllFilms()
                    return@withContext allMovies.map {
                        Movie(
                            id = it.id,
                            title = it.title,
                            genre = it.genre,
                            rating = it.rating,
                            status = it.status,
                            premiere = it.premiere,
                            comments = it.comments ?: "",
                            moviesUserId = userId
                        )
                    }
                }

                // Obtener películas según los IDs guardados
                val moviesList = mutableListOf<Movie>()
                for (idStr in movieIds) {
                    try {
                        val id = idStr.toInt()
                        val movie = appDatabase.movieDao().getMovieById(id) // Necesitarás añadir este método
                        if (movie != null) {
                            moviesList.add(
                                Movie(
                                    id = movie.id,
                                    title = movie.title,
                                    genre = movie.genre,
                                    rating = movie.rating,
                                    status = movie.status,
                                    premiere = movie.premiere,
                                    comments = movie.comments ?: "",
                                    moviesUserId = userId
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("DatabaseHelper", "Error obteniendo película con ID $idStr: ${e.message}")
                    }
                }

                return@withContext moviesList
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error en getOfflineMoviesForUser: ${e.message}")
                return@withContext emptyList<Movie>()
            }
        }
    }
}