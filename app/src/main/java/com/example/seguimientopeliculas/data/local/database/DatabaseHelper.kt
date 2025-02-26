package com.example.seguimientopeliculas.data.local.database

import android.content.Context
import android.util.Log
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.login.UserResponse
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
                // 1. Obtener la lista actual de películas del usuario
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                val oldMovieIds = sharedPrefs.getStringSet("user_${userId}_movies", emptySet()) ?: emptySet()
                val oldMovieIdsInts = oldMovieIds.mapNotNull { it.toIntOrNull() }.toSet()

                // 2. Obtener la nueva lista de películas
                val newMovieIdsInts = movies.map { it.id }.toSet()

                // 3. Determinar qué películas deben eliminarse (están en la lista vieja pero no en la nueva)
                val moviesToDelete = oldMovieIdsInts - newMovieIdsInts

                // 4. Eliminar películas que ya no existen en el servidor
                for (movieId in moviesToDelete) {
                    try {
                        appDatabase.movieDao().deleteFilmById(movieId)
                        Log.d("DatabaseHelper", "Película eliminada: $movieId")
                    } catch (e: Exception) {
                        Log.e("DatabaseHelper", "Error eliminando película: ${e.message}")
                    }
                }

                // 5. Guardar nuevas películas o actualizar existentes
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
                        Log.d("DatabaseHelper", "Película guardada: ${movie.title}")
                    } catch (e: Exception) {
                        try {
                            appDatabase.movieDao().updateFilm(movieEntity)
                            Log.d("DatabaseHelper", "Película actualizada: ${movie.title}")
                        } catch (e2: Exception) {
                            Log.e("DatabaseHelper", "Error guardando película: ${e2.message}")
                        }
                    }
                }

                // 6. Guardar la nueva lista de películas
                with(sharedPrefs.edit()) {
                    val newMovieIds = movies.map { it.id.toString() }.toSet()
                    putStringSet("user_${userId}_movies", newMovieIds)
                    apply()
                }

                Log.d("DatabaseHelper", "Sincronización offline completada: ${movies.size} películas guardadas, ${moviesToDelete.size} eliminadas")
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

    // Añadir estos métodos a DatabaseHelper.kt

    suspend fun saveUserDataForOffline(userData: UserResponse) {
        withContext(Dispatchers.IO) {
            try {
                // Guardar datos básicos del usuario en SharedPreferences
                val sharedPrefs = context.getSharedPreferences("offline_user", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putInt("user_id", userData.id)
                    putString("username", userData.username)
                    putString("email", userData.email)
                    apply()
                }

                Log.d("DatabaseHelper", "Datos del usuario guardados para uso offline")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error guardando datos del usuario: ${e.message}")
            }
        }
    }

    suspend fun getOfflineUserData(): UserResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPrefs = context.getSharedPreferences("offline_user", Context.MODE_PRIVATE)
                val userId = sharedPrefs.getInt("user_id", -1)

                if (userId == -1) {
                    return@withContext null
                }

                val username = sharedPrefs.getString("username", "") ?: ""
                val email = sharedPrefs.getString("email", "") ?: ""

                // Crear objeto UserResponse con los datos almacenados
                UserResponse(
                    id = userId,
                    username = username,
                    email = email,
                    provider = "local",
                    confirmed = true,
                    blocked = false,
                    createdAt = "",
                    updatedAt = "",
                    movies_user = null
                )
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error obteniendo datos del usuario: ${e.message}")
                null
            }
        }
    }

    // Añadir a DatabaseHelper.kt

    suspend fun saveMoviesUserForOffline(moviesUser: MoviesUser) {
        withContext(Dispatchers.IO) {
            try {
                val sharedPrefs = context.getSharedPreferences("offline_moviesuser", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putInt("id", moviesUser.id)
                    putString("username", moviesUser.username)
                    putString("email", moviesUser.email)
                    putInt("userId", moviesUser.userId)
                    putString("imageUrl", moviesUser.imageUrl ?: "")
                    apply()
                }

                Log.d("DatabaseHelper", "Datos de MoviesUser guardados para modo offline")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error guardando datos de MoviesUser: ${e.message}")
            }
        }
    }

    suspend fun getOfflineMoviesUserData(): MoviesUser? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPrefs = context.getSharedPreferences("offline_moviesuser", Context.MODE_PRIVATE)
                val id = sharedPrefs.getInt("id", -1)

                if (id == -1) {
                    return@withContext null
                }

                val username = sharedPrefs.getString("username", "") ?: ""
                val email = sharedPrefs.getString("email", "") ?: ""
                val userId = sharedPrefs.getInt("userId", -1)
                val imageUrl = sharedPrefs.getString("imageUrl", "")

                if (userId == -1) {
                    return@withContext null
                }

                MoviesUser(
                    id = id,
                    username = username,
                    email = email,
                    password = "",
                    userId = userId,
                    imageUrl = if (imageUrl.isNullOrEmpty()) null else imageUrl,
                    image = null
                )
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error obteniendo datos de MoviesUser: ${e.message}")
                null
            }
        }
    }
}