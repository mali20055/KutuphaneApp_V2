package com.example.kutuphaneapp.viewmodel

import android.net.Uri
import androidx.lifecycle.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _userData = MutableLiveData<Map<String, Any?>>()
    val userData: LiveData<Map<String, Any?>> = _userData

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _profileImageUrl = MutableLiveData<String>()
    val profileImageUrl: LiveData<String> = _profileImageUrl

    fun loadProfile() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = db.collection("users").document(userId).get().await()
                val data = doc.data ?: emptyMap()
                _userData.value = data
                _profileImageUrl.value = data["profileImageUrl"] as? String ?: ""
            } catch (e: Exception) {
                _statusMessage.value = "Profil bilgileri yüklenemedi: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val profileRef = storage.reference.child("covers").child(userId).child("profile.jpg")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                profileRef.putFile(uri).await()
                val downloadUri = profileRef.downloadUrl.await()
                db.collection("users").document(userId)
                    .update("profileImageUrl", downloadUri.toString()).await()
                
                _profileImageUrl.value = downloadUri.toString()
                _statusMessage.value = "Profil fotoğrafı güncellendi"
            } catch (e: Exception) {
                _statusMessage.value = "Fotoğraf yüklenemedi: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(
        newDisplayName: String,
        newEmail: String,
        currentPassword: String,
        newPassword: String
    ) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        if (newDisplayName.isEmpty() || newEmail.isEmpty()) {
            _statusMessage.value = "Kullanıcı adı ve e-posta boş olamaz"
            return
        }

        val requiresSensitiveUpdate = (newEmail != currentUser.email) || newPassword.isNotEmpty()
        if (requiresSensitiveUpdate && currentPassword.isEmpty()) {
            _statusMessage.value = "E-posta/şifre değişimi için mevcut şifre zorunlu"
            return
        }

        if (newPassword.isNotEmpty() && newPassword.length < 6) {
            _statusMessage.value = "Yeni şifre en az 6 karakter olmalı"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (requiresSensitiveUpdate) {
                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                    currentUser.reauthenticate(credential).await()

                    if (newEmail != currentUser.email) {
                        currentUser.updateEmail(newEmail).await()
                    }

                    if (newPassword.isNotEmpty()) {
                        currentUser.updatePassword(newPassword).await()
                    }
                }

                val updateMap = hashMapOf<String, Any>(
                    "displayName" to newDisplayName,
                    "fullName" to newDisplayName,
                    "email" to newEmail
                )

                db.collection("users").document(userId)
                    .set(updateMap, SetOptions.merge()).await()

                _statusMessage.value = "Profil bilgileri güncellendi"
                loadProfile() // Refresh data
            } catch (e: Exception) {
                _statusMessage.value = "Güncelleme hatası: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun logout() {
        auth.signOut()
    }
}
