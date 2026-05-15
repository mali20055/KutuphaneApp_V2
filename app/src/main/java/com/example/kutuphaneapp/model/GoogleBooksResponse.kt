package com.example.kutuphaneapp.model

import com.google.gson.annotations.SerializedName

data class GoogleBooksResponse(val items: List<VolumeItem>?)

data class VolumeItem(val id: String, val volumeInfo: VolumeInfo)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val publisher: String?,
    val publishedDate: String?,
    val description: String?,
    val pageCount: Int?,
    val imageLinks: ImageLinks?,
    val industryIdentifiers: List<IndustryIdentifier>?
)

data class ImageLinks(
    @SerializedName("extraLarge") val extraLarge: String?,
    @SerializedName("large") val large: String?,
    @SerializedName("medium") val medium: String?,
    @SerializedName("small") val small: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("smallThumbnail") val smallThumbnail: String?
) {
    /**
     * Mevcut en iyi kaliteli görsel URL'sini döndürür.
     * - http → https (Android cleartext trafiği engeller)
     * - zoom= parametresi kaldırılır (zoom=2 gibi manipülasyonlar URL'yi bozuyor)
     * - edge=curl kaldırılır
     */
    fun bestAvailableUrl(): String {
        val raw = thumbnail ?: smallThumbnail ?: small ?: medium ?: large ?: extraLarge ?: ""
        if (raw.isEmpty()) return ""
        return raw
            .replace("http://", "https://")
            .replace(Regex("&?zoom=\\d+"), "")
            .replace(Regex("&?edge=curl"), "")
    }
}

data class IndustryIdentifier(val type: String?, val identifier: String?)