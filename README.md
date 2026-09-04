# KeuanganKu — Aplikasi Laporan Keuangan Pribadi

Aplikasi Android (Kotlin + Jetpack Compose) untuk mencatat pengeluaran harian, pemasukan, saldo tunai & bank, analisis statistik, dan pengingat tagihan — dengan tampilan bergaya *mobile banking* (inspirasi MYBCA), mode offline, dan sinkronisasi cloud opsional via Firebase.

## ✨ Fitur

- **Dasbor**: total saldo, saldo cash vs bank, kartu kredit, dan aktivitas terakhir
- **Pencatatan transaksi**: pemasukan, pengeluaran, tarik/setor tunai, multi kategori, pencarian & filter (tipe/akun)
- **Kartu kredit**: pelacakan akumulasi belanja bulan ini + pembuatan tagihan kartu kredit otomatis untuk bulan berjalan
- **Jadwal tagihan**: pengingat jatuh tempo & terlambat lewat notifikasi (WorkManager harian), tandai lunas dengan pencatatan otomatis
- **Analisis**: pengeluaran per kategori, tren bulanan, proyeksi, rasio tabungan + saran (periode: semua / bulan ini / bulan lalu / tahun ini)
- **Mode offline penuh**: semua data tersimpan lokal di Room — tanpa akun pun aplikasi tetap berfungsi
- **Sinkronisasi cloud opsional**: login email/password (Firebase Auth) → sinkron dua arah + realtime (Firestore)
- **Ekspor CSV**: backup/portabilitas data transaksi & tagihan (RFC 4180)
- **Tema**: light/dark/system, palet brand BCA, Material 3
- **🔐 Kunci aplikasi (PIN)**: proteksi data finansial — terkunci otomatis saat app dibuka kembali/di-background, bisa dibuka dengan sidik jari/biometrik
- **🔁 Transaksi berulang**: gaji, cicilan, langganan & pengeluaran rutin dicatat otomatis (harian/mingguan/bulanan/tahunan; jadwal bulanan tetap di tanggal mulai walau bulan pendek)
- **🎯 Budget bulanan per kategori**: batas pengeluaran per kategori + progres bulan berjalan + notifikasi saat terlampaui
- **📊 Grafik tren interaktif**: pemasukan vs pengeluaran 6 bulan terakhir (tap kolom untuk detail bulan)

## 🛠️ Tech Stack

| Bagian | Teknologi |
|---|---|
| Bahasa / UI | Kotlin, Jetpack Compose (Material 3) |
| Penyimpanan lokal | Room (SQLite), offline-first |
| Backend cloud | Firebase Auth + Cloud Firestore |
| Notifikasi terjadwal | WorkManager (harian) |
| Arsitektur | MVVM sederhana (Activity → ViewModel → Repository → Room) |
| Testing | JUnit, Robolectric, Roborazzi |

## 📁 Struktur Kode

```
app/src/main/java/com/example/
├── MainActivity.kt                  # Entry point, bottom navigation, izin notifikasi
├── data/                            # Room entities/DAO/database + repository
│   ├── Transaction.kt / Bill.kt / Category.kt
│   ├── FinanceDao.kt / FinanceDatabase.kt
│   ├── FinanceRepository.kt         # + logika tagihan kartu kredit
│   ├── FirebaseSyncManager.kt       # Sinkronisasi penuh + realtime ke Firestore
│   └── BillReminderWorker.kt        # Notifikasi tagihan latar belakang
├── viewmodel/
│   └── FinanceViewModel.kt          # State UI + helper format Rupiah, CSV, parse angka
└── ui/
    ├── screens/                     # Dashboard, Transaksi, Analisis, Tagihan, Settings, Auth
    └── theme/                       # Palet & tema BCA
```

### Skema Database (Room, tabel `transactions`, `bills`, `categories`)

| Field | Keterangan |
|---|---|
| `id` | `Int` primary key. Nilai < 1.000.000 = auto-increment lama; nilai acak 1.000.000–2.000.000.000 dipakai agar unik lintas perangkat untuk sinkron cloud |
| `title`, `amount`, `note` | Data transaksi/tagihan |
| `type` | `INCOME` / `EXPENSE` / `WITHDRAWAL` / `DEPOSIT` |
| `accountType` | `CASH` / `BANK` / `CREDIT_CARD` |
| `dateMillis` / `dueDateMillis` | Waktu transaksi / jatuh tempo tagihan |
| `isPaid`, `category` | Status & kategori tagihan |

## 🚀 Menjalankan di Lokal

### Prasyarat
- Android Studio (versi terbaru) atau JDK 17+
- Firebase project (untuk fitur cloud):
  - Firebase Auth (Email/Password) dan Cloud Firestore **diaktifkan**
  - `app/google-services.json` milik project Anda (ganti yang ada, atau hapus untuk build tanpa Firebase)

### Build & test
```bash
./gradlew assembleDebug        # menghasilkan app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # menjalankan unit test (JVM/Robolectric)
./gradlew installDebug         # install ke emulator/perangkat yang terhubung
```

## 📲 Mendapatkan APK yang Bisa Diinstal (GitHub Actions)

