# Bounty

Android coursework MVP using Java and XML. M0 project setup, M1 mock
authentication, and M2 main navigation are implemented. M3 and later features
are not implemented.

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
./gradlew.bat connectedDebugAndroidTest
```

The second command needs a running device or emulator. Tests check the launcher,
INTERNET permission, registration validation, duplicate email handling, login,
session persistence, all four navigation destinations, restored tab selection,
Back behavior, logout, and the signed-in screen guard. The authentication
test resets local mock accounts on the test device.
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

Home greets the current user and shows an empty challenge section.
My Challenges shows an empty state. Create explains that the form is not yet
available. Profile displays the local user's name/email and supports logout.
These are navigation screens only: Room entities remain M3, the challenge feed
M4, publishing M5, and balances/rewards M9.

Screens reuse the existing colors, typography, and spacing, and scroll when
content or font sizes need more space. The root handles system-bar, cutout,
and keyboard insets so the bottom navigation does not apply them twice.
