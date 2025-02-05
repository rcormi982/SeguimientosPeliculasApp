package com.example.seguimientopeliculas.data

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
    val imageUrl: Int? = null
)

data class UpdateImageUrl(
    val data: ImageUrlData
)

data class ImageUrlData(
    val id: Int? = null,
    val attributes: ImageAttributes
)

data class ImageAttributes(
    val url: String
)
data class PhotoUploadResult(
    val url: String,
    val id: Int
)