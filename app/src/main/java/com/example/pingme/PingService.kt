package com.example.pingme

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PingService : Service() {

    override fun onCreate() {
        super.onCreate()
        buatNotifikasiStandby()
        mulaiMendengarPing()
    }

    private fun buatNotifikasiStandby() {
        val channelId = "PingMeChannel"
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sinyal PingMe", NotificationManager.IMPORTANCE_HIGH)
            notifManager.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PingMe Siaga 💖")
            .setContentText("Aplikasi berjalan dan siap menerima alarm.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notif)
    }

    private fun mulaiMendengarPing() {
        val auth = FirebaseAuth.getInstance()
        val myUid = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance("https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        database.child("connections").child(myUid).child("incomingCall").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val title = snapshot.child("title").value as? String ?: "Ping Masuk!"
                        val isSilent = snapshot.child("isSilent").value as? Boolean ?: false
                        val audioData = snapshot.child("audioData").value as? String ?: ""

                        val prefs = getSharedPreferences("PingMePrefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("TEMP_AUDIO", audioData).apply()

                        bukaLayarPanggilan(title, isSilent)

                        database.child("connections").child(myUid).child("incomingCall").removeValue()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun bukaLayarPanggilan(title: String, isSilent: Boolean) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("EXTRA_PESAN", title)
            putExtra("EXTRA_SILENT", isSilent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(this, "PingMeChannel")
            .setContentTitle("Panggilan PingMe! ")
            .setContentText(title)
            .setSmallIcon(R.drawable.logo_pingme)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(System.currentTimeMillis().toInt(), notif)
    }

    // =========================================================================
    // ZOMBIE PROTOCOL: BANGKITKAN DIRI SENDIRI SAAT DI-SWIPE / DITUTUP PAKSA
    // =========================================================================
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        bangkitkanService()
    }

    override fun onDestroy() {
        super.onDestroy()
        bangkitkanService()
    }

    private fun bangkitkanService() {
        // Buat niat (intent) untuk menyalakan PingService lagi
        val restartIntent = Intent(applicationContext, PingService::class.java)
        restartIntent.setPackage(packageName)

        val restartPendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Panggil sistem Alarm bawaan Android
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Pasang bom waktu: Nyalakan ulang aplikasi ini 1 detik (1000 ms) setelah dibunuh!
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartPendingIntent
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Beritahu Android bahwa jika service ini mati, ia harus dihidupkan lagi
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}