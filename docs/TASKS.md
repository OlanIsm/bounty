# Bounty — Development Tasks

## Rule

Kerjakan milestone secara berurutan.

Jangan mulai fitur berikutnya sebelum flow milestone sebelumnya dapat dijalankan.

---

# M0 — Project Setup

- [x] Create Android Studio project
- [x] Set application name menjadi `Bounty`
- [x] Gunakan Java
- [x] Setup Material Components
- [x] Setup RecyclerView
- [x] Setup Retrofit
- [x] Setup Gson Converter
- [x] Setup Glide
- [x] Setup Room
- [x] Tambahkan INTERNET permission
- [x] Buat package structure

Target:

```text
Project dapat di-build dan dijalankan di emulator.
```

Verified: `assembleDebug`, `lintDebug`, and `connectedDebugAndroidTest` passed
on the Pixel_7 emulator (API 37). The setup smoke test launches the Java activity,
checks the Bounty title, and checks INTERNET permission. This records the M0
verification before authentication was added.
Lint reports 0 errors and 4 non-blocking warnings: target SDK 36, an available
AGP update, and the template's two unused color resources.

---

# M1 — Mock Authentication

- [x] Create SplashActivity
- [x] Create LoginActivity
- [x] Create RegisterActivity
- [x] Create SessionManager
- [x] Simpan login state dengan SharedPreferences
- [x] Create mock current user
- [x] Implement logout

Target:

```text
Login → Home → Logout
```

Verified: `assembleDebug`, `lintDebug`, and `connectedDebugAndroidTest` passed
on Pixel_7 (API 37); both instrumentation tests passed. Authentication checks
cover invalid input, duplicate registration, normalized email, mock login,
session persistence, logout, Back behavior, and guarding the signed-in screen.
Manual emulator checks confirmed session persistence after a process restart,
login/Home/register layouts, dark mode, 1.3x font scale, and keyboard insets.

Demo login: `demo@bounty.local` (Insan), no password. Registration stores local
mock accounts and returns to login with the registered email filled in.
Logout clears the session but retains accounts. MainActivity is only a signed-in
name/email/logout placeholder for the M1 target; M2 has not been started.

Remaining: lint has 0 errors and 4 non-blocking warnings (target SDK 36,
available AGP update, the required SplashActivity name triggering the custom
splash heuristic, and an unused template white color). SplashActivity routes
immediately without a custom splash layout or artificial delay.

---

# M2 — Main Navigation

- [ ] Create MainActivity
- [ ] Setup bottom navigation
- [ ] Create Home screen
- [ ] Create My Challenges screen
- [ ] Create Create Challenge screen
- [ ] Create Profile screen

Navigation:

```text
Home
My Challenges
Create
Profile
```

Target:

```text
Semua main screen dapat dibuka.
```

---

# M3 — Local Database

- [ ] Create Challenge entity
- [ ] Create Proof entity
- [ ] Create ChallengeDao
- [ ] Create ProofDao
- [ ] Create AppDatabase
- [ ] Add initial mock challenges

Target:

```text
Challenge mock dapat dibaca dari Room.
```

---

# M4 — Challenge Feed

- [ ] Create Challenge model
- [ ] Create challenge card XML
- [ ] Create RecyclerView
- [ ] Create ChallengeAdapter
- [ ] Load challenges from Room
- [ ] Show reward
- [ ] Show status
- [ ] Open Challenge Detail

Target:

```text
Home menampilkan challenge feed.
```

---

# M5 — Create Challenge

- [ ] Create form
- [ ] Title input
- [ ] Description input
- [ ] Reward input
- [ ] Deadline input
- [ ] Form validation
- [ ] Insert challenge ke Room
- [ ] Refresh Home setelah publish

Target:

```text
User membuat challenge → challenge muncul di Home.
```

---

# M6 — Accept Challenge

- [ ] Add Accept button
- [ ] Check challenge status
- [ ] Save participant
- [ ] Update status OPEN → ACCEPTED
- [ ] Show accepted challenge di My Challenges

Target:

```text
Hunter dapat menerima challenge.
```

---

# M7 — Submit Proof

- [ ] Create SubmitProofActivity
- [ ] Implement image picker
- [ ] Show selected image
- [ ] Add proof description
- [ ] Insert Proof into Room
- [ ] Update challenge ACCEPTED → SUBMITTED

Target:

```text
Hunter dapat submit proof.
```

---

# M8 — Review Proof

- [ ] Create ReviewProofActivity
- [ ] Display proof image
- [ ] Display proof description
- [ ] Add Approve button
- [ ] Add Reject button

Approve:

```text
Proof → APPROVED
Challenge → COMPLETED
```

Reject:

```text
Proof → REJECTED
Challenge → ACCEPTED
```

Target:

```text
Creator dapat approve atau reject proof.
```

---

# M9 — Mock Reward

- [ ] Add Demo Balance
- [ ] Save balance with SharedPreferences
- [ ] Tambahkan reward setelah approval
- [ ] Prevent duplicate reward
- [ ] Show reward result

Target:

```text
Approve challenge → Demo Balance hunter bertambah.
```

---

# M10 — External API

- [ ] Create RandomUserApi interface
- [ ] Create Retrofit client
- [ ] Create response model
- [ ] GET `/api/?results=10`
- [ ] Parse response
- [ ] Display API user/avatar
- [ ] Load image using Glide
- [ ] Add loading state
- [ ] Add error/fallback state

Target:

```text
Aplikasi berhasil consume external REST API.
```

---

# M11 — UI Polish

- [ ] Apply design system
- [ ] Consistent spacing
- [ ] Status badges
- [ ] Reward styling
- [ ] Empty state
- [ ] Loading indicator
- [ ] Error messages
- [ ] Confirm dialog untuk Accept
- [ ] Confirm dialog untuk Approve

Target:

```text
App siap dipresentasikan.
```

---

# M12 — Testing

Test scenario:

- [ ] Fresh install
- [ ] Register
- [ ] Login
- [ ] Logout
- [ ] View challenge
- [ ] Create challenge
- [ ] Accept challenge
- [ ] Submit proof
- [ ] Reject proof
- [ ] Resubmit proof
- [ ] Approve proof
- [ ] Balance update
- [ ] API success
- [ ] API failure
- [ ] App restart
- [ ] Invalid form input

Target:

```text
Main flow tidak crash.
```

---

# M13 — APK Build

- [ ] Update app icon
- [ ] Check application name
- [ ] Remove debug UI
- [ ] Build APK
- [ ] Install APK di Android device/emulator
- [ ] Test APK
- [ ] Prepare demo account/data

Final output:

```text
Bounty.apk
```

---

# Final Demo Flow

Gunakan flow ini saat presentasi:

```text
1. Login
2. Show challenge feed
3. Show external API user/avatar
4. Create challenge
5. Open challenge
6. Accept challenge
7. Submit proof
8. Review proof
9. Approve proof
10. Show Completed status
11. Show Demo Balance bertambah
```

Flow ini menunjukkan hampir semua requirement utama aplikasi dalam satu demo singkat.
