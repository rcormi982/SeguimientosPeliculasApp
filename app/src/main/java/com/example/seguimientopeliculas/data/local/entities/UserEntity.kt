package com.example.seguimientopeliculas.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: Int,
    val username: String,
    val email: String,
    val password: String
)
