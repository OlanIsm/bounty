# Bounty

Android coursework MVP using Java and XML. M0 project setup, M1 mock
authentication, M2 main navigation, M3 local database, M4 challenge feed,
M5 challenge publishing, M6 challenge acceptance, M7 proof submission,
M8 proof review, M9 mock reward, M10 external demo profiles, and M11 UI polish
are implemented. M12 testing is verified; M13 has not started.

## Requirements

- Android Studio with JDK 21 (the bundled JBR works).
- Android SDK Platform 37 and the SDK build tools requested by Gradle.
- An Android device or emulator running API 29 or higher.

Open this directory in Android Studio, let Gradle sync, then run the `app`
configuration. Set your SDK path in the untracked `local.properties` file if
Android Studio has not created it. For command-line builds, set `JAVA_HOME`
to your JDK installation.

```powershell
./gradlew.bat assembleDebug lintDebug
./scripts/Test-Android.ps1
```

The second command needs a running device or emulator and uses a separate
`id.ac.binus.bounty.testing` installation. Pass `-Adb` for a custom SDK location
and `-Serial` when multiple devices are connected. Tests check the launcher,
INTERNET permission, registration validation, duplicate email handling, login,
session persistence, all four navigation destinations, restored tab selection,
Back behavior, logout, the signed-in screen guard, Room seed/CRUD/persistence,
feed/detail navigation, recreation, refresh, empty states, missing IDs,
Create validation, draft restoration, single-insert publishing, and acceptance
with rotation, duplicate protection, persistence, and current-user filtering,
plus native image picking, transactional proof submission, creator review,
mock reward/recovery, external API parsing/loading/fallback behavior, and
confirmation cancellation/recreation before accepting or approving.
The script resets only the isolated installation to avoid restored backup
fixtures, checks fresh Login, runs the feature tests and a continuous
creator/hunter flow, then checks the paid hunter after two actual process
restarts. It removes only the temporary app/test packages and restores emulator
animation settings in `finally`. Reports are written under
`app/build/reports/m12/`. Raw instrumentation resets local mock accounts and
temporarily deletes challenges/proofs, so use the isolated script rather than
`connectedDebugAndroidTest` against your normal installation.
The script builds isolated APKs; run `assembleDebug` again to produce the
canonical `id.ac.binus.bounty` APK for normal use.
The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## M0 setup

