# Bounty API and Cloud Data Contract

## Integration boundaries

- **Firebase Authentication and Cloud Firestore** provide app sign-in and
  cloud application data. They are not the qualifying third-party API for the
  coursework requirement.
- **Random User API** is the qualifying public external API. It provides
  read-only demo names, email addresses, and profile photos.

Keep account, challenge, proof, and reward data in Firebase. Do not use Random
User profiles as Bounty accounts or modify cloud data from that API.

## Firebase Authentication

Use Firebase Authentication's Email/Password provider. Registration requires
name, email, and password; login uses email and password. Firebase SDK retains
the signed-in session. Never store the password in app preferences. Handle
invalid credentials, duplicate email, weak password, network failure, and
signed-out states in the forms.

Create `users/{uid}` after successful registration using the Firebase Auth UID.
The profile stores display name, email, initial mock balance, and creation time.

## Firebase data operations

The repository layer exposes these app operations through Firestore. Auth and
Firestore rules are the trust boundary; UI code does not write arbitrary
documents directly.

| Operation | Read/write | Required behavior |
|---|---|---|
| Challenge feed | Read `challenges` | Newest first; handle empty, loading, and retry |
| My Challenges | Read `challenges` where creator or participant is current UID | Refresh after mutations |
| Challenge detail | Read one challenge and latest proof | Show missing/unauthorized state |
| Create challenge | Create `OPEN` challenge with current UID/name | Validate fields before write |
| Accept challenge | Transaction updates only `OPEN` challenge | Reject own challenge and stale/competing accepts |
| Submit proof | Upload photo, then transaction creates proof and changes challenge to `SUBMITTED` | Keep description/photo draft on failure |
| Review proof | Transaction updates latest proof and challenge | Approve creates one reward receipt; reject returns to `ACCEPTED` |
| Demo Balance | Read own profile and reward receipts | Initial Rp50.000 plus receipts; never client-write a balance |

Schema, Storage paths, and authorization behavior are documented in
[`DATABASE.md`](DATABASE.md). Firebase security rules belong in the repository
and must be deployed before the app writes cloud data.

## Qualifying public API: Random User

Base URL: `https://randomuser.me/`

```http
GET /api/?results=10
```

Use only `login.uuid`, `name.first`, `name.last`, `email`, and `picture.large`.
Show loading, retry, and local fallback states. API users remain read-only and
do not affect Bounty authentication or cloud records.

## Configuration

Place the Firebase Android app configuration at `app/google-services.json`.
The Google Services Gradle plugin generates the SDK resources from this file.
It contains app identifiers, not an Admin SDK credential; never put a service
account key in client code. Firebase Authentication Email/Password, Cloud
Firestore, and Storage must be enabled in the matching Firebase project.
