package com.example.pingme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PENGAMAN 1: Mencegah crash jika file desain XML bermasalah
        try {
            setContentView(R.layout.activity_register)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat layar pendaftaran.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()

        // Ambil ID dari desain (Pastikan ID sesuai di activity_register.xml)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        // LOGIKA TOMBOL DAFTAR
        btnRegister?.setOnClickListener {
            val email = etEmail?.text.toString().trim()
            val password = etPassword?.text.toString().trim()

            // PENGAMAN 2: Mencegah input kosong
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password terlalu pendek! Minimal 6 karakter.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Matikan tombol sementara agar tidak diklik berkali-kali (Spam)
            btnRegister.isEnabled = false
            btnRegister.text = "Membuat Akun..."

            // Eksekusi Pendaftaran ke Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        // =========================================================
                        // LOGIKA BARU: KEMBALI KE LOGIN MANUAL
                        // =========================================================

                        // 1. Putuskan status "Otomatis Login" dari Firebase
                        auth.signOut()

                        // 2. Beri pesan sukses ke pengguna
                        Toast.makeText(this, "Pendaftaran Berhasil! Silakan Login manual.", Toast.LENGTH_LONG).show()

                        // 3. Tutup layar pendaftaran, yang akan otomatis memunculkan layar Login di bawahnya
                        finish()

                    } else {
                        // PENGAMAN 3: Tangkap error dari Firebase (Misal: Email sudah terdaftar)
                        val errorMsg = task.exception?.message ?: "Terjadi kesalahan sistem."
                        Toast.makeText(this, "Gagal: $errorMsg", Toast.LENGTH_LONG).show()

                        // Nyalakan kembali tombolnya jika gagal
                        btnRegister.isEnabled = true
                        btnRegister.text = "Daftar Sekarang"
                    }
                }
        }

        // LOGIKA TOMBOL KEMBALI KE LOGIN
        tvLogin?.setOnClickListener {
            finish() // Menutup layar pendaftaran
        }
    }
}