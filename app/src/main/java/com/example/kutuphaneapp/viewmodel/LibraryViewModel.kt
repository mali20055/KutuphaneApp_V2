package com.example.kutuphaneapp.viewmodel

import androidx.lifecycle.*
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.model.FilterState
import com.example.kutuphaneapp.repository.BooksRepository

class LibraryViewModel : ViewModel() {
    private val repository = BooksRepository()
    
    private val allBooks = repository.getUserBooksFlow().asLiveData()
    
    private val _filterState = MutableLiveData(FilterState())
    val filterState: LiveData<FilterState> = _filterState

    private val _selectedTab = MutableLiveData(0)
    val selectedTab: LiveData<Int> = _selectedTab

    val availableTags: LiveData<List<String>> = allBooks.map { books ->
        books.flatMap { it.tags }.distinct().sorted()
    }

    val filteredBooks: LiveData<List<Book>> = MediatorLiveData<List<Book>>().apply {
        val update = Observer<Any?> {
            val books = allBooks.value ?: emptyList()
            val state = _filterState.value ?: FilterState()
            val tabIndex = _selectedTab.value ?: 0
            
            value = filterBooks(books, state, tabIndex)
        }
        addSource(allBooks, update)
        addSource(_filterState, update)
        addSource(_selectedTab, update)
    }

    fun updateFilter(newState: FilterState) {
        _filterState.value = newState
    }

    fun setSearchQuery(query: String) {
        val current = _filterState.value ?: FilterState()
        _filterState.value = current.copy(titleQuery = query)
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    private fun filterBooks(books: List<Book>, state: FilterState, tabIndex: Int): List<Book> {
        return books.filter { book ->
            // 1. Tab Filtresi (Quick status filter)
            val matchesTab = when (tabIndex) {
                1 -> book.status == "reading"
                2 -> book.status == "to_read"
                3 -> book.status == "read"
                else -> true
            }
            if (!matchesTab) return@filter false

            // 2. Text Search (Title + Author)
            if (state.titleQuery.isNotEmpty()) {
                val query = state.titleQuery.lowercase()
                val matchesSearch = book.title.lowercase().contains(query) || 
                                   book.author.lowercase().contains(query)
                if (!matchesSearch) return@filter false
            }

            // 3. Author Filter (Explicit)
            if (state.authorQuery.isNotEmpty() && !book.author.lowercase().contains(state.authorQuery.lowercase())) return@filter false

            // 4. Publisher Filter
            if (state.publisherQuery.isNotEmpty() && !book.publisher.lowercase().contains(state.publisherQuery.lowercase())) return@filter false

            // 5. Page Count Range
            if (state.minPageCount != null && book.pageCount < state.minPageCount) return@filter false
            if (state.maxPageCount != null && book.pageCount > state.maxPageCount) return@filter false

            // 6. Rating
            if (book.rating < state.minRating) return@filter false

            // 7. Status Filter (from Bottom Sheet)
            if (state.selectedStatus != null && book.status != state.selectedStatus) return@filter false

            // 8. Tags (Must contain all selected tags)
            if (state.selectedTags.isNotEmpty() && !book.tags.containsAll(state.selectedTags)) return@filter false

            true
        }
    }
}
