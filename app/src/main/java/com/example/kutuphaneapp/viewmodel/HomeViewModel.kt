package com.example.kutuphaneapp.viewmodel

import androidx.lifecycle.*
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = BooksRepository()
    private val auth = FirebaseAuth.getInstance()

    val userBooks: LiveData<List<Book>> = repository.getUserBooksFlow().asLiveData()

    private val _readingGoal = MutableLiveData<Int>()
    val readingGoal: LiveData<Int> = _readingGoal

    val displayName: String
        get() = auth.currentUser?.displayName ?: "Kullanıcı"

    init {
        fetchReadingGoal()
    }

    fun fetchReadingGoal() {
        viewModelScope.launch {
            _readingGoal.value = repository.getReadingGoal()
        }
    }
}
