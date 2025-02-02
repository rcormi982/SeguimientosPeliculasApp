package com.example.seguimientopeliculas.data

import com.example.seguimientopeliculas.data.remote.Media

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
data class ImageUrl(
    val url: String?
)

data class UpdateMoviesUserPayload(
    val data: Any
)

data class DataPayload(
    val username: String? = null,
    val email: String? = null,
    val imageUrl: String? = null
)


