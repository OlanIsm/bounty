# Bounty Application Architecture

## M14 implementation

The Android app uses Kotlin and XML. Firebase Authentication owns account
identity. Cloud Firestore stores profiles, challenges, proofs, and mock reward
receipts. Random User API remains the qualifying external API.

```text
Kotlin Activities + XML
       |                   |
       v                   v
FirebaseAccountRepository  FirebaseBountyRepository
       |                   |
Firebase Auth              Cloud Firestore
                           (users/challenges/proofs/receipts)

Retrofit + Gson -> Random User API -> demo profiles
Glide -> remote/local image rendering
```

- `activities`: screens, input validation, navigation, and UI state.
- `data`: Firebase Auth/profile and Firestore Bounty operations.
- `models`: Kotlin data models shared by UI and repositories.
- `network`: read-only Random User API integration.
- `adapters`: challenge list presentation.
- `utils`: session cache and shared UI helpers. Passwords are never cached.
- `database`: legacy Room schema retained, but not used for active M14 Bounty
  data flows.

Firestore transactions enforce single-hunter acceptance, proof state changes,
and atomic reward receipt creation. Firestore Security Rules enforce the same
ownership and state constraints at the backend. Reward balance is initial demo
balance plus receipts; no real payment occurs.

Proof images are compressed on-device and stored as Firestore bytes (maximum
512 KiB per image). Firebase Storage is intentionally not used because it
requires the Blaze plan for this project. Firestore's 1 MiB document limit
leaves space for proof metadata.

## API boundaries

Firebase SDKs are backend services, not the qualifying external API. Random
User API supplies display-only demo profiles; it does not authenticate users or
write Bounty data. Each integration has a separate repository/client boundary.

## Local development tests

Instrumentation tests can opt into Firebase Auth and Firestore emulators using
`-PbountyFirebaseEmulator=true`. Emulator endpoints are configured only for
debug test builds; production Firebase configuration is not redirected.
