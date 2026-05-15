package com.example.kutuphaneapp.model

data class FilterState(
    val titleQuery: String = "",
    val authorQuery: String = "",
    val publisherQuery: String = "",
    val minPageCount: Int? = null,
    val maxPageCount: Int? = null,
    val minRating: Int = 0,
    val selectedTags: List<String> = emptyList(),
    val selectedStatus: String? = null
)
