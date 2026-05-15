package com.example.kutuphaneapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kutuphaneapp.repository.BooksRepository
import com.example.kutuphaneapp.util.StatsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel : ViewModel() {

    private val repository = BooksRepository()

    private val _stats = MutableLiveData<StatsUtil.StatsData>()
    val stats: LiveData<StatsUtil.StatsData> = _stats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Verileri Repository'den çek
                val books = repository.getUserBooksOnce()
                val goal = repository.getReadingGoal()

                // Ağır hesaplamaları arka planda (Default Dispatcher) yap
                val computedStats = withContext(Dispatchers.Default) {
                    StatsUtil.computeAllStats(books, goal)
                }

                _stats.value = computedStats
            } catch (e: Exception) {
                _error.value = "İstatistikler hesaplanırken hata oluştu: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
