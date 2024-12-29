package com.example.seguimientopeliculas.di

import com.example.seguimientopeliculas.data.DefaultMovieRepository
import com.example.seguimientopeliculas.data.MovieRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindMovieRepository(repository: DefaultMovieRepository): MovieRepository

}