Material Components, RecyclerView, Retrofit with Gson Converter, Glide, and
Room are configured in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
Room uses the [Java annotation processor](https://developer.android.com/jetpack/androidx/releases/room#declaring_dependencies).
No Room entities or API calls are implemented during M0.

Packages under `id.ac.binus.bounty` follow `docs/ARCHITECTURE.md`:
`activities`, `adapters`, `models`, `database`, `network`, and `utils`.
Empty packages use `package-info.java` so Git preserves the structure without
adding placeholder feature classes.

Project requirements and milestone order are in `docs/` and `AGENTS.md`.

## M1 mock authentication

Launch the app to enter login, or resume Home if a session
already exists. Use `demo@bounty.local` (name: Insan), or register a name and
email, then log in with that email. There is no password or authentication
server: any person using this installation can select a registered mock account.
Use demo data only.

`SessionManager` stores users by normalized email in `bounty_accounts`
SharedPreferences and the current email/login state in `bounty_session`.
Each user has the fields from `docs/DATABASE.md`, including a stable ID,
an empty local avatar URL, and an initial mock balance of 50000. Balance UI
and reward updates remain M9 work. Logout clears only the session, preserving
accounts for later login. Session preferences are excluded from backups and
device transfer.

Profile shows the current name/email and a logout button.
Authentication transitions clear the activity task so Back cannot return to
the signed-in screen after logout. M1 provides authentication; navigation was
added in M2.

Auth forms use Material controls, the documented orange/yellow palette,
16dp screen padding, scrollable layouts, and keyboard/system-bar insets.
Button labels use dark text on orange to maintain readable contrast.

## M2 main navigation

MainActivity hosts four XML screens through Material BottomNavigationView:
Home, My Challenges, Create, and Profile. Switching tabs replaces the content
inside the activity; selecting the current tab keeps it in place. The selected
destination survives activity recreation. Back returns to Home from other tabs;
Back on Home follows the default Android behavior.

At M2 completion, Home greeted the current user and showed an empty challenge section.
At M2 completion, My Challenges showed an empty state and Create was a screen shell;
M5 adds the publishing form. Profile displays the local user's name/email and supports logout.
These were navigation screens only at M2 completion: the feed was added in M4,
publishing was added in M5, and balances/rewards remain M9.

Screens reuse the existing colors, typography, and spacing, and scroll when
content or font sizes need more space. The root handles system-bar, cutout,
and keyboard insets so the bottom navigation does not apply them twice.

## M3 local database

`models/Challenge.java` and `models/Proof.java` implement the Room tables from
`docs/DATABASE.md`. `database/ChallengeDao.java` provides insert, list, lookup,
creator/participant filtering, update, and delete operations. `ProofDao.java`
inserts/updates proofs and returns the latest submission for a challenge,
ordered by submission time and then ID. Deleting a challenge also deletes its
proofs; SQLite rejects proofs referencing a missing challenge.

Use `AppDatabase.getInstance(context)` to obtain the application-context
singleton named `bounty_database`. DAO operations are synchronous and must run
off the main thread. Room opens the database lazily on the first operation;
its creation callback inserts the three documented OPEN mock challenges in
the creation transaction. Their creator is Bounty Demo and their deadlines
are seven days after database creation. Reopening does not seed again, even
after a demo challenge is deleted. Schema v1 is exported under `app/schemas/`.

`AppDatabaseTest` checks seed values, CRUD/filtering, latest proof selection,
reopening/persistence, absence of reseeding, and foreign-key enforcement using
a separate test database. M3 added the data layer only; M4 connects Home to Room.

## M4 challenge feed and detail

Home now reads Room challenges into a RecyclerView using `ChallengeAdapter`
and `item_challenge.xml`. The M3 `Challenge` entity also serves as the model;
there is no duplicate DTO. Cards display creator/avatar (with local fallback),
title, a two-line description preview, reward labelled as a demo, deadline,
status, and a detail button. Rupiah formatting and date presentation are
shared with detail in `ChallengeDisplay`; non-ISO dates retain their stored text.

Queries run on Room's query executor. Home refreshes on resume or after
switching back to its tab. Loading, empty, and database error states are
handled; errors offer retry and show a Snackbar. Results from destroyed
activities or replaced Home views are ignored. ListAdapter applies changed
rows without replacing the entire list.

`ChallengeDetailActivity` opens by challenge ID, checks the local session,
and reads the latest Room record. It shows the full description and all card
fields in a scrollable layout. Back returns to the feed; activity recreation
retains the ID. Missing, invalid, and deleted IDs show an explanatory state.
At M4 completion, detail was read-only. M5 adds publishing and M6 adds acceptance.

`ChallengeFeedTest` verifies the Room-backed feed/detail flow, fields and
formatting, activity recreation, refreshed status, legacy dates, empty feed,
and missing/invalid detail records. It inserts a test challenge directly
through the DAO, then restores the original challenge data during cleanup.

## M5 create and publish

Open the Create tab, enter a title, description, positive whole-rupiah demo
reward (no separators), and deadline in `YYYY-MM-DD` format, then choose
Publish challenge. The calendar icon also opens Android's date picker and
remains available when a date has a validation error. Titles are limited to
100 characters and descriptions to 2000; whitespace-only text, zero/invalid
rewards, invalid dates, and past dates are rejected with field errors.

Publishing creates an OPEN Room challenge with the current user's ID,
name/avatar, and a creation timestamp. It runs on Room's transaction executor,
disables the form while saving, then clears the completed draft and returns
to Home with a refreshed feed and success message. Errors preserve the input
and let the user retry Publish. The operation does not modify demo balances.

MainActivity retains the Create view between tabs and saves its hierarchy
even when another destination is active. Drafts survive activity recreation.
A small nested Android ViewModel/LiveData holds in-flight publish state across
rotation, without retaining an Activity, and rejects duplicate submissions.
Results are consumed after Android restores the navigation/view state.

`CreateChallengeTest` verifies required-field, zero-reward, invalid/past-date
validation, native calendar access, draft restoration across tabs/rotation,
duplicate-click protection during a held Room insert, rotation during saving,
persisted creator/status/reward fields, refreshed Home, detail access, draft
reset, and unchanged mock balance.

## M6 accept and My Challenges

Open an OPEN challenge created by another account and choose Accept challenge.
Room atomically changes it to ACCEPTED and stores the current hunter's ID/name.
The conditional update rejects non-OPEN, already assigned, missing, and creator-owned
records, so repeated or competing acceptance cannot overwrite the first hunter.
The button disables while saving and after acceptance; detail displays the hunter.
A nested ViewModel retains the write across recreation. Errors show a Snackbar
and reload the latest record for retry. Acceptance leaves demo balances unchanged.

My Challenges reuses the feed adapter, detail navigation, and loading/empty/retry
states. It lists challenges created or accepted by the signed-in account, newest
first, refreshing on tab entry and resume. No schema change or library was needed.

`AcceptChallengeTest` checks creator rejection, disabled controls and duplicate
clicks during a held write, rotation during/after saving, persisted hunter/status,
rejection of a second hunter and invalid ID, creator/hunter filtering, My Challenges
across recreation, and unchanged balance. All seven instrumentation tests pass
on Pixel_7 API 37 using an isolated test application ID. Proof submission is added in M7.

## M7 submit proof

From My Challenges or Home, open an ACCEPTED challenge assigned to your account
and choose Submit proof. Pick an image with Android's document picker, add a
nonblank description (up to 2000 characters), and submit. The form previews the
selected image, retains its URI and description across recreation, and disables
controls during saving. Cancelling the picker retains the draft.

The picker uses [OpenDocument and persistable read access](https://developer.android.com/training/data-storage/shared/documents-files)
without a broad storage permission. The app stores the local content URI, not a
cloud upload. Access persists across restarts; deleting or moving the source
image can still make it unavailable. Submission checks image readability off the
main thread using decode bounds; an unreadable image produces an actionable error.

`AppDatabase.submitProof` conditionally changes the assigned hunter's ACCEPTED
challenge to SUBMITTED and inserts its PENDING proof within one Room transaction.
Stale/duplicate submissions and other hunters are rejected; insertion failure
rolls back the status. A ViewModel retains in-flight saving across rotation.
Success returns to the refreshed detail; mock balance stays unchanged.
Review and approval/rejection are added in M8; rewards remain M9.

`SubmitProofTest` exercises required description/photo, cancellation and selection
through the native picker, draft recreation, duplicate clicks and rotation during
a held write, stored URI/read grant, proof fields, challenge status, hunter guards,
duplicate rejection, transaction rollback, and unchanged balance.

M7 verification: canonical debug app/test APK builds and lint pass (0 errors,
4 existing warnings). Seven regression tests passed in the full emulator suite;
the focused SubmitProofTest rerun passed after fixing native picker automation.
Manual checks confirmed light/keyboard layouts, retained photo/text in dark mode
at 1.3x font scale, and return to detail with SUBMITTED status. Tests and QA used
an isolated application ID; the canonical APK was installed without clearing data.

## M8 review proof

Sign in as a challenge's creator, open its SUBMITTED detail from Home or My
Challenges, and choose Review proof. The screen shows the latest hunter, photo,
description, and PENDING status. Approve changes the proof to APPROVED and the
challenge to COMPLETED. Reject changes the proof to REJECTED and the challenge
back to ACCEPTED, preserving its hunter so another proof can be submitted.
Success returns to refreshed detail. At M8 completion, balances remained unchanged;
M9 adds the hunter reward below.

`AppDatabase.reviewProof` checks the creator, submitted challenge, latest proof
ID, PENDING status, and matching participant inside a Room transaction. It updates
both records together and rolls back on failure. A stale proof cannot approve a
new submission; repeated/opposing decisions cannot change a reviewed challenge.
The native ViewModel retains an in-flight review across rotation.

Controls disable while saving. Approve enables after the image loads. If its local
URI no longer works, the screen displays an error and retry; Reject remains
available after reading the description. Missing records, unauthorized access,
stale states, and database failures show explanatory states or a Snackbar.
M8 introduced no schema change, new dependency, or review confirmation dialog (M11).

`ReviewProofTest` checks Reject with held-write rotation and duplicate protection,
resubmission, stale/unauthorized/repeated review rejection, photo failure/recovery,
recreation, Approve, rollback after a forced second-record write failure, unavailable
and missing records, and unchanged balance. Eight regression tests passed in the
full Pixel_7 API 37 suite; the focused M8 test passed after narrowing its UI setup
to the review screen and waiting for Room results. Canonical app/test APK builds
and lint pass with 0 errors and 4 existing warnings. Manual screenshots confirmed
light/dark review, 1.3x fonts, photo/description/action layout, and detail refresh to
COMPLETED. Testing used an isolated installation; the main APK was updated without
clearing data, and emulator settings and temporary installations were restored.


## M9 mock reward

Home and Profile display the current account's Demo Balance (initially Rp50.000).
Approve credits the challenge reward to the registered hunter, without reducing
the creator balance. Reject pays nothing. COMPLETED detail displays the amount
and hunter after payment; this is local simulation, not real money.

`SessionManager.creditCompletedRewards` runs off the main thread. It finds Room
COMPLETED challenges with APPROVED proofs and saves the updated account plus a
per-challenge receipt in one `bounty_accounts` SharedPreferences commit. A local
lock serializes manager instances. Receipts prevent duplicate credits, including
after rotation, reopening, or repeated review. Earned balance is persisted rather
than recomputed from remaining challenges, so deletion cannot remove old earnings.

Approval stays atomic within Room; preferences are a separate store. If the app
stops after approval and before reward persistence, the hunter's next Home/Profile
visit settles completed unpaid challenges. Existing M8 approvals are also credited
once. The review result distinguishes approval with reward pending from review
failure. A later refresh retries failed preferences disk commits. This mechanism
is for the single-process coursework app; no backend ledger, Room migration, new
dependency, real payment, or M10 external API is introduced. Clearing app data
resets local mock accounts, receipts, and balances.

`MockRewardTest` covers real Approve UI, duplicate clicks, unchanged creator balance,
Reject/stale guards, Rp50.000 + Rp20.000 = Rp70.000, recovery after an interrupted
approval, Home/Profile refresh, recreation, logout/login persistence, concurrent
payments from separate SessionManager instances, and retained balance after deletion.
All ten tests passed on Pixel_7 API 37; the strengthened final reward test also
passed separately. Canonical app/test builds and lint pass with 0 errors and 4
existing warnings. Light/dark screenshots and 1.3x font scale were checked in an
isolated installation; temporary QA artifacts/settings were cleaned up, and the
canonical app was updated without clearing existing data. M10 has not started.


## M10 external demo profiles

Open Profile and scroll to User demo, below the local account controls. Retrofit
requests `https://randomuser.me/api/?results=10`; Gson maps UUID, first/last name,
email, and large picture to read-only User models. Glide loads HTTPS avatars with
a local placeholder if the photo fails. This list never registers API users or
changes local account identity, challenges, proofs, or Demo Balance.

The request starts on the first Profile visit. A native ViewModel retains the
in-flight call and result across rotation and tab changes, ignores duplicate
loads, and cancels the call when cleared. The HTTP call has a 20-second timeout.
Loading shows a progress indicator. Network/HTTP errors, invalid JSON, API error
payloads, empty results, or no usable profiles show Bounty User with a local avatar
and a retry button. The main challenge flow remains local and available offline.
Results remain in memory for the current MainActivity; a fresh launch fetches again.

Reference: [RandomUser documentation](https://randomuser.me/documentation).
Only the documented demo fields are modeled; credentials from the API response
are neither mapped nor stored. The list is capped at ten native rows without
pagination, a new dependency, account mutation, or Room schema change.

`ExternalApiTest` uses the existing OkHttp interceptor support to exercise actual
Retrofit request construction and Gson conversion without relying on the public
service. It checks path/query, loading, one request through rotation/tab changes,
profile rendering, malformed/empty/API-error responses, HTTP failure, offline
failure, successful recovery, ten-row limit, unsafe avatar fallback, and unchanged
local account/balance. M11 UI polish and confirmation dialogs remain separate.


M10 verification: canonical debug app/test APK builds and lint pass with 0 errors
and 4 existing warnings. Ten tests passed in the full suite; the existing native
picker check failed during cancellation/focus, then passed separately after waiting
for either AOSP or Google DocumentsUI and return to the app instead of a fixed
500ms delay. The focused API test also passed. Tests used a temporary application
ID, and a smaller temporary build heap relieved host RAM pressure.

Manual checks confirmed real API profiles and Glide photos, light mode, dark mode
at 1.3x font scale, offline fallback, local Home while offline, and the actual retry
button loading profiles after connectivity returned. Emulator network/appearance/
animation settings were restored and the isolated app removed. The canonical APK
was installed without clearing existing Bounty data. M11 has not started.


## M11 UI polish

Challenge cards/detail and proof review show labeled status badges with semantic
light/dark colors. Rounded orange reward labels keep the demo amount prominent.
Native shared styles preserve the documented typography, padding, spacing,
button/card/dialog corners, and readable contrast. Home's welcome and balance
scroll with the feed to keep challenges reachable in landscape and larger fonts.
Existing empty/loading/error/retry states remain available; asynchronous messages
are announced through polite accessibility live regions.

Accept Challenge now asks for confirmation before assigning the hunter. Approve
Proof asks for confirmation with the challenge, reward amount, and recipient,
explicitly identifying the reward as demo money. Batal dismisses without changing
status or balance. Pending confirmations restore after activity recreation;
confirmation still checks the current actor, Room permissions, latest proof,
and current challenge state. Existing duplicate/busy guards and exactly-once
mock reward crediting remain in place. No new dependency or database migration.


## M12 testing

Use `scripts/Test-Android.ps1` for the complete campaign. The debug-only
`bountyIsolatedTest` flag installs the app beside normal Bounty, so account-reset
and destructive fixture checks affect only the disposable test installation.
The script verifies fresh Login, runs the existing feature tests, and runs
MainFlowTest through public creator/hunter screens, including native image
selection, rejection, resubmission, approval, and mock balance updates.

The final paid hunter is retained only for the script's restart checks. The
script force-stops and relaunches the real process twice and checks the session,
COMPLETED challenge, and Rp70.000 balance. It then removes the isolated app/test
packages and restores animation settings, including originally absent values.
API parsing/rendering/fallback tests use deterministic Retrofit HTTP fixtures.
Coverage, results, and limitations are recorded in [TEST_REPORT.md](docs/TEST_REPORT.md).
After a flow failure, `-MainFlowOnly` repeats fresh install, the continuous flow,
and process restarts without rerunning feature checks. It does not replace the
full campaign. M13 APK packaging and presentation data remain separate work.
