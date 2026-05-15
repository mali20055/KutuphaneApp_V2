package com.example.kutuphaneapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val isbn: String = "",
    val pageCount: Int = 0,
    val publisher: String = "",
    val publishedDate: String = "",
    val description: String = "",
    val status: String = "to_read",
    val rating: Float = 0f,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val addedAt: Long = 0L,
    val finishedAt: Long = 0L
) : Parcelable
