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
At M4 completion, Create remained the M2 shell and M5 had not started.

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

- [x] Create form
- [x] Title input
- [x] Description input
- [x] Reward input
- [x] Deadline input
- [x] Form validation
- [x] Insert challenge ke Room
- [x] Refresh Home setelah publish

Target:

```text
User membuat challenge → challenge muncul di Home.
```

Verified M5: `assembleDebug`, `assembleDebugAndroidTest`, and `lintDebug` pass
with the canonical application ID. Five existing regression tests passed in
the full emulator suite. The focused `CreateChallengeTest` rerun also passed
after fixing the calendar icon hidden by validation errors and anchoring
Snackbars above bottom navigation. Checks cover required fields, zero reward,
invalid/past dates, calendar access, draft restoration across tabs/recreation,
rotation during a held Room insert, duplicate-click protection, saved creator
and OPEN status, Home refresh, detail access, draft reset, and unchanged balance.

Create uses Material text inputs and Android's date picker. Draft state is
retained between tabs and saved even when Create is detached. A small nested
ViewModel holds in-flight write state across rotation; writes use Room's
transaction executor. Failed writes retain the draft for retry. Rewards are
positive whole rupiah, titles have a 100-character limit, descriptions a
2000-character limit, and deadlines must be valid ISO dates from today onward.

Manual screenshots confirmed light mode, keyboard insets, dark mode at 1.3x
font scaling, retained draft text, scrolling, and a reachable Publish action.
Tests/QA used a temporary application ID to preserve existing app data;
emulator animation, keyboard, font, and night settings were restored. The
canonical APK was rebuilt and installed without clearing data. Lint remains
at 0 errors and 4 existing warnings. M6 has not started.

---

# M6 — Accept Challenge

- [x] Add Accept button
- [x] Check challenge status
- [x] Save participant
- [x] Update status OPEN → ACCEPTED
- [x] Show accepted challenge di My Challenges

Target:

```text
Hunter dapat menerima challenge.
```