Repositori ini sudah menyertakan workflow `.github/workflows/android-ci.yml`. Workflow tersebut otomatis menjalankan unit test, membangun APK, lalu mengunggahnya:

1. Buka tab **Actions** di GitHub → pilih workflow **"Android CI - Build, Test & Release APK"** → **Run workflow**.
2. Tunggu sampai job selesai, lalu buka run tersebut.
3. Unduh artefak **`keuanganku-apk`** → ada file `Keuanganku-vX.Y.Z-debug.apk` yang langsung bisa diinstall di HP.
4. Untuk rilis permanen: **push tag** `v1.0.0` (atau apa pun berawalan `v`) → GitHub Release dibuat otomatis dengan APK terlampir.

### Menandatangani rilis (opsional tapi disarankan)
Tambahkan secret berikut di **Settings → Secrets and variables → Actions** supaya workflow juga menghasilkan APK rilis bertanda tangan (`Keuanganku-vX.Y.Z-release.apk`):

| Secret | Isi |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | file `.keystore`/`.jks` Anda dalam base64 (`base64 -w0 release.keystore`) |
| `KEYSTORE_PASSWORD` | password keystore |
| `KEY_ALIAS` | alias kunci penandatangan |
| `KEY_PASSWORD` | password kunci |

> Tanpa secret di atas workflow tetap sukses dan tetap menghasilkan APK debug yang bisa diinstall.

### Secret opsional lain
- `GOOGLE_SERVICES_JSON` — base64 dari `app/google-services.json`, berguna bila file tersebut tidak ikut di-commit.

## 🔐 Keamanan & Privasi

- **Firestore rules**: deploy `firestore.rules` yang ada di root repositori agar setiap user hanya bisa membaca/menulis datanya sendiri:
  ```bash
  firebase deploy --only firestore:rules
  ```
  Pola akses yang dipakai aplikasi: `users/{uid}/transactions|bills|categories/...`
- **App Check (disarankan)**: aktifkan App Check di Firebase Console (Play Integrity untuk rilis / Debug provider untuk pengembangan) lalu tambahkan provider-nya di `MainActivity` agar Firestore hanya menerima request dari aplikasi asli.
- **Android backup**: database Room & preferensi **tidak** ikut Auto Backup/cloud backup (lihat `app/src/main/res/xml/backup_rules.xml` & `data_extraction_rules.xml`) — data finansial hanya "keluar" perangkat lewat sinkronisasi Firebase yang Anda pilih.
- **`google-services.json`**: berisi kunci API Android project Firebase Anda. Restrict key tersebut di Firebase Console (package + SHA-1) bila repositori bersifat publik, atau pindahkan ke secret `GOOGLE_SERVICES_JSON` dan hapus dari repo.
### Cara kerja sinkronisasi (2 HP / multi-perangkat)

- Setiap data baru/ubah langsung dikirim ke Firestore **dan** diterima real-time oleh HP lain yang sedang online (snapshot listener).
- Saat HP Anda **offline**, data tetap tersimpan lokal; begitu ada koneksi, aplikasi **otomatis mencoba sinkronisasi ulang** (10 dtk → 30 dtk → 1 mnt → 5 mnt, berhenti setelah berhasil).
- Saat aplikasi dibuka dalam kondisi sudah login, satu sinkronisasi penuh otomatis dijalankan agar data dari HP lain ikut masuk.
- Data yang dibuat saat **guest/offline sebelum login** ikut diunggah saat login pertama kali (tidak dihapus).
- Sinkronisasi penuh juga bisa dijalankan manual dari layar Settings.

**Cara tes dengan 2 HP:**
1. Install APK di HP A dan B → login dengan akun yang sama di keduanya.
2. Di HP A tambahkan transaksi → dalam beberapa detik muncul di HP B.
3. Aktifkan mode pesawat di HP B, tambahkan 2 transaksi → matikan mode pesawat → data otomatis tersinkron (retry otomatis, atau saat app dibuka ulang).
4. Tandai tagihan lunas di HP A → status berubah juga di HP B.

> **Syarat cloud berfungsi:** `google-services.json` harus milik Firebase project Anda, Auth + Firestore diaktifkan, dan `firestore.rules` di-deploy. Tanpa itu, akun bisa dibuat tetapi data tidak tersinkron (lihat bagian Keamanan & Privasi).

> **Catatan teknis (conflict resolution):** setiap baris kini membawa timestamp `updatedAt`. Sinkronisasi memakai *last-write-wins* deterministik: edit yang lebih lama tidak akan pernah menimpa edit yang lebih baru (baik saat sinkronisasi penuh maupun lewat listener real-time). Catatan: aturan transaksi berulang & budget bersifat per-perangkat (yang tersinkron ke cloud adalah transaksi hasil generasinya); timestamp memakai jam perangkat, jadi idealnya jam kedua HP akurat.

## 🧪 Testing

```bash
./gradlew testDebugUnitTest          # unit + Robolectric (host)
./gradlew connectedAndroidTest       # instrumented (butuh emulator/perangkat)
```

## 📄 Lisensi

MIT License.
