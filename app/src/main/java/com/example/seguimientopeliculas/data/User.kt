package com.example.seguimientopeliculas.data

data class User(
    val id: Int,
    val username: String,
    val email: String
)

/*data class UpdateUserRequest(
    val username: String,
    val email: String,
    val password: String? = null//Opcional, si no se proporciona, no se actualizará
)

data class UpdateUserRequestWithToken(
    val username: String,
    val email: String,
    val password: String?,
    val token: String
)

data class TokenRequest(
    val token: String
)*/

data class MoviesUser(
    val id: Int,
    val username: String,
    val email: String,
    val password: String,
    val userId: Int
)



