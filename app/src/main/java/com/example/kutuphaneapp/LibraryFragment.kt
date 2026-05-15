package com.example.kutuphaneapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kutuphaneapp.viewmodel.LibraryViewModel
import com.google.android.material.tabs.TabLayout

class LibraryFragment : Fragment() {

    private val viewModel: LibraryViewModel by viewModels()
    private lateinit var adapter: BookAdapter

    private lateinit var rvBooks: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var btnFilter: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        rvBooks = view.findViewById(R.id.rv_books)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        etSearch = view.findViewById(R.id.et_search)
        tabLayout = view.findViewById(R.id.tab_layout)
        btnFilter = view.findViewById(R.id.btn_filter)
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java)
            intent.putExtra("book", book)
            startActivity(intent)
        }
        rvBooks.layoutManager = LinearLayoutManager(requireContext())
        rvBooks.adapter = adapter
    }

    private fun setupListeners() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.setSelectedTab(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFilter.setOnClickListener {
            val bottomSheet = FilterBottomSheetFragment()
            bottomSheet.show(childFragmentManager, FilterBottomSheetFragment.TAG)
        }
    }

    private fun observeViewModel() {
        viewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            adapter.updateList(books)
            if (books.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvBooks.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvBooks.visibility = View.VISIBLE
            }
        }
    }
}
