package com.example.seguimientopeliculas.di

import android.content.Context
import com.example.seguimientopeliculas.data.DefaultMovieRepository
import com.example.seguimientopeliculas.data.MovieRepository
import com.example.seguimientopeliculas.data.local.iLocalDataSource.IMoviesUserFilmLocalDataSource
import com.example.seguimientopeliculas.data.local.iLocalDataSource.IUserLocalDataSource
import com.example.seguimientopeliculas.data.local.localDataSource.MoviesUserFilmLocalDataSource
import com.example.seguimientopeliculas.data.local.localDataSource.UserLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindMovieRepository(repository: DefaultMovieRepository): MovieRepository

    @Singleton
    @Binds
    abstract fun bindMoviesUserFilmLocalDataSource(
        dataSource: MoviesUserFilmLocalDataSource
    ): IMoviesUserFilmLocalDataSource

    @Singleton
    @Binds
    abstract fun bindUserLocalDataSource(
        dataSource: UserLocalDataSource
    ): IUserLocalDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}