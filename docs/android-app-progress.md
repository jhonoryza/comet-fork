# Android App Progress

Plan: [android-app-plan.md](./android-app-plan.md) — planning only, no Android implementation exists yet.

## Current phase

Phase 0 — project reconnaissance and contracts (Tasks 0.1–0.4). Contract docs drafted; checkpoint 0 ready for human review.

## Completed tasks

- [x] Task 0.1 — progress ledger (`docs/android-app-progress.md`)
- [x] Task 0.2 — protocol contract (`docs/android-protocol-contract.md`)
- [x] Task 0.3 — Loro API inventory (`docs/android-loro-api.md`)
- [x] Task 0.4 — fixture strategy (`docs/android-fixtures.md`)

## Blockers

- None blocking Phase 0. Unknowns explicitly listed in `android-protocol-contract.md` (chat2 HTTP route strings, WorkOS deep-link scheme/host, device-id shape, probe-ok seq).

## Verification results

- 2026-08-27: Contract docs cross-checked against `apps/ios/Zeron/Auth/AuthClient.swift`, `RegistryClient.swift`, `ChatRoomClient.swift`, `WorkspaceStore.swift`, `SessionStore.swift`, `docs/{registry-sync,chat2-sync}.md`, `crates/doc/src/{schema,registry}.rs`, `docs/research/loro-rust.md`. No implementation files changed — docs only. Each doc contains no unsupported technical claims beyond cited sources.

## Checkpoint 0

- [x] Tasks 0.1–0.4 complete
- [ ] Human review of contract docs before native FFI work (Phase 1)
- No Android implementation started before contract review.

## Next

- Phase 1 — Loro Android native binding spike (Tasks 1.1–1.5). Do not start full UI before Loro/FFI spike and fixture work pass.
