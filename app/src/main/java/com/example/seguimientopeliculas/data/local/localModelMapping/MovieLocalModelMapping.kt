package com.example.seguimientopeliculas.data.remote.mappers

import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.local.entities.MovieEntity
import com.example.seguimientopeliculas.data.remote.MovieRaw

// Convierte de MovieRaw (remoto) a MovieEntity (local)
fun MovieRaw.toLocal(): MovieEntity {
    return MovieEntity(
        id = this.id,
        title = this.attributes?.Title ?: "",
        genre = this.attributes?.Genre ?: "",
        rating = this.attributes?.Rating ?: 0,
        premiere = this.attributes?.Premiere ?: false,
        status = this.attributes?.Status ?: "",
        comments = this.attributes?.Comments ?: ""
    )
}

// Convierte una lista de MovieRaw a una lista de MovieEntity
fun List<MovieRaw>.toLocal(): List<MovieEntity> {
    return this.map { it.toLocal() }
}

// Convierte una entidad local (MovieEntity) a un modelo de dominio (Movie)
fun MovieEntity.toExternal() = Movie(
    id = this.id,
    title = this.title,
    genre = this.genre,
    rating = this.rating,
    premiere = this.premiere,
    status = this.status,
    comments = this.comments?: " "
)

// Convierte una lista de entidades locales a una lista de modelos de dominio
fun List<MovieEntity>.toExternal(): List<Movie> {
    return map { it.toExternal() }
}


