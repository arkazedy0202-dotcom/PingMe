package com.example.pingme

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

class CallActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_call)

        // ID SUDAH DISESUAIKAN DENGAN XML MILIKMU
        val tvPesan = findViewById<TextView>(R.id.tvPingMessage)
        val btnStop = findViewById<MaterialButton>(R.id.btnStopAlarm)

        // Terima data teks dan mode silent dari Intent
        val pesan = intent.getStringExtra("EXTRA_PESAN") ?: "Ping Masuk!"
        val isSilent = intent.getBooleanExtra("EXTRA_SILENT", false)

        // Ambil lagu yang disembunyikan di memori
        val prefs = getSharedPreferences("PingMePrefs", Context.MODE_PRIVATE)
        val audioData = prefs.getString("TEMP_AUDIO", "") ?: ""
        prefs.edit().remove("TEMP_AUDIO").apply() // Bersihkan agar HP tidak penuh

        tvPesan.text = pesan

        mulaiGetaran()

        if (!isSilent && audioData.isNotEmpty()) {
            putarSuara(audioData)
        }

        btnStop.setOnClickListener {
            matikanAlarmDanMasuk()
        }
    }

    private fun mulaiGetaran() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun putarSuara(base64Audio: String) {
        Thread {
            try {
                val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                val tempFile = File(cacheDir, "temp_ping_audio.3gp")
                val fos = FileOutputStream(tempFile)
                fos.write(audioBytes)
                fos.close()

                runOnUiThread {
                    try {
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

                        mediaPlayer = MediaPlayer().apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val attrs = AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .build()
                                setAudioAttributes(attrs)
                            } else {
                                @Suppress("DEPRECATION")
                                setAudioStreamType(AudioManager.STREAM_ALARM)
                            }

                            setDataSource(tempFile.absolutePath)
                            isLooping = true
                            prepare()
                            start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun matikanAlarmDanMasuk() {
        try { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null } catch (e: Exception) {}
        try { vibrator?.cancel() } catch (e: Exception) {}

        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { mediaPlayer?.release() } catch (e: Exception) {}
        try { vibrator?.cancel() } catch (e: Exception) {}
    }
}