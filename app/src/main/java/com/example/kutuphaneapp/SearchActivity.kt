package com.example.kutuphaneapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.viewmodel.SearchViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*

class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchResultAdapter
    private var searchJob: Job? = null
    
    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var progressBar: View
    private lateinit var tvMessage: TextView
    private lateinit var layoutEmptyState: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchTextInputLayout: TextInputLayout

    private val barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val isbn = result.data?.getStringExtra("SCAN_RESULT")
            if (!isbn.isNullOrEmpty()) {
                etSearch.setText(isbn)
                viewModel.searchBooks(isbn)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        toolbar = findViewById(R.id.toolbar)
        etSearch = findViewById(R.id.etSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        progressBar = findViewById(R.id.progressBar)
        tvMessage = findViewById(R.id.tvMessage)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        searchTextInputLayout = findViewById(R.id.searchTextInputLayout)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        observeViewModel()

        // Dışarıdan (AddBookFragment gibi) gelen bir ISBN var mı kontrol et
        val initialIsbn = intent.getStringExtra("EXTRA_ISBN")
        if (!initialIsbn.isNullOrEmpty()) {
            etSearch.setText(initialIsbn)
            viewModel.searchBooks(initialIsbn)
        }
        
        searchTextInputLayout.setEndIconOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            barcodeLauncher.launch(intent)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter { book ->
            showAddBookBottomSheet(book)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500L)
                    val query = s.toString()
                    if (query.isNotEmpty()) {
                        viewModel.searchBooks(query)
                    } else {
                        layoutEmptyState.visibility = View.VISIBLE
                        rvSearchResults.visibility = View.GONE
                        adapter.submitList(emptyList())
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { books ->
            adapter.submitList(books)
            rvSearchResults.visibility = if (books.isNotEmpty()) View.VISIBLE else View.GONE
            layoutEmptyState.visibility = if (books.isEmpty() && viewModel.isLoading.value == false) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                layoutEmptyState.visibility = View.GONE
                tvMessage.visibility = View.GONE
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                tvMessage.text = it
                tvMessage.visibility = View.VISIBLE
            }
        }
    }

    private fun showAddBookBottomSheet(book: Book) {
        val bottomSheet = AddBookBottomSheetFragment.newInstance(book)
        bottomSheet.show(supportFragmentManager, AddBookBottomSheetFragment.TAG)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}
