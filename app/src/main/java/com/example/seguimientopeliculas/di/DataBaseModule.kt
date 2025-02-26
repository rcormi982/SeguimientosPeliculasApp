package com.example.seguimientopeliculas.di

import android.content.Context
import androidx.room.Room
import com.example.seguimientopeliculas.data.local.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "seguimientopeliculas.db"
        )
            .fallbackToDestructiveMigration() // Esto puede ayudar si hay problemas de esquema
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideMoviesUserDao(database: AppDatabase): MoviesUserDao {
        return database.moviesUserDao()
    }

    @Provides
    fun provideMoviesUserFilmDao(database: AppDatabase): MoviesUserFilmDao {
        return database.moviesUserFilmDao()
    }

    @Provides
    fun provideMovieDao(database: AppDatabase): MovieDao {
        return database.movieDao()
    }
    @Provides
    @Singleton
    fun provideDatabaseHelper(
        appDatabase: AppDatabase,
        @ApplicationContext context: Context
    ): DatabaseHelper {
        return DatabaseHelper(appDatabase, context)
    }
}