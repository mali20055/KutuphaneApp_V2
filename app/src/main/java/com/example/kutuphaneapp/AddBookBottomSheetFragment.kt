package com.example.kutuphaneapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kutuphaneapp.model.Book
import com.google.gson.Gson

class AddBookBottomSheetFragment : BottomSheetDialogFragment() {

    private var book: Book? = null
    private val tags = mutableListOf<String>()

    private lateinit var ivBookPreview: ImageView
    private lateinit var tvTitlePreview: TextView
    private lateinit var tvAuthorPreview: TextView
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var chipToRead: Chip
    private lateinit var etTag: EditText
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val bookJson = it.getString(ARG_BOOK)
            book = Gson().fromJson(bookJson, Book::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_book_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        ivBookPreview = view.findViewById(R.id.ivBookPreview)
        tvTitlePreview = view.findViewById(R.id.tvTitlePreview)
        tvAuthorPreview = view.findViewById(R.id.tvAuthorPreview)
        chipGroupStatus = view.findViewById(R.id.chipGroupStatus)
        chipToRead = view.findViewById(R.id.chipToRead)
        etTag = view.findViewById(R.id.etTag)
        chipGroupTags = view.findViewById(R.id.chipGroupTags)
        btnSave = view.findViewById(R.id.btnSave)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        book?.let {
            tvTitlePreview.text = it.title
            tvAuthorPreview.text = it.author
            Glide.with(this)
                .load(it.coverUrl)
                .placeholder(R.color.input_background)
                .into(ivBookPreview)
        }
        chipToRead.isChecked = true
    }

    private fun setupListeners() {
        etTag.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val tag = etTag.text.toString().trim()
                if (tag.isNotEmpty()) {
                    addTagChip(tag)
                    etTag.text.clear()
                }
                true
            } else {
                false
            }
        }

        btnSave.setOnClickListener {
            saveBookToFirestore()
        }
    }

    private fun addTagChip(tag: String) {
        if (!tags.contains(tag)) {
            tags.add(tag)
            val chip = Chip(context)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                chipGroupTags.removeView(chip)
                tags.remove(tag)
            }
            chipGroupTags.addView(chip)
        }
    }

    private fun saveBookToFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentBook = book ?: return

        val status = when (chipGroupStatus.checkedChipId) {
            R.id.chipReading -> "reading"
            R.id.chipFinished -> "read"
            else -> "to_read"
        }

        val updatedBook = currentBook.copy(
            status = status,
            tags = tags,
            addedAt = System.currentTimeMillis()
        )

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("books").document(updatedBook.id)
            .set(updatedBook)
            .addOnSuccessListener {
                Toast.makeText(context, "Kitap kütüphaneye eklendi!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Hata oluştu!", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        const val TAG = "AddBookBottomSheet"
        private const val ARG_BOOK = "arg_book"

        fun newInstance(book: Book): AddBookBottomSheetFragment {
            val fragment = AddBookBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_BOOK, Gson().toJson(book))
            fragment.arguments = args
            return fragment
        }
    }
}
