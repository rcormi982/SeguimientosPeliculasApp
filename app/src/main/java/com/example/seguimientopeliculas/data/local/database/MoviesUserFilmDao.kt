package com.example.seguimientopeliculas.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserFilmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoviesUserFilmDao {
    //Obtiene todas las películas asociadas a un usuario específico.
    @Query("""
        SELECT f.* FROM film f
        INNER JOIN movies_user_film muf ON f.id = muf.film_id 
        WHERE muf.movies_user_id = :userId
    """)
    suspend fun getFilmsForUser(userId: Int): List<MovieEntity>

    //Observa todas las películas asociadas a un usuario específico en tiempo real.
    @Query("""
        SELECT f.* FROM film f
        INNER JOIN movies_user_film muf ON f.id = muf.film_id 
        WHERE muf.movies_user_id = :userId
    """)
    fun observeFilmsForUser(userId: Int): Flow<List<MovieEntity>>

    //Inserta una relación entre un usuario y una película.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: MoviesUserFilmEntity)

    //Elimina la relación entre un usuario y una película específica.
    @Query("DELETE FROM movies_user_film WHERE movies_user_id = :userId AND film_id = :filmId")
    suspend fun deleteRelation(userId: Int, filmId: Int)

    // Consulta para depuración
    @Query("SELECT COUNT(*) FROM movies_user_film WHERE movies_user_id = :userId")
    suspend fun countRelationsForUser(userId: Int): Int

    @Query("SELECT * FROM movies_user_film WHERE movies_user_id = :userId AND film_id = :filmId")
    suspend fun getRelationForUserAndFilm(userId: Int, filmId: Int): MoviesUserFilmEntity?

    @Query("DELETE FROM movies_user_film WHERE movies_user_id = :userId")
    suspend fun deleteAllRelationsForUser(userId: Int)

    @Query("DELETE FROM movies_user_film")
    suspend fun deleteAllRelations()

}