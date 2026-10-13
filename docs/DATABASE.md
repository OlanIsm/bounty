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

Each submitted proof is a separate document with `hunterId`, `description`,
`imagePath`, `status` (`PENDING`, `APPROVED`, or `REJECTED`), and `submittedAt`.
Only the challenge creator and assigned hunter can read proof documents.
Resubmission creates a new proof document after rejection.

### `users/{uid}/rewardReceipts/{challengeId}`

One receipt per completed challenge stores `challengeId`, `proofId`, `amount`,
and `creditedAt`. A receipt is created atomically with approval. Its document ID
prevents duplicate mock rewards. Balance is `initialDemoBalance + sum(receipts)`;
this is not payment processing or transferable money.

## Storage

Store proof photos in a private Firebase Storage bucket at
`proofs/{challengeId}/{hunterUid}/{uniqueFileName}`. Only the assigned hunter
can upload while the challenge is ACCEPTED. Only the challenge creator and
hunter can read a proof photo. Store the cloud object path in `imagePath`, not
the device's local content URI.

## Atomic operations and rules

- Accept: transaction changes one OPEN challenge to ACCEPTED and assigns the
  signed-in hunter; creator cannot accept their own challenge.
- Submit: upload the photo, then use a Firestore transaction to create a PENDING
  proof and set the challenge to SUBMITTED with `latestProofId`. If the database
  write fails, keep form data for retry; the uploaded orphan can be removed later.
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

Firebase Authentication is integrated, and the app implements reads/writes for
`users/{uid}`. The profile write has not been end-to-end verified with a real
account; no user or sample document was created during this implementation.
Challenge and proof operations still use the previous local Room schema, and
mock balances/reward receipts still use SharedPreferences. Firebase Storage and
cloud challenge transactions remain pending; the app has not yet uploaded local
challenge/proof data.
