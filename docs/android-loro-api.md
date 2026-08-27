# Android Loro API — Minimal Native Surface

Scope: only what Android needs to mirror workspace + session docs. Do not expose the full Loro API.

## Decision

Rust wrapper crate holds the Loro `1.13.x` dependency (same as desktop/engine). Kotlin sees only a small `LoroDoc` interface; CRDT merge stays behind FFI.

## Document lifecycle

| Op | Rust usage | iOS usage | Android need |
|----|------------|-----------|--------------|
| `create()` | `LoroDoc::new()` in `crates/doc` | `LoroDoc()` in SessionStore/WorkspaceStore | `create(schemaVersion?)` — new empty doc with meta |
| `fromBytes(bytes, deviceId)` | `RegistryDoc::from(data, device)` / session doc import | `RegistryDoc.from(data, deviceId)` / `LoroDoc.from(bytes)` | Import snapshot or update; returns doc handle or typed error |
| `toBytes(): Data` / `exportSnapshot()` | `doc.export(ExportMode::Snapshot)` | `doc.toData()` via loro-swift | Persist doc + cursor atomically (C2 rule) |
| `exportUpdates(from: vv?)` | `ExportMode::Updates` for push bytes | `doc.exportUpdate()` for `ChatRoomClient.enqueue` | Produce push blobs (`maxPushBytes`) |
| `close()` | drop doc | `doc.close()` | Deterministic native dispose |

## Snapshot / update import & export

- `importBytes(bytes)` — must accept both snapshot and incremental update blobs (chat2 rows carry Loro updates). Round-trip convergence with `zeron-doc` bytes required.
- `exportSnapshot()` → bytes that Rust `import` accepts.
- `exportUpdate(since: frontier?)` → bytes for `push { batchId, bytes }`.
- `importCheckpoint(bytes, seq)` — checkpoint blob import (chat2 `applyCheckpoint`). Maps to `doc.import(checkpointBytes)` with cursor set to `seq`.
- Malformed bytes → typed error, not panic/process crash. Tests: truncated, empty, random bytes rejected.

## JSON / state reading

| Op | Source | Notes |
|----|--------|-------|
| `getDeepValue()` / typed read | `crates/doc/src/schema.rs` `getDeepValue`, `registry.rs` overlay reads | Android reads via `getDeepValue()` projected into domain models, not raw JSON in UI |
| `registry: overlayRows(kind)` | `RegistryDoc.overlayRows(kind)` | Used by `android-protocol-contract` WorkspaceStore projection (devices/spaces/chats/sessions) |
| `registry: overlayRow(kind, id)` / `rowExists` | same | Guard `update` never-creates |
| `session: entries + parts + commands` | `schema.rs` SessionMessageEntry / MessagePart / SessionCommandEntry | Transcript adapter joins `parts` + `continuationOf` |
| `getVersion()` / `oplogVV()` / `frontiers()` | `LoroDoc.oplog_vv()`, `state_frontiers()` | Checkpoint frontier containment check (`containsFrontier`) and cursor bookkeeping |
| `containsFrontier(payload)` | `chat_client.rs` `contains_frontier` | Client-side checkpoint precision: skip fetch if local frontier contains payload |

Reading must be deterministic and not re-parse on every recomposition (cache `revision`-gated).

## Change subscriptions

- `subscribeRoot(callback)` or `subscribe(callback)` — iOS `doc.subscribe` / `subscribeRoot` drives `project()` / transcript `revision` bump.
- Android: callback marshaled to `Dispatchers.Main` / `MainActor` equivalent; subscription guard dropped on `close()`.
- `subscribeLocalUpdate(cb)` — optional, for producing push bytes immediately after local write.
- Requirement: no leak when doc closed; no callback after dispose; coalesce streaming appends.

## Version / frontier / cursor

Separate from core ops:

| Op | Purpose |
|----|---------|
| `frontiers()` → opaque `Data` / `Vec<u8>` | Sent as `checkpointFrontier` in state, compared via `containsFrontier` |
| `oplogVV()` → version vector | `ExportMode::Updates{from: vv}` and debugging |
| `cursor: UInt64` (per chat) + `helloCursor: UInt64?` (registry) | Persisted alongside doc bytes atomically; cursor never skips gaps (`seq > cursor+1` triggers backfill) |
| `checkout(frontiers)` / `checkoutToLatest()` | Only if shallow-snapshot flow needed; otherwise `latest` only. Keep optional. |

Evidence links: `crates/doc/src/lib.rs`, `schema.rs`, `registry.rs`, `docs/research/loro-rust.md`, `apps/ios/Zeron/Sync/{SessionStore,WorkspaceStore}.swift`.

## What is NOT included

Full LoroText/Map/List mutators, UndoManager, EphemeralStore, `ensure_mergeable_*`, `redact`, `fork_at`, `StateOnly`/`ShallowSnapshot` — unless the checkpoint flow proves they are needed. Keep out of MVP.

## Kotlin wrapper rules (preview of Task 1.5)

- Single `LoroDoc` interface; UI never imports UniFFI names.
- `close()` explicit; finalizer is safety net only.
- Native calls off main thread (`Dispatchers.IO` / `withContext`), reads safe to observe on main via subscription.
- Cancellation propagates; import/export errors map to `AppError.Loro`.

## Verification

- No op included merely because it sounds useful — every row above has a caller in the iOS/Rust sources listed.
