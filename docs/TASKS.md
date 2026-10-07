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

- [x] Create RandomUserApi interface
- [x] Create Retrofit client
- [x] Create response model
- [x] GET `/api/?results=10`
- [x] Parse response
- [x] Display API user/avatar
- [x] Load image using Glide
- [x] Add loading state
- [x] Add error/fallback state

Target:

```text
Aplikasi berhasil consume external REST API.
```

Verified M10: canonical `assembleDebug`, `assembleDebugAndroidTest`, and
`lintDebug` passed with 0 errors and 4 existing warnings. Ten of eleven tests
passed in the full Pixel_7 API 37 suite, including ExternalApiTest. The existing
SubmitProofTest failed during native picker cancellation/focus. Its fixed-delay
Back was replaced with package readiness checks for both AOSP and Google
DocumentsUI; the focused final picker rerun passed. The API test also passed in
a focused rerun. Low host RAM was relieved by stopping the idle build daemon and
using a smaller temporary build heap, without restarting the user emulator.

RandomUserApi, ApiClient, and RandomUserResponse implement the documented HTTPS
GET api/?results=10 with Retrofit/Gson and a 20-second call timeout. Only UUID,
name, email, and large avatar are modeled. Valid profiles map to read-only User
objects, capped at ten; invalid avatar URLs use the local placeholder. Credentials
are neither modeled nor persisted. No new dependency, Room migration, or API
mutation of local accounts/challenges/proofs/balances.

Profile shows User demo below account controls, with native rows and Glide avatars.
A nested ViewModel retains a single in-flight request/result across rotation and
tab changes, guards repeated loads, and cancels its call when cleared. Loading,
HTTP/network failure, invalid JSON, API error, and empty/unusable results are
handled. Failure shows Bounty User/local avatar and Coba lagi. Results stay in
memory for the current MainActivity; a fresh launch fetches again.

ExternalApiTest exercises the real Retrofit path/query/converter with deterministic
OkHttp fixtures, loading, duplicate guard, held request/recreation/tab changes,
rendered names/emails, malformed/empty/API-error payloads, HTTP 503, offline error,
recovery, ten-row cap, unsafe avatar fallback, and unchanged local identity/balance.

A real request returned ten usable profiles. Manual emulator checks confirmed real
API names/photos, light mode, dark mode with 1.3x font scale, offline fallback, Home
availability offline, and recovery through the actual retry button after restoring
connectivity. Tests/QA used an isolated app ID. Network/font/night/animation settings
were restored, isolated installations removed, and the canonical APK updated without
clearing existing Bounty data. Changed: MainActivity, User documentation, ApiClient,
RandomUserApi, RandomUserResponse, Profile/user-row XML, strings, ExternalApiTest,
SubmitProofTest synchronization, README, and this ledger. Remaining: four existing
lint warnings; public service/photo availability uses the documented fallback.
M11 remained pending at M10 completion.

---

# M11 — UI Polish

- [x] Apply design system
- [x] Consistent spacing
- [x] Status badges
- [x] Reward styling
- [x] Empty state
- [x] Loading indicator
- [x] Error messages
- [x] Confirm dialog untuk Accept
- [x] Confirm dialog untuk Approve

Target:

```text
App siap dipresentasikan.
```

Implementation and verification (2026-10-08):

Shared Java/XML presentation now applies labeled status badges to cards/detail
and proof review, rounded orange demo reward highlights, consistent spacing,
secondary button styling, and native 20dp confirmation dialogs. Light/dark badge
pairs all exceed 4.5:1. Dialog action text uses themed readable colors after visual
QA identified insufficient contrast in the stock orange text buttons. Existing
empty/loading/error/retry states are retained, with polite live regions for
asynchronous messages and descriptive loading labels.

Home uses the existing RecyclerView with a native ConcatAdapter welcome/balance
header so the feed remains scrollable in short landscape windows and larger
fonts. No new dependency, custom animation, payment feature, or Room migration.

Accept and Approve now show Batal/action confirmations describing the challenge
and demo reward; Approve also identifies the recipient and says it is not real
money. A native DialogFragment retains the captured actor/challenge/proof payload
through recreation, prevents duplicate dialogs, and routes confirmed actions
through the existing retained models and atomic Room authorization/state checks.
Cancel changes neither status nor balance.

