package com.example.kutuphaneapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kutuphaneapp.model.Book
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var tvGoalProgress: TextView
    private lateinit var tvGoalPercentage: TextView
    private lateinit var progressGoal: LinearProgressIndicator
    private lateinit var tvTotalBooks: TextView
    private lateinit var tvReadBooks: TextView
    private lateinit var tvReadingBooks: TextView
    private lateinit var tvToReadBooks: TextView
    private lateinit var rvRecentBooks: RecyclerView
    private lateinit var layoutEmptyHome: LinearLayout
    private lateinit var btnAddBookEmpty: MaterialButton
    private lateinit var tvSeeAll: TextView
    private lateinit var btnProfile: ImageButton

    private lateinit var recentBooksAdapter: RecentBooksAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var userListener: ListenerRegistration? = null
    private var booksListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupUser()
        listenToBooks()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        tvUsername = view.findViewById(R.id.tv_username)
        tvGoalProgress = view.findViewById(R.id.tv_goal_progress)
        tvGoalPercentage = view.findViewById(R.id.tv_goal_percentage)
        progressGoal = view.findViewById(R.id.progress_goal)
        tvTotalBooks = view.findViewById(R.id.tv_total_books)
        tvReadBooks = view.findViewById(R.id.tv_read_books)
        tvReadingBooks = view.findViewById(R.id.tv_reading_books)
        tvToReadBooks = view.findViewById(R.id.tv_to_read_books)
        rvRecentBooks = view.findViewById(R.id.rv_recent_books)
        layoutEmptyHome = view.findViewById(R.id.layout_empty_home)
        btnAddBookEmpty = view.findViewById(R.id.btn_add_book_empty)
        tvSeeAll = view.findViewById(R.id.tv_see_all)
        btnProfile = view.findViewById(R.id.btn_profile)

        recentBooksAdapter = RecentBooksAdapter { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java)
            intent.putExtra("book", book)
            startActivity(intent)
        }
        rvRecentBooks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvRecentBooks.adapter = recentBooksAdapter
    }

    private fun setupUser() {
        val currentUser = auth.currentUser
        tvUsername.text = currentUser?.displayName ?: "Kullanıcı"

        // Hedef bilgisini çek (varsayılan 24)
        currentUser?.uid?.let { uid ->
            userListener?.remove()
            userListener = db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                val goal = snapshot?.getLong("readingGoal")?.toInt() ?: 24
                // Mevcut okunan sayısını koruyarak UI'ı güncelle
                val readCount = tvReadBooks.text.toString().toIntOrNull() ?: 0
                updateGoalUI(readCount, goal)
            }
        }
    }

    private fun listenToBooks() {
        val userId = auth.currentUser?.uid ?: return

        booksListener?.remove()
        booksListener = db.collection("users").document(userId).collection("books")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                val books = snapshots.toObjects(Book::class.java)
                
                // İstatistikleri hesapla
                val totalCount = books.size
                val readCount = books.count { it.status == "read" }
                val readingCount = books.count { it.status == "reading" }
                val toReadCount = books.count { it.status == "to_read" }

                // UI güncelle
                tvTotalBooks.text = totalCount.toString()
                tvReadBooks.text = readCount.toString()
                tvReadingBooks.text = readingCount.toString()
                tvToReadBooks.text = toReadCount.toString()

                // Boş durum kontrolü
                layoutEmptyHome.visibility = if (totalCount == 0) View.VISIBLE else View.GONE
                rvRecentBooks.visibility = if (totalCount == 0) View.GONE else View.VISIBLE

                // Okuma Hedefi UI güncelleme
                db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                    val goal = doc?.getLong("readingGoal")?.toInt() ?: 24
                    updateGoalUI(readCount, goal)
                }

                // RecyclerView güncelle (Son 5 kitap)
                recentBooksAdapter.submitList(books.take(5))
            }
    }

    private fun updateGoalUI(current: Int, goal: Int) {
        tvGoalProgress.text = "$current / $goal kitap"
        val percentage = if (goal > 0) (current * 100) / goal else 0
        tvGoalPercentage.text = "%$percentage"
        progressGoal.progress = percentage
    }

    private fun setupClickListeners() {
        tvSeeAll.setOnClickListener {
            (activity as? MainActivity)?.seciliMenuOgesiniAyarla(R.id.nav_kutuphane)
        }

        btnAddBookEmpty.setOnClickListener {
            (activity as? MainActivity)?.seciliMenuOgesiniAyarla(R.id.nav_ekle)
        }

        btnProfile.setOnClickListener {
            (activity as? MainActivity)?.seciliMenuOgesiniAyarla(R.id.nav_profil)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        userListener = null
        booksListener?.remove()
        booksListener = null
    }
}
