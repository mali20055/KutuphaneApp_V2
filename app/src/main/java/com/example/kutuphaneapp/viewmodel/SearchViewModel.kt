package com.example.kutuphaneapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.repository.BooksRepository
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = BooksRepository()
    
    private val _searchResults = MutableLiveData<List<Book>>()
    val searchResults: LiveData<List<Book>> = _searchResults
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun searchBooks(query: String) {
        if (query.length < 2) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val results = repository.searchBooks(query)
                _searchResults.value = results
                if (results.isEmpty()) {
                    _error.value = "Sonuç bulunamadı"
                }
            } catch (e: Exception) {
                _error.value = "Bağlantı hatası"
            } finally {
                _isLoading.value = false
            }
        }
    }
}