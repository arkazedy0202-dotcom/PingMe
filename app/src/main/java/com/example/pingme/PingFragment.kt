package com.example.pingme

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PingFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference
    private var partnerUid: String? = null

    // =========================================================
    // SENJATA ANTI-GHOST LISTENER (Menyimpan status Firebase)
    // =========================================================
    private var pingListener: ValueEventListener? = null
    private var myUid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ping, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAddPing = view.findViewById<MaterialButton>(R.id.btnAddPing)
        val containerPings = view.findViewById<LinearLayout>(R.id.containerPings)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)

        val urlDb = "https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(urlDb).reference

        myUid = auth.currentUser?.uid
        if (myUid == null) return

        btnAddPing.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, PingEditorActivity::class.java))
        }

        // =========================================================
        // MEMBUAT LISTENER YANG BISA DIKONTROL (DI-STOP/DI-START)
        // =========================================================
        pingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Pastikan layar benar-benar sedang aktif di mata user
                if (!isAdded || context == null || activity == null || view == null) return

                val ctx = requireContext()
                partnerUid = snapshot.child("connectedTo").value as? String

                // Gunakan runOnUiThread / view.post agar gambar UI tidak bentrok
                view.post {
                    try {
                        containerPings.removeAllViews()
                        val pingsSnapshot = snapshot.child("pings")

                        if (pingsSnapshot.exists() && pingsSnapshot.childrenCount > 0) {
                            tvEmptyState.visibility = View.GONE
                            for (item in pingsSnapshot.children) {
                                val id = item.key ?: continue
                                val title = item.child("title").value as? String ?: "Alarm"
                                val isSilent = item.child("isSilent").value as? Boolean ?: false
                                val audioName = item.child("audioName").value as? String ?: "Suara Bawaan"
                                val audioBase64 = item.child("audioData").value as? String ?: ""

                                val kartuPing = buatKartuPing(ctx, id, title, isSilent, audioName, audioBase64)
                                containerPings.addView(kartuPing)
                            }
                        } else {
                            tvEmptyState.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        // Mulai mendengarkan Firebase
        database.child("connections").child(myUid!!).addValueEventListener(pingListener!!)
    }

    // =========================================================
    // REM TANGAN: MATIKAN FIREBASE SAAT LAYAR DITUTUP/DIGESER
    // Mencegah Force Close Mutlak!
    // =========================================================
    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (myUid != null && pingListener != null) {
                database.child("connections").child(myUid!!).removeEventListener(pingListener!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buatKartuPing(ctx: Context, id: String, title: String, isSilent: Boolean, audioName: String, audioBase64: String): View {
        val card = MaterialCardView(ctx)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 40)
        card.layoutParams = layoutParams
        card.radius = 48f
        card.cardElevation = 6f
        card.setCardBackgroundColor(Color.WHITE)
        card.strokeWidth = 0

        val mainLayout = LinearLayout(ctx)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(50, 50, 50, 50)

        val rowAtas = LinearLayout(ctx)
        rowAtas.orientation = LinearLayout.HORIZONTAL
        rowAtas.gravity = android.view.Gravity.CENTER_VERTICAL

        val iconContainer = MaterialCardView(ctx)
        iconContainer.layoutParams = LinearLayout.LayoutParams(120, 120)
        iconContainer.radius = 60f
        iconContainer.cardElevation = 0f
        iconContainer.strokeWidth = 2
        iconContainer.strokeColor = Color.parseColor(if (isSilent) "#E0E0E0" else "#FFDEE9")
        iconContainer.setCardBackgroundColor(Color.WHITE)

        val icon = ImageView(ctx)
        icon.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        icon.setPadding(30, 30, 30, 30)
        icon.setImageResource(if (isSilent) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_btn_speak_now)
        icon.setColorFilter(Color.parseColor(if (isSilent) "#9E898E" else "#FF8FA3"))
        iconContainer.addView(icon)

        val textLayout = LinearLayout(ctx)
        textLayout.orientation = LinearLayout.VERTICAL
        val textParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        textParams.marginStart = 30
        textLayout.layoutParams = textParams

        val tvTitle = TextView(ctx)
        tvTitle.text = title
        tvTitle.textSize = 18f
        tvTitle.setTextColor(Color.parseColor("#5C454A"))
        tvTitle.setTypeface(null, Typeface.BOLD)

        val tvSub = TextView(ctx)
        tvSub.text = if (isSilent) "Mode Senyap" else "Suara: $audioName"
        tvSub.textSize = 12f
        tvSub.setTextColor(Color.parseColor("#9E898E"))

        textLayout.addView(tvTitle)
        textLayout.addView(tvSub)

        val btnDelete = ImageView(ctx)
        btnDelete.layoutParams = LinearLayout.LayoutParams(60, 60)
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete)
        btnDelete.setColorFilter(Color.parseColor("#FFCDD2"))

        btnDelete.setOnClickListener { _ ->
            try {
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("Hapus Alarm?")
                    .setMessage("Yakin ingin menghapus alarm '$title'?")
                    .setPositiveButton("Hapus") { dialog, _ ->
                        try {
                            val safeUid = FirebaseAuth.getInstance().currentUser?.uid
                            if (safeUid != null) {
                                val urlDb = "https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app"
                                FirebaseDatabase.getInstance(urlDb).reference
                                    .child("connections").child(safeUid).child("pings").child(id).removeValue()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Gagal menghapus di server", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Batal") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } catch (e: Exception) {}
        }

        rowAtas.addView(iconContainer)
        rowAtas.addView(textLayout)
        rowAtas.addView(btnDelete)

        val btnKirim = MaterialButton(ctx)
        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
        btnParams.topMargin = 40
        btnKirim.layoutParams = btnParams
        btnKirim.text = if (isSilent) "Kirim Pesan Layar" else "Kirim Alarm"
        btnKirim.isAllCaps = false
        btnKirim.textSize = 16f
        btnKirim.setTypeface(null, Typeface.BOLD)
        btnKirim.setTextColor(Color.parseColor(if (isSilent) "#5C454A" else "#FFFFFF"))
        btnKirim.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (isSilent) "#F0E6E8" else "#FF8FA3"))
        btnKirim.cornerRadius = 40
        btnKirim.elevation = 0f

        btnKirim.setOnClickListener { v ->
            try {
                if (partnerUid.isNullOrEmpty()) {
                    Toast.makeText(v.context, "Kamu belum terhubung dengan Si Dia!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnKirim.isEnabled = false
                btnKirim.text = "Mengirim..."

                val cleanUid = partnerUid!!.trim().replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "")

                val callData = mapOf("title" to title, "isSilent" to isSilent, "audioData" to audioBase64, "timestamp" to System.currentTimeMillis())

                val urlDb = "https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app"
                FirebaseDatabase.getInstance(urlDb).reference
                    .child("connections").child(cleanUid).child("incomingCall").setValue(callData)
                    .addOnSuccessListener {
                        Toast.makeText(v.context, "Sinyal Terkirim ke Si Dia! 🚀", Toast.LENGTH_SHORT).show()
                        btnKirim.isEnabled = true
                        btnKirim.text = if (isSilent) "Kirim Pesan Layar" else "Kirim Alarm"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(v.context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                        btnKirim.isEnabled = true
                        btnKirim.text = "Coba Lagi"
                    }
            } catch (e: Exception) {
                Toast.makeText(v.context, "CRASH DICEGAH: ${e.message}", Toast.LENGTH_LONG).show()
                btnKirim.isEnabled = true
                btnKirim.text = "Kirim Alarm"
            }
        }

        mainLayout.addView(rowAtas)
        mainLayout.addView(btnKirim)
        card.addView(mainLayout)

        return card
    }
}