package com.example.kutuphaneapp.network

import com.example.kutuphaneapp.model.GoogleBooksResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiService {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("orderBy") orderBy: String = "relevance",
        @Query("printType") printType: String = "books"
        // langRestrict=tr KALDIRILDI — yalnızca Türkçe baskıları filtreliyordu,
        // yabancı dil orijinallerine sahip Türkçe başlıklı kitaplar gelemiyor.
    ): GoogleBooksResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    val api: GoogleBooksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApiService::class.java)
    }
}