Verified M6: `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, and
`connectedDebugAndroidTest` passed on Pixel_7 API 37. All seven instrumentation
tests passed, including held acceptance/rotation, duplicate-click protection,
creator rejection, saved participant/status, second-hunter rejection, missing ID,
current-user filtering, My Challenges recreation, and unchanged demo balance.

A conditional Room UPDATE accepts only OPEN, unassigned challenges created by
another user. Detail shows loading/disabled controls and the assigned hunter;
a nested ViewModel retains the write across rotation. My Challenges shares the
existing adapter and query flow, listing records created or accepted by the
current account with loading, empty, retry, and resume refresh behavior.

Manual emulator checks confirmed enabled OPEN acceptance, ACCEPTED/disabled
controls and hunter name, and the accepted record in My Challenges. Screenshots
confirmed light mode and My Challenges in dark mode at 1.3x font scale. Testing
used a temporary application ID to preserve existing Bounty data. The canonical
APK was rebuilt and installed without clearing data; temporary app and emulator
settings were restored. Lint remains at 0 errors and 4 existing warnings.
Changed: ChallengeDao, ChallengeDetailActivity, MainActivity, detail/My Challenges
XML, strings, AcceptChallengeTest, README, and this task ledger. M7 has not started.

---

# M7 — Submit Proof

- [x] Create SubmitProofActivity
- [x] Implement image picker
- [x] Show selected image
- [x] Add proof description
- [x] Insert Proof into Room
- [x] Update challenge ACCEPTED → SUBMITTED

Target:

```text
Hunter dapat submit proof.
```

Verified M7: canonical `assembleDebug`, `assembleDebugAndroidTest`, and
`lintDebug` passed with 0 errors and 4 existing warnings. Seven existing tests
passed in the full emulator suite. The focused `SubmitProofTest` passed after
correcting native picker automation to match the file's accessibility description
and use Android's input command. No production picker change was needed.

Checks cover required description/photo, native picker cancellation/selection,
photo/description restoration, held write and rotation, duplicate-click protection,
stored PENDING proof fields, persisted URI read access, SUBMITTED status, rejection
of another hunter and duplicate submission, insert-failure transaction rollback,
and unchanged demo balance. Tests ran on Pixel_7 API 37 with a temporary application
ID to preserve existing Bounty accounts and challenge data.

SubmitProofActivity uses OpenDocument with persistent URI read permission, Glide
preview, a required description capped at 2000 characters, background image bounds
validation, and retained saving state. Only the assigned hunter of an ACCEPTED
challenge sees the detail action. The Room transaction conditionally changes status
and inserts proof together; a failed insert cannot leave SUBMITTED without proof.
No new dependency, broad storage permission, schema migration, review, or reward.

Manual checks confirmed detail-to-form navigation, native image selection/preview,
keyboard insets, retained image/text in dark mode at 1.3x font scale, a reachable
Submit action, and return to detail with SUBMITTED status. Emulator settings were
restored, temporary app removed, and canonical APK installed without clearing data.
Changed: SubmitProofActivity, ChallengeDetailActivity, AppDatabase, ChallengeDao,
manifest, proof/detail XML, strings, SubmitProofTest, README, and this ledger.
Remaining: moving/deleting the selected source photo can invalidate its local URI.
M8 has not started.

---

# M8 — Review Proof

- [x] Create ReviewProofActivity
- [x] Display proof image
- [x] Display proof description
- [x] Add Approve button
- [x] Add Reject button

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

Verified M8: canonical `assembleDebug`, `assembleDebugAndroidTest`, and
`lintDebug` passed with 0 errors and 4 existing warnings. Eight existing tests
passed in the full emulator suite. The focused `ReviewProofTest` passed after
narrowing UI setup to ReviewProofActivity and waiting for Room status changes;
feed ordering and cross-activity waits no longer obscure the review checks.

Tests cover Reject, resubmission, Approve, latest-proof/creator/participant guards,
invalid IDs, repeated/opposing decisions, rotation during a held review, duplicate
clicks, photo error/retry, recreation, unavailable/missing records, unchanged
balance, and rollback when the challenge update fails after the proof update.

ReviewProofActivity is private and opens from SUBMITTED detail for the creator.
It displays hunter, photo, description, and PENDING status using existing Material
styles and Glide. Approve requires a loaded photo; a missing local image offers
retry and still permits Reject. Controls disable during retained saving.
The Room transaction checks current records and updates both statuses together:
APPROVED/COMPLETED or REJECTED/ACCEPTED. Reject retains the hunter for resubmission.

Manual checks confirmed feed/detail/review navigation, light mode, dark mode at
1.3x font scale, complete action labels, readable proof content, and return to
COMPLETED detail. Tests and visual checks used an isolated application ID;
temporary QA data/helper were removed, settings restored, and canonical APK
installed without clearing existing data. Changed: ReviewProofActivity,
ChallengeDetailActivity, AppDatabase, manifest, review/detail XML, strings,
ReviewProofTest, README, and this ledger. Remaining: deleted/moved local photos
can become unavailable; retry/error handling is present. M9 has not started.

---

# M9 — Mock Reward

- [x] Add Demo Balance
- [x] Save balance with SharedPreferences
- [x] Tambahkan reward setelah approval
- [x] Prevent duplicate reward
- [x] Show reward result

Target:

```text
Approve challenge → Demo Balance hunter bertambah.
```

Verified M9: canonical `assembleDebug`, `assembleDebugAndroidTest`, and
`lintDebug` passed with 0 errors and 4 existing warnings. All ten instrumentation
tests passed on Pixel_7 API 37. A focused final MockRewardTest rerun also passed
with strengthened concurrent settlement and asynchronous balance rendering checks.

Home/Profile share a Demo Balance card and rupiah formatter. Successful approval
credits the registered hunter, preserves the creator balance, and shows a result
in COMPLETED detail. Reject and stale/repeated approvals cannot pay a reward.
SessionManager saves the updated account and one receipt per challenge together
in bounty_accounts SharedPreferences, with synchronous disk commit off the main
thread and a process-local lock across manager instances. Paid balances survive
logout, recreation, restart, and challenge deletion. No Room schema change.

Room approval and preferences cannot share a transaction. If approval commits
before reward persistence, Home/Profile recover approved/completed unpaid records
for the signed-in hunter. This also credits pre-M9 approvals once. Failed reward
persistence reports approval success with reward pending, rather than claiming
review failure; recovery retries disk persistence even if the snapshot is already
visible in memory. The approach remains a single-process local mock, not payment.

Tests cover Approve through the real review screen, duplicate clicks, creator/
hunter separation, Reject and stale-proof guards, 50000-to-70000 reward, interrupted
approval recovery, Home/Profile rendering, recreation, logout/login persistence,
concurrent settlement from separate managers, and retained earnings after deletion.
Manual checks confirmed light Home/completed result and dark Home/Profile at 1.3x
font scale. An XML encoding issue caught during QA was corrected before delivery.

Checks used an isolated app ID; its helper/data were removed, emulator settings
restored, and the canonical APK installed without clearing existing Bounty data.
Changed: SessionManager, ChallengeDao, ChallengeDisplay, MainActivity,
ReviewProofActivity, ChallengeDetailActivity, shared balance/Home/Profile/detail
XML, strings, MockRewardTest, README, and this ledger. Remaining: the four existing
lint warnings; local mock balances are reset by clearing app data. M10 not started.

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
