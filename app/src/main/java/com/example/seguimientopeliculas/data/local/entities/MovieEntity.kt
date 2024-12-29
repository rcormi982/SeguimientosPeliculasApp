package com.example.seguimientopeliculas.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "film")
data class MovieEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val genre: String,
    val rating: Int,
    val status: String,
    val premiere: Boolean,
    val comments: String?
    )
