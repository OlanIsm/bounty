# Bounty

Bounty adalah aplikasi Android untuk membuat dan menyelesaikan challenge berhadiah. Creator membuat challenge dengan reward demo; hunter menerima challenge, mengirim bukti foto, lalu creator menyetujui atau menolak bukti. Reward dan saldo hanya simulasi lokal, bukan uang sungguhan.

Project ini dibuat sebagai MVP/coursework Android. Challenge utama tetap bisa digunakan tanpa backend. Data profil demo diambil dari Random User API.

## Fitur

- Login dan registrasi mock tanpa password atau server autentikasi
- Feed, detail, pembuatan, dan penerimaan challenge
- Pengiriman bukti foto dari perangkat dan deskripsi
- Review bukti: approve atau reject; bukti yang ditolak bisa dikirim ulang
- My Challenges, profil, serta Demo Balance dan reward mock
- Profil demo dari Random User API dengan loading, retry, dan fallback lokal

## Teknologi

- Android native: Java, XML Layout, Material Components
- Penyimpanan challenge dan proof: Room/SQLite
- Akun, sesi, saldo demo, dan catatan reward: SharedPreferences
- HTTP/API: Retrofit dan Gson
- Foto avatar: Glide
- Minimum Android: API 29 (Android 10); compile SDK: API 37

## Arsitektur

```text
Activities + XML UI
        |
        +-- Room: Challenge, Proof, DAO
        +-- SharedPreferences: akun mock, sesi, saldo demo
        +-- Retrofit + Gson: Random User API
                         Glide: avatar
```

Kode aplikasi berada di `app/src/main/java/id/ac/binus/bounty/` dan dibagi ke package `activities`, `adapters`, `models`, `database`, `network`, serta `utils`. UI dan alur aplikasi berada di Activities; operasi data lokal memakai DAO Room. Model utama database adalah `Challenge` dan `Proof`. Satu challenge hanya memiliki satu hunter aktif.

Status challenge: `OPEN` → `ACCEPTED` → `SUBMITTED` → `COMPLETED`. Jika proof ditolak, status kembali ke `ACCEPTED`. Persetujuan proof menyelesaikan challenge dan menambahkan reward mock ke saldo hunter satu kali.

Challenge, proof, dan saldo tetap lokal. Random User API (`https://randomuser.me/api/?results=10`) hanya memasok profil demo; data tersebut tidak membuat akun aplikasi. Foto proof dipilih dari perangkat dan disimpan sebagai URI lokal, tidak diunggah ke server.

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

## Akun demo

Pada instalasi baru, login dengan `demo@bounty.local` tanpa password. Akun mendapat Demo Balance Rp50.000 dan database menyediakan tiga challenge awal. Untuk alur creator-hunter lengkap, gunakan dua akun pada perangkat yang sama; panduan demo ada di [`docs/DEMO.md`](docs/DEMO.md).

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
