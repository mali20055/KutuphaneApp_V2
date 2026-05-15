package com.example.kutuphaneapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.model.Book
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BookDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var book: Book? = null
    private var userId: String? = null

    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvPublisher: TextView
    private lateinit var tvYear: TextView
    private lateinit var tvPages: TextView
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var ratingBar: RatingBar
    private lateinit var etTag: EditText
    private lateinit var btnAddTag: Button
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var etNote: EditText
    private lateinit var btnSaveNote: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnEdit: ImageButton

    private val editBookLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        book = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("book", Book::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("book")
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        initViews()
        setupData()
        setupListeners()
    }

    private fun initViews() {
        ivCover = findViewById(R.id.iv_cover)
        tvTitle = findViewById(R.id.tv_title)
        tvAuthor = findViewById(R.id.tv_author)
        tvPublisher = findViewById(R.id.tv_publisher)
        tvYear = findViewById(R.id.tv_year)
        tvPages = findViewById(R.id.tv_pages)
        chipGroupStatus = findViewById(R.id.chip_group_status)
        ratingBar = findViewById(R.id.rating_bar)
        etTag = findViewById(R.id.et_tag)
        btnAddTag = findViewById(R.id.btn_add_tag)
        chipGroupTags = findViewById(R.id.chip_group_tags)
        etNote = findViewById(R.id.et_note)
        btnSaveNote = findViewById(R.id.btn_save_note)
        btnDelete = findViewById(R.id.btn_delete)
        btnBack = findViewById(R.id.btn_back)
        btnEdit = findViewById(R.id.btn_edit)
    }

    private fun setupData() {
        book?.let { b ->
            Glide.with(this).load(b.coverUrl).placeholder(R.drawable.ic_menu_book).into(ivCover)
            tvTitle.text = b.title
            tvAuthor.text = b.author
            tvPublisher.text = b.publisher
            tvYear.text = b.publishedDate
            tvPages.text = "${b.pageCount} Sayfa"
            etNote.setText(b.note)
            ratingBar.rating = b.rating

            when (b.status) {
                "reading" -> chipGroupStatus.check(R.id.chip_reading)
                "to_read" -> chipGroupStatus.check(R.id.chip_to_read)
                "read" -> chipGroupStatus.check(R.id.chip_read)
            }

            b.tags.forEach { tag -> addTagChip(tag) }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnEdit.setOnClickListener {
            val intent = Intent(this, EditBookActivity::class.java)
                .putExtra("book", book)
            editBookLauncher.launch(intent)
        }

        chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val newStatus = when (checkedIds[0]) {
                    R.id.chip_reading -> "reading"
                    R.id.chip_to_read -> "to_read"
                    R.id.chip_read -> "read"
                    else -> book?.status ?: "to_read"
                }
                updateFirestoreField("status", newStatus)
            }
        }

        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) updateFirestoreField("rating", rating)
        }

        btnAddTag.setOnClickListener {
            val tagText = etTag.text.toString().trim()
            if (tagText.isNotEmpty()) {
                addTagChip(tagText)
                updateFirestoreArrayField("tags", tagText, true)
                etTag.text.clear()
            }
        }

        btnSaveNote.setOnClickListener {
            updateFirestoreField("note", etNote.text.toString()) {
                Toast.makeText(this, "Not kaydedildi!", Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener { showDeleteConfirmation() }
    }

    private fun addTagChip(tag: String) {
        val chip = Chip(this)
        chip.text = tag
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipGroupTags.removeView(chip)
            updateFirestoreArrayField("tags", tag, false)
        }
        chipGroupTags.addView(chip)
    }

    private fun updateFirestoreField(field: String, value: Any, onSuccess: (() -> Unit)? = null) {
        val uid = userId ?: return
        val bid = book?.id ?: return
        db.collection("users").document(uid)
            .collection("books").document(bid)
            .update(field, value)
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener {
                Toast.makeText(this, "Güncelleme başarısız", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestoreArrayField(field: String, value: String, isAdd: Boolean) {
        val uid = userId ?: return
        val bid = book?.id ?: return
        val updateTask = if (isAdd) FieldValue.arrayUnion(value) else FieldValue.arrayRemove(value)
        db.collection("users").document(uid)
            .collection("books").document(bid)
            .update(field, updateTask)
            .addOnFailureListener {
                Toast.makeText(this, "Etiket güncellenemedi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Kitabı Sil")
            .setMessage("Bu kitabı kütüphanenizden silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                val uid = userId ?: return@setPositiveButton
                val bid = book?.id ?: return@setPositiveButton
                db.collection("users").document(uid)
                    .collection("books").document(bid)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Kitap silindi", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Silme işlemi başarısız", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
