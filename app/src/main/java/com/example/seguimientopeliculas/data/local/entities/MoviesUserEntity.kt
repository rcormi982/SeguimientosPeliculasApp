package com.example.seguimientopeliculas.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "movies_user",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_Id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_Id"])])
data class MoviesUserEntity (
    @PrimaryKey
    val id: Int,
    val user_Id: Int, // Relación uno a uno con UserEntity
    val username: String,
    val email: String,
    val password: String
)