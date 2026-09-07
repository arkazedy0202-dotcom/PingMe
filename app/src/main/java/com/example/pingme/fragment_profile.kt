package com.example.pingme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // =========================================================
        // 1. MEMUAT DATA PROFIL (EMAIL & UID)
        // =========================================================
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvUID = view.findViewById<TextView>(R.id.tvUID)

        val user = FirebaseAuth.getInstance().currentUser
        tvEmail.text = user?.email ?: "Email tidak ditemukan"
        tvUID.text = "UID: ${user?.uid ?: "-"}"

        // =========================================================
        // 2. TOMBOL PUSAT KONTROL IZIN
        // =========================================================
        val btnIzin = view.findViewById<View>(R.id.btnSettings)
        btnIzin?.setOnClickListener {
            bukaPusatKontrolIzin()
        }

        // =========================================================
        // 3. TOMBOL LOGOUT (KELUAR AKUN)
        // =========================================================
        val btnLogout = view.findViewById<View>(R.id.btnLogout)
        btnLogout?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Keluar Akun")
                .setMessage("Yakin ingin keluar dari akun ini?")
                .setPositiveButton("Keluar") { _, _ ->
                    // Hapus sesi Auth
                    FirebaseAuth.getInstance().signOut()

                    // Lempar kembali ke halaman Register/Login
                    val intent = Intent(requireContext(), RegisterActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // =========================================================
        // 4. TOMBOL PUTUS HUBUNGAN (ZONA BERBAHAYA)
        // =========================================================
        val btnDisconnect = view.findViewById<View>(R.id.btnDisconnect)
        btnDisconnect?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Putus Hubungan? 💔")
                .setMessage("Yakin ingin memutuskan koneksi dengan Si Dia? Kamu harus scan QR Code lagi untuk terhubung.")
                .setPositiveButton("Putus") { _, _ ->
                    val myUid = user?.uid ?: return@setPositiveButton
                    val db = FirebaseDatabase.getInstance("https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app").reference

                    // Ambil UID pasangan untuk menghapus status koneksi di HP-nya juga
                    db.child("connections").child(myUid).child("connectedTo").get().addOnSuccessListener { snapshot ->
                        val partnerUid = snapshot.value as? String

                        val updates = hashMapOf<String, Any?>()
                        updates["connections/$myUid/connectedTo"] = null // Hapus di akun kita

                        if (partnerUid != null) {
                            updates["connections/$partnerUid/connectedTo"] = null // Hapus di akun dia
                        }

                        db.updateChildren(updates).addOnSuccessListener {
                            Toast.makeText(requireContext(), "Hubungan resmi diputus.", Toast.LENGTH_SHORT).show()

                            // Lempar kembali ke halaman Scanner
                            val intent = Intent(requireContext(), ScannerActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal memutuskan hubungan", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    // =========================================================
    // FUNGSI PUSAT KONTROL IZIN (TETAP SAMA SEPERTI SEBELUMNYA)
    // =========================================================
    private fun bukaPusatKontrolIzin() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_permissions, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val switchKamera = dialogView.findViewById<SwitchCompat>(R.id.switchKamera)
        val switchMikrofon = dialogView.findViewById<SwitchCompat>(R.id.switchMikrofon)
        val switchPenyimpanan = dialogView.findViewById<SwitchCompat>(R.id.switchPenyimpanan)
        val switchNotifikasi = dialogView.findViewById<SwitchCompat>(R.id.switchNotifikasi)
        val switchOverlay = dialogView.findViewById<SwitchCompat>(R.id.switchOverlay)
        val switchBaterai = dialogView.findViewById<SwitchCompat>(R.id.switchBaterai)

        switchKamera.isChecked = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        switchMikrofon.isChecked = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            switchPenyimpanan.isChecked = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            switchPenyimpanan.isChecked = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        switchNotifikasi.isChecked = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()

        switchOverlay.isChecked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(requireContext())
        } else {
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            switchBaterai.isChecked = pm.isIgnoringBatteryOptimizations(requireContext().packageName)
        } else {
            switchBaterai.isChecked = true
        }

        switchKamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }

        switchMikrofon.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 102)
        }

        switchPenyimpanan.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 103)
                } else {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 103)
                }
            }
        }

        switchNotifikasi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    startActivity(intent)
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${requireContext().packageName}")
                    startActivity(intent)
                }
            }
        }

        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(requireContext())) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
                    startActivity(intent)
                }
            }
        }

        switchBaterai.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            }
        }

        dialog.show()
    }
}