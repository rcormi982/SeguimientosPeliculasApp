package com.example.seguimientopeliculas.data.remote

data class MoviePostRequest(
    val data: MovieAttributes
)
data class MovieAttributes(
    val Title: String,
    val Genre: String,
    val Rating: Int?,
    val Premiere: Boolean,
    val Status: String?,
    val Comments: String?,
    val movies_users: List<Int>,
    //val users_permissions_user: Int
)
