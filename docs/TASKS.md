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
Logout clears the session but retains accounts. At M1 completion, MainActivity
was only a signed-in name/email/logout placeholder; navigation is tracked in M2.

Remaining: lint has 0 errors and 4 non-blocking warnings (target SDK 36,
available AGP update, the required SplashActivity name triggering the custom
splash heuristic, and an unused template white color). SplashActivity routes
immediately without a custom splash layout or artificial delay.

---

# M2 — Main Navigation

- [x] Create MainActivity
- [x] Setup bottom navigation
- [x] Create Home screen
- [x] Create My Challenges screen
- [x] Create Create Challenge screen
- [x] Create Profile screen

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

Verified: `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, and
`connectedDebugAndroidTest` passed on Pixel_7 (API 37). All three instrumentation
tests passed, including authentication regression and navigation through all
four destinations, activity recreation, tab selection, Back to Home, and
logout from Profile. Emulator checks used a temporary application ID to keep
the existing Bounty installation's mock accounts intact; the original
`id.ac.binus.bounty` application ID was restored and the deliverable APK rebuilt.

Manual screenshots confirmed Home, My Challenges, Create, and Profile, plus
dark mode and 1.3x font scaling with complete navigation labels and safe insets.
Lint remains at 0 errors and 4 existing non-blocking warnings listed under M1.

Home and My Challenges show empty states. Create is a screen shell without
the M5 form or publish action. Profile shows the current mock user and logout.
At M2 completion, database, feed, and reward behavior remained later milestones.

---

# M3 — Local Database

- [x] Create Challenge entity
- [x] Create Proof entity
- [x] Create ChallengeDao
- [x] Create ProofDao
- [x] Create AppDatabase
- [x] Add initial mock challenges

Target:

```text
Challenge mock dapat dibaca dari Room.
```

Verified: `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, and
`connectedDebugAndroidTest` passed on Pixel_7 (API 37). All four instrumentation
tests passed on the final emulator run. The first run passed the Room test
but failed an existing authentication UI assertion; a full rerun passed
without changes to authentication code or tests.

The Room check covers the three documented seed titles/rewards/OPEN states,
generated IDs, CRUD, creator/participant queries, latest proof selection,
updates, persistence across reopening, no reseeding after deletion, and
foreign-key rejection/cascade deletion. The test uses a separate database.
The suite ran with a temporary application ID to preserve existing Bounty
accounts; the original application ID was restored for the deliverable APK.

Schema v1 is exported under `app/schemas/`. `AppDatabase.getInstance(context)`
opens `bounty_database` lazily on the first DAO operation and seeds inside the
creation transaction. DAO calls must run off the main thread. Mock deadlines
are seven days after creation. M3 adds no UI changes; Home's Room feed remains
M4 at M3 completion. Lint has 0 errors and the same 4 existing warnings.

---

# M4 — Challenge Feed

- [x] Create Challenge model
- [x] Create challenge card XML
- [x] Create RecyclerView
- [x] Create ChallengeAdapter
- [x] Load challenges from Room
- [x] Show reward
- [x] Show status
- [x] Open Challenge Detail

Target:

```text
Home menampilkan challenge feed.
```

Reuses the M3 `Challenge` entity as the model. `MainActivity` loads the Room
feed through its query executor into `ChallengeAdapter`/`item_challenge.xml`.
Cards show creator/fallback avatar, title, description preview, rupiah reward
labelled as a demo, deadline, status, and a detail action. Loading, empty,
and database error/retry states are handled; stale screen results are ignored.

`ChallengeDetailActivity` checks the session and reads the latest record by ID.
It displays full fields, supports Back and recreation, and handles missing,
deleted, and invalid IDs. Detail remains read-only; Accept belongs to M6.
Create remains the M2 shell; M5 has not started.

Verified: `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, and
`connectedDebugAndroidTest` passed on Pixel_7 (API 37). All five instrumentation
tests passed, including the new feed/detail flow, fields/formatting, activity
recreation, status refresh, non-ISO dates, empty feed, and missing/invalid IDs.
The first run exposed a test-launcher task-clearing issue; the standalone
detail scenarios now run after the Home checks. No production behavior was
changed for this test fix.

Manual screenshots confirmed feed/detail in light mode and dark mode with
1.3x font scaling, readable rewards, complete action labels, and safe insets.
Emulator tests and visual checks used a temporary application ID to preserve
existing Bounty data. The original `id.ac.binus.bounty` ID was restored, final
APKs rebuilt, and the app updated without clearing data. Font scale and night
mode were restored; the temporary app was removed. Lint has 0 errors and
the same four existing non-blocking warnings.

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
