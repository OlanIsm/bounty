# Bounty — Database Design

## 1. Database

Bounty menggunakan local database:

```text
Room Database
```

Database:

```text
bounty_database
```

---

## 2. Entity Relationship

```text
User
 │
 │ creates
 ▼
Challenge
 │
 │ accepted by
 ▼
Participant/User
 │
 │ submits
 ▼
Proof
```

Untuk MVP, User dari external API tidak wajib disimpan sebagai entity Room penuh.

---

## 3. Challenge Entity

Table:

```text
challenges
```

Fields:

| Field | Type | Description |
|---|---|---|
| id | int | Primary key |
| creatorId | String | ID creator |
| creatorName | String | Creator name |
| creatorAvatar | String | Avatar URL |
| title | String | Challenge title |
| description | String | Challenge description |
| reward | double | Mock reward |
| deadline | String | Deadline |
| status | String | OPEN / ACCEPTED / SUBMITTED / COMPLETED |
| participantId | String? | Hunter ID |
| participantName | String? | Hunter name |
| createdAt | long | Timestamp |

---

## 4. Proof Entity

Table:

```text
proofs
```

Fields:

| Field | Type | Description |
|---|---|---|
| id | int | Primary key |
| challengeId | int | Related challenge |
| hunterId | String | Hunter ID |
| description | String | Proof description |
| imageUri | String | Local image URI |
| status | String | PENDING / APPROVED / REJECTED |
| submittedAt | long | Timestamp |

---

## 5. Mock User Model

User model:

| Field | Type |
|---|---|
| id | String |
| name | String |
| email | String |
| avatarUrl | String |
| demoBalance | double |

Current user dapat disimpan dengan SharedPreferences.

Example:

```text
user_id = local_user_001
user_name = Insan
demo_balance = 50000
logged_in = true
```

---

## 6. Challenge Status

Constants:

```java
OPEN
ACCEPTED
SUBMITTED
COMPLETED
```

Optional:

```java
EXPIRED
```

---

## 7. Proof Status

```java
PENDING
APPROVED
REJECTED
```

---

## 8. Main DAO Operations

### ChallengeDao

```text
insertChallenge()
getAllChallenges()
getChallengeById()
getChallengesByCreator()
getChallengesByParticipant()
updateChallenge()
deleteChallenge()
```

### ProofDao

```text
insertProof()
getProofByChallengeId()
updateProof()
```

---

## 9. Example Initial Mock Data

Challenge 1:

```text
Title:
30 Push-ups at the Park

Reward:
Rp20.000

Status:
OPEN
```

Challenge 2:

```text
Title:
Sing a Song in Public

Reward:
Rp35.000

Status:
OPEN
```

Challenge 3:

```text
Title:
Finish 5km Walk

Reward:
Rp50.000

Status:
OPEN
```

---

## 10. Reward Update Logic

Jika proof approved:

```text
challenge.status = COMPLETED

hunter.demoBalance =
hunter.demoBalance + challenge.reward
```

Reward hanya data simulasi dan tidak merepresentasikan transaksi nyata.
