# Bounty — Product Requirements Document

## 1. Product Overview

**Bounty** adalah aplikasi Android untuk membuat dan menerima challenge berhadiah secara fun.

Contoh:
- User A sedang ingin membuat challenge: **"Push-up 30 kali di taman"**
- User A menentukan reward mock: **Rp20.000**
- User B melihat challenge tersebut dan menekan **Accept Challenge**
- User B menyelesaikan challenge dan mengirim bukti berupa foto + deskripsi
- User A memeriksa bukti dan menekan **Approve**
- Challenge berubah menjadi **Completed**
- Reward ditampilkan sebagai **mock payment/reward**

Aplikasi dibuat menggunakan Android Studio dan menghasilkan file `.apk`.

---

## 2. Goal

Membuat aplikasi Android sederhana yang:
- Memiliki flow user yang jelas
- Menggunakan komponen Android native
- Memiliki CRUD sederhana untuk challenge
- Memiliki proses accept, submit proof, dan approve
- Menggunakan minimal 1 external API
- Dapat di-build menjadi APK

---

## 3. Target User

### Challenge Creator
User yang ingin membuat challenge dan menawarkan reward.

### Challenge Hunter
User yang ingin mencari, menerima, dan menyelesaikan challenge untuk mendapatkan reward.

Satu akun dapat menjadi creator maupun hunter.

---

## 4. MVP Scope

### Included
- Login mock
- Register mock
- Home / challenge feed
- Detail challenge
- Create challenge
- Accept challenge
- Submit proof
- Approve / reject proof
- My Challenges
- Profile
- Mock balance / mock reward
- External API untuk data user/avatar demo
- Local/mock data

### Out of Scope
- Pembayaran uang asli
- Payment gateway
- Withdraw
- Real bank account
- Chat real-time
- Push notification
- GPS verification
- Identity verification
- Admin dashboard
- Production backend
- Real authentication server

---

## 5. Main User Flow

### Creator Flow

```text
Login
  ↓
Home
  ↓
Create Challenge
  ↓
Input title, description, reward, deadline
  ↓
Publish
  ↓
Challenge muncul di feed
  ↓
Hunter accept challenge
  ↓
Hunter submit proof
  ↓
Creator review proof
  ↓
Approve
  ↓
Completed + mock reward diberikan
```

### Hunter Flow

```text
Login
  ↓
Home
  ↓
Browse Challenge
  ↓
Challenge Detail
  ↓
Accept Challenge
  ↓
Submit Proof
  ↓
Waiting for Approval
  ↓
Approved
  ↓
Mock reward masuk
```

---

## 6. Core Features

### 6.1 Mock Authentication
User dapat:
- Login
- Register
- Logout

Untuk MVP, authentication tidak perlu terhubung server.

---

### 6.2 Challenge Feed
Home menampilkan daftar challenge.

Setiap card menampilkan:
- Title
- Creator
- Reward
- Deadline
- Status
- Thumbnail/avatar

User dapat membuka detail challenge.

---

### 6.3 Create Challenge

Input:
- Challenge title
- Description
- Reward amount
- Deadline
- Optional image

Validation:
- Title wajib
- Description wajib
- Reward harus lebih dari 0
- Deadline tidak boleh kosong

---

### 6.4 Challenge Detail

Menampilkan:
- Title
- Description
- Creator
- Reward
- Deadline
- Status
- Accept button

Accept button hanya aktif jika challenge masih `OPEN`.

---

### 6.5 Accept Challenge

Saat hunter menekan Accept:

```text
OPEN → ACCEPTED
```

Data hunter disimpan sebagai participant.

Satu challenge MVP hanya memiliki satu participant aktif.

---

### 6.6 Submit Proof

Hunter yang sudah menerima challenge dapat submit:
- Photo
- Description

Setelah submit:

```text
ACCEPTED → SUBMITTED
```

---

### 6.7 Review Proof

Creator dapat melihat:
- Proof image
- Proof description

Creator dapat:

```text
Approve
Reject
```

Jika approve:

```text
SUBMITTED → COMPLETED
```

Jika reject:

```text
SUBMITTED → ACCEPTED
```

Hunter dapat submit proof kembali.

---

### 6.8 Mock Reward

Tidak ada transaksi uang asli.

Contoh:

```text
Balance sebelum:
Rp50.000

Challenge reward:
Rp20.000

Balance setelah approved:
Rp70.000
```

Data hanya tersimpan sebagai nilai lokal/mock.

UI harus memberi label seperti:

> Demo Balance

agar tidak terlihat seperti transaksi uang asli.

---

## 7. Challenge Status

```text
OPEN
ACCEPTED
SUBMITTED
COMPLETED
```

Optional:

```text
EXPIRED
```

---

## 8. External API Requirement

Bounty menggunakan **Random User API** untuk mengambil data user demo seperti:
- Name
- Profile photo
- Email

Endpoint:

```http
GET https://randomuser.me/api/?results=10
```

Data API dapat digunakan untuk:
- Avatar creator di feed
- Dummy/demo users
- Profile preview

Challenge data tetap dapat menggunakan local/mock data.

---

## 9. Screens

MVP memiliki:

1. Splash Screen
2. Login
3. Register
4. Home
5. Challenge Detail
6. Create Challenge
7. Submit Proof
8. Review Proof
9. My Challenges
10. Profile

---

## 10. Navigation

Bottom Navigation:

```text
Home
My Challenges
Create
Profile
```

---

## 11. Functional Requirements

### FR-01
User dapat login menggunakan mock account.

### FR-02
User dapat melihat challenge feed.

### FR-03
User dapat melihat detail challenge.

### FR-04
User dapat membuat challenge baru.

### FR-05
User dapat menerima challenge yang berstatus OPEN.

### FR-06
User dapat mengirim proof setelah menerima challenge.

### FR-07
Creator dapat approve atau reject proof.

### FR-08
Reward mock diberikan setelah proof disetujui.

### FR-09
User dapat melihat challenge miliknya.

### FR-10
Aplikasi mengambil data dari external API.

---

## 12. Non-Functional Requirements

- Android app dapat berjalan tanpa backend production
- UI responsive untuk smartphone Android
- Network request memiliki loading state
- Network error tidak menyebabkan aplikasi crash
- Form memiliki basic validation
- APK dapat di-install dan dijalankan
- Data mock/local tetap dapat digunakan ketika external API gagal

---

## 13. Acceptance Criteria

Project dianggap selesai jika:

- APK berhasil dibuat
- Login mock berjalan
- Challenge feed tampil
- User dapat create challenge
- User dapat accept challenge
- User dapat submit proof
- Creator dapat approve proof
- Reward mock berubah setelah approval
- External API berhasil dipanggil dan data ditampilkan
- Tidak ada crash pada main flow
