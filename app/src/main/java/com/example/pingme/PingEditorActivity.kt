package com.example.pingme

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import kotlin.random.Random

class PingEditorActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isFlyingHeartsActive = true
    private lateinit var bgAnimationLayer: ConstraintLayout

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null

    private var finalAudioBase64: String = ""
    private var finalAudioName: String = "Suara Bawaan"

    private lateinit var tvRecordText: TextView
    private lateinit var tvImportText: TextView

    private val requestMicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) mulaiAtauBerhentiRekam()
        else Toast.makeText(this, "Izin Mic ditolak! Tidak bisa merekam.", Toast.LENGTH_SHORT).show()
    }

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    if (bytes.size > 1024 * 1024) {
                        Toast.makeText(this, "File terlalu besar! Maks 1MB.", Toast.LENGTH_LONG).show()
                        return@registerForActivityResult
                    }
                    finalAudioBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                    finalAudioName = "Lagu_Pilihan.mp3"
                    tvImportText.text = "Lagu Tersimpan!"
                    tvRecordText.text = "Rekam"
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membaca lagu.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ping_editor)

        bgAnimationLayer = findViewById(R.id.bgAnimationLayer)
        // Keep this input-heavy screen calm and battery-friendly.
        isFlyingHeartsActive = false

        val etPingName = findViewById<EditText>(R.id.etPingName)
        val switchSilent = findViewById<SwitchMaterial>(R.id.switchSilent)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnRecord = findViewById<MaterialCardView>(R.id.btnRecord)
        val btnImport = findViewById<MaterialCardView>(R.id.btnImport)

        tvRecordText = findViewById(R.id.tvRecordText)
        tvImportText = findViewById(R.id.tvImportText)

        btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                mulaiAtauBerhentiRekam()
            } else {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        btnImport.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        btnSave.setOnClickListener {
            val title = etPingName.text.toString().trim()
            val isSilent = switchSilent.isChecked

            if (title.isEmpty()) {
                Toast.makeText(this, "Isi pesan layarnya dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isRecording) {
                Toast.makeText(this, "Matikan dulu rekamannya!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Nonaktifkan tombol sementara agar tidak diklik dua kali
            btnSave.isEnabled = false
            simpanKeFirebase(title, isSilent, finalAudioBase64, finalAudioName)
        }
    }

    private fun mulaiAtauBerhentiRekam() {
        if (!isRecording) {
            try {
                // PENGAMAN: Gunakan cacheDir internal yang pasti ada di semua HP
                audioFile = File(cacheDir, "rekaman_cinta.3gp")
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(audioFile!!.absolutePath)
                    prepare()
                    start()
                }
                isRecording = true
                tvRecordText.text = "Berhenti ⏹️"
                tvRecordText.setTextColor(android.graphics.Color.RED)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal merekam: Mic digunakan aplikasi lain", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false

                tvRecordText.text = "Rekaman Siap!"
                tvRecordText.setTextColor(android.graphics.Color.parseColor("#5C454A"))
                tvImportText.text = "Pilih Lagu"

                if (audioFile?.exists() == true) {
                    val bytes = audioFile!!.readBytes()
                    finalAudioBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                    finalAudioName = "Voice_Note.3gp"
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal menyimpan rekaman", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun simpanKeFirebase(title: String, isSilent: Boolean, audioBase64: String, audioName: String) {
        try {
            val auth = FirebaseAuth.getInstance()
            val myUid = auth.currentUser?.uid ?: return

            val urlDb = "https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app"
            val database = FirebaseDatabase.getInstance(urlDb).reference

            val idBaru = database.push().key ?: return

            val dataPing = mapOf(
                "id" to idBaru,
                "title" to title,
                "isSilent" to isSilent,
                "audioData" to audioBase64,
                "audioName" to audioName
            )

            database.child("connections").child(myUid).child("pings").child(idBaru).setValue(dataPing)
                .addOnSuccessListener {
                    Toast.makeText(this, "Alarm berhasil disiapkan! 🚀", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan alarm.", Toast.LENGTH_SHORT).show()
                    findViewById<MaterialButton>(R.id.btnSave).isEnabled = true
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Terjadi kesalahan sistem", Toast.LENGTH_SHORT).show()
            findViewById<MaterialButton>(R.id.btnSave).isEnabled = true
        }
    }

    private fun startFlyingHearts() {
        val heartRunnable = object : Runnable {
            override fun run() {
                if (isFlyingHeartsActive && !isDestroyed) {
                    spawnHeart()
                    handler.postDelayed(this, Random.nextLong(400, 900))
                }
            }
        }
        handler.post(heartRunnable)
    }

    private fun spawnHeart() {
        val heart = ImageView(this)
        heart.setImageResource(R.drawable.ic_love)
        val sizePx = (Random.nextInt(20, 50) * resources.displayMetrics.density).toInt()
        heart.layoutParams = ConstraintLayout.LayoutParams(sizePx, sizePx)
        bgAnimationLayer.addView(heart)

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        heart.x = Random.nextFloat() * screenWidth
        heart.y = screenHeight
        heart.alpha = Random.nextFloat() * 0.3f + 0.1f

        heart.animate().x(heart.x + Random.nextInt(-150, 150)).y(-sizePx.toFloat()).alpha(0f)
            .rotation(Random.nextInt(-45, 45).toFloat()).setDuration(Random.nextLong(3000, 6000))
            .setInterpolator(LinearInterpolator()).withEndAction { bgAnimationLayer.removeView(heart) }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isFlyingHeartsActive = false
        handler.removeCallbacksAndMessages(null)
        try { mediaRecorder?.release() } catch (e: Exception) {}
    }
}
