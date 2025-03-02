package com.example.seguimientopeliculas.data.remote.models

data class MovieRaw(
    val id: Int,
    val attributes: MovieAttributesRaw?
)
data class MovieAttributesRaw(
    val Title: String?,
    val Genre: String?,
    val Rating: Int?,
    val Premiere: Boolean?,
    val Status: String?,
    val Comments: String?,
)

