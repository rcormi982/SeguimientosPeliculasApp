package com.example.seguimientopeliculas.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.seguimientopeliculas.data.local.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity

    @Query("DELETE FROM user WHERE id = :userId")
    suspend fun deleteUser(userId: Int)

    @Insert
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}