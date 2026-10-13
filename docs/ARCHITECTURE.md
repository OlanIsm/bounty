# Bounty Application Architecture

## M14 implementation status

The app source uses Kotlin and XML. Firebase Auth owns account identity and
profiles; challenge, proof, and reward flows remain local pending later M14 work.

## Current layers

```text
Kotlin Activities + XML
                 |
                 v
FirebaseAccountRepository.kt
       |                     |
       v                     v
Firebase Authentication   Cloud Firestore users/{uid}

Kotlin feature Activities + XML
       |                     |
       v                     v
Room: Challenge/Proof    Random User API (Retrofit/Gson)
       |
       v
SharedPreferences: local session cache and mock reward ledger
```

- `activities`: Kotlin Android screens and user interaction.
- `data`: Firebase account operations, isolated from the Auth form UI.
- `models`: Kotlin user, challenge, and proof models.
- `database`: Kotlin Room schema and local challenge/proof operations.
- `network`: Kotlin read-only Random User API integration, independent of Firebase.
- `utils`: Kotlin screen helpers and the current local-session/reward compatibility
  layer. No password is stored in SharedPreferences.

Registration creates an Auth account, updates its display name, and creates
`users/{uid}` with a server timestamp and initial mock balance. Login reads that
profile; if a profile is missing, it creates one from the authenticated user.
The app enters its authenticated screens only after profile synchronization
succeeds. Failed profile synchronization signs out the current Auth session so
the user can retry without entering a partially initialized app.

## Current data ownership

Firebase owns authentication and account profile documents. Challenge feed,
create/accept, proof submission/review, and mock reward remain in Room and
SharedPreferences. Proof photos remain local content URIs. Those operations are
not yet cloud-synchronized and do not persist across devices.

Random User API remains the qualifying public API and supplies display-only demo
profiles. It is not used for authentication or Bounty-owned records.

## M14 target architecture

```text
XML Activities (Kotlin)
          |
          v
Feature/data repositories
     |              |
     v              v
Firebase Auth   Firestore + Firebase Storage
          \
           +---- Retrofit: Random User API
```

The target keeps presentation, app data access, and network integration
separate. Firestore transactions and deployed security rules must enforce
challenge state changes and exactly-once mock reward receipts. Real payment is
out of scope. Remaining M14 work is tracked in [`TASKS.md`](TASKS.md).
