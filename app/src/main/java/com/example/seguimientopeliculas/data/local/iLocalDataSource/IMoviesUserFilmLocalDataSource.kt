package com.example.seguimientopeliculas.data.local.iLocalDataSource

import com.example.seguimientopeliculas.data.remote.models.Movie
import kotlinx.coroutines.flow.Flow

interface IMoviesUserFilmLocalDataSource {

    //Asocia una película a un usuario.
    suspend fun insertRelation(userId: Int, movieId: Int)

    //Obtiene todas las películas asociadas a un usuario específico.
    suspend fun readAllRelationsForUser(userId: Int): List<Movie>

    //Proporciona un flujo para observar cambios en las películas asociadas a un usuario
    fun observeAllRelationsForUser(userId: Int): Flow<List<Movie>>

    //Elimina la asociación entre un usuario y una película específica.
    suspend fun deleteRelation(userId: Int, movieId: Int)
}