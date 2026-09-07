package com.example.pingme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class ScannerActivity : AppCompatActivity() {

    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private val auth = FirebaseAuth.getInstance()
    private val urlDatabase = "https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val database = FirebaseDatabase.getInstance(urlDatabase).reference

    // =========================================================
    // MESIN PENERIMA GAMBAR DARI GALERI
    // =========================================================
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            bacaQRdariGaleri(uri)
        } else {
            barcodeScannerView.resume() // Jika batal pilih gambar, nyalakan kamera lagi
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        barcodeScannerView = findViewById(R.id.barcode_scanner)
        val btnGallery = findViewById<MaterialButton>(R.id.btnGallery)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Tombol Kembali
        btnBack.setOnClickListener { finish() }

        // 1. FITUR BACA DARI KAMERA LANGSUNG
        barcodeScannerView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { scannedUid ->
                    barcodeScannerView.pause() // Hentikan kamera biar tidak dobel scan
                    hubungkanDenganPasangan(scannedUid)
                }
            }
        })

        // 2. FITUR BUKA GALERI
        btnGallery.setOnClickListener {
            barcodeScannerView.pause() // Matikan kamera sementara saat buka galeri
            pickImageLauncher.launch("image/*")
        }
    }

    // =========================================================
    // LOGIKA MEMBACA QR CODE DARI FOTO GALERI
    // =========================================================
    private fun bacaQRdariGaleri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    val intArray = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                    val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                    val reader = MultiFormatReader()
                    val result = reader.decode(binaryBitmap)

                    val scannedUid = result.text
                    hubungkanDenganPasangan(scannedUid) // Jika ketemu, langsung hubungkan!
                } else {
                    Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                    barcodeScannerView.resume()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "QR Code tidak ditemukan di foto tersebut!", Toast.LENGTH_LONG).show()
            barcodeScannerView.resume() // Nyalakan kamera lagi jika gagal
        }
    }

    // =========================================================
    // LOGIKA IKATAN CINTA (Menyimpan di Firebase secara 2 Arah)
    // =========================================================
    private fun hubungkanDenganPasangan(partnerUid: String) {
        val myUid = auth.currentUser?.uid ?: return

        // Mencegah pengguna scan QR Code miliknya sendiri
        if (myUid == partnerUid) {
            Toast.makeText(this, "Jangan scan QR milikmu sendiri!", Toast.LENGTH_SHORT).show()
            barcodeScannerView.resume()
            return
        }

        // Menyimpan ikatan di Firebase (Akun A terikat ke B, Akun B terikat ke A)
        val updates = hashMapOf<String, Any>(
            "connections/$myUid/connectedTo" to partnerUid,
            "connections/$partnerUid/connectedTo" to myUid
        )

        database.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Berhasil terhubung dengan Si Dia! 💖", Toast.LENGTH_LONG).show()
            finish() // Tutup layar scanner, kembali ke Dashboard
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal terhubung, coba lagi.", Toast.LENGTH_SHORT).show()
            barcodeScannerView.resume()
        }
    }

    // Wajib ada agar kamera menyala saat layar ini aktif
    override fun onResume() {
        super.onResume()
        barcodeScannerView.resume()
    }

    // Wajib ada agar kamera mati saat layar ini ditutup (hemat baterai)
    override fun onPause() {
        super.onPause()
        barcodeScannerView.pause()
    }
}