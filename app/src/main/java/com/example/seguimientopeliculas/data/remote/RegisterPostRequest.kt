package com.example.seguimientopeliculas.data.remote

data class RegisterPostRequest(
    val username: String,
    val email: String,
    val password: String
)

// Datos necesarios para crear un MoviesUser relacionado con el usuario principal
data class MoviesUserRequest(
    val data: RegisterUserData
)

// Detalles del MoviesUser, incluyendo la relación con el usuario principal
data class RegisterUserData(
    val users_permissions_user: Int,
    val username: String,
    val email: String
)
// Respuesta al crear o consultar un MoviesUser
data class MoviesUserResponse(
    val data: List<MoviesUserLoginData>
)

data class MoviesUserLoginData(
    val id: Int,
    val attributes: MoviesUserAttributes
)

data class MoviesUserAttributes(
    val username: String,
    val email: String,
    val createdAt: String,
    val updatedAt: String,
    val publishedAt: String,
    val imageUrl: ImageUrlWrapper?
)

data class ImageUrlWrapper(
    val data: ImageUrlData?
)

data class ImageUrlData(
    val id: Int?,
    val attributes: ImageAttributes
)

data class ImageAttributes(
    val url: String?,
    val formats: ImageFormats?
)

data class ImageFormats(
    val thumbnail: ImageFormat?
)

data class ImageFormat(
    val url: String?,
    val width: Int?,
    val height: Int?
)

data class Media(
    val id: Int,
    val url: String,
    val formats: MediaFormats? = null
)

data class MediaFormats(
    val thumbnail: MediaFormat? = null,
    val small: MediaFormat? = null,
    val medium: MediaFormat? = null,
    val large: MediaFormat? = null
)

data class MediaFormat(
    val url: String,
    val width: Int,
    val height: Int
)
