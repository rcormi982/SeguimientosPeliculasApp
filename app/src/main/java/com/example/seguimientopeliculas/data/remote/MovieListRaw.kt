package com.example.seguimientopeliculas.data.remote

data class MovieListRaw(
    val data: List<MovieRaw>,
    val meta: PaginationMeta?
)

data class PaginationMeta(
    val pagination: Pagination
)

data class Pagination(
    val page: Int,
    val pageSize: Int,
    val pageCount: Int,
    val total: Int
)