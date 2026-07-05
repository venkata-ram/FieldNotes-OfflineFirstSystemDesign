# Agent Instructions

This file remembers the project requirements for future work on the Offline First System Design Android demo app.

## Project Goal

Build an educational Android demo app that teaches offline-first system design using modern Android best practices.

The app should be implemented in small, reviewable micro milestones so the user can inspect each step, understand the tradeoffs, and learn how a production-quality offline-first app is designed.

## User Requirements

- Build a demo Android app for offline-first system design.
- Use Kotlin and the existing Android project structure.
- Prefer modern Android best practices.
- Implement the project in micro milestones.
- Stop after each milestone so the user can review before continuing.
- Create a Git commit for each micro milestone when the repository is initialized.
- For each milestone, create a learning note at `docs/learning/mN-milestone-name.md`.
- Explain possible solutions in simple English.
- Include advantages and disadvantages for each meaningful approach.
- Include simple diagrams in the learning docs.
- Include interview questions for junior, mid-level, senior, and architect levels.
- Keep the project educational, explicit, and easy to follow.

## Current First Task

Historical first task was to create only:

- `agent.md`
- `roadmap.md`

That task is complete. Current work should maintain the completed offline-first demo and keep documentation aligned with implementation changes.

Recent implementation state to remember:

- Main UI is a multi-screen Compose demo: Notes, Remote, Sync, Learn, plus a hidden dedicated editor screen.
- Notes screen is list-first with `Create local note` and a `+` floating action button.
- Create/edit opens the editor screen; after save or cancel the app returns to Notes.
- Long note titles and bodies should adapt naturally instead of using fixed-height cards.
- Deleting a note asks for confirmation.
- Remote screen lets the demo edit fake server notes directly.
- Conflict resolution supports keep local, use remote, and merge both. Merge both is the preferred demo path.
- Auto sync uses WorkManager one-time work with a network constraint. It is not periodic/time based.
- Turning on auto sync queues existing syncable pending notes. Conflict notes wait until the user resolves them.
- Repository sync uses a Kotlin `Mutex` so manual sync and background sync do not run the push/pull loop at the same time.

## Collaboration Style

- Explain decisions clearly and simply.
- Prefer small, safe changes over large rewrites.
- Keep each milestone focused on one learning objective.
- Before writing code for a milestone, briefly describe what will be changed.
- After implementing a milestone, run relevant verification when possible.
- Report any blockers honestly, especially Git, Gradle, Android SDK, or dependency issues.

## Technical Direction

Use this architecture direction unless the user asks to change it:

- UI: Jetpack Compose.
- Architecture: MVVM with unidirectional data flow.
- Local source of truth: Room database.
- Remote API: fake API first, then optionally real mock server.
- Repository pattern: UI reads from local database and sync code updates local database.
- Background sync: WorkManager.
- Dependency injection: Hilt.
- Networking: Retrofit or Ktor, decided in a milestone with tradeoff explanation.
- Serialization: Kotlinx Serialization or Moshi, decided with networking choice.
- Async/reactive data: Kotlin Coroutines and Flow.
- Testing: unit tests for domain/repository/sync logic, instrumentation tests where useful.

## Offline-First Principles To Demonstrate

- The app should remain useful without network access.
- The local database should be the main source of truth for the UI.
- Network calls should update local state instead of directly driving screens.
- User writes should be saved locally first, then synced later.
- Sync state should be visible to users.
- Conflicts should be explained and handled deliberately.
- Retry, backoff, idempotency, and error handling should be demonstrated.
- Advanced concepts such as `Mutex`, unique WorkManager work, network constraints, tombstones, and conflict metadata should be explained in simple English in docs and interview questions.
- The final result should teach how simple offline-first ideas scale into system design.

## Git Notes

The user wants one commit per micro milestone. If `.git` does not exist, do not silently initialize unless the user has agreed. Tell the user that commits are blocked until Git is initialized.

Suggested commit format:

```text
mN: short milestone name
```

Example:

```text
m1: document roadmap and agent requirements
```

## Learning Doc Template

Each `docs/learning/mN-milestone-name.md` should include:

- Goal
- What changed
- Why this matters for offline-first design
- Possible solutions
- Advantages and disadvantages
- Simple diagram
- Key Android best practices
- Testing or verification
- Junior interview questions
- Mid-level interview questions
- Senior interview questions
- Architect interview questions
