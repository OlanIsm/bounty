# M12 test report

Date: 2026-10-08. Status: all 16 M12 scenarios verified.

Environment: Windows PowerShell, bundled Android Studio JBR, Pixel_7 emulator
on Android API 37. Production code targets API 29 and later; this campaign uses
the current phone emulator, not a separate API 29 device or tablet.

The canonical installation is `id.ac.binus.bounty`. Testing uses the separate
`id.ac.binus.bounty.testing` debug installation, selected with
`-PbountyIsolatedTest=true`. No canonical accounts, challenges, proofs, or balances
are cleared. Namespace and normal/release application ID remain unchanged.

## Reproduce

Set `JAVA_HOME` to a JDK compatible with the installed Android Gradle plugin.
Connect one device/emulator, then run from the repository:

```powershell
./scripts/Test-Android.ps1
./gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug
```

Use `-Adb <path>` for a custom SDK location and `-Serial <device>` if multiple
devices are connected. The script builds isolated APKs, removes stale isolated
installations, clears only the isolated app data to prevent restored backup
fixtures, verifies fresh Login, runs the feature checks, then runs the
continuous creator/hunter flow. It checks the final hunter session, COMPLETED
challenge, and balance after actual process restarts. Its `finally` block restores
animation settings and removes only the fixed isolated app/test packages.

After a failed flow, `-MainFlowOnly` repeats fresh install, the continuous flow
and restart checks while retaining the previous regression report. It does not
replace the full feature campaign.

The final Gradle command rebuilds canonical APKs for normal use. Reports from the
script are under `app/build/reports/m12/`: `regression.txt`, `main-flow.txt`, and
`restart.txt`. Generated reports and APKs are not committed to Git.

## Results

- Feature/regression batch: `OK (11 tests)`.
- Continuous creator/hunter flow: `OK (1 test)` in the final isolated runner.
- Fresh install: Login shown after resetting only the disposable installation.
- Process restart: two real force-stop/relaunch checks preserve the hunter,
  COMPLETED challenge and Rp70.000 balance without duplicate credit.
- Cleanup: isolated app/test packages removed and animation settings restored.
- Canonical assembleDebug, assembleDebugAndroidTest and lintDebug: pass, with
  0 errors and four existing lint warnings. The normal APK was installed without
  clearing original accounts/data.

These results combine the successful feature batch with the final focused
flow/restart campaign. The first full campaign was interrupted by the canonical
app's emulator renderer ANR described below; it was not an uninterrupted 12-test
pass. The final focused command was:

```powershell
./scripts/Test-Android.ps1 -Serial emulator-5554 -MainFlowOnly
```

## Scenario coverage

| M12 scenario | Evidence |
|---|---|
| Fresh install | Runner removes the isolated install, installs the APK, and waits for Login. |
| Register | MainFlowTest creates creator and hunter through the public forms; MockAuthenticationTest checks validation and duplicate email. |
| Login | Both accounts log in through the public form; auth checks invalid/unknown accounts. |
| Logout | Account switching uses Profile logout and verifies the session is cleared. |
| View challenge | MainFlowTest opens the published feed item; ChallengeFeedTest covers fields, refresh, recreation, empty and missing records. |
| Create challenge | Public Create form publishes an OPEN challenge with Rp20.000 reward; CreateChallengeTest covers persisted fields and single inserts. |
| Accept challenge | Hunter confirms Accept; participant and ACCEPTED status persist. AcceptChallengeTest also checks cancellation, rotation and duplicate guards. |
| Submit proof | Native DocumentsUI selects a test-owned PNG; UI submission stores a PENDING proof and SUBMITTED challenge. |
| Reject proof | Creator uses Reject; proof becomes REJECTED and challenge returns to ACCEPTED without reward. |
| Resubmit proof | Same hunter selects the photo again and submits a new description; latest proof has a newer ID and PENDING status. |
| Approve proof | Creator checks the latest description and confirms Approve; latest proof becomes APPROVED and challenge COMPLETED. |
| Balance update | Creator stays at Rp50.000; hunter changes from Rp50.000 to Rp70.000, with a saved receipt. MockRewardTest covers interrupted recovery and concurrent settlement. |
| API success | ExternalApiTest uses the real Retrofit path/query/converter with deterministic successful HTTP fixtures and rendered profiles. |
| API failure | ExternalApiTest covers HTTP 503, offline I/O, invalid/empty/API-error responses, local fallback and retry recovery without altering the account. |
| App restart | Runner force-stops and relaunches the actual app process twice; the paid hunter and Rp70.000 balance remain, without duplicate credit. |
| Invalid form input | MainFlowTest rejects empty Create; dedicated auth/Create/proof tests cover invalid fields, amounts, dates and missing photos/descriptions. |

