package com.example.kutuphaneapp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditBookViewModel @Inject constructor() : ViewModel() {

    val repository = BooksRepository()
    var pendingCoverUri: Uri? = null

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateResult = MutableLiveData<Result<Unit>?>()
    val updateResult: LiveData<Result<Unit>?> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>?>()
    val deleteResult: LiveData<Result<Unit>?> = _deleteResult

    fun updateBook(book: Book) {
        _isLoading.value = true
        viewModelScope.launch {
            val finalBook = if (pendingCoverUri != null) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val coverResult = repository.uploadCoverImage(uid, book.id, pendingCoverUri!!)
                    if (coverResult.isSuccess) {
                        book.copy(coverUrl = coverResult.getOrThrow())
                    } else {
                        _isLoading.postValue(false)
                        _updateResult.postValue(Result.failure(coverResult.exceptionOrNull()!!))
                        return@launch
                    }
                } else book
            } else book

            val result = repository.updateBook(finalBook)
            _isLoading.postValue(false)
            _updateResult.postValue(result)
        }
    }

    fun deleteBook(bookId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.deleteBook(bookId)
            _isLoading.postValue(false)
            _deleteResult.postValue(result)
        }
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }
}
