package com.example.seguimientopeliculas.di

import MovieNetworkDataSource
import android.content.Context
import android.content.SharedPreferences
import com.example.seguimientopeliculas.data.local.database.LocalDatabase
import com.example.seguimientopeliculas.data.remote.MovieRemoteDataSource
import com.example.seguimientopeliculas.data.remote.StrapiApi
import com.example.seguimientopeliculas.data.remote.UserNetworkDataSource
import com.example.seguimientopeliculas.data.remote.UserRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val STRAPI_URL = "https://tasks-ewy9.onrender.com/api/"

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideStrapiApiService(sharedPreferences: SharedPreferences): StrapiApi {
        val client = OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val token = sharedPreferences.getString("jwt", null)
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(STRAPI_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StrapiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMovieNetworkDataSource(
        strapiApi: StrapiApi,
        sharedPreferences: SharedPreferences
    ): MovieRemoteDataSource {
        return MovieNetworkDataSource(strapiApi, sharedPreferences)
    }

    /*@Provides
    @Singleton
    fun provideUserNetworkDataSource(
        strapiApi: StrapiApi,
        sharedPreferences: SharedPreferences,
        @ApplicationContext context: Context
    ): UserRemoteDataSource {
        return UserNetworkDataSource(strapiApi, sharedPreferences, context)
    }*/

    @Provides
    @Singleton
    fun provideUserNetworkDataSource(
        strapiApi: StrapiApi,
        sharedPreferences: SharedPreferences,
        @ApplicationContext context: Context,
        localDatabase: LocalDatabase
    ): UserRemoteDataSource {
        return UserNetworkDataSource(strapiApi, sharedPreferences, context, localDatabase)
    }
}