Validation: assembleDebug, assembleDebugAndroidTest, and lintDebug pass; lint has
0 errors and the same four pre-existing warnings. All 11 existing instrumentation
tests pass on Pixel_7/API 37 using an isolated app ID. Updated acceptance/review
checks cover cancellation and pending-dialog recreation; mock reward checks cover
duplicate clicks and unchanged exactly-once crediting. Feed checks account for the
scrolling header and still cover empty state, refresh, recreation, and missing IDs.

Manual isolated-install QA checked Home/status/reward presentation, Accept and
Approve dialogs, native Back cancellation, light/dark modes, 1.3x font scale, and
landscape feed scrolling. A final visual confirmation checked readable dialog
button text in both themes. Temporary fixtures/installations were removed and
font/night/rotation/animation settings restored. The canonical APK was rebuilt
and installed without clearing existing Bounty data.

Changed: ChallengeDetailActivity, ReviewProofActivity, MainActivity,
ActionConfirmation, ChallengeDisplay, shared theme/colors/strings/reward drawable,
Home header XML, async-message layouts, four existing instrumentation tests,
README, DESIGN_SYSTEM, and this ledger. Remaining: four existing lint warnings;
QA used the current phone emulator rather than a separate tablet device. M12
remained pending at M11 completion.

---

# M12 — Testing

Test scenario:

- [x] Fresh install
- [x] Register
- [x] Login
- [x] Logout
- [x] View challenge
- [x] Create challenge
- [x] Accept challenge
- [x] Submit proof
- [x] Reject proof
- [x] Resubmit proof
- [x] Approve proof
- [x] Balance update
- [x] API success
- [x] API failure
- [x] App restart
- [x] Invalid form input

Target:

```text
Main flow tidak crash.
```

Implementation and verification (2026-10-08):

All 16 scenarios are verified on Pixel_7/API 37 using a separate
id.ac.binus.bounty.testing installation. The full feature batch passed 11 tests;
the final focused runner passed the continuous MainFlowTest, fresh Login, and
two actual process force-stop/relaunch checks. Hunter session, COMPLETED state,
and Rp70.000 balance persisted without duplicate credit. Creator stayed at
Rp50.000. Public UI flow covers creator/hunter registration/login/logout, Create
validation/publication, Accept confirmation, native photo selection/submission,
Reject, resubmission, Approve confirmation, and My Challenges/reward rendering.
Dedicated feature checks cover invalid input, persistence, atomic rollback,
rotation, stale/duplicate actions, recovery and deterministic API success/failure.

scripts/Test-Android.ps1 builds isolated APKs, resets only the disposable install
(including potentially restored backup fixtures), checks fresh Login, runs the
feature batch and continuous flow, and verifies process restarts. It preserves
plain-text reports under app/build/reports/m12 and restores animation settings
and removes isolated packages in finally. MainFlowOnly repeats the flow/restart
part after a failure without repeating already passed feature checks. Normal
builds retain id.ac.binus.bounty; the debug-only isolation flag does not change
the namespace or release application ID.

Testing exposed a shifting DocumentsUI grid tap during resubmission; the reused
picker helper now prefers native accessibility click with the existing tap
fallback. The corrected full flow and picker feature checks passed. A subsequent
full campaign passed all 11 feature tests but its flow phase was blocked by an
ANR dialog from the old canonical app. The trace showed HardwareRenderer/EGL/
qemu_pipe waits, consistent with an emulator graphics stall. The original process
was stopped without clearing data or restarting the emulator; focused flow and
restart reruns passed. The runner brings the launcher forward between batches,
and UI timeout errors identify the active package. No production rendering
workaround was added. Aggregate coverage is reported honestly rather than as
one uninterrupted campaign.

Canonical assembleDebug, assembleDebugAndroidTest, and lintDebug pass with
0 errors and the same four existing lint warnings. The canonical APK was rebuilt,
original accounts/data preserved, and temporary app/test installations removed.
Changed: isolated debug build flag, MainFlowTest, shared SubmitProofTest picker
helper, Test-Android.ps1, README, TEST_REPORT.md, and this ledger.

Full evidence and reproduction steps: docs/TEST_REPORT.md. Remaining: four old
lint warnings, the observed long-session emulator renderer stall, no separate
API 29/tablet device campaign, public API/photo uptime, and local photo URI
invalidation after moving/deleting the source. M13 has not started.

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
