package com.example.kutuphaneapp.util

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object StorageUtil {

    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadBookCover(userId: String, bookId: String, imageUri: Uri): String {
        val ref = storage.reference
            .child("covers")
            .child(userId)
            .child("$bookId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteBookCover(coverUrl: String) {
        if (coverUrl.isNotEmpty()) {
            storage.getReferenceFromUrl(coverUrl).delete().await()
        }
    }
}
