# Bounty — API Documentation

## 1. API Strategy

Bounty tidak membutuhkan backend API production untuk MVP.

Challenge, proof, dan mock balance disimpan secara lokal.

External API digunakan untuk memenuhi requirement consume API dan memberikan data profile/avatar demo.

---

# External API

## 2. Random User API

Base URL:

```text
https://randomuser.me/
```

Endpoint:

```http
GET /api/?results=10
```

Full request:

```text
https://randomuser.me/api/?results=10
```

---

## 3. Purpose

Digunakan untuk mendapatkan data demo:

- User name
- Email
- Profile photo

Data dapat digunakan pada:
- Creator avatar
- Demo users
- Profile card
- Challenge feed

---

## 4. Request

```http
GET https://randomuser.me/api/?results=10
```

Tidak membutuhkan request body.

---

## 5. Example Response

```json
{
  "results": [
    {
      "name": {
        "first": "John",
        "last": "Doe"
      },
      "email": "john@example.com",
      "picture": {
        "large": "https://example.com/avatar.jpg"
      },
      "login": {
        "uuid": "example-id"
      }
    }
  ]
}
```

---

## 6. Fields Used by Bounty

```text
login.uuid
name.first
name.last
email
picture.large
```

Mapped menjadi:

```java
User {
    id
    name
    email
    avatarUrl
}
```

---

## 7. Retrofit Interface

```java
public interface RandomUserApi {

    @GET("api/")
    Call<RandomUserResponse> getUsers(
        @Query("results") int results
    );
}
```

---

## 8. Retrofit Client

Base URL:

```java
https://randomuser.me/
```

Dependencies:

```text
Retrofit
Gson Converter
```

---

## 9. Network States

UI harus menangani:

```text
LOADING
SUCCESS
ERROR
```

### Loading
Tampilkan progress indicator.

### Success
Tampilkan data user/avatar.

### Error
Gunakan fallback mock data.

Contoh:

```text
Name:
Bounty User

Avatar:
Local placeholder
```

Aplikasi tidak boleh bergantung penuh pada external API untuk main flow.

---

# Internal App Operations

Bagian berikut bukan HTTP endpoint. Ini adalah operasi local database yang memiliki fungsi seperti service API di dalam aplikasi.

## 10. Challenge Operations

### Get Challenges

```text
getAllChallenges()
```

Output:

```text
List<Challenge>
```

### Create Challenge

```text
insertChallenge(challenge)
```

### Accept Challenge

Update:

```text
status = ACCEPTED
participantId = currentUserId
participantName = currentUserName
```

### Submit Proof

```text
insertProof(proof)
```

Kemudian:

```text
challenge.status = SUBMITTED
```

### Approve Proof

```text
proof.status = APPROVED
challenge.status = COMPLETED
```

Kemudian tambahkan mock reward ke demo balance hunter.

### Reject Proof

```text
proof.status = REJECTED
challenge.status = ACCEPTED
```

---

## 11. Future Backend API

Jika project nanti dikembangkan dengan backend, contract dapat menjadi:

```http
POST   /auth/login
POST   /auth/register

GET    /challenges
GET    /challenges/:id
POST   /challenges
POST   /challenges/:id/accept

POST   /challenges/:id/proof
POST   /challenges/:id/approve
POST   /challenges/:id/reject

GET    /users/:id
GET    /users/:id/challenges
```

Endpoint tersebut **tidak termasuk MVP coursework saat ini**.
