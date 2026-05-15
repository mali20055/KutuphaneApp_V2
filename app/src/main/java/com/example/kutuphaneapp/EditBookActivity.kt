package com.example.kutuphaneapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.viewmodel.EditBookViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class EditBookActivity : AppCompatActivity() {

    private val viewModel: EditBookViewModel by viewModels()

    private lateinit var cvCoverPicker: MaterialCardView
    private lateinit var ivCoverPreview: ImageView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etAuthor: TextInputEditText
    private lateinit var etPublisher: TextInputEditText
    private lateinit var etPublishYear: TextInputEditText
    private lateinit var etPageCount: TextInputEditText
    private lateinit var etIsbn: TextInputEditText
    private lateinit var cgStatus: ChipGroup
    private lateinit var ratingBar: RatingBar
    private lateinit var etNotes: TextInputEditText
    private lateinit var etTags: TextInputEditText
    private lateinit var cgTags: ChipGroup
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDelete: MaterialButton

    private var book: Book? = null
    private val tagsList = mutableListOf<String>()
    private var photoFile: File? = null

    // ── ActivityResult Launchers ──────────────────────────────────────────────

    private val cameraPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, getString(R.string.error_camera_permission), Toast.LENGTH_SHORT).show()
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile!!)
            startCrop(uri)
        }
    }

    private val galleryPickerLauncher = registerForActivityResult(
        PickVisualMedia()
    ) { uri ->
        uri?.let { startCrop(it) }
    }

    private val galleryLegacyLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { startCrop(it) }
    }

    private val uCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!)
            croppedUri?.let {
                viewModel.pendingCoverUri = it
                Glide.with(this).load(it).centerCrop().into(ivCoverPreview)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_book)

        book = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("book", Book::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("book")
        }

        if (book == null) {
            finish()
            return
        }

        initViews()
        populateFields()
        setupListeners()
        observeViewModel()
        setupBackPressHandler()
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.setNavigationOnClickListener { handleBack() }

        cvCoverPicker = findViewById(R.id.cvCoverPicker)
        ivCoverPreview = findViewById(R.id.ivCoverPreview)
        progressIndicator = findViewById(R.id.progressIndicator)
        tilTitle = findViewById(R.id.til_title)
        etTitle = findViewById(R.id.et_title)
        etAuthor = findViewById(R.id.et_author)
        etPublisher = findViewById(R.id.et_publisher)
        etPublishYear = findViewById(R.id.et_publish_year)
        etPageCount = findViewById(R.id.et_page_count)
        etIsbn = findViewById(R.id.et_isbn)
        cgStatus = findViewById(R.id.cg_status)
        ratingBar = findViewById(R.id.ratingBar)
        etNotes = findViewById(R.id.et_notes)
        etTags = findViewById(R.id.et_tags)
        cgTags = findViewById(R.id.cg_tags)
        btnSave = findViewById(R.id.btn_save)
        btnDelete = findViewById(R.id.btn_delete)
    }

    private fun populateFields() {
        val b = book ?: return
        etTitle.setText(b.title)
        etAuthor.setText(b.author)
        etPublisher.setText(b.publisher)
        etPublishYear.setText(b.publishedDate)
        etPageCount.setText(if (b.pageCount > 0) b.pageCount.toString() else "")
        etIsbn.setText(b.isbn)
        etNotes.setText(b.note)
        ratingBar.rating = b.rating

        when (b.status) {
            "reading" -> cgStatus.check(R.id.chip_reading)
            "read" -> cgStatus.check(R.id.chip_read)
            else -> cgStatus.check(R.id.chip_to_read)
        }

        b.tags.forEach { addTagChip(it) }

        if (b.coverUrl.isNotEmpty()) {
            Glide.with(this).load(b.coverUrl).centerCrop().into(ivCoverPreview)
        }
    }

    private fun setupListeners() {
        cvCoverPicker.setOnClickListener { showCoverPickerDialog() }

        etTags.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
                        && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val tagText = etTags.text.toString().trim()
                if (tagText.isNotEmpty()) {
                    addTagChip(tagText)
                    etTags.setText("")
                }
                true
            } else false
        }

        btnSave.setOnClickListener { saveBook() }

        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_book_title)
                .setMessage(R.string.confirm_delete_book_msg)
                .setPositiveButton(R.string.btn_delete) { _, _ ->
                    viewModel.deleteBook(book!!.id)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            btnSave.isEnabled = !loading
            btnDelete.isEnabled = !loading
        }

        viewModel.updateResult.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                Toast.makeText(this, getString(R.string.msg_book_updated), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Hata: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            viewModel.clearUpdateResult()
        }

        viewModel.deleteResult.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                Toast.makeText(this, getString(R.string.btn_delete_book), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Hata: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            viewModel.clearDeleteResult()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })
    }

    private fun handleBack() {
        if (hasChanges()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.msg_unsaved_changes)
                .setMessage(R.string.msg_discard_changes)
                .setPositiveButton(R.string.btn_discard) { _, _ -> finish() }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        } else {
            finish()
        }
    }

    private fun hasChanges(): Boolean {
        val b = book ?: return false
        return etTitle.text.toString() != b.title ||
                etAuthor.text.toString() != b.author ||
                etPublisher.text.toString() != b.publisher ||
                etPublishYear.text.toString() != b.publishedDate ||
                (etPageCount.text.toString().toIntOrNull() ?: 0) != b.pageCount ||
                etIsbn.text.toString() != b.isbn ||
                getSelectedStatus() != b.status ||
                ratingBar.rating != b.rating ||
                etNotes.text.toString() != b.note ||
                tagsList.toList() != b.tags ||
                viewModel.pendingCoverUri != null
    }

    private fun getSelectedStatus() = when (cgStatus.checkedChipId) {
        R.id.chip_reading -> "reading"
        R.id.chip_read -> "read"
        else -> "to_read"
    }

    private fun addTagChip(text: String) {
        if (tagsList.contains(text)) return
        tagsList.add(text)
        val chip = Chip(this)
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            cgTags.removeView(chip)
            tagsList.remove(text)
        }
        cgTags.addView(chip)
    }

    private fun saveBook() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            tilTitle.error = getString(R.string.error_title_required)
            return
        }
        tilTitle.error = null

        val updatedBook = book!!.copy(
            title = title,
            author = etAuthor.text.toString().trim(),
            publisher = etPublisher.text.toString().trim(),
            publishedDate = etPublishYear.text.toString().trim(),
            pageCount = etPageCount.text.toString().trim().toIntOrNull() ?: 0,
            isbn = etIsbn.text.toString().trim(),
            status = getSelectedStatus(),
            rating = ratingBar.rating,
            note = etNotes.text.toString().trim(),
            tags = tagsList.toList()
        )
        viewModel.updateBook(updatedBook)
    }

    // ── Camera / Gallery / UCrop ──────────────────────────────────────────────

    private fun showCoverPickerDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cover_picker_title)
            .setItems(
                arrayOf(
                    getString(R.string.cover_option_camera),
                    getString(R.string.cover_option_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> checkCameraAndLaunch()
                    1 -> launchGallery()
                }
            }
            .show()
    }

    private fun checkCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        photoFile = File.createTempFile("cover_", ".jpg", cacheDir)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile!!)
        cameraLauncher.launch(uri)
    }

    private fun launchGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            galleryPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        } else {
            galleryLegacyLauncher.launch("image/*")
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destFile = File(cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg")
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setToolbarColor(ContextCompat.getColor(this@EditBookActivity, R.color.primary))
            setStatusBarColor(ContextCompat.getColor(this@EditBookActivity, R.color.primary))
        }
        val intent = UCrop.of(sourceUri, Uri.fromFile(destFile))
            .withOptions(options)
            .withMaxResultSize(1024, 1024)
            .getIntent(this)
        uCropLauncher.launch(intent)
    }
}
