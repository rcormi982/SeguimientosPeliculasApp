package com.example.seguimientopeliculas.data.remote.models

data class LoginRequestApi(
    val identifier: String,
    val password: String
)

data class LoginResponse(
    val jwt: String,
    val user: UserResponse
)





