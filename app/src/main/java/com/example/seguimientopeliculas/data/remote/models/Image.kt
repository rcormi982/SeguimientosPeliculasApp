package com.example.seguimientopeliculas.data.remote.models

// Para representar atributos de imagen
data class ImageAttributes(
    val url: String?,
    val formats: ImageFormats? = null
)

// Para representar formatos disponibles
data class ImageFormats(
    val thumbnail: ImageFormat? = null,
    val small: ImageFormat? = null,
    val medium: ImageFormat? = null,
    val large: ImageFormat? = null
)

// Para representar un formato específico
data class ImageFormat(
    val url: String?,
    val width: Int?,
    val height: Int?
)

data class ImageUrlWrapper(
    val data: ImageUrlData?
)

data class ImageUrl(
    val url: String?
)

data class ImageUrlData(
    val id: Int? = null,
    val attributes: ImageAttributes
)

data class PhotoUploadResult(
    val url: String,
    val id: Int
)