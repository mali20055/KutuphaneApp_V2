package com.example.kutuphaneapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.repository.BooksRepository
import com.example.kutuphaneapp.viewmodel.SettingsViewModel
import com.example.kutuphaneapp.worker.ReminderWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels()
    private val booksRepository = BooksRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var tvThemeValue: TextView
    private lateinit var tvLanguageValue: TextView
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var rowReminderTime: View
    private lateinit var tvReminderTimeValue: TextView
    private lateinit var tvSortValue: TextView
    private lateinit var tvViewModeValue: TextView
    private lateinit var tvVersionValue: TextView

    private lateinit var csvExportLauncher: ActivityResultLauncher<Intent>
    private lateinit var csvImportLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermLauncher: ActivityResultLauncher<String>

    private var exportCsvData: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupActivityResultLaunchers()
        setupClickListeners(view)
        observeSettings()
    }

    private fun bindViews(view: View) {
        tvThemeValue = view.findViewById(R.id.tvThemeValue)
        tvLanguageValue = view.findViewById(R.id.tvLanguageValue)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        rowReminderTime = view.findViewById(R.id.rowReminderTime)
        tvReminderTimeValue = view.findViewById(R.id.tvReminderTimeValue)
        tvSortValue = view.findViewById(R.id.tvSortValue)
        tvViewModeValue = view.findViewById(R.id.tvViewModeValue)
        tvVersionValue = view.findViewById(R.id.tvVersionValue)
        tvVersionValue.text = BuildConfig.VERSION_NAME
    }

    private fun setupActivityResultLaunchers() {
        csvExportLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> writeExportCsvToUri(uri) }
            }
        }

        csvImportLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> importGoodreadsCsv(uri) }
            }
        }

        notificationPermLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                viewModel.setNotificationsEnabled(true)
                scheduleReminder()
            } else {
                switchNotifications.isChecked = false
            }
        }
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.rowTheme).setOnClickListener { showThemeDialog() }
        view.findViewById<View>(R.id.rowLanguage).setOnClickListener { showLanguageDialog() }
        view.findViewById<View>(R.id.innerRowReminderTime).setOnClickListener { showTimePickerDialog() }
        view.findViewById<View>(R.id.rowSortOrder).setOnClickListener { showSortDialog() }
        view.findViewById<View>(R.id.rowViewMode).setOnClickListener { showViewModeDialog() }
        view.findViewById<View>(R.id.rowExportCsv).setOnClickListener { startCsvExport() }
        view.findViewById<View>(R.id.rowImportCsv).setOnClickListener { startCsvImport() }
        view.findViewById<View>(R.id.rowBackup).setOnClickListener { backupToFirebase() }
        view.findViewById<View>(R.id.rowRestore).setOnClickListener { confirmAndRestore() }
        view.findViewById<View>(R.id.rowChangeEmail).setOnClickListener { showChangeEmailDialog() }
        view.findViewById<View>(R.id.rowChangePassword).setOnClickListener { showChangePasswordDialog() }
        view.findViewById<View>(R.id.rowDeleteAccount).setOnClickListener { showDeleteAccountDialog() }
        view.findViewById<View>(R.id.rowSignOut).setOnClickListener { confirmSignOut() }
        view.findViewById<View>(R.id.rowPrivacyPolicy).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy")))
        }
        view.findViewById<View>(R.id.rowFeedback).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@example.com"))
                putExtra(Intent.EXTRA_SUBJECT, "KütüphaneApp Geri Bildirim")
            }
            startActivity(intent)
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermissionAndEnable()
            } else {
                viewModel.setNotificationsEnabled(false)
                cancelReminder()
            }
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.themeMode.collect { mode ->
                        tvThemeValue.text = when (mode) {
                            "light" -> getString(R.string.settings_theme_light)
                            "dark" -> getString(R.string.settings_theme_dark)
                            else -> getString(R.string.settings_theme_system)
                        }
                    }
                }

                launch {
                    viewModel.appLanguage.collect { lang ->
                        tvLanguageValue.text = when (lang) {
                            "en" -> getString(R.string.settings_language_en)
                            else -> getString(R.string.settings_language_tr)
                        }
                    }
                }

                launch {
                    viewModel.notificationsEnabled.collect { enabled ->
                        switchNotifications.setOnCheckedChangeListener(null)
                        switchNotifications.isChecked = enabled
                        rowReminderTime.visibility = if (enabled) View.VISIBLE else View.GONE
                        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) requestNotificationPermissionAndEnable()
                            else {
                                viewModel.setNotificationsEnabled(false)
                                cancelReminder()
                            }
                        }
                    }
                }

                launch {
                    viewModel.reminderHour.combine(viewModel.reminderMinute) { h, m ->
                        String.format("%02d:%02d", h, m)
                    }.collect { tvReminderTimeValue.text = it }
                }

                launch {
                    viewModel.defaultSortOrder.collect { order ->
                        tvSortValue.text = when (order) {
                            "title" -> getString(R.string.settings_sort_title)
                            "author" -> getString(R.string.settings_sort_author)
                            "rating" -> getString(R.string.settings_sort_rating)
                            else -> getString(R.string.settings_sort_date)
                        }
                    }
                }

                launch {
                    viewModel.defaultViewMode.collect { mode ->
                        tvViewModeValue.text = when (mode) {
                            "grid" -> getString(R.string.settings_view_grid)
                            else -> getString(R.string.settings_view_list)
                        }
                    }
                }
            }
        }
    }

    // ── Tema ──────────────────────────────────────────────────────────────────

    private fun showThemeDialog() {
        val options = arrayOf(
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark),
            getString(R.string.settings_theme_system)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_select_theme)
            .setItems(options) { _, which ->
                val mode = when (which) {
                    0 -> "light"
                    1 -> "dark"
                    else -> "system"
                }
                viewModel.setThemeMode(mode)
                AppCompatDelegate.setDefaultNightMode(
                    when (mode) {
                        "light" -> AppCompatDelegate.MODE_NIGHT_NO
                        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
            }
            .show()
    }

    // ── Dil ───────────────────────────────────────────────────────────────────

    private fun showLanguageDialog() {
        val options = arrayOf(
            getString(R.string.settings_language_tr),
            getString(R.string.settings_language_en)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_select_language)
            .setItems(options) { _, which ->
                val lang = if (which == 0) "tr" else "en"
                viewModel.setAppLanguage(lang)
                requireActivity().recreate()
            }
            .show()
    }

    // ── Bildirimler ───────────────────────────────────────────────────────────

    private fun requestNotificationPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                switchNotifications.isChecked = false
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.setNotificationsEnabled(true)
        scheduleReminder()
    }

    private fun showTimePickerDialog() {
        lifecycleScope.launch {
            val hour = viewModel.reminderHour.first()
            val minute = viewModel.reminderMinute.first()
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(R.string.settings_reminder_time)
                .build()
            picker.addOnPositiveButtonClickListener {
                viewModel.setReminderTime(picker.hour, picker.minute)
                scheduleReminder(picker.hour, picker.minute)
            }
            picker.show(childFragmentManager, "time_picker")
        }
    }

    private fun scheduleReminder() {
        lifecycleScope.launch {
            scheduleReminder(viewModel.reminderHour.first(), viewModel.reminderMinute.first())
        }
    }

    private fun scheduleReminder(hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_MONTH, 1)
        val delay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelReminder() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(ReminderWorker.WORK_NAME)
    }

    // ── Kütüphane ─────────────────────────────────────────────────────────────

    private fun showSortDialog() {
        val options = arrayOf(
            getString(R.string.settings_sort_date),
            getString(R.string.settings_sort_title),
            getString(R.string.settings_sort_author),
            getString(R.string.settings_sort_rating)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_select_sort)
            .setItems(options) { _, which ->
                viewModel.setDefaultSortOrder(
                    when (which) { 1 -> "title"; 2 -> "author"; 3 -> "rating"; else -> "date" }
                )
            }
            .show()
    }

    private fun showViewModeDialog() {
        val options = arrayOf(
            getString(R.string.settings_view_list),
            getString(R.string.settings_view_grid)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_select_view)
            .setItems(options) { _, which ->
                viewModel.setDefaultViewMode(if (which == 0) "list" else "grid")
            }
            .show()
    }

    // ── CSV Dışa Aktar ────────────────────────────────────────────────────────

    private fun startCsvExport() {
        lifecycleScope.launch {
            try {
                val books = booksRepository.getUserBooksOnce()
                val sb = StringBuilder()
                sb.append("id,title,author,publisher,pageCount,status,rating,tags,addedAt\n")
                books.forEach { b ->
                    sb.append("${b.id},")
                    sb.append("\"${b.title.replace("\"", "\"\"")}\",")
                    sb.append("\"${b.author.replace("\"", "\"\"")}\",")
                    sb.append("\"${b.publisher.replace("\"", "\"\"")}\",")
                    sb.append("${b.pageCount},")
                    sb.append("${b.status},")
                    sb.append("${b.rating},")
                    sb.append("\"${b.tags.joinToString("|")}\",")
                    sb.append("${b.addedAt}\n")
                }
                exportCsvData = sb.toString()

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TITLE, "kutuphanem_${System.currentTimeMillis()}.csv")
                }
                csvExportLauncher.launch(intent)
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    private fun writeExportCsvToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(exportCsvData.toByteArray(Charsets.UTF_8))
                    }
                }
                showSnackbar(getString(R.string.msg_export_success))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    // ── Goodreads CSV İçe Aktar ───────────────────────────────────────────────

    private fun startCsvImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
        }
        csvImportLauncher.launch(intent)
    }

    private fun importGoodreadsCsv(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText() ?: ""
                }
                val lines = content.lines().filter { it.isNotBlank() }
                if (lines.size < 2) return@launch

                val header = parseCsvLine(lines.first())
                fun idx(vararg names: String) =
                    names.firstNotNullOfOrNull { n -> header.indexOfFirst { it.trim() == n }.takeIf { it >= 0 } } ?: -1

                val titleIdx = idx("Title")
                val authorIdx = idx("Author", "Author l-f")
                val ratingIdx = idx("My Rating")
                val pagesIdx = idx("Number of Pages")
                val publisherIdx = idx("Publisher")
                val shelfIdx = idx("Exclusive Shelf", "Bookshelves")

                val books = lines.drop(1).mapNotNull { line ->
                    try {
                        val cols = parseCsvLine(line)
                        val title = cols.getOrElse(titleIdx) { "" }.trim().ifEmpty { return@mapNotNull null }
                        Book(
                            id = "",
                            title = title,
                            author = cols.getOrElse(authorIdx) { "" }.trim(),
                            publisher = if (publisherIdx >= 0) cols.getOrElse(publisherIdx) { "" }.trim() else "",
                            pageCount = if (pagesIdx >= 0) cols.getOrElse(pagesIdx) { "0" }.trim().toIntOrNull() ?: 0 else 0,
                            rating = if (ratingIdx >= 0) cols.getOrElse(ratingIdx) { "0" }.trim().toFloatOrNull() ?: 0f else 0f,
                            status = when (if (shelfIdx >= 0) cols.getOrElse(shelfIdx) { "" }.trim() else "") {
                                "read" -> "read"
                                "currently-reading" -> "reading"
                                else -> "to_read"
                            },
                            addedAt = System.currentTimeMillis()
                        )
                    } catch (e: Exception) { null }
                }

                val batch = db.batch()
                books.forEach { book ->
                    val ref = db.collection("users").document(uid).collection("books").document()
                    batch.set(ref, book.copy(id = ref.id))
                }
                withContext(Dispatchers.IO) { batch.commit().await() }
                showSnackbar("${books.size} ${getString(R.string.msg_import_success)}")
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }

    // ── Firebase Yedekleme ────────────────────────────────────────────────────

    private fun backupToFirebase() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val books = booksRepository.getUserBooksOnce()
                val json = Gson().toJson(books).toByteArray(Charsets.UTF_8)
                withContext(Dispatchers.IO) {
                    storage.reference.child("covers/$uid/backup.json").putBytes(json).await()
                }
                showSnackbar(getString(R.string.msg_backup_success))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    private fun confirmAndRestore() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_restore_confirm)
            .setMessage(R.string.dialog_restore_message)
            .setPositiveButton(R.string.btn_confirm) { _, _ -> restoreFromFirebase() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun restoreFromFirebase() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    storage.reference.child("covers/$uid/backup.json")
                        .getBytes(10 * 1024 * 1024L).await()
                }
                val type = object : TypeToken<List<Book>>() {}.type
                val books: List<Book> = Gson().fromJson(String(bytes, Charsets.UTF_8), type)

                val batch = db.batch()
                books.forEach { book ->
                    val ref = db.collection("users").document(uid).collection("books").document(book.id)
                    batch.set(ref, book)
                }
                withContext(Dispatchers.IO) { batch.commit().await() }
                showSnackbar(getString(R.string.msg_restore_success))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    // ── Hesap İşlemleri ───────────────────────────────────────────────────────

    private fun showChangeEmailDialog() {
        val currentPwField = editTextOf(getString(R.string.hint_current_password), InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val newEmailField = editTextOf(getString(R.string.hint_new_email), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_change_email)
            .setView(verticalContainer(currentPwField, newEmailField))
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val pw = currentPwField.text.toString()
                val email = newEmailField.text.toString()
                if (pw.isNotEmpty() && email.isNotEmpty()) changeEmail(pw, email)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun changeEmail(currentPassword: String, newEmail: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email ?: return, currentPassword)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    user.reauthenticate(credential).await()
                    user.updateEmail(newEmail).await()
                }
                showSnackbar(getString(R.string.msg_email_changed))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    private fun showChangePasswordDialog() {
        val currentPwField = editTextOf(getString(R.string.hint_current_password), InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val newPwField = editTextOf(getString(R.string.hint_new_password), InputType.TYPE_TEXT_VARIATION_PASSWORD)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_change_password)
            .setView(verticalContainer(currentPwField, newPwField))
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                val cur = currentPwField.text.toString()
                val new = newPwField.text.toString()
                if (cur.isNotEmpty() && new.isNotEmpty()) changePassword(cur, new)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email ?: return, currentPassword)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    user.reauthenticate(credential).await()
                    user.updatePassword(newPassword).await()
                }
                showSnackbar(getString(R.string.msg_password_changed))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    private fun showDeleteAccountDialog() {
        val pwField = editTextOf(getString(R.string.hint_current_password), InputType.TYPE_TEXT_VARIATION_PASSWORD)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_account)
            .setMessage(R.string.dialog_delete_account_message)
            .setView(verticalContainer(pwField))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                val pw = pwField.text.toString()
                if (pw.isNotEmpty()) deleteAccount(pw)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun deleteAccount(currentPassword: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email ?: return, currentPassword)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    user.reauthenticate(credential).await()
                    user.delete().await()
                }
                navigateToLogin()
            } catch (e: Exception) {
                showSnackbar(getString(R.string.msg_operation_failed))
            }
        }
    }

    private fun confirmSignOut() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_logout_title)
            .setMessage(R.string.dialog_logout_message)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                auth.signOut()
                navigateToLogin()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    // ── Yardımcı fonksiyonlar ─────────────────────────────────────────────────

    private fun editTextOf(hint: String, inputVariation: Int): EditText =
        EditText(requireContext()).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT or inputVariation
        }

    private fun verticalContainer(vararg views: View): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            views.forEach { addView(it) }
        }

    private fun showSnackbar(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }
}
