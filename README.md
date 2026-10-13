# Bounty

Bounty adalah aplikasi Android untuk membuat dan menyelesaikan challenge berhadiah. Creator membuat challenge dengan reward demo; hunter menerima challenge, mengirim bukti foto, lalu creator menyetujui atau menolak bukti. Reward dan saldo hanya simulasi, bukan uang sungguhan.

Project ini dibuat sebagai MVP/coursework Android. Aplikasi juga menggunakan Random User API untuk menampilkan profil demo; data API tersebut tidak digunakan sebagai akun Bounty.

## Fitur

- Registrasi dan login Email/Password melalui Firebase Authentication
- Profil akun disimpan di Cloud Firestore pada `users/{uid}`
- Feed, detail, pembuatan, dan penerimaan challenge
- Pengiriman bukti foto dari perangkat dan deskripsi
- Review bukti: approve atau reject; bukti yang ditolak bisa dikirim ulang
- My Challenges, profil, serta Demo Balance dan reward mock
- Profil demo dari Random User API dengan loading, retry, dan fallback lokal

## Teknologi

- Android native app: Kotlin, XML Layout, Material Components
- Firebase Authentication dan Cloud Firestore untuk akun, challenge, proof, dan reward mock
- Foto proof disimpan sebagai bytes terkompresi pada dokumen Firestore; batas aplikasi 512 KiB
- SharedPreferences hanya menyimpan cache sesi; Room tetap tersedia untuk skema lokal lama
- HTTP/API: Retrofit dan Gson
- Foto avatar: Glide
- Minimum Android: API 29 (Android 10); compile SDK: API 37

## Arsitektur

```text
Kotlin Activities + XML
        +-- FirebaseAccountRepository -> Firebase Auth + Firestore users
        +-- FirebaseBountyRepository -> Firestore challenges/proofs/receipts
        +-- Retrofit + Gson -> Random User API (profil demo)
        +-- Glide -> avatar dan foto proof
```

Kode aplikasi berada di `app/src/main/java/id/ac/binus/bounty/` dan dibagi ke package `activities`, `adapters`, `models`, `database`, `data`, `network`, serta `utils`. Seluruh kode utama menggunakan Kotlin. Repository memisahkan operasi Firebase dari UI. Satu challenge hanya memiliki satu hunter aktif.

Status challenge: `OPEN` → `ACCEPTED` → `SUBMITTED` → `COMPLETED`. Jika proof ditolak, status kembali ke `ACCEPTED`. Persetujuan proof menyelesaikan challenge dan menambahkan reward mock ke saldo hunter satu kali.

Challenge, proof, receipts, dan saldo demo disimpan/dihitung melalui Firestore. Foto proof dikompres dan disimpan sebagai bytes dalam Firestore, sehingga tidak memerlukan Firebase Storage/Blaze. Random User API (`https://randomuser.me/api/?results=10`) hanya memasok profil demo; data tersebut tidak membuat akun aplikasi.

## Ambil project

Clone repository:

```powershell
git clone https://github.com/OlanIsm/bounty.git
cd bounty
```

Atau pilih **Code → Download ZIP** di halaman GitHub, lalu ekstrak ZIP. Buka folder project di Android Studio dan tunggu Gradle Sync selesai.

### Persiapan

- Android Studio dengan JDK 21 (JBR bawaan Android Studio bisa digunakan)
- Android SDK Platform 37 dan SDK Build Tools yang diminta Gradle
- Emulator atau perangkat Android API 29 atau lebih baru untuk menjalankan aplikasi
- Firebase Email/Password Auth dan Cloud Firestore aktif untuk registrasi/login; file `app/google-services.json` mencocokkan package aplikasi

Android Studio biasanya membuat `local.properties` saat SDK dikonfigurasi. Untuk build dari terminal, gunakan PowerShell dan pastikan `JAVA_HOME` menunjuk ke JDK 21.

## Build dan jalankan

Di Android Studio, pilih konfigurasi `app` lalu tekan **Run**. Atau build APK debug:

```powershell
./gradlew.bat assembleDebug
```

APK berada di `app/build/outputs/apk/debug/app-debug.apk`. Untuk membuat deliverable bernama `Bounty.apk` sekaligus menjalankan lint:

```powershell
./scripts/Build-Apk.ps1
```

Hasilnya `build/Bounty.apk`. APK ini ditandatangani dengan debug key untuk coursework, bukan untuk distribusi produksi atau Play Store. Pasang ke perangkat dengan:

```powershell
adb install -r build/Bounty.apk
```

## Akun aplikasi

Buat akun melalui layar Register menggunakan email dan password. Firebase Auth mengelola kredensial; profil, challenge, proof, dan receipt reward tersimpan di Firestore. Pastikan Firestore Rules di `firebase/firestore.rules` terpasang. Firebase Storage tidak digunakan.

## Push perubahan ke GitHub

Setelah mengubah file, jalankan perintah berikut dari folder project. Ganti pesan commit agar menjelaskan perubahan yang dibuat:

```powershell
git status
git add README.md
git commit -m "docs: explain Bounty project and setup"
git push origin main
```

Jika perubahan mencakup file lain, tambahkan file yang memang ingin dikirim dengan `git add <nama-file>`. Jangan commit `local.properties`, file build lokal, atau konfigurasi pribadi. Untuk repository fork, pastikan `origin` mengarah ke repository GitHub milikmu sebelum push.

## Dokumentasi project

Dokumen requirement dan implementasi ada di folder [`docs/`](docs/):

- [`PRD.md`](docs/PRD.md): tujuan, cakupan MVP, dan alur fitur
- [`ARCHITECTURE.md`](docs/ARCHITECTURE.md): struktur aplikasi dan batas integrasi
- [`DATABASE.md`](docs/DATABASE.md): tabel dan model data
- [`API.md`](docs/API.md): kontrak Random User API
- [`DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md): aturan visual
- [`TASKS.md`](docs/TASKS.md): milestone dan progres pengerjaan
- [`TEST_REPORT.md`](docs/TEST_REPORT.md): hasil pengujian
