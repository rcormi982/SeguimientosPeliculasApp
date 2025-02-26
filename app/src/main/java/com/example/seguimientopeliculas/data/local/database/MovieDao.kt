package com.example.seguimientopeliculas.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.seguimientopeliculas.data.local.entities.MovieEntity

@Dao
interface MovieDao {
    @Query("SELECT * FROM film WHERE id = :movieId")
    fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM film")
    fun getAllFilms(): List<MovieEntity>

    @Insert
    fun insertFilm(film: MovieEntity)

    @Update
    fun updateFilm(film: MovieEntity)

    @Query("DELETE FROM film WHERE id = :filmId")
    fun deleteFilmById(filmId: Int)
}