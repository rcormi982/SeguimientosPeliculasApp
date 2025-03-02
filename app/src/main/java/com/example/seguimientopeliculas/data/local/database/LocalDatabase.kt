package com.example.seguimientopeliculas.data.local.database

import android.content.Context
import android.util.Log
import com.example.seguimientopeliculas.data.remote.models.Movie
import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserFilmEntity
import com.example.seguimientopeliculas.data.local.entities.UserEntity
import com.example.seguimientopeliculas.data.remote.models.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalDatabase @Inject constructor(
    private val appDatabase: AppDatabase,
    private val context: Context
) {
    // Esta función intentará guardar películas sin depender de las relaciones
    suspend fun saveMoviesForOffline(movies: List<Movie>, userId: Int) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Guardar siempre las películas en la tabla film
                movies.forEach { movie ->
                    try {
                        val movieEntity = MovieEntity(
                            id = movie.id,
                            title = movie.title,
                            genre = movie.genre,
                            rating = movie.rating,
                            status = movie.status,
                            premiere = movie.premiere,
                            comments = movie.comments ?: ""
                        )

                        // Insertar o actualizar película
                        appDatabase.movieDao().insertFilm(movieEntity)
                        Log.d("LocalDatabase", "Película insertada/actualizada: ${movie.title}")
                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "Error al insertar/actualizar película ${movie.title}: ${e.message}")
                    }
                }

                // 2. Verificar si el usuario existe (sin crear uno nuevo)
                val userEntity = try {
                    appDatabase.userDao().getUserById(userId)
                    true
                } catch (e: Exception) {
                    Log.e("LocalDatabase", "No se encontró el usuario con ID: $userId. No se crearán relaciones.")
                    false
                }

                // Solo crear relaciones si el usuario existe
                if (userEntity) {
                    // 3. Verificar si el MoviesUserEntity existe
                    val moviesUserExists = try {
                        appDatabase.moviesUserDao().getMoviesUserById(userId)
                        true
                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "No se encontró MoviesUserEntity con ID: $userId. No se crearán relaciones.")
                        false
                    }

                    // 4. Crear relaciones solo si tanto el usuario como el moviesUser existen
                    if (moviesUserExists) {
                        movies.forEach { movie ->
                            try {
                                // Verificar si la relación ya existe
                                val existingRelation = appDatabase.moviesUserFilmDao()
                                    .getRelationForUserAndFilm(userId, movie.id)

                                if (existingRelation == null) {
                                    val relation = MoviesUserFilmEntity(
                                        moviesUserId = userId,
                                        filmId = movie.id
                                    )
                                    appDatabase.moviesUserFilmDao().insertRelation(relation)
                                    Log.d("LocalDatabase", "Relación creada para película: ${movie.id}")
                                } else {
                                    Log.d("LocalDatabase", "Relación ya existente para película: ${movie.id}")
                                }
                            } catch (e: Exception) {
                                Log.e("LocalDatabase", "Error al crear relación para película ${movie.id}: ${e.message}")
                            }
                        }
                    }
                }

                // 5. Guardar IDs de películas en SharedPreferences (siempre)
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                val movieIds = movies.map { it.id.toString() }.toSet()

                with(sharedPrefs.edit()) {
                    putStringSet("user_${userId}_movies", movieIds)
                    apply()
                }

                Log.d("LocalDatabase", "Películas guardadas para usuario $userId: $movieIds")

            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error en saveMoviesForOffline: ${e.message}")
            }
        }
    }

    suspend fun getOfflineMoviesForUser(userId: Int): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("LocalDatabase", "Obteniendo películas offline para usuario: $userId")

                // Utilizar directamente la relación de la base de datos para obtener las películas
                val moviesFromRelations = try {
                    val films = appDatabase.moviesUserFilmDao().getFilmsForUser(userId)

                    val movies = films.map { movieEntity ->
                        Movie(
                            id = movieEntity.id,
                            title = movieEntity.title,
                            genre = movieEntity.genre,
                            rating = movieEntity.rating,
                            status = movieEntity.status,
                            premiere = movieEntity.premiere,
                            comments = movieEntity.comments ?: "",
                            moviesUserId = userId
                        )
                    }

                    Log.d("LocalDatabase", "Recuperadas ${movies.size} películas desde relaciones en la base de datos")
                    movies
                } catch (e: Exception) {
                    Log.e("LocalDatabase", "Error al obtener películas desde relaciones: ${e.message}")
                    emptyList()
                }

                // Si hay películas en las relaciones, devolverlas
                if (moviesFromRelations.isNotEmpty()) {
                    return@withContext moviesFromRelations
                }

                // Como fallback, intentar obtener desde SharedPreferences
                Log.d("LocalDatabase", "No se encontraron relaciones, intentando desde SharedPreferences")
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                val movieIdsSet = sharedPrefs.getStringSet("user_${userId}_movies", emptySet()) ?: emptySet()

                if (movieIdsSet.isNotEmpty()) {
                    Log.d("LocalDatabase", "IDs de películas en SharedPreferences: $movieIdsSet")

                    val movieIds = movieIdsSet.map { it.toInt() }
                    val movies = movieIds.mapNotNull { movieId ->
                        try {
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
                        } catch (e: Exception) {
                            Log.e("LocalDatabase", "Error al recuperar película $movieId: ${e.message}")
                            null
                        }
                    }

                    Log.d("LocalDatabase", "Películas recuperadas desde SharedPreferences: ${movies.size}")

                    // Si encontramos películas desde SharedPreferences, intentemos crear las relaciones
                    // para sincronizar la base de datos
                    if (movies.isNotEmpty()) {
                        createMissingRelations(userId, movies)
                    }

                    movies
                } else {
                    // Si no hay nada en SharedPreferences, intentar con todas las películas
                    val allMovies = appDatabase.movieDao().getAllFilms().map {
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

                    Log.d("LocalDatabase", "Recuperadas todas las películas como último recurso: ${allMovies.size}")
                    allMovies
                }
            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error general en getOfflineMoviesForUser: ${e.message}")
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
            Log.e("LocalDatabase", "Error guardando relaciones: ${e.message}")
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

                Log.d("LocalDatabase", "Datos del usuario guardados para uso offline")
            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error guardando datos del usuario: ${e.message}")
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
                Log.e("LocalDatabase", "Error obteniendo datos del usuario: ${e.message}")
                null
            }
        }
    }

    // Añadir a LocalDatabase.kt

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

                Log.d("LocalDatabase", "Datos de MoviesUser guardados para modo offline")
            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error guardando datos de MoviesUser: ${e.message}")
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
                Log.e("LocalDatabase", "Error obteniendo datos de MoviesUser: ${e.message}")
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
                Log.d("LocalDatabase", "Inicializando base de datos para usuario: $userId")

                // 1. Insertar el UserEntity
                try {
                    val userEntity = UserEntity(
                        id = userId,
                        username = username,
                        email = email,
                        password = ""
                    )
                    appDatabase.userDao().insertUser(userEntity)
                    Log.d("LocalDatabase", "Usuario guardado en la tabla user")
                } catch (e: Exception) {
                    Log.e("LocalDatabase", "Error guardando UserEntity: ${e.message}")
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
                    Log.d("LocalDatabase", "Usuario guardado en la tabla movies_user")
                } catch (e: Exception) {
                    Log.e("LocalDatabase", "Error guardando MoviesUserEntity: ${e.message}")
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
                        Log.d("LocalDatabase", "Película guardada: ${movie.title}")

                        // Crear la relación
                        val relation = MoviesUserFilmEntity(
                            moviesUserId = userId,
                            filmId = movie.id
                        )

                        appDatabase.moviesUserFilmDao().insertRelation(relation)
                        Log.d("LocalDatabase", "Relación creada para película: ${movie.id}")
                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "Error al procesar película ${movie.id}: ${e.message}")
                    }
                }

                Log.d("LocalDatabase", "Inicialización de base de datos completada")
            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error general en initializeDatabaseWithUserAndMovies: ${e.message}")
            }
        }
    }

    suspend fun resetAndSyncMovies(userId: Int, movies: List<Movie>) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("LocalDatabase", "Iniciando reset y sincronización para usuario $userId")

                // 1. Siempre insertar películas
                movies.forEach { movie ->
                    try {
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
                        Log.d("LocalDatabase", "Película insertada: ${movie.title} (ID: ${movie.id})")
                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "Error al insertar película ${movie.id}: ${e.message}")
                    }
                }

                // 2. Verificar que el usuario existe en user y movies_user
                val userExists = try {
                    appDatabase.userDao().getUserById(userId)
                    true
                } catch (e: Exception) {
                    Log.e("LocalDatabase", "Usuario NO encontrado en tabla user: $userId")
                    false
                }

                val moviesUserExists = try {
                    appDatabase.moviesUserDao().getMoviesUserById(userId)
                    true
                } catch (e: Exception) {
                    Log.e("LocalDatabase", "Usuario NO encontrado en tabla movies_user: $userId")
                    false
                }

                // 3. Solo gestionar relaciones si ambos existen
                if (userExists && moviesUserExists) {
                    // Eliminar relaciones existentes
                    try {
                        appDatabase.moviesUserFilmDao().deleteAllRelationsForUser(userId)
                        Log.d("LocalDatabase", "Relaciones eliminadas para usuario $userId")
                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "Error al eliminar relaciones: ${e.message}")
                    }

                    // Crear nuevas relaciones
                    val movieIds = movies.map { it.id }
                    movieIds.forEach { movieId ->
                        try {
                            val relation = MoviesUserFilmEntity(
                                moviesUserId = userId,
                                filmId = movieId
                            )
                            appDatabase.moviesUserFilmDao().insertRelation(relation)
                            Log.d("LocalDatabase", "Relación creada: userId=$userId, movieId=$movieId")
                        } catch (e: Exception) {
                            Log.e("LocalDatabase", "Error al crear relación para película $movieId: ${e.message}")
                        }
                    }
                } else {
                    Log.d("LocalDatabase", "No se crearán relaciones porque falta el usuario o el moviesUser")
                }

                // 4. Actualizar SharedPreferences (siempre)
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                val movieIdsSet = movies.map { it.id.toString() }.toSet()

                with(sharedPrefs.edit()) {
                    putStringSet("user_${userId}_movies", movieIdsSet)
                    apply()
                }

                Log.d("LocalDatabase", "Sincronización completada para usuario $userId con ${movies.size} películas")

            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error en resetAndSyncMovies: ${e.message}")
            }
        }
    }

    /*suspend fun checkDatabaseIntegrity(userId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val userExists = try {
                    val user = appDatabase.userDao().getUserById(userId)
                    Log.d("DBCheck", "Usuario existe: $userId (${user.username})")
                    true
                } catch (e: Exception) {
                    Log.e("DBCheck", "Usuario NO existe en tabla user: $userId")
                    false
                }

                val moviesUserExists = try {
                    val moviesUser = appDatabase.moviesUserDao().getMoviesUserById(userId)
                    Log.d("DBCheck", "MoviesUser existe: $userId (${moviesUser.username})")
                    true
                } catch (e: Exception) {
                    Log.e("DBCheck", "Usuario NO existe en tabla movies_user: $userId")
                    false
                }

                // Verificar películas y relaciones
                val movies = appDatabase.movieDao().getAllFilms()
                Log.d("DBCheck", "Total películas: ${movies.size}")

                val relations = try {
                    val count = appDatabase.moviesUserFilmDao().countRelationsForUser(userId)
                    Log.d("DBCheck", "Total relaciones para usuario $userId: $count")
                    count
                } catch (e: Exception) {
                    Log.e("DBCheck", "Error contando relaciones: ${e.message}")
                    0
                }
            } catch (e: Exception) {
                Log.e("DBCheck", "Error en checkDatabaseIntegrity: ${e.message}")
            }
        }
    }*/
    private suspend fun createMissingRelations(userId: Int, movies: List<Movie>) {
        withContext(Dispatchers.IO) {
            try {
                // Verificar que el usuario existe en movies_user
                val moviesUserExists = try {
                    appDatabase.moviesUserDao().getMoviesUserById(userId)
                    true
                } catch (e: Exception) {
                    // Si no existe, crearlo
                    try {
                        val userEntity = appDatabase.userDao().getUserById(userId)
                        val moviesUserEntity = MoviesUserEntity(
                            id = userId,
                            user_Id = userId,
                            username = userEntity.username,
                            email = userEntity.email,
                            password = ""
                        )
                        appDatabase.moviesUserDao().insertMoviesUser(moviesUserEntity)
                        true
                    } catch (e2: Exception) {
                        Log.e("LocalDatabase", "No se pudo crear MoviesUserEntity: ${e2.message}")
                        false
                    }
                }

                if (!moviesUserExists) {
                    Log.e("LocalDatabase", "No se pudo asegurar que exista el MoviesUser $userId")
                    return@withContext
                }

                // Crear relaciones faltantes
                var relationsCreated = 0
                movies.forEach { movie ->
                    try {
                        // Verificar si la relación ya existe
                        val existingRelation = appDatabase.moviesUserFilmDao()
                            .getRelationForUserAndFilm(userId, movie.id)

                        if (existingRelation == null) {
                            val relation = MoviesUserFilmEntity(
                                moviesUserId = userId,
                                filmId = movie.id
                            )
                            appDatabase.moviesUserFilmDao().insertRelation(relation)
                            relationsCreated++
                        }
                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "Error al crear relación para película ${movie.id}: ${e.message}")
                    }
                }

                Log.d("LocalDatabase", "Se crearon $relationsCreated relaciones faltantes para usuario $userId")
            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error en createMissingRelations: ${e.message}")
            }
        }
    }

    // Añade este método para verificar si el usuario existe en movies_user
    suspend fun userExistsInMoviesUser(userId: Int): Boolean {
        return try {
            val user = appDatabase.moviesUserDao().getMoviesUserById(userId)
            Log.d("LocalDatabase", "Usuario encontrado en movies_user: ${user.id}")
            true
        } catch (e: Exception) {
            Log.e("LocalDatabase", "Usuario NO encontrado en movies_user: $userId")
            false
        }
    }

    /*suspend fun cleanAndRebuildDatabase() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Eliminar todas las relaciones
                appDatabase.moviesUserFilmDao().deleteAllRelations()
                Log.d("LocalDatabase", "Todas las relaciones eliminadas")

                // 2. Eliminar todas las películas
                appDatabase.movieDao().deleteAllMovies()
                Log.d("LocalDatabase", "Todas las películas eliminadas")

                // 3. Eliminar todos los usuarios
                appDatabase.moviesUserDao().deleteAllMoviesUsers()
                Log.d("LocalDatabase", "Todos los usuarios de movies_user eliminados")

                appDatabase.userDao().deleteAllUsers()
                Log.d("LocalDatabase", "Todos los usuarios eliminados")

                // 4. Limpiar SharedPreferences
                val sharedPrefs = context.getSharedPreferences("offline_movies", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    clear()
                    apply()
                }

                Log.d("LocalDatabase", "Base de datos limpiada completamente")
            } catch (e: Exception) {
                Log.e("LocalDatabase", "Error al limpiar la base de datos: ${e.message}")
            }
        }
    }*/
}