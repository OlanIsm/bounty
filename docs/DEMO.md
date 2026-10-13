# Bounty APK and presentation

Build from PowerShell with `JAVA_HOME` pointing to JDK 21 or Android Studio's
JBR:

```powershell
./scripts/Build-Apk.ps1
```

The final file is `build/Bounty.apk`. It is a debug-signed coursework build for
`id.ac.binus.bounty`, Android 10 (API 29) or later, not a Play Store release.

## Firebase account setup

Use a Firebase project with Email/Password Authentication and Cloud Firestore
enabled. Register through the app with a real email you control; the app stores
the Auth account in Firebase and creates its `users/{uid}` profile. Do not use
production passwords in screenshots or share them. Firestore rules in
`firebase/firestore.rules` must be deployed before profile writes will succeed.

## Current demo scope

Login/register and user profiles use Firebase. Challenge/proof operations and
demo rewards still use Room and SharedPreferences on the device. Existing local
starter challenges are not cloud records and are not shared across devices.
For a two-account demo, register a creator and hunter, publish a challenge from
the creator, log out and sign in as the hunter to accept/submit proof, then
switch back to the creator to review it. Both accounts must use the same app
installation for the local challenge and reward flow.

The Random User API is independent from Firebase accounts and appears in the
Profile demo-user section. If network access fails, the existing retry/fallback
state keeps the rest of the app usable.

## M14 limitations

Only authentication and profile synchronization have been implemented against
Firebase so far. Cloud challenge/proof/reward persistence and Firebase Storage
remain incomplete. The real Firebase account
and profile write flow was not exercised during this implementation; connection
checks were read-only to avoid creating user or test data.
