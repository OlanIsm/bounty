# Bounty

Bounty adalah aplikasi Android untuk membuat dan menyelesaikan challenge berhadiah. Creator membuat challenge dengan reward demo; hunter menerima challenge, mengirim bukti foto, lalu creator menyetujui atau menolak bukti. Reward dan saldo hanya simulasi, bukan uang sungguhan. Integrasi M14 masih berjalan: autentikasi dan profil sudah memakai Firebase, sedangkan challenge, proof, dan reward masih lokal.

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
- Firebase Authentication dan Cloud Firestore untuk akun/profil
- Penyimpanan challenge dan proof: Room/SQLite
- Cache sesi, saldo demo, dan catatan reward lokal: SharedPreferences
- HTTP/API: Retrofit dan Gson
- Foto avatar: Glide
- Minimum Android: API 29 (Android 10); compile SDK: API 37

## Arsitektur

```text
Login/Register Kotlin + XML
        +-- FirebaseAccountRepository
              +-- Firebase Auth
              +-- Firestore: users/{uid}

Kotlin Activities + XML (fitur lain masih lokal)
        +-- Room: Challenge, Proof
        +-- SharedPreferences: cache sesi dan reward demo
        +-- Retrofit + Gson: Random User API
        +-- Glide: avatar
```

Kode aplikasi berada di `app/src/main/java/id/ac/binus/bounty/` dan dibagi ke package `activities`, `adapters`, `models`, `database`, `data`, `network`, serta `utils`. Seluruh kode aplikasi utama (`app/src/main`) menggunakan Kotlin. `FirebaseAccountRepository` memisahkan operasi Auth/profil dari layar login dan registrasi. Fitur challenge saat ini masih memakai DAO Room; integrasi Firestore untuk challenge/proof/reward belum selesai. Satu challenge hanya memiliki satu hunter aktif.

Status challenge: `OPEN` → `ACCEPTED` → `SUBMITTED` → `COMPLETED`. Jika proof ditolak, status kembali ke `ACCEPTED`. Persetujuan proof menyelesaikan challenge dan menambahkan reward mock ke saldo hunter satu kali.

Challenge, proof, foto, dan saldo/reward demo tetap lokal. Random User API (`https://randomuser.me/api/?results=10`) hanya memasok profil demo; data tersebut tidak membuat akun aplikasi. Foto proof dipilih dari perangkat dan disimpan sebagai URI lokal, belum diunggah ke Firebase Storage.

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

Buat akun melalui layar Register menggunakan email dan password. Firebase Auth mengelola kredensial; profil dibuat di Firestore. Challenge demo awal masih berasal dari Room lokal. Alur lintas perangkat belum tersedia sampai migrasi challenge/proof/reward ke Firestore selesai.

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
