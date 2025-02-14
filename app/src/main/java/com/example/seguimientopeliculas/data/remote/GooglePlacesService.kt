package com.example.seguimientopeliculas.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesService {
    @GET("nearbysearch/json")
    suspend fun getNearbyCinemas(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("keyword") keyword: String,
        @Query("key") apiKey: String
    ): Response<PlacesResponse>
}
