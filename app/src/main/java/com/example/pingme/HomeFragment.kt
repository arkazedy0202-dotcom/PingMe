package com.example.pingme

import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class HomeFragment : Fragment() {

    private var _view: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isFlyingHeartsActive = true
    private lateinit var bgAnimationLayer: ConstraintLayout

    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference
    private var myUid: String? = null
    private var partnerUid: String? = null

    // =========================================================
    // SENJATA ANTI-GHOST LISTENER (Menyimpan status Firebase)
    // =========================================================
    private var homeListener: ValueEventListener? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) uploadFotoHeroKeFirebase(uri)
    }

    private val pickMomentImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) tampilkanDialogDetailMomen(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _view = inflater.inflate(R.layout.fragment_home, container, false)
        return _view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bgAnimationLayer = view.findViewById(R.id.bgAnimationLayer)

        val urlDb = "https://pingme-1adb4-default-rtdb.asia-southeast1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(urlDb).reference
        myUid = auth.currentUser?.uid
        if (myUid == null) return

        inisialisasiUIAsli(view)
        inisialisasiFirebaseDanLogika(view)
    }

    private fun inisialisasiFirebaseDanLogika(view: View) {
        val tvDaysConnected = view.findViewById<TextView>(R.id.tvDaysConnected)
        val tvMyBirthday = view.findViewById<TextView>(R.id.tvMyBirthday)
        val tvPartnerBirthday = view.findViewById<TextView>(R.id.tvPartnerBirthday)
        val cardBirthday = view.findViewById<View>(R.id.cardBirthday)

        val cardQuote = view.findViewById<View>(R.id.cardQuote)
        val tvQuote = view.findViewById<TextView>(R.id.tvQuote)

        val containerBucketList = view.findViewById<LinearLayout>(R.id.containerBucketList)
        val tvBucketCount = view.findViewById<TextView>(R.id.tvBucketCount)
        val pbBucketList = view.findViewById<ProgressBar>(R.id.pbBucketList)
        val btnNewWish = view.findViewById<View>(R.id.btnNewWish)

        val tvUploadHint = view.findViewById<TextView>(R.id.tvUploadHint)
        val ivCouplePhoto = view.findViewById<ImageView>(R.id.ivCouplePhoto)
        val btnNewMoment = view.findViewById<View>(R.id.btnNewMoment)
        val containerMoments = view.findViewById<LinearLayout>(R.id.containerMoments)

        tvUploadHint.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnNewMoment.setOnClickListener { pickMomentImageLauncher.launch("image/*") }

        cardQuote.setOnClickListener {
            val inputKetikan = EditText(requireContext())
            val quoteSaatIni = tvQuote.text.toString().replace("\"", "")
            inputKetikan.setText(quoteSaatIni)
            inputKetikan.setPadding(50, 50, 50, 50)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ubah Quote 💖")
                .setView(inputKetikan)
                .setPositiveButton("Simpan") { _, _ ->
                    val teksBaru = "\"${inputKetikan.text.toString()}\""
                    val updates = hashMapOf<String, Any>("connections/$myUid/quote" to teksBaru)
                    if (partnerUid != null) updates["connections/$partnerUid/quote"] = teksBaru
                    database.updateChildren(updates)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // =========================================================
        // MEMBUAT LISTENER YANG BISA DIKONTROL (DI-STOP/DI-START)
        // =========================================================
        homeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // PENGAMAN 1: Pastikan Fragment masih hidup sebelum memproses data
                if (!isAdded || context == null || _view == null) return

                val ctx = context ?: return

                // PENGAMAN 2: Gunakan view.post agar UI digambar di waktu yang aman
                _view?.post {
                    try {
                        partnerUid = snapshot.child("connectedTo").value as? String

                        // A. Teks Quote
                        val quoteData = snapshot.child("quote").value as? String ?: "\"Notifikasi\nfavoritku\nadalah namamu.\""
                        tvQuote.text = quoteData

                        // B. Foto Raksasa
                        val base64Foto = snapshot.child("couplePhoto").value as? String
                        if (base64Foto != null) {
                            try {
                                val byteFoto = Base64.decode(base64Foto, Base64.DEFAULT)
                                ivCouplePhoto.setImageBitmap(BitmapFactory.decodeByteArray(byteFoto, 0, byteFoto.size))
                            } catch (e: Exception) {}
                        }

                        // C. Hari Jadian
                        var timestampJadian = snapshot.child("connectedDate").value as? Long
                        if (timestampJadian == null && partnerUid != null) {
                            timestampJadian = System.currentTimeMillis()
                            database.child("connections").child(myUid!!).child("connectedDate").setValue(timestampJadian)
                        }
                        if (timestampJadian != null) {
                            val selisih = System.currentTimeMillis() - timestampJadian
                            jalankanAnimasiHari(tvDaysConnected, TimeUnit.MILLISECONDS.toDays(selisih).toInt())
                        }

                        // D. Ulang Tahun
                        tvMyBirthday.text = snapshot.child("myBirthday").value as? String ?: "Atur >>"
                        tvPartnerBirthday.text = snapshot.child("partnerBirthday").value as? String ?: "Menunggu"

                        // E. Bucket List
                        containerBucketList.removeAllViews()
                        var totalItem = 0
                        var itemSelesai = 0
                        for (item in snapshot.child("bucketList").children) {
                            totalItem++
                            val isDone = item.child("isDone").value as? Boolean ?: false
                            if (isDone) itemSelesai++
                            containerBucketList.addView(buatTampilanBucketList(ctx, item.child("id").value.toString(), item.child("title").value.toString(), isDone))
                        }
                        tvBucketCount.text = "$itemSelesai/$totalItem"
                        pbBucketList.max = if (totalItem == 0) 1 else totalItem
                        pbBucketList.progress = itemSelesai

                        // F. Daftar Momen
                        containerMoments.removeAllViews()
                        for (item in snapshot.child("moments").children) {
                            val id = item.child("id").value.toString()
                            val tempat = item.child("tempat").value.toString()
                            val tanggal = item.child("tanggal").value.toString()
                            val caption = item.child("caption").value.toString()
                            val photoData = item.child("photo").value.toString()
                            containerMoments.addView(buatTampilanMomen(ctx, id, tempat, tanggal, caption, photoData))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        // Mulai mendengarkan Firebase
        database.child("connections").child(myUid!!).addValueEventListener(homeListener!!)

        cardBirthday.setOnClickListener {
            val kalender = Calendar.getInstance()
            DatePickerDialog(requireContext(), R.style.Theme_PingMe, { _, y, m, d ->
                kalender.set(y, m, d)
                val tgl = SimpleDateFormat("dd MMM", Locale("id", "ID")).format(kalender.time)
                val updates = hashMapOf<String, Any>("connections/$myUid/myBirthday" to tgl)
                if (partnerUid != null) updates["connections/$partnerUid/partnerBirthday"] = tgl
                database.updateChildren(updates)
            }, kalender.get(Calendar.YEAR), kalender.get(Calendar.MONTH), kalender.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnNewWish.setOnClickListener {
            val inputKetikan = EditText(requireContext())
            inputKetikan.hint = "Cth: Nonton Konser Berdua"
            inputKetikan.setPadding(50, 50, 50, 50)
            MaterialAlertDialogBuilder(requireContext()).setTitle("Tambah Bucket List ✨").setView(inputKetikan).setPositiveButton("Tambah") { _, _ ->
                val teks = inputKetikan.text.toString()
                if (teks.isNotEmpty()) {
                    val idBaru = database.push().key ?: return@setPositiveButton
                    val dataWish = mapOf("id" to idBaru, "title" to teks, "isDone" to false)
                    val updates = hashMapOf<String, Any>("connections/$myUid/bucketList/$idBaru" to dataWish)
                    if (partnerUid != null) updates["connections/$partnerUid/bucketList/$idBaru"] = dataWish
                    database.updateChildren(updates)
                }
            }.setNegativeButton("Batal", null).show()
        }
    }

    private fun tampilkanDialogDetailMomen(uri: Uri) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 20)

        val etTempat = EditText(requireContext()).apply { hint = "Judul Tempat" }
        val etTanggal = EditText(requireContext()).apply {
            hint = "Tanggal"
            setText(SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Calendar.getInstance().time))
        }
        val etCaption = EditText(requireContext()).apply { hint = "Caption Momen" }

        layout.addView(etTempat)
        layout.addView(etTanggal)
        layout.addView(etCaption)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ceritakan Momen Ini ✨")
            .setView(layout)
            .setPositiveButton("Simpan Momen") { _, _ ->
                uploadMomentKeFirebase(uri, etTempat.text.toString(), etTanggal.text.toString(), etCaption.text.toString())
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun uploadMomentKeFirebase(uri: Uri, tempat: String, tanggal: String, caption: String) {
        try {
            val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(uri))
            val rasio = 500.0f / Math.max(bitmap.width, bitmap.height)
            val fotoKecil = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.width * rasio), Math.round(bitmap.height * rasio), true)
            val baos = ByteArrayOutputStream()
            fotoKecil.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            val base64Foto = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            val idBaru = database.push().key ?: return
            val dataMoment = mapOf("id" to idBaru, "tempat" to tempat, "tanggal" to tanggal, "caption" to caption, "photo" to base64Foto)

            val updates = hashMapOf<String, Any>("connections/$myUid/moments/$idBaru" to dataMoment)
            if (partnerUid != null) updates["connections/$partnerUid/moments/$idBaru"] = dataMoment
            database.updateChildren(updates)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal memproses gambar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buatTampilanMomen(ctx: Context, id: String, tempat: String, tanggal: String, caption: String, photoBase64: String): View {
        val card = MaterialCardView(ctx)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 40)
        card.layoutParams = params
        card.radius = 32f
        card.cardElevation = 4f
        card.setCardBackgroundColor(Color.parseColor("#FFF0F5"))
        card.strokeWidth = 0

        val layout = LinearLayout(ctx)
        layout.orientation = LinearLayout.VERTICAL

        val iv = ImageView(ctx)
        iv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        try {
            val byteFoto = Base64.decode(photoBase64, Base64.DEFAULT)
            iv.setImageBitmap(BitmapFactory.decodeByteArray(byteFoto, 0, byteFoto.size))
        } catch(e:Exception){}

        val textLayout = LinearLayout(ctx)
        textLayout.orientation = LinearLayout.VERTICAL
        textLayout.setPadding(40, 30, 40, 40)

        val tvTempat = TextView(ctx).apply {
            text = tempat
            textSize = 18f
            setTextColor(Color.parseColor("#5C454A"))
            setTypeface(null, Typeface.BOLD)
        }
        val tvTanggal = TextView(ctx).apply {
            text = tanggal
            textSize = 12f
            setTextColor(Color.parseColor("#FF8FA3"))
            setPadding(0, 0, 0, 16)
        }
        val tvCaption = TextView(ctx).apply {
            text = "\"$caption\""
            textSize = 14f
            setTextColor(Color.parseColor("#9E898E"))
            setTypeface(null, Typeface.ITALIC)
        }

        textLayout.addView(tvTempat)
        textLayout.addView(tvTanggal)
        textLayout.addView(tvCaption)
        layout.addView(iv)
        layout.addView(textLayout)
        card.addView(layout)

        card.setOnLongClickListener {
            val opsi = arrayOf("✏️ Edit Teks Momen", "🗑️ Hapus Momen")
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Pilihan Momen")
                .setItems(opsi) { _, which ->
                    if (which == 0) {
                        editTeksMomen(ctx, id, tempat, tanggal, caption)
                    } else {
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle("Hapus Momen?")
                            .setMessage("Yakin ingin menghapus?")
                            .setPositiveButton("Hapus") { _, _ ->
                                val updates = hashMapOf<String, Any?>("connections/$myUid/moments/$id" to null)
                                if (partnerUid != null) updates["connections/$partnerUid/moments/$id"] = null
                                database.updateChildren(updates)
                            }.setNegativeButton("Batal", null).show()
                    }
                }.show()
            true
        }

        return card
    }

    private fun editTeksMomen(ctx: Context, id: String, oldTempat: String, oldTanggal: String, oldCaption: String) {
        val layout = LinearLayout(ctx)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 20)

        val etTempat = EditText(ctx).apply { setText(oldTempat) }
        val etTanggal = EditText(ctx).apply { setText(oldTanggal) }
        val etCaption = EditText(ctx).apply { setText(oldCaption) }

        layout.addView(etTempat)
        layout.addView(etTanggal)
        layout.addView(etCaption)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Edit Momen 📝")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val updates = hashMapOf<String, Any>(
                    "connections/$myUid/moments/$id/tempat" to etTempat.text.toString(),
                    "connections/$myUid/moments/$id/tanggal" to etTanggal.text.toString(),
                    "connections/$myUid/moments/$id/caption" to etCaption.text.toString()
                )
                if (partnerUid != null) {
                    updates["connections/$partnerUid/moments/$id/tempat"] = etTempat.text.toString()
                    updates["connections/$partnerUid/moments/$id/tanggal"] = etTanggal.text.toString()
                    updates["connections/$partnerUid/moments/$id/caption"] = etCaption.text.toString()
                }
                database.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(ctx, "Momen diperbarui!", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Batal", null).show()
    }

    private fun uploadFotoHeroKeFirebase(uri: Uri) {
        try {
            val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(uri))
            val rasio = 600.0f / Math.max(bitmap.width, bitmap.height)
            val fotoKecil = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.width * rasio), Math.round(bitmap.height * rasio), true)
            val baos = ByteArrayOutputStream()
            fotoKecil.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            val base64Foto = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            val updates = hashMapOf<String, Any>("connections/$myUid/couplePhoto" to base64Foto)
            if (partnerUid != null) updates["connections/$partnerUid/couplePhoto"] = base64Foto
            database.updateChildren(updates)
        } catch (e: Exception) {}
    }

    private fun buatTampilanBucketList(ctx: Context, id: String, title: String, isDone: Boolean): View {
        val layout = LinearLayout(ctx)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.gravity = android.view.Gravity.CENTER_VERTICAL
        layout.setPadding(0, 0, 0, 40)

        val icon = ImageView(ctx)
        icon.layoutParams = LinearLayout.LayoutParams(70, 70)
        icon.setImageResource(if (isDone) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background)
        icon.setColorFilter(if (isDone) Color.parseColor("#FF8FA3") else Color.parseColor("#D9C8CB"))

        val teks = TextView(ctx)
        val paramTeks = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        paramTeks.marginStart = 30
        teks.layoutParams = paramTeks
        teks.text = title
        teks.textSize = 16f
        teks.setTextColor(if (isDone) Color.parseColor("#5C454A") else Color.parseColor("#9E898E"))

        layout.addView(icon)
        layout.addView(teks)
        layout.setOnClickListener {
            val statusBaru = !isDone
            val updates = hashMapOf<String, Any>("connections/$myUid/bucketList/$id/isDone" to statusBaru)
            if (partnerUid != null) updates["connections/$partnerUid/bucketList/$id/isDone"] = statusBaru
            database.updateChildren(updates)
        }
        return layout
    }

    private fun inisialisasiUIAsli(view: View) {
        val cardHero = view.findViewById<View>(R.id.cardHero)
        val rowWidgets = view.findViewById<View>(R.id.rowWidgets)
        val cardBucketList = view.findViewById<View>(R.id.cardBucketList)

        val viewsToAnimate = listOf(cardHero, rowWidgets, cardBucketList)
        viewsToAnimate.forEach { v -> v.alpha = 0f; v.translationY = 150f }

        var delay = 100L
        val interpolator = DecelerateInterpolator(2f)
        viewsToAnimate.forEach { v ->
            v.animate().alpha(1f).translationY(0f).setDuration(1000).setStartDelay(delay).setInterpolator(interpolator).start()
            delay += 150L
        }

        // PremiumOrbView remains the only continuous decorative motion on this screen.
        isFlyingHeartsActive = false
    }

    private fun jalankanAnimasiHari(textView: TextView, targetHari: Int) {
        val target = if (targetHari <= 0) 1 else targetHari
        val animator = ValueAnimator.ofInt(0, target)
        animator.duration = 2000
        animator.addUpdateListener { animation -> textView.text = animation.animatedValue.toString() }
        animator.start()
    }

    private fun startFlyingHearts() {
        handler.post(object : Runnable {
            override fun run() {
                if (isFlyingHeartsActive && context != null && _view != null) {
                    spawnHeart()
                    handler.postDelayed(this, Random.nextLong(400, 900))
                }
            }
        })
    }

    private fun spawnHeart() {
        if (context == null) return
        val heart = ImageView(requireContext())
        heart.setImageResource(R.drawable.ic_love)
        val sizePx = (Random.nextInt(25, 60) * resources.displayMetrics.density).toInt()
        heart.layoutParams = ConstraintLayout.LayoutParams(sizePx, sizePx)
        bgAnimationLayer.addView(heart)

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val startX = Random.nextFloat() * screenWidth
        heart.x = startX
        heart.y = screenHeight
        heart.alpha = Random.nextFloat() * 0.3f + 0.1f

        heart.animate().x(startX + Random.nextInt(-200, 200)).y(-sizePx.toFloat()).alpha(0f)
            .rotation(Random.nextInt(-45, 45).toFloat()).setDuration(Random.nextLong(4000, 8000))
            .setInterpolator(LinearInterpolator()).withEndAction { if (_view != null) bgAnimationLayer.removeView(heart) }.start()
    }

    // =========================================================
    // REM TANGAN: MATIKAN FIREBASE SAAT LAYAR DITUTUP
    // Mencegah Force Close Mutlak!
    // =========================================================
    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (myUid != null && homeListener != null) {
                database.child("connections").child(myUid!!).removeEventListener(homeListener!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isFlyingHeartsActive = false
        handler.removeCallbacksAndMessages(null)
        _view = null
    }
}
