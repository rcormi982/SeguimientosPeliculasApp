package com.example.seguimientopeliculas.data.local.iLocalDataSource

import com.example.seguimientopeliculas.data.remote.models.MoviesUser

interface IUserLocalDataSource {

    //Obtiene los datos del usuario actualmente logueado.
    suspend fun getLoggedUser(): MoviesUser

    //Actualiza la información del usuario (nombre, email, contraseña).
    suspend fun updateLoggedUser(user: MoviesUser)

    //Elimina al usuario logueado de la base de datos.
    //Este método también asegura que se eliminen las entradas correspondientes en `movies_user`.
    suspend fun deleteLoggedUser(userId: Int)
}