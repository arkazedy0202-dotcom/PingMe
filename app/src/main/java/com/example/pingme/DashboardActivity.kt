package com.example.pingme

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class DashboardActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference

    // =======================================================
    // KUNCI PENGAMAN: Mencegah layar refresh terus-menerus!
    // =======================================================
    private var isHomeLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val layoutConnect = findViewById<ConstraintLayout>(R.id.layout_connect)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val ivQRCode = findViewById<ImageView>(R.id.ivQRCode)
        val tvBackupCode = findViewById<TextView>(R.id.tvBackupCode)
        val btnScanQR = findViewById<MaterialButton>(R.id.btnScanQR)

        val urlDatabaseKamu = "https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(urlDatabaseKamu).reference

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val myUid = currentUser.uid
        tvBackupCode.text = "UID: $myUid"

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(myUid, BarcodeFormat.QR_CODE, 600, 600)
            ivQRCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // NYALAKAN MESIN BACKGROUND SERVICE 24 JAM
        val serviceIntent = Intent(this, PingService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // =======================================================
        // CEK KONEKSI (PENYEBAB CRASH SUDAH DIPERBAIKI DI SINI!)
        // =======================================================
        database.child("connections").child(myUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild("connectedTo")) {
                    // JIKA SUDAH PUNYA PASANGAN
                    layoutConnect.visibility = View.GONE
                    bottomNav.visibility = View.VISIBLE

                    // HANYA MUAT HALAMAN HOME JIKA BELUM DIMUAT SEBELUMNYA
                    if (!isHomeLoaded) {
                        loadFragment(HomeFragment())
                        isHomeLoaded = true // Gembok dikunci!
                    }
                } else {
                    // JIKA PUTUS HUBUNGAN / JOMBLO
                    layoutConnect.visibility = View.VISIBLE
                    bottomNav.visibility = View.GONE
                    isHomeLoaded = false // Buka gembok lagi
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // LOGIKA PINDAH-PINDAH MENU
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_ping -> { loadFragment(PingFragment()); true }
                R.id.nav_profile -> { loadFragment(ProfileFragment()); true }
                else -> false
            }
        }

        btnScanQR.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}