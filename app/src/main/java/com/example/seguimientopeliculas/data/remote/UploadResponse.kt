package com.example.seguimientopeliculas.data.remote

data class UploadResponse(
    val id: Int,
    val name: String,
    val url: String,
    val formats: MediaFormats? = null
)