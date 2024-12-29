package com.example.seguimientopeliculas.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "movies_user_film",
    foreignKeys = [
        ForeignKey(
            entity = MoviesUserEntity::class,
            parentColumns = ["id"],
            childColumns = ["movies_user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["film_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["movies_user_id"]), Index(value = ["film_id"])]
)
data class MoviesUserFilmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "movies_user_id") val moviesUserId: Int,
    @ColumnInfo(name = "film_id") val filmId: Int
)
