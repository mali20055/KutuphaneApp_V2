package com.example.kutuphaneapp.model

import com.google.gson.annotations.SerializedName

data class OpenLibraryResponse(
    @SerializedName("docs") val docs: List<OpenLibraryDoc>?
)

data class OpenLibraryDoc(
    @SerializedName("title") val title: String?,
    @SerializedName("author_name") val authorName: List<String>?,
    @SerializedName("isbn") val isbn: List<String>?,
    @SerializedName("cover_i") val coverI: Long?,
    @SerializedName("publisher") val publisher: List<String>?,
    @SerializedName("publish_year") val publishYear: List<Int>?,
    // Yeni alanlar — fields= parametresiyle API'den çekiliyor
    @SerializedName("number_of_pages_median") val pageCount: Int?,
    @SerializedName("first_publish_year") val firstPublishYear: Int?,
    @SerializedName("language") val language: List<String>?,
    @SerializedName("edition_count") val editionCount: Int?  // Popülerlik göstergesi
)