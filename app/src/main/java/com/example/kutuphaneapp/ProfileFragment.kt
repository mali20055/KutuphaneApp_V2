package com.example.kutuphaneapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.viewmodel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.yalantis.ucrop.UCrop
import java.io.File

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var ivProfilePhoto: ShapeableImageView
    private lateinit var btnChangePhoto: MaterialButton
    private lateinit var etDisplayName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
        if (imageUri != null) {
            startCrop(imageUri)
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val resultUri = UCrop.getOutput(data)
            if (resultUri != null) {
                viewModel.uploadImage(resultUri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val data = result.data ?: return@registerForActivityResult
            val cropError = UCrop.getError(data)
            Toast.makeText(requireContext(), cropError?.localizedMessage ?: "Kırpma başarısız", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupClickListeners()
        observeViewModel()
        
        viewModel.loadProfile()
    }

    private fun bindViews(view: View) {
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo)
        btnChangePhoto = view.findViewById(R.id.btn_change_photo)
        etDisplayName = view.findViewById(R.id.et_display_name)
        etEmail = view.findViewById(R.id.et_email)
        etCurrentPassword = view.findViewById(R.id.et_current_password)
        etNewPassword = view.findViewById(R.id.et_new_password)
        btnSaveProfile = view.findViewById(R.id.btn_save_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
    }

    private fun setupClickListeners() {
        btnChangePhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnSaveProfile.setOnClickListener {
            val newDisplayName = etDisplayName.text?.toString()?.trim().orEmpty()
            val newEmail = etEmail.text?.toString()?.trim().orEmpty()
            val currentPassword = etCurrentPassword.text?.toString()?.trim().orEmpty()
            val newPassword = etNewPassword.text?.toString()?.trim().orEmpty()

            viewModel.updateProfile(newDisplayName, newEmail, currentPassword, newPassword)
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner) { data ->
            val displayName = data["displayName"] as? String
                ?: data["fullName"] as? String
                ?: ""
            etDisplayName.setText(displayName)
            etEmail.setText(data["email"] as? String ?: "")
        }

        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            if (isAdded) {
                Glide.with(this)
                    .load(url.ifEmpty { R.drawable.ic_launcher_foreground })
                    .circleCrop()
                    .into(ivProfilePhoto)
            }
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                if (it == "Profil bilgileri güncellendi") {
                    etCurrentPassword.setText("")
                    etNewPassword.setText("")
                }
                viewModel.clearStatusMessage()
            }
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationFile = File(requireContext().cacheDir, "profile_crop_${System.currentTimeMillis()}.jpg")
        val destinationUri = Uri.fromFile(destinationFile)

        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .getIntent(requireContext())

        cropLauncher.launch(cropIntent)
    }
}
