# PingMe - Aplikasi Alarm dan Jurnal Pasangan

PingMe adalah aplikasi Android berbasis Kotlin yang dirancang khusus untuk menjaga koneksi interaktif antar pasangan. Aplikasi ini menggabungkan fitur jurnal bersama secara waktu nyata (real-time) dengan sistem alarm prioritas tinggi yang mampu menembus mode siaga (lockscreen) pada perangkat Android. 

Proyek ini dibangun menggunakan arsitektur Android modern, memanfaatkan Firebase Realtime Database untuk sinkronisasi data, dan mengimplementasikan layanan latar belakang (Foreground Service) tingkat lanjut untuk memastikan keandalan pengiriman pesan.

## Daftar Isi
1. [Fitur Utama](#fitur-utama)
2. [Teknologi yang Digunakan](#teknologi-yang-digunakan)
3. [Prasyarat Sistem](#prasyarat-sistem)
4. [Panduan Instalasi dan Konfigurasi](#panduan-instalasi-dan-konfigurasi)
5. [Konfigurasi Perizinan Perangkat (Penting)](#konfigurasi-perizinan-perangkat-penting)
6. [Struktur Proyek](#struktur-proyek)
7. [Lisensi](#lisensi)

---

## Fitur Utama

### 1. Sistem Pasangan Berbasis QR Code
*   **Autentikasi Aman:** Pendaftaran dan proses masuk menggunakan Firebase Authentication.
*   **Pemindaian QR (Scanner):** Pengguna dapat terhubung satu sama lain secara instan dengan memindai kode QR unik yang dihasilkan oleh akun pasangan mereka.

### 2. Dasbor Jurnal Bersama (Home)
Seluruh data pada dasbor disinkronisasi secara langsung (real-time) antara kedua perangkat:
*   **Penghitung Hari:** Menampilkan jumlah hari sejak kedua akun terhubung.
*   **Kutipan Bersama (Shared Quote):** Teks kutipan yang dapat diedit oleh kedua belah pihak.
*   **Bucket List:** Daftar impian atau target bersama yang dilengkapi dengan kotak centang (checkbox) interaktif dan bilah kemajuan (progress bar).
*   **Garis Waktu Momen (Moments Timeline):** Jurnal digital tempat pengguna dapat mengunggah foto (dikonversi dan dikompresi menjadi format Base64), lengkap dengan lokasi, tanggal, dan deskripsi. Foto dapat diedit atau dihapus.
*   **Pelacak Ulang Tahun:** Pengaturan dan pengingat tanggal lahir masing-masing pengguna.

### 3. Sistem Alarm Pendobrak Layar (Ping)
Ini adalah fitur inti aplikasi yang difokuskan pada keandalan tingkat sistem:
*   **Kirim Sinyal:** Mengirimkan alarm atau pesan layar penuh ke perangkat pasangan.
*   **Mode Senyap & Mode Suara:** Pengguna dapat memilih untuk mengirim pesan senyap (hanya visual dan getaran) atau pesan bersuara (menggunakan audio khusus atau bawaan).
*   **Bypass Lockscreen:** Memanfaatkan `FullScreenIntent` dan izin `SYSTEM_ALERT_WINDOW` agar layar panggilan otomatis muncul meskipun perangkat pasangan dalam keadaan terkunci atau layar mati.
*   **Volume Maksimal Otomatis:** Memaksa volume perangkat penerima ke tingkat maksimal menggunakan saluran `STREAM_ALARM` agar panggilan tidak terlewatkan.

### 4. Pusat Kontrol Izin Terintegrasi (Profile)
Aplikasi memiliki modul profil yang memuat panel pengaturan perizinan terpusat untuk memudahkan pengguna mengatur hak akses sistem yang kompleks, meliputi:
*   Kamera dan Mikrofon (Untuk memindai QR dan merekam audio).
*   Penyimpanan (Untuk mengunggah foto momen).
*   Notifikasi Sistem.
*   Tampil di Atas Aplikasi Lain (Overlay Permission).
*   Pengecualian Optimasi Baterai (Mencegah aplikasi dimatikan oleh sistem).

---

## Teknologi yang Digunakan

*   **Bahasa Pemrograman:** Kotlin
*   **Lingkungan Pengembangan:** Android Studio
*   **Antarmuka Pengguna (UI):** XML (Material Design Components, ConstraintLayout)
*   **Backend & Basis Data:** Firebase Authentication, Firebase Realtime Database
*   **Pemrosesan Media:** Base64 Encoding/Decoding (untuk kompresi dan transfer gambar serta audio tanpa penyimpanan eksternal/Cloud Storage).
*   **Layanan Sistem Android:** 
    *   Foreground Services
    *   AlarmManager (Protokol pemulihan otomatis saat aplikasi ditutup paksa)
    *   AudioManager & VibratorManager
    *   NotificationCompat

---

## Prasyarat Sistem

*   Android Studio versi terbaru (Iguana/Jellyfish atau lebih baru disarankan).
*   Perangkat fisik atau emulator dengan sistem operasi minimum Android 8.0 (API Level 26). Direkomendasikan Android 10 ke atas.
*   Koneksi internet aktif.
*   Akun Google Firebase.

---

## Panduan Instalasi dan Konfigurasi

### Tahap 1: Kloning Repositori
Jalankan perintah berikut pada terminal Anda:
```bash
git clone [https://github.com/username-anda/PingMe.git](https://github.com/username-anda/PingMe.git)
Buka proyek tersebut menggunakan Android Studio.

Tahap 2: Konfigurasi Firebase
Aplikasi ini sangat bergantung pada Firebase. Anda harus membuat proyek Firebase sendiri untuk menjalankannya.

Buka Firebase Console.

Buat proyek baru dengan nama "PingMe".

Tambahkan aplikasi Android ke proyek tersebut (pastikan Package Name sesuai dengan yang ada di AndroidManifest.xml Anda, misalnya com.example.pingme).

Unduh file google-services.json dan letakkan di dalam direktori app/ pada proyek Android Studio Anda.

Aktifkan Authentication pada Firebase Console (pilih metode masuk Email/Password).

Aktifkan Realtime Database dan atur region penyimpanan.

Tahap 3: Aturan Realtime Database (Rules)
Salin dan tempel aturan berikut ke dalam tab Rules di Firebase Realtime Database Anda agar aplikasi dapat menulis dan membaca data:

JSON
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
Catatan: Aturan di atas disederhanakan untuk tahap pengembangan. Untuk produksi rilis, pastikan Anda membatasi aturan baca/tulis hanya pada UID milik pengguna dan pasangannya masing-masing.

Tahap 4: Build dan Run
Lakukan Sync Project with Gradle Files, kemudian klik tombol Run untuk memasang aplikasi ke perangkat Anda.

Konfigurasi Perizinan Perangkat (Penting)
Dikarenakan sistem keamanan Android terbaru (Android 12+) sangat membatasi aktivitas latar belakang, pengguna diwajibkan untuk memberikan izin khusus melalui menu "Pusat Kontrol Izin" di halaman Profil aplikasi.

Bagi pengguna antarmuka pabrikan Tiongkok (seperti MIUI/HyperOS pada Xiaomi, ColorOS pada Oppo, FuntouchOS pada Vivo), diperlukan konfigurasi manual tambahan di luar aplikasi:

Mulai Otomatis (Auto-Start): Harus diaktifkan melalui Pengaturan Aplikasi perangkat agar layanan pemeriksa alarm dapat berjalan saat ponsel dihidupkan ulang.

Tidak Ada Pembatasan Baterai (No Restrictions): Penghemat baterai bawaan perangkat harus dinonaktifkan khusus untuk aplikasi PingMe agar koneksi real-time ke Firebase tidak diputus oleh sistem.

Aplikasi telah mengimplementasikan mekanisme Restart Intent (memanfaatkan AlarmManager pada onDestroy dan onTaskRemoved di dalam PingService) untuk berusaha menghidupkan kembali layanan ketika pengguna menghapus aplikasi dari Recent Apps.

Struktur Proyek
Berikut adalah gambaran singkat mengenai komponen utama di dalam kode sumber:

MainActivity.kt: Memeriksa sesi masuk pengguna. Mengarahkan pengguna ke halaman Login jika belum masuk, atau ke Pemindai/Dasbor jika sudah masuk.

ScannerActivity.kt: Menangani logika pemindaian QR Code dan penyatuan UID antara pengguna dan pasangannya ke dalam Firebase.

PingService.kt: Layanan latar belakang (Foreground Service) yang berjalan 24/7. Bertugas mendengarkan perubahan data incomingCall di Firebase dan menembakkan FullScreenIntent.

CallActivity.kt: Layar antarmuka panggilan yang muncul saat alarm diterima. Menangani logika penyalaan layar, pemutaran audio, getaran, dan pengabaian mode senyap sistem.

HomeFragment.kt: Logika untuk dasbor utama (Jurnal, Penghitung Hari, Bucket List). Termasuk penanganan siklus hidup ValueEventListener untuk mencegah kerusakan sistem (Memory Leak).

PingFragment.kt: Antarmuka untuk membuat, menyimpan, menghapus, dan mengirimkan paket data alarm (judul, status senyap, audio Base64) ke basis data.

ProfileFragment.kt: Menangani info akun pengguna, pemutusan hubungan akun pasangan, sistem keluar akun (logout), dan memuat kotak dialog Pusat Kontrol Izin.

Lisensi
Proyek ini dikembangkan sebagai bagian dari portofolio dan sarana penerapan ilmu pada bidang Rekayasa Perangkat Lunak. Anda bebas untuk menggunakan, memodifikasi, dan mendistribusikan kode ini untuk tujuan edukasi maupun non-komersial.
