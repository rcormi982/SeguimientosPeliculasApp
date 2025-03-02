package com.example.seguimientopeliculas.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserEntity
import com.example.seguimientopeliculas.data.local.entities.MoviesUserFilmEntity
import com.example.seguimientopeliculas.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        MoviesUserEntity::class,
        MoviesUserFilmEntity::class,
        MovieEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun moviesUserDao(): MoviesUserDao
    abstract fun moviesUserFilmDao(): MoviesUserFilmDao
    abstract fun movieDao(): MovieDao
}