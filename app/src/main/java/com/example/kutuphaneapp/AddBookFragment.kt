package com.example.kutuphaneapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.kutuphaneapp.model.Book
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AddBookFragment : Fragment() {

    private lateinit var cardSearchApi: MaterialCardView
    private lateinit var cardManualAdd: MaterialCardView
    private lateinit var btnBarcode: View
    private lateinit var rvRecentAdd: RecyclerView
    private lateinit var adapter: RecentBooksAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val isbn = result.data?.getStringExtra("SCAN_RESULT")
            if (!isbn.isNullOrEmpty()) {
                val intent = Intent(requireContext(), SearchActivity::class.java).apply {
                    putExtra("EXTRA_ISBN", isbn)
                }
                startActivity(intent)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardSearchApi = view.findViewById(R.id.card_search_api)
        cardManualAdd = view.findViewById(R.id.card_manual_add)
        btnBarcode = view.findViewById(R.id.btn_barcode)
        rvRecentAdd = view.findViewById(R.id.rv_recent_add)

        setupClickListeners()
        setupRecyclerView()
        loadRecentBooks()
    }

    private fun setupClickListeners() {
        cardSearchApi.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        cardManualAdd.setOnClickListener {
            val intent = Intent(requireContext(), ManualAddActivity::class.java)
            startActivity(intent)
        }

        btnBarcode.setOnClickListener {
            val intent = Intent(requireContext(), BarcodeScannerActivity::class.java)
            barcodeLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = RecentBooksAdapter { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java)
            intent.putExtra("book", book)
            startActivity(intent)
        }
        rvRecentAdd.adapter = adapter
    }

    private fun loadRecentBooks() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(userId)
            .collection("books")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { documents ->
                val bookList = documents.toObjects(Book::class.java)
                adapter.submitList(bookList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Yeni kitap eklenip dönüldüğünde listeyi yenile
        loadRecentBooks()
    }
}
