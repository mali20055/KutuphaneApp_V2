package com.example.kutuphaneapp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var btnSendReset: MaterialButton
    private lateinit var progressSendReset: CircularProgressIndicator
    private var sendResetButtonText: CharSequence = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        btnSendReset = findViewById(R.id.btnSendReset)
        progressSendReset = findViewById(R.id.progressSendReset)
        sendResetButtonText = btnSendReset.text

        btnBack.setOnClickListener {
            finish()
        }

        btnSendReset.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta adresinizi girin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        setLoading(false)
                        Toast.makeText(
                            this,
                            "Bu e-posta adresi kayıtlı değil veya bir hata oluştu.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSendReset.isEnabled = !loading
        btnSendReset.text = if (loading) "" else sendResetButtonText
        progressSendReset.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
