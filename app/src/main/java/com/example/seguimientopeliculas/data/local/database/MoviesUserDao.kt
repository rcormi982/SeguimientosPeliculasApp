package com.example.seguimientopeliculas.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.seguimientopeliculas.data.local.entities.MoviesUserEntity

@Dao
interface MoviesUserDao {
    @Transaction
    @Query("SELECT * FROM movies_user WHERE id = :userId")
    suspend fun getMoviesUserById(userId: Int): MoviesUserEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoviesUser(user: MoviesUserEntity)

    @Query("DELETE FROM movies_user")
    suspend fun deleteAllMoviesUsers()

}