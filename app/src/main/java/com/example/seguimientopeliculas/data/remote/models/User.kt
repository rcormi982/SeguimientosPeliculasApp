package com.example.seguimientopeliculas.data.remote.models

data class User(
    val id: Int,
    val username: String,
    val email: String
)

data class MoviesUser(
    val id: Int,
    val username: String,
    val email: String,
    val password: String,
    val userId: Int,
    val imageUrl: String?,
    val image: ImageUrl?
)

data class UpdateMoviesUserPayload(
    val data: UpdateData
)

data class UpdateData(
    val username: String? = null,
    val email: String? = null,
    val imageUrl: Int? = null
)
