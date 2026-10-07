USE PONYTAIL, CAVEMAN skills before executing anything or even talk
always push to github with detailed commit messages
if the task took longer than usual, divide the task and push to github first then push the 2nd one

# Bounty Agent Instructions

Before making any code changes, read these files:

- `docs/PRD.md`
- `docs/DESIGN_SYSTEM.md`
- `docs/ARCHITECTURE.md`
- `docs/DATABASE.md`
- `docs/API.md`
- `docs/TASKS.md`

These documents are the source of truth for this project.

## Rules

1. Do not invent features outside the PRD.
2. Follow the architecture defined in `docs/ARCHITECTURE.md`.
3. Follow the database schema defined in `docs/DATABASE.md`.
4. Follow the API contract and external API usage defined in `docs/API.md`.
5. Follow the UI rules in `docs/DESIGN_SYSTEM.md`.
6. Use `docs/TASKS.md` as the implementation order.
7. Work on one milestone at a time.
8. Do not skip milestones unless explicitly requested.
9. Keep the project simple and appropriate for an Android coursework MVP.
10. Do not add real payment processing. Reward/payment must remain mock.
11. Do not replace Java/XML with another stack unless explicitly requested.
12. Before implementing a milestone, inspect the existing code first.
13. Avoid unnecessary libraries or architecture complexity.
14. Keep changes scoped to the requested task.
15. After changes, verify that the project still builds.

## Current Stack

- Android Studio
- Java
- XML Layout
- Room
- SharedPreferences
- Retrofit
- Gson
- Glide

## Development Flow

Use this order:

`PRD → Architecture → Database → API → Design System → TASKS`

For implementation progress, always refer to `docs/TASKS.md`.

When a milestone is completed:
- verify the implementation
- report which files were changed
- report any remaining issues
- do not automatically start the next milestone unless requested