## Findings and limits

The first continuous run reached resubmission but timed out after a coordinate
tap in DocumentsUI. The shared picker helper now prefers the native accessibility
click on the item or clickable parent, avoiding grid positions that change during
refresh; the original input-tap fallback remains. The focused corrected flow
passed. No production picker change was required.

The runner's first preflight passed but exposed PowerShell treating Android's
`-w` switch as an ambiguous common parameter. The native command wrapper now
passes positional arguments without advanced PowerShell parameter binding.

The full feature batch passed all 11 tests. A following flow attempt was blocked
by a system ANR dialog for the old canonical Bounty process, not the isolated
installation. Its trace showed the main thread waiting in
HardwareRenderer.setStopped and RenderThread waiting in EGL/qemu_pipe graphics
calls. This is consistent with an emulator graphics stall; the trace did not
show a Room or business-logic wait. The stalled app process was stopped without
clearing data or restarting the emulator, and the focused native flow then passed.

The runner now brings the launcher forward between instrumentation batches,
and a timed-out UI check reports the active package for diagnosis. It does not
suppress ANRs. This campaign encountered a long-running API 37 emulator graphics
stall, so it does not establish that all supported devices or long sessions are
free of renderer problems. No production rendering workaround was added.

API fixtures make success/failure checks repeatable without relying on the public
service's availability. The live public API was previously verified in M10;
these fixture checks do not promise current public-service or avatar uptime.
Deleting or moving a proof's source photo can still invalidate its local URI;
the existing photo error/retry behavior is covered. This remains a local mock
application with no production authentication or real payments.

M13 packaging, icon changes, and presentation data are outside this milestone.

## M13 APK delivery verification (2026-10-08)

The final package is `build/Bounty.apk`, 18,440,420 bytes, SHA256
`E3F76E566A68295F3422001A4172A3F817C35947A86530838B51C4B839DB6B5A`.
SDK apksigner verifies its v2 signature. aapt2 confirms Bounty, version 1.0,
canonical application ID and API 29 minimum. It is intentionally a debug-signed,
debuggable coursework build. It contains no instrumentation/temporary QA class.
`Build-Apk.ps1` rebuilds this variant, runs lint and prints its checksum.

The final icon/resource change passed `Test-Android.ps1 -MainFlowOnly`: fresh
Login, one continuous public-UI flow test (160.913 seconds), and two real process
restarts preserving the paid hunter, COMPLETED record and Rp70.000 balance.
The earlier M12 feature batch was not rerun for this icon/packaging-only change.
The canonical APK was rebuilt afterwards; assembleDebug/lintDebug pass with
0 errors and the same four warnings. `adb install -r build/Bounty.apk` succeeded
on Pixel_7/API 37. Pulling the installed base APK and comparing SHA256 confirmed
it matches the delivered file. Home shows the original Insan and Rp50.000.
Launcher visual QA confirms the orange trophy and Bounty label.

Preparation used public registration/Create UI to add Demo Hunter and an OPEN
Insan-owned presentation challenge; both accounts retain Rp50.000. Home was
returned to Insan. No existing data was reset; temporary test packages were
removed and animations restored. Screenshots are local build artifacts under
`build/m13/`. Demo instructions are in DEMO.md. Existing device, external API,
photo URI and long-session renderer limitations above still apply.
