# Bounty APK and presentation

Build from PowerShell with `JAVA_HOME` pointing to Android Studio's JBR:

```powershell
./scripts/Build-Apk.ps1
```

The final file is `build/Bounty.apk`. This is an installable, debug-signed
coursework build, version 1.0, application ID `id.ac.binus.bounty`, Android 10
(API 29) or later. It is not a production release or Play Store submission.
The script explicitly disables the isolated test ID and runs lint before copying.
Rebuilding on another computer can use a different debug certificate; an update
requires the same certificate. Keep the current installation/data for the demo.

```powershell
adb install -r build/Bounty.apk
```

## Prepared local data

A fresh installation provides Insan (`demo@bounty.local`, no password), a
Rp50.000 Demo Balance, and three OPEN challenges with deadlines seven days after
database creation. Existing installations retain their accounts, balances,
challenges and session; updating does not reset the demo.

The three starter challenges belong to the placeholder Bounty Demo creator.
Use a newly published challenge for the full presentation so its creator can
sign in and review proof. Accounts and challenges exist on one installation;
switch accounts on the same device rather than using two devices.

## Presentation flow

1. Log in with `demo@bounty.local`. Show Home, initial challenges and balance.
2. Open Profile and show Random User API names/photos. Internet is required
   for live profiles; an error shows the local fallback and Coba lagi.
3. Open Create. Publish `Demo: 30 push-ups`, a short description, reward `20000`
   and a future deadline using the calendar. Open its OPEN detail.
4. Profile > Logout. Register `Demo Hunter` / `hunter@bounty.local`, then log in.
   If already registered, log in directly. Note this account's starting balance.
5. Open the newly published challenge, choose Accept challenge and confirm.
6. Choose Submit proof, select an existing readable photo using the native picker,
   enter a description and submit. Show SUBMITTED in My Challenges.
7. Logout and log in as `demo@bounty.local`. Open the same challenge and Review
   proof. Show photo/description, choose Approve and confirm the demo reward.
8. Show COMPLETED. Log in as the hunter and show Demo Balance increasing by
   Rp20.000 (Rp50.000 to Rp70.000 for a fresh hunter). Relaunch to show persistence.

For another run, publish another challenge. Paid rewards cannot be credited
twice. Keep the selected proof photo in place until after presentation.
No password, backend, real money, or debug-only UI is needed.

## Current emulator preparation (2026-10-08)

Pixel_7/API 37 has both Insan and Demo Hunter (`hunter@bounty.local`) registered,
each at Rp50.000. Insan owns an OPEN `Demo: 30 push-ups` challenge, reward
Rp20.000, deadline 2026-10-15. It was published through Create, preserving all
existing data. The app is left signed in as Insan. Use this challenge or publish
another to demonstrate Create during presentation. These records are local to
this emulator; a fresh installation still uses the initial data described above.
