package com.example.kutuphaneapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.util.StorageUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class ManualAddActivity : AppCompatActivity() {

    private lateinit var cvCoverPicker: MaterialCardView
    private lateinit var ivCoverPreview: ImageView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var etTitle: TextInputEditText
    private lateinit var etAuthor: TextInputEditText
    private lateinit var etPublisher: TextInputEditText
    private lateinit var etPublishYear: TextInputEditText
    private lateinit var etPageCount: TextInputEditText
    private lateinit var etIsbn: TextInputEditText
    private lateinit var etTags: TextInputEditText
    private lateinit var cgStatus: ChipGroup
    private lateinit var cgTags: ChipGroup
    private lateinit var btnSave: MaterialButton
    private lateinit var tilTitle: TextInputLayout

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tagsList = mutableListOf<String>()
    private var pendingCoverUri: Uri? = null
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
                pendingCoverUri = it
                Glide.with(this).load(it).centerCrop().into(ivCoverPreview)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_add)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        cvCoverPicker = findViewById(R.id.cvCoverPicker)
        ivCoverPreview = findViewById(R.id.ivCoverPreview)
        progressIndicator = findViewById(R.id.progressIndicator)
        etTitle = findViewById(R.id.et_title)
        etAuthor = findViewById(R.id.et_author)
        etPublisher = findViewById(R.id.et_publisher)
        etPublishYear = findViewById(R.id.et_publish_year)
        etPageCount = findViewById(R.id.et_page_count)
        etIsbn = findViewById(R.id.et_isbn)
        etTags = findViewById(R.id.et_tags)
        cgStatus = findViewById(R.id.cg_status)
        cgTags = findViewById(R.id.cg_tags)
        btnSave = findViewById(R.id.btn_save)
        tilTitle = findViewById(R.id.til_title)

        cvCoverPicker.setOnClickListener { showCoverPickerDialog() }
        setupTagInput()
        btnSave.setOnClickListener { saveBook() }
    }

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
            setToolbarColor(ContextCompat.getColor(this@ManualAddActivity, R.color.primary))
            setStatusBarColor(ContextCompat.getColor(this@ManualAddActivity, R.color.primary))
        }
        val intent = UCrop.of(sourceUri, Uri.fromFile(destFile))
            .withOptions(options)
            .withMaxResultSize(1024, 1024)
            .getIntent(this)
        uCropLauncher.launch(intent)
    }

    private fun setupTagInput() {
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

        val userId = auth.currentUser?.uid ?: return
        val bookRef = firestore.collection("users").document(userId).collection("books").document()

        val status = when (cgStatus.checkedChipId) {
            R.id.chip_reading -> "reading"
            R.id.chip_read -> "read"
            else -> "to_read"
        }

        val book = Book(
            id = bookRef.id,
            title = title,
            author = etAuthor.text.toString().trim(),
            publisher = etPublisher.text.toString().trim(),
            publishedDate = etPublishYear.text.toString().trim(),
            pageCount = etPageCount.text.toString().trim().toIntOrNull() ?: 0,
            isbn = etIsbn.text.toString().trim(),
            status = status,
            tags = tagsList.toList(),
            addedAt = System.currentTimeMillis(),
            coverUrl = ""
        )

        btnSave.isEnabled = false
        progressIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val coverUrl = if (pendingCoverUri != null) {
                    StorageUtil.uploadBookCover(userId, book.id, pendingCoverUri!!)
                } else ""

                bookRef.set(book.copy(coverUrl = coverUrl)).await()
                Toast.makeText(
                    this@ManualAddActivity,
                    getString(R.string.msg_book_added),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                progressIndicator.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this@ManualAddActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
