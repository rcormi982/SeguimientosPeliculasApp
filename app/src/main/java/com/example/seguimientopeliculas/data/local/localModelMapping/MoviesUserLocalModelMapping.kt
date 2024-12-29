package com.example.seguimientopeliculas.data.local.localModelMapping

import com.example.seguimientopeliculas.data.MoviesUser
import com.example.seguimientopeliculas.data.local.entities.MoviesUserEntity
import com.example.seguimientopeliculas.data.local.entities.UserEntity

// De MoviesUserEntity a MoviesUser
fun MoviesUserEntity.toExternal() = MoviesUser(
    id = this.id,
    username = this.username,
    email = this.email,
    password = this.password,
    userId = this.user_Id
)

// De UserEntity a MoviesUser
fun UserEntity.toExternal() = MoviesUser(
    id = this.id,
    username = this.username,
    email = this.email,
    password = this.password,
    userId = this.id
)
fun MoviesUser.toUserEntityFromMoviesUser(): UserEntity {
    return UserEntity(
        id = this.id,
        username = this.username,
        email = this.email,
        password = this.password
    )
}

// Convierte una lista de MoviesUserEntity a una lista de MoviesUser
fun List<MoviesUserEntity>.toMoviesUserListFromMoviesUserEntity(): List<MoviesUser> {
    return map { it.toExternal() }
}

// Convierte una lista de UserEntity a una lista de MoviesUser
fun List<UserEntity>.toMoviesUserListFromUserEntity(): List<MoviesUser> {
    return map { it.toExternal() }
}
