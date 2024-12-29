package com.example.seguimientopeliculas.login

import com.example.seguimientopeliculas.data.remote.MoviesUserResponse

data class RegisterResponse(
    val jwt: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val provider: String,
    val confirmed: Boolean,
    val blocked: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val movies_user: MoviesUserResponse? // Relación con `movies_user`
)
