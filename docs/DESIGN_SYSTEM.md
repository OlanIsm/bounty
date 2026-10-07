# Bounty — Design System

## 1. Design Direction

Bounty memiliki tampilan:
- Fun
- Social
- Friendly
- Energetic
- Tidak terlalu formal
- Tetap mudah dibaca

Visual utama dapat mengambil inspirasi dari konsep:
- Mission
- Challenge
- Reward
- Bounty card

---

## 2. Color Palette

### Primary

```text
Orange
#FF7A00
```

Digunakan untuk:
- Primary button
- Reward highlight
- Active navigation

### Secondary

```text
Yellow
#FFC83D
```

Digunakan untuk:
- Prize
- Badge
- Highlight

### Background

```text
#F7F7F7
```

### Surface

```text
#FFFFFF
```

### Main Text

```text
#202020
```

### Secondary Text

```text
#777777
```

### Success

```text
#2EAD69
```

### Error

```text
#E5484D
```

---

## 3. Typography

Gunakan font Android yang sederhana.

Recommended:

```text
Roboto
```

Hierarchy:

```text
Heading 1   28sp Bold
Heading 2   22sp Bold
Heading 3   18sp Medium
Body        16sp Regular
Caption     13sp Regular
Button      16sp Medium
```

---

## 4. Spacing

Gunakan sistem kelipatan 4dp.

```text
4dp
8dp
12dp
16dp
24dp
32dp
```

Default horizontal screen padding:

```text
16dp
```

---

## 5. Corner Radius

```text
Small card   8dp
Button       12dp
Main card    16dp
Modal        20dp
```

---

## 6. Main Components

### Challenge Card

Isi:

```text
[Avatar] Creator Name

Challenge Title
Short Description

Reward: Rp20.000
Deadline: 10 Oct

[View Challenge]
```

---

### Primary Button

Contoh:

```text
Accept Challenge
Create Challenge
Submit Proof
Approve
```

Style:
- Orange background
- White text
- 12dp radius
- Height sekitar 48–52dp

---

### Secondary Button

Contoh:

```text
Cancel
Reject
Back
```

Style:
- White/light background
- Border
- Dark text

---

## 7. Status Badge

### OPEN
Orange / Yellow

### ACCEPTED
Blue

### SUBMITTED
Purple

### COMPLETED
Green

### EXPIRED
Gray

---

## 8. Reward Component

Reward harus menjadi informasi yang mudah terlihat.

Contoh:

```text
🏆 Rp20.000
```

Karena payment masih mock, pada balance gunakan label:

```text
Demo Balance
```

---

## 9. Home Screen

Struktur:

```text
Header
Hello, [User]

Demo Balance Card

Available Challenges

[Challenge Card]
[Challenge Card]
[Challenge Card]

Bottom Navigation
```

---

## 10. Create Challenge Screen

Form:

```text
Title

Description

Reward

Deadline

Optional Image

[Publish Challenge]
```

Gunakan form sederhana agar mudah dibuat dengan Android XML.

---

## 11. Challenge Detail Screen

Struktur:

```text
Creator

Challenge Title

Reward

Description

Deadline

Status

[Accept Challenge]
```

---

## 12. Submit Proof Screen

```text
Challenge Title

Selected Photo

Proof Description

[Choose Photo]

[Submit Proof]
```

---

## 13. Review Proof Screen

```text
Hunter

Proof Image

Proof Description

[Reject] [Approve]
```

---

## 14. Bottom Navigation

Menu:

```text
Home
My Challenges
Create
Profile
```

Gunakan icon Android Material sederhana.

---

## 15. UI Principle

Prioritas:

```text
Clarity > Decoration
```

Jangan menggunakan terlalu banyak:
- Gradient
- Animation
- Custom assets
- Complex layout

Karena tujuan utama project adalah aplikasi Android yang functional dan mudah didemonstrasikan.


## 16. Native implementation notes (M11)

Primary buttons and reward labels use dark text on #FF7A00 for readable contrast;
white text on this orange does not meet 4.5:1. Status badges retain their text
labels alongside semantic colors: OPEN orange/yellow, ACCEPTED blue, SUBMITTED
and PENDING purple, COMPLETED and APPROVED green, REJECTED red, and unknown or
EXPIRED neutral. Foreground/background pairs have separate light/dark resources
and all meet 4.5:1. Status is never communicated through color alone.

Use shared native typography, 16dp screen/card padding, spacing in multiples of
4dp, 16dp card corners, 12dp button corners, and 20dp Material dialog corners.
Home's welcome and demo balance scroll with the challenge list so landscape
and larger fonts leave the feed reachable. Existing forms/detail/review/Profile
remain scrollable. Asynchronous empty/error messages use polite live regions;
loading indicators retain descriptive accessibility labels.

Accept and Approve require a native confirmation with Batal and the action
label. Accept identifies the challenge and explains when demo reward is granted.
Approve identifies the challenge, recipient, and demo amount and explicitly
states that it is not real money. Cancelling leaves local state unchanged;
pending dialogs restore across activity recreation and recheck permissions and
current Room status when confirmed.
