# Bounty Database Design

## M14 cloud database

Bounty uses Supabase PostgreSQL as its source of truth. Supabase Auth identifies
app users; the Android app reads and writes application data through Supabase's
PostgREST API. Random User API remains a separate external API used for demo
profiles. The Supabase publishable/anon key is a client key, not a service-role
secret; Row Level Security (RLS) and database functions enforce access.

The migration is [`supabase/migrations/20261013000000_bounty_cloud.sql`](../supabase/migrations/20261013000000_bounty_cloud.sql).
Run it once in the Supabase SQL Editor before using the cloud-backed app.
Credentials belong in the ignored `local.properties`, never in Git.

## Tables

### `profiles`

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key and Supabase Auth user ID |
| `display_name` | text | Name shown in the app |
| `demo_balance` | bigint | Local-style mock balance, starts at Rp50.000 |
| `created_at` | timestamptz | Account creation time |

An Auth trigger creates the profile. Users can read their own profile and
change their own display name. Only the review database function can change
`demo_balance`.

### `challenges`

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Challenge ID |
| `creator_id` | UUID | Creator profile/Auth ID |
| `creator_name` | text | Name snapshot used in the feed |
| `title` | text | Required, maximum 100 characters |
| `description` | text | Required, maximum 2000 characters |
| `reward_amount` | bigint | Positive mock reward in whole rupiah |
| `deadline` | date | Challenge deadline |
| `status` | text | `OPEN`, `ACCEPTED`, `SUBMITTED`, or `COMPLETED` |
| `participant_id` | UUID nullable | Accepted hunter ID |
| `participant_name` | text nullable | Hunter name snapshot |
| `created_at` | timestamptz | Creation time |

Authenticated users can browse the feed and create challenges as themselves.
Accept, submit, and review operations use database functions, not unrestricted
client updates.

### `proofs`

Each submission is a separate row, so a hunter can resubmit after rejection.
Fields: `id` (UUID), `challenge_id`, `hunter_id`, `description`, `image_path`,
`status` (`PENDING`, `APPROVED`, `REJECTED`), and `submitted_at`. Only the
challenge creator and assigned hunter can read its proofs. A private Storage
bucket stores proof photos; `image_path` stores the object path, not a local
device URI.

### `mock_reward_receipts`

Stores `challenge_id` (unique), `hunter_id`, `proof_id` (unique), reward
`amount`, and `credited_at`. Unique constraints plus the review function prevent
duplicate mock credits. This is not payment processing or transferable money.

## Atomic operations

- `accept_challenge(challenge_id)` assigns one hunter only while the challenge
  is `OPEN` and rejects the creator.
- `submit_proof(challenge_id, description, image_path)` changes `ACCEPTED` to
  `SUBMITTED` and inserts the proof in one transaction.
- `review_proof(challenge_id, proof_id, approve)` checks the creator and latest
  pending proof, updates proof and challenge together, then credits the hunter
  once when approved. Reject returns the challenge to `ACCEPTED`.

RLS protects profile, challenge, proof, reward, and private-photo access. The
app must use only the publishable/anon key. Never put a service-role key in the
Android app.

## Previous local schema

M0-M13 used Room/SQLite table models `Challenge` and `Proof`, plus
SharedPreferences for mock accounts, session, balance, and reward receipts.
That local schema is retained only as migration history and will be removed
from the active data path after M14 cloud integration is complete. Do not seed
cloud production data with local test accounts automatically.
