# Bounty Database Design

## M14 cloud database

Bounty uses Cloud Firestore as its source of truth. Firebase Authentication
provides account identity. The Android app accesses cloud data with Firebase's
Android SDK. Firebase is the cloud database/backend and does not satisfy the
separate external API requirement; Random User API remains the qualifying API.

Firebase project setup uses `app/google-services.json` for the Android app.
It contains non-secret app identifiers; do not add service account credentials
to the repository or client. Deploy rules and indexes from
`firebase/firestore.rules`, `firebase/storage.rules`, and
`firebase/firestore.indexes.json` with the Firebase CLI.

## Firestore collections

### `users/{uid}`

`uid` matches Firebase Authentication. Fields: `displayName`, `email`,
`initialDemoBalance` (50000), and `createdAt`. Current balance is derived from
the initial amount plus that user's reward receipt documents; clients cannot
write a balance field.

### `challenges/{challengeId}`

Fields: `creatorId`, `creatorName`, `title`, `description`, `rewardAmount`,
`deadline`, `status`, `participantId`, `participantName`, `latestProofId`, and
`createdAt`. `status` is `OPEN`, `ACCEPTED`, `SUBMITTED`, or `COMPLETED`.
Challenge cards are readable by signed-in users. Only the creator can create
an OPEN challenge as themselves.

### `challenges/{challengeId}/proofs/{proofId}`

Each submitted proof is a separate document with `challengeId`, `hunterId`,
`description`, `imageData` (Firestore bytes), `status` (`PENDING`, `APPROVED`,
or `REJECTED`), and `submittedAt`. The app compresses photos and limits bytes to
512 KiB, below Firestore's 1 MiB document limit. Only the challenge creator and
assigned hunter can read proofs. Resubmission creates a new proof document.

### `users/{uid}/rewardReceipts/{challengeId}`

One receipt per completed challenge stores `challengeId`, `proofId`, `amount`,
and `creditedAt`. A receipt is created atomically with approval. Its document ID
prevents duplicate mock rewards. Balance is `initialDemoBalance + sum(receipts)`;
this is not payment processing or transferable money.

## Proof image storage

Firebase Storage is not used. Storage access requires Blaze, while this project
must remain on Firebase Spark. Compressed image bytes live in each proof
document's `imageData` field. Firestore rules enforce the 512 KiB app limit and
member-only reads. Keep the size cap to leave room under Firestore's 1 MiB
document limit.

## Atomic operations and rules

- Accept: transaction changes one OPEN challenge to ACCEPTED and assigns the
  signed-in hunter; creator cannot accept their own challenge.
- Submit: use one Firestore transaction to create a PENDING proof containing
  compressed image bytes and set the challenge to SUBMITTED with `latestProofId`.
- Review: transaction checks creator, SUBMITTED state, and latest pending proof.
  Approve marks proof/challenge complete and creates one reward receipt; reject
  marks proof rejected and returns challenge to ACCEPTED.
- Security rules validate owner, state transition, and related atomic writes.
  Never use the Firebase Admin/service account key in the Android app.

## Previous local schema

M0-M13 used Room/SQLite for challenges and proofs and SharedPreferences for mock
accounts, sessions, balances, and reward receipts. M14 replaces that active data
path with Firebase. Existing local records are not uploaded automatically;
users must register in Firebase and create cloud challenges after setup.

## Current M14 implementation status

Firebase Authentication and Firestore operations are implemented. An Auth and
Firestore emulator instrumentation flow verifies create, accept, proof submit,
reject/resubmit, approve, completed status, receipt uniqueness, and balance.
No production user or sample document was created during testing. Existing local
Room data is not migrated automatically; users create new cloud data after
signing in. Firebase Storage is intentionally unused on the Spark plan.
