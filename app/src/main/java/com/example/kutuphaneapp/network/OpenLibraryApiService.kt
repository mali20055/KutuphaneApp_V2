package com.example.kutuphaneapp.network

import com.example.kutuphaneapp.model.OpenLibraryBookData
import com.example.kutuphaneapp.model.OpenLibraryResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryApiService {

    /**
     * Başlığa göre arama — q= yerine title= kullanır, çok daha kesin sonuç verir.
     * sort=editions → en çok baskısı olan (= en popüler) kitaplar önce gelir.
     */
    @GET("search.json")
    suspend fun searchByTitle(
        @Query("title") title: String,
        @Query("limit") limit: Int = 15,
        @Query("fields") fields: String =
            "title,author_name,isbn,cover_i,publisher,publish_year," +
            "number_of_pages_median,first_publish_year,language,edition_count",
        @Query("sort") sort: String = "editions"
    ): OpenLibraryResponse

    /**
     * Yazara göre arama — author= parametresi ile yazar odaklı sonuç.
     */
    @GET("search.json")
    suspend fun searchByAuthor(
        @Query("author") author: String,
        @Query("limit") limit: Int = 15,
        @Query("fields") fields: String =
            "title,author_name,isbn,cover_i,publisher,publish_year," +
            "number_of_pages_median,first_publish_year,edition_count"
    ): OpenLibraryResponse

    /**
     * Genel full-text arama — fallback olarak kullanılır.
     */
    @GET("search.json")
    suspend fun searchGeneral(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("fields") fields: String =
            "title,author_name,isbn,cover_i,publisher,publish_year," +
            "number_of_pages_median,edition_count"
    ): OpenLibraryResponse

    /**
     * ISBN ile birebir kitap verisi çekme.
     */
    @GET("api/books")
    suspend fun getBookByIsbn(
        @Query("bibkeys") bibkeys: String,
        @Query("format") format: String = "json",
        @Query("jscmd") jscmd: String = "data"
    ): Map<String, OpenLibraryBookData>
}

object OpenLibraryRetrofitInstance {
    private const val BASE_URL = "https://openlibrary.org/"

    val api: OpenLibraryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenLibraryApiService::class.java)
    }
}
