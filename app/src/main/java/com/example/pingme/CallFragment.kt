package com.example.pingme

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class CallFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_call, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivHeartPulse = view.findViewById<ImageView>(R.id.ivHeartPulse)
        val tvCallStatus = view.findViewById<TextView>(R.id.tvCallStatus)
        val tvPingMessage = view.findViewById<TextView>(R.id.tvPingMessage)
        val btnStopAlarm = view.findViewById<MaterialButton>(R.id.btnStopAlarm)

        // NANTI LOGIKA FIREBASE AKAN DI SINI:
        // Jika ada alarm masuk, kita ubah teks, warna, mulai animasi, dan munculkan tombol Matikan Alarm.
        // Untuk sekarang, karena sedang tidak ada panggilan, tampilannya meredup ("Menunggu").
    }
}