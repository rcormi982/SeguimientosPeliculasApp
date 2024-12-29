package com.example.seguimientopeliculas.data.local.localDataSource

import com.example.seguimientopeliculas.data.Movie
import com.example.seguimientopeliculas.data.local.database.MoviesUserFilmDao
import com.example.seguimientopeliculas.data.local.localModelMapping.toExternal
import com.example.seguimientopeliculas.data.local.entities.MoviesUserFilmEntity
import com.example.seguimientopeliculas.data.local.iLocalDataSource.IMoviesUserFilmLocalDataSource
import com.example.seguimientopeliculas.data.remote.mappers.toExternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MoviesUserFilmLocalDataSource @Inject constructor(
    private val moviesUserFilmDao: MoviesUserFilmDao
) : IMoviesUserFilmLocalDataSource {

    override suspend fun insertRelation(userId: Int, movieId: Int) {
        val relation = MoviesUserFilmEntity(
            moviesUserId = userId,
            filmId = movieId
        )
        moviesUserFilmDao.insertRelation(relation)
    }

    override suspend fun readAllRelationsForUser(userId: Int): List<Movie> {
        // Convierte directamente las entidades obtenidas desde el DAO
        return moviesUserFilmDao.getFilmsForUser(userId).toExternal()
    }

    override fun observeAllRelationsForUser(userId: Int): Flow<List<Movie>> {
        // Observa los cambios en tiempo real y convierte las entidades a modelos de dominio
        return moviesUserFilmDao.observeFilmsForUser(userId).map { it.toExternal() }
    }

    override suspend fun deleteRelation(userId: Int, movieId: Int) {
        moviesUserFilmDao.deleteRelation(userId, movieId)
    }
}
