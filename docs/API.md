# Bounty API Contract

## API boundaries

Bounty uses two network services:

1. **Supabase Auth and PostgREST** provide app sign-in and cloud CRUD/RPC over
   the Supabase PostgreSQL database. Supabase is the app backend, but does not
   count as the required independent external API for the coursework rubric.
2. **Random User API** is the qualifying public external API. It provides
   read-only demo names, email addresses, and avatars in Profile.

The client uses the Supabase project URL and publishable/anon key. Every
authenticated request also uses the signed-in user's access token. Never put a
service-role key in the Android app or Git.

## Supabase request headers

```http
apikey: <SUPABASE_PUBLISHABLE_KEY>
Authorization: Bearer <access_token>
Content-Type: application/json
```

Use the anon key for pre-authentication sign-in and sign-up requests; use the
access token returned by Supabase Auth for protected resources. RLS policies in
[`supabase/migrations/20261013000000_bounty_cloud.sql`](../supabase/migrations/20261013000000_bounty_cloud.sql)
authorize reads and writes.

## Authentication

Base path: `/auth/v1`

| Operation | Request | Purpose |
|---|---|---|
| Register | `POST /signup` with email/password and `data.name` | Create Auth user; DB trigger creates profile |
| Login | `POST /token?grant_type=password` with email/password | Return access and refresh tokens |
| Current user | `GET /user` | Restore/check signed-in identity |
| Logout | `POST /logout` | End current session |

Passwords are sent only to Supabase Auth over HTTPS. Save refreshable session
tokens in app-private storage; never store the password. Registration must show
an email-confirmation state if confirmation is enabled in the Supabase project.

## Bounty data

Base path: `/rest/v1`. UUIDs identify users, challenges, and proof rows.

| Operation | Request | Contract |
|---|---|---|
| Feed | `GET /challenges?select=*&order=created_at.desc` | RLS-protected challenge list |
| My challenges | `GET /challenges?select=*&or=(creator_id.eq.<uid>,participant_id.eq.<uid>)&order=created_at.desc` | Challenges created or accepted by current user |
| Detail | `GET /challenges?id=eq.<challenge_uuid>&select=*` | One challenge |
| Create | `POST /challenges` with `Prefer: return=representation` | Insert `creator_id`, `creator_name`, title, description, reward, deadline, and `OPEN` status |
| Accept | `POST /rpc/accept_challenge` with `{"target_challenge":"<uuid>"}` | Atomically assign one hunter; returns boolean |
| Latest proof | `GET /proofs?challenge_id=eq.<uuid>&select=*&order=submitted_at.desc,id.desc&limit=1` | RLS permits challenge creator and hunter |
| Submit | `POST /rpc/submit_proof` with challenge UUID, description, and object path | Atomically insert proof and change challenge to `SUBMITTED`; returns proof UUID |
| Review | `POST /rpc/review_proof` with challenge UUID, proof UUID, and `approve` boolean | Atomically approve/reject, update status, and credit mock reward once |
| Profile | `GET /profiles?id=eq.<uid>&select=id,display_name,demo_balance` | Current user's profile only |

Challenge creation, acceptance, submission, and review must show progress while
requests run. Handle permission, conflict/stale-state, validation, network, and
server errors without losing form input. Refresh feed/detail after a successful
mutation. A false RPC result means the challenge/proof is no longer actionable.

## Proof photo storage

Bucket: `bounty-proof-photos` (private, maximum 5 MiB, JPEG/PNG/WebP).

1. Upload image bytes to `POST /storage/v1/object/bounty-proof-photos/<uid>/<challenge_uuid>/<unique-file>`, with the standard headers and the image `Content-Type`.
2. Send the object path to `submit_proof` only after upload succeeds.
3. Request a time-limited URL with `POST /storage/v1/object/sign/bounty-proof-photos/<object_path>` and `{"expiresIn":3600}` when displaying a private image.

Storage RLS restricts uploads to the assigned hunter and reads to challenge
members. If proof insertion fails after upload, retain the form for retry; the
unused object can be cleaned up later without changing challenge state.

## Qualifying public API: Random User

Base URL: `https://randomuser.me/`

```http
GET /api/?results=10
```

Use only `login.uuid`, `name.first`, `name.last`, `email`, and `picture.large`.
Show loading, retry, and local fallback states. These demo profiles do not
create Bounty accounts or modify cloud data.

## Configuration

Set `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` in the ignored local
`local.properties` file. Never commit credentials or generated local config.
Keep the Android app's `INTERNET` permission enabled.
