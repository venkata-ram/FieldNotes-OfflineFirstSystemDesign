# Offline First System Design Android Demo

Educational Android demo app for learning offline-first system design.

The app is a small Field Notes tool built with Kotlin, Jetpack Compose, Room, Flow, WorkManager, Hilt, and a fake remote API. It is intentionally implemented through micro milestones so each architectural idea can be reviewed in Git history and in `docs/learning/`.

## What The App Demonstrates

- Local database as the source of truth.
- Hilt dependency injection instead of a manual app container.
- Local writes before network sync.
- Visible sync status.
- Pending create, update, and delete operations.
- Manual sync.
- Background sync with WorkManager.
- Auto sync that queues existing pending notes when enabled.
- Tombstones for offline deletes.
- Fake remote API.
- Conflict detection.
- Conflict resolution with keep local, use remote, and merge both.
- Connectivity awareness.
- Dedicated create/edit note screen with a notes list and floating action button.
- Delete confirmation before removing a note.
- Debug sync log.
- Serialized sync with a Kotlin `Mutex` so manual sync and background sync do not race.
- Fast unit tests for offline-first behavior.

## Architecture

```mermaid
flowchart TB
    User["User"]

    subgraph UI_LAYER["UI layer"]
        Screen["Compose screens<br/>Notes, Remote, Sync, Learn"]
        VM["NotesViewModel<br/>UI state + events"]
        Connectivity["ConnectivityObserver<br/>online/offline hint"]
    end

    subgraph DOMAIN_LAYER["Domain boundary"]
        Repo["NotesRepository<br/>offline-first API"]
        Lock["sync Mutex<br/>one sync at a time"]
    end

    subgraph DATA_LAYER["Data layer"]
        Room["Room database<br/>local source of truth"]
        Api["FakeNotesApi<br/>remote demo source"]
    end

    subgraph BACKGROUND_LAYER["Background sync"]
        Scheduler["SyncScheduler"]
        Work["WorkManager<br/>one-time + network constraint"]
        Worker["NotesSyncWorker"]
    end

    subgraph DI_LAYER["Dependency injection"]
        Hilt["Hilt modules<br/>create and wire dependencies"]
    end

    User --> Screen
    Connectivity -->|network status| Screen
    Screen -->|NotesUiEvent| VM
    VM -->|save/edit/delete/resolve/sync| Repo
    Repo --> Lock
    Lock --> Repo
    Repo -->|read/write local state| Room
    Repo -->|push/pull sync| Api
    Room -->|Flow emits notes| Repo
    Repo -->|Flow emits state| VM
    VM -->|NotesUiState| Screen

    VM -->|auto sync enabled| Scheduler
    Scheduler --> Work
    Work -->|runs when connected| Worker
    Worker --> Repo

    Hilt -.-> VM
    Hilt -.-> Repo
    Hilt -.-> Scheduler
    Hilt -.-> Worker
    Hilt -.-> Connectivity
```

The key rule is simple:

The UI observes local state. Sync updates local state.

Manual sync and WorkManager both call `NotesRepository.syncNow()`. The repository protects that sync loop with a `Mutex`, which means only one sync can push/pull at a time.

## Demo Flow

1. Open `Notes` and tap `Create local note` or the `+` floating action button.
2. Save the note from the dedicated editor screen.
3. See it marked as `Pending create`.
4. Tap `Sync pending changes`.
5. See it become `Synced`.
6. Edit the note.
7. See it become `Pending update`.
8. Open `Remote` and edit the fake server copy.
9. Open `Notes`, edit the same local note differently, and save.
10. Open `Sync`, tap `Sync pending changes`, and review conflict behavior.
11. Choose `Merge both` to combine local and remote text, or choose `Keep local` / `Use remote` to compare strategies.
12. Enable auto sync and create/edit a pending note. WorkManager queues one-time sync work that waits for network if needed.
13. Delete a synced note, confirm deletion, and observe tombstone-driven sync behavior.

## Auto Sync Behavior

Auto sync is not a timer in this project.

- The app schedules one-time WorkManager work.
- The work has a `NetworkType.CONNECTED` constraint.
- If network is unavailable, WorkManager waits.
- When network is available, Android can run the queued sync.
- If auto sync is turned on while syncable pending notes already exist, the app queues work immediately.
- Conflict notes are not auto-synced until the user resolves the conflict.

## Milestone Docs

Learning notes live in `docs/learning/`.

Each milestone includes:

- Goal.
- What changed.
- Why it matters.
- Possible solutions.
- Advantages and disadvantages.
- Simple diagram.
- Android best practices.
- Verification.
- Junior, mid-level, senior, and architect interview questions.

## Verification

Run:

```bash
./gradlew testDebugUnitTest
```

## Git History

Each milestone is committed separately:

```text
m1  document roadmap and agent requirements
m2  build baseline field notes shell
m3  introduce notes ui state and viewmodel
m4  persist notes with room source of truth
m5  track local write sync status
m6  add fake remote notes api
m7  add manual notes sync
m8  schedule background sync with workmanager
m9  support deletes with tombstones
m10 detect note sync conflicts
m11 add conflict resolution controls
m12 show connectivity awareness
m13 add offline first behavior tests
m14 add sync debug log
m15 final polish and architecture review
post-m15 polish: dedicated editor flow, merge-both conflict resolution, auto-sync pending queue, delete confirmation
m16 replace manual dependency container with Hilt
```
