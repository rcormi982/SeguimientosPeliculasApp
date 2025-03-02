package com.example.seguimientopeliculas.data.remote.models

import com.example.seguimientopeliculas.data.User

data class Movie(
    val id: Int,
    val title: String,
    val genre: String,
    val rating: Int,
    val status: String,
    val premiere: Boolean,
    val comments: String,
    val moviesUsers: List<User> = emptyList(), // Relación con usuarios (lista de usuarios relacionados)
    val moviesUserId: Int? = null, // ID del usuario asociado
    val userPermissions: User? = null, // Usuario con permisos específicos
)