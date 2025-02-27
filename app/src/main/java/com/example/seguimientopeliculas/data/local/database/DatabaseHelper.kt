package com.example.seguimientopeliculas.data.local.database

import android.content.Context
import android.util.Log
import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserFilmEntity
import com.example.seguimientopeliculas.data.local.entities.UserEntity
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
                // 1. Guardar películas en base de datos local
                movies.forEach { movie ->
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
                        Log.d("DatabaseHelper", "Película insertada: ${movie.title}")
                    } catch (e: Exception) {
                        try {
                            appDatabase.movieDao().updateFilm(movieEntity)
                            Log.d("DatabaseHelper", "Película actualizada: ${movie.title}")
                        } catch (e2: Exception) {
                            Log.e("DatabaseHelper", "Error al actualizar película ${movie.title}: ${e2.message}")
                        }
                    }

                    // Crear la relación
                    try {
                        val relation = MoviesUserFilmEntity(
                            moviesUserId = userId,
                            filmId = movie.id
                        )
                        appDatabase.moviesUserFilmDao().insertRelation(relation)
                        Log.d("DatabaseHelper", "Relación creada para película: ${movie.id}")
                    } catch (e: Exception) {
                        Log.e("DatabaseHelper", "Error al crear relación para película ${movie.id}: ${e.message}")
                    }
                }

                // 2. Guardar IDs de películas en SharedPreferences para persistencia
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                val movieIds = movies.map { it.id.toString() }.toSet()

                with(sharedPrefs.edit()) {
                    putStringSet("user_${userId}_movies", movieIds)
                    apply()
                }

                Log.d("DatabaseHelper", "Películas guardadas para usuario $userId: $movieIds")

            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error en saveMoviesForOffline: ${e.message}")
            }
        }
    }

    suspend fun getOfflineMoviesForUser(userId: Int): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Intentar obtener de SharedPreferences
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                val movieIdsSet = sharedPrefs.getStringSet("user_${userId}_movies", emptySet()) ?: emptySet()

                Log.d("DatabaseHelper", "IDs de películas en SharedPreferences: $movieIdsSet")

                // 2. Si hay IDs en SharedPreferences, buscar en la base de datos
                if (movieIdsSet.isNotEmpty()) {
                    val movieIds = movieIdsSet.map { it.toInt() }
                    val movies = movieIds.mapNotNull { movieId ->
                        val movie = appDatabase.movieDao().getMovieById(movieId)
                        movie?.let {
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

                    Log.d("DatabaseHelper", "Películas offline recuperadas: ${movies.size}")
                    movies
                } else {
                    // 3. Si no hay películas, intentar obtener todas
                    val allMovies = appDatabase.movieDao().getAllFilms()

                    val movies = allMovies.map {
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

                    Log.d("DatabaseHelper", "Todas las películas recuperadas: ${movies.size}")
                    movies
                }
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error en getOfflineMoviesForUser: ${e.message}")
                emptyList()
            }
        }
    }

    // Método auxiliar para guardar relaciones usuario-película
    private suspend fun saveUserMovieRelations(userId: Int, movieIds: List<Int>) {
        try {
            // 1. Guardar el usuario primero (si no existe)
            val userEntity = try {
                appDatabase.userDao().getUserById(userId)
            } catch (e: Exception) {
                // Si no existe, crear uno básico (se actualizará después)
                UserEntity(
                    id = userId,
                    username = "User $userId",
                    email = "user$userId@example.com",
                    password = ""
                )
            }

            try {
                appDatabase.userDao().insertUser(userEntity)
            } catch (e: Exception) {
                // Ignorar si ya existe
            }

            // 2. Guardar MoviesUserEntity si no existe
            try {
                val moviesUserEntity = MoviesUserEntity(
                    id = userId,
                    user_Id = userId,
                    username = userEntity.username,
                    email = userEntity.email,
                    password = ""
                )
                appDatabase.moviesUserDao().insertMoviesUser(moviesUserEntity)
            } catch (e: Exception) {
                // Ignorar si ya existe
            }

            // 3. Guardar las relaciones película-usuario
            for (movieId in movieIds) {
                try {
                    val relation = MoviesUserFilmEntity(
                        moviesUserId = userId,
                        filmId = movieId
                    )
                    appDatabase.moviesUserFilmDao().insertRelation(relation)
                } catch (e: Exception) {
                    // Ignorar errores de duplicados
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error guardando relaciones: ${e.message}")
        }
    }

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
    suspend fun initializeDatabaseWithUserAndMovies(
        userId: Int,
        username: String,
        email: String,
        movies: List<Movie>
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("DatabaseHelper", "Inicializando base de datos para usuario: $userId")

                // 1. Insertar el UserEntity
                try {
                    val userEntity = UserEntity(
                        id = userId,
                        username = username,
                        email = email,
                        password = ""
                    )
                    appDatabase.userDao().insertUser(userEntity)
                    Log.d("DatabaseHelper", "Usuario guardado en la tabla user")
                } catch (e: Exception) {
                    Log.e("DatabaseHelper", "Error guardando UserEntity: ${e.message}")
                }

                // 2. Insertar el MoviesUserEntity
                try {
                    val moviesUserEntity = MoviesUserEntity(
                        id = userId,
                        user_Id = userId,
                        username = username,
                        email = email,
                        password = ""
                    )
                    appDatabase.moviesUserDao().insertMoviesUser(moviesUserEntity)
                    Log.d("DatabaseHelper", "Usuario guardado en la tabla movies_user")
                } catch (e: Exception) {
                    Log.e("DatabaseHelper", "Error guardando MoviesUserEntity: ${e.message}")
                }

                // 3. Insertar las películas y relaciones
                for (movie in movies) {
                    try {
                        // Guardar la película
                        val movieEntity = MovieEntity(
                            id = movie.id,
                            title = movie.title,
                            genre = movie.genre,
                            rating = movie.rating,
                            status = movie.status,
                            premiere = movie.premiere,
                            comments = movie.comments ?: ""
                        )

                        appDatabase.movieDao().insertFilm(movieEntity)
                        Log.d("DatabaseHelper", "Película guardada: ${movie.title}")

                        // Crear la relación
                        val relation = MoviesUserFilmEntity(
                            moviesUserId = userId,
                            filmId = movie.id
                        )

                        appDatabase.moviesUserFilmDao().insertRelation(relation)
                        Log.d("DatabaseHelper", "Relación creada para película: ${movie.id}")
                    } catch (e: Exception) {
                        Log.e("DatabaseHelper", "Error al procesar película ${movie.id}: ${e.message}")
                    }
                }

                Log.d("DatabaseHelper", "Inicialización de base de datos completada")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error general en initializeDatabaseWithUserAndMovies: ${e.message}")
            }
        }
    }
}