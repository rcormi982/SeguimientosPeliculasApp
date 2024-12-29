package com.example.seguimientopeliculas.login

data class LoginRequestApi(
    val identifier: String,
    val password: String
)

data class LoginResponse(
    val jwt: String,
    val user: UserResponse
)





