package com.example.kutuphaneapp.model

import com.google.gson.annotations.SerializedName

data class OpenLibraryBookData(
    @SerializedName("title") val title: String?,
    @SerializedName("authors") val authors: List<OpenLibraryAuthor>?,
    @SerializedName("cover") val cover: OpenLibraryCover?,
    @SerializedName("publishers") val publishers: List<OpenLibraryPublisher>?,
    @SerializedName("publish_date") val publishDate: String?,
    @SerializedName("identifiers") val identifiers: OpenLibraryIdentifiers?
)

data class OpenLibraryAuthor(
    @SerializedName("name") val name: String?
)

data class OpenLibraryCover(
    @SerializedName("small") val small: String?,
    @SerializedName("medium") val medium: String?,
    @SerializedName("large") val large: String?
)

data class OpenLibraryPublisher(
    @SerializedName("name") val name: String?
)

data class OpenLibraryIdentifiers(
    @SerializedName("isbn_10") val isbn10: List<String>?,
    @SerializedName("isbn_13") val isbn13: List<String>?
)
