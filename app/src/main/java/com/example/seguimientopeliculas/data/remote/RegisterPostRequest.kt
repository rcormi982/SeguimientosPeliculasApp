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
    val publishedAt: String
)

