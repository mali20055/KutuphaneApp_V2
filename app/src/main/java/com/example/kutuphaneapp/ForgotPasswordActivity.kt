package com.example.kutuphaneapp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val btnSendReset = findViewById<MaterialButton>(R.id.btnSendReset)

        btnBack.setOnClickListener {
            finish()
        }

        btnSendReset.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta adresinizi girin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                        Toast.makeText(
                            this,
                            "Bu e-posta adresi kayıtlı değil veya bir hata oluştu.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
