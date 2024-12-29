package com.example.seguimientopeliculas.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserFilmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoviesUserFilmDao {
    //Obtiene todas las películas asociadas a un usuario específico.
    @Query("""
        SELECT * FROM film 
        INNER JOIN movies_user_film 
        ON film.id = movies_user_film.film_id 
        WHERE movies_user_film.movies_user_id = :userId
    """)
    suspend fun getFilmsForUser(userId: Int): List<MovieEntity>

    //Observa todas las películas asociadas a un usuario específico en tiempo real.
    @Query("""
        SELECT * FROM film 
        INNER JOIN movies_user_film 
        ON film.id = movies_user_film.film_id 
        WHERE movies_user_film.movies_user_id = :userId
    """)
    fun observeFilmsForUser(userId: Int): Flow<List<MovieEntity>>

    //Inserta una relación entre un usuario y una película.
    @Insert
    suspend fun insertRelation(relation: MoviesUserFilmEntity)

    //Elimina la relación entre un usuario y una película específica.
    @Query("DELETE FROM movies_user_film WHERE movies_user_id = :userId AND film_id = :filmId")
    suspend fun deleteRelation(userId: Int, filmId: Int)
}