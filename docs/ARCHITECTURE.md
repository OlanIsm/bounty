# Bounty — Application Architecture

## 1. Architecture Goal

Architecture Bounty dibuat sesederhana mungkin untuk project Android coursework.

Tidak menggunakan backend production.

Main stack:

```text
Android Studio
Java
XML Layout
Retrofit
Gson
Room / local data
Random User API
```

---

## 2. High-Level Architecture

```text
┌──────────────────────────┐
│       Android UI         │
│ Activity / Fragment / XML│
└─────────────┬────────────┘
              │
              ▼
┌──────────────────────────┐
│      App Logic           │
│ Challenge / User Logic   │
└───────┬──────────┬───────┘
        │          │
        ▼          ▼
┌─────────────┐ ┌─────────────────┐
│ Local Data  │ │ External API    │
│ Room / Mock │ │ Random User API │
└─────────────┘ └─────────────────┘
```

---

## 3. Technology Stack

### Language

```text
Java
```

### UI

```text
Android XML Layout
Material Components
```

### HTTP Client

```text
Retrofit
```

### JSON Parser

```text
Gson Converter
```

### Local Persistence

Recommended:

```text
Room Database
```

Untuk data yang sangat sederhana:

```text
SharedPreferences
```

SharedPreferences hanya digunakan untuk:
- Current mock user
- Login state
- Demo balance sederhana

Room digunakan untuk:
- Challenge
- Proof
- Participant

---

## 4. Suggested Package Structure

```text
com.example.bounty
│
├── activities
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── MainActivity.java
│   ├── ChallengeDetailActivity.java
│   ├── CreateChallengeActivity.java
│   ├── SubmitProofActivity.java
│   └── ReviewProofActivity.java
│
├── adapters
│   └── ChallengeAdapter.java
│
├── models
│   ├── User.java
│   ├── Challenge.java
│   └── Proof.java
│
├── database
│   ├── AppDatabase.java
│   ├── ChallengeDao.java
│   └── ProofDao.java
│
├── network
│   ├── ApiClient.java
│   ├── RandomUserApi.java
│   └── RandomUserResponse.java
│
└── utils
    └── SessionManager.java
```

---

## 5. Screen Navigation

```text
Splash
  ↓
Login
  ↓
MainActivity
  │
  ├── Home
  │     ↓
  │   Challenge Detail
  │     ↓
  │   Accept
  │
  ├── My Challenges
  │     ↓
  │   Submit Proof / Review Proof
  │
  ├── Create Challenge
  │
  └── Profile
```

---

## 6. Challenge State Flow

```text
OPEN
  ↓ Accept
ACCEPTED
  ↓ Submit Proof
SUBMITTED
  ├── Reject → ACCEPTED
  └── Approve → COMPLETED
```

---

## 7. Local-First Data

Challenge utama tidak bergantung pada internet.

```text
Challenge
Proof
Reward
Status
```

disimpan secara lokal.

External API hanya digunakan sebagai requirement API consumption dan untuk enrich UI menggunakan demo user/avatar.

Keuntungan:
- Demo tetap berjalan jika internet buruk
- Scope backend kecil
- Tidak perlu deployment server
- Lebih aman untuk deadline tugas

---

## 8. External API Flow

```text
Android App
    ↓
Retrofit
    ↓
GET randomuser.me/api
    ↓
JSON Response
    ↓
Gson
    ↓
User Model
    ↓
RecyclerView / Profile UI
```

---

## 9. Image Handling

Untuk avatar dari API dapat menggunakan:

```text
Glide
```

Untuk proof:
- User memilih image dari gallery
- Simpan URI lokal
- URI direferensikan oleh data Proof

Tidak perlu upload image ke cloud untuk MVP.

---

## 10. Error Handling

Jika API gagal:

```text
Show fallback avatar
Show local/mock username
App tetap berjalan
```

Jika form invalid:

```text
TextInputLayout.setError(...)
```

Jika database operation gagal:

```text
Show Toast / Snackbar
```

---

## 11. Security Scope

Karena aplikasi bersifat prototype/coursework:

Tidak ada:
- Real payment
- Sensitive banking information
- Production authentication
- Real prize transfer

Jangan menyimpan:
- Credit card
- Bank account
- Password production
- API secret sensitif
