package com.example.seguimientopeliculas.data

import com.example.seguimientopeliculas.data.remote.ImageUrlData

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
    val data: UpdateData
)

data class UpdateData(
    val username: String? = null,
    val email: String? = null,
    val imageUrl: UpdateImageUrl? = null
)

data class UpdateImageUrl(
    val id: Int? = null,
    val data: ImageUrlData? = null
)


