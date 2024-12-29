package com.example.seguimientopeliculas.data.local.localDataSource

import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.local.database.UserDao
import com.example.seguimientopeliculas.data.local.iLocalDataSource.IUserLocalDataSource
import com.example.seguimientopeliculas.data.local.localModelMapping.toExternal
import com.example.seguimientopeliculas.data.local.localModelMapping.toUserEntityFromMoviesUser
import javax.inject.Inject

class UserLocalDataSource @Inject constructor(
    private val userDao: UserDao
) : IUserLocalDataSource {

    override suspend fun getLoggedUser(): MoviesUser {
        val userEntity = userDao.getUserById(1) // Cambia `1` por el ID del usuario logueado
        return userEntity?.toExternal()
            ?: throw IllegalStateException("No hay usuario logueado.")
    }

    override suspend fun updateLoggedUser(user: MoviesUser) {
        val userEntity = user.toUserEntityFromMoviesUser()
        userDao.updateUser(userEntity)
    }

    override suspend fun deleteLoggedUser(userId: Int) {
        userDao.deleteUser(userId)
    }
}