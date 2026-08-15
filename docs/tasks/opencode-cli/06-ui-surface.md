# Task 06 — UI surface (blurbs, picker, slug)

**Goal:** Make OpenCode readable in Settings → Agents and the composer picker.

**Twin:** Hermes/Pi arms in the same files.

## Edit 1 — Settings blurbs

File: `crates/ui/src/settings/harnesses.rs`

In `blurb`:

```rust
        HarnessId::OpenCode => "OpenCode coding agent, driven through the opencode CLI (ACP).",
```

In `cli_name` (if not already done in task 01):

```rust
        HarnessId::OpenCode => "opencode",
```

## Edit 2 — Picker brand icon

File: `crates/ui/src/pickers.rs` — `harness_brand_icon`

Ensure:

```rust
        HarnessId::OpenCode => (crate::icons::OPENCODE_MARK, None),
```

## Edit 3 — Accounts page icon map

File: `crates/ui/src/settings/accounts.rs`

In the `provider_icon` match used by the accounts page, add an explicit arm
**before** the `_ => Claude` fallback:

```rust
            HarnessId::OpenCode => (crate::icons::OPENCODE_MARK, None),
```

**Do not** add OpenCode to `PROVIDERS` (still only Claude Code + Codex).
OpenCode manages its own auth via the CLI (`/connect`); zeron does not swap
credentials for it.

## Edit 4 — Engine slug (if still missing)

File: `crates/engine/src/agent_accounts.rs` — `harness_slug`

```rust
        HarnessId::OpenCode => "opencode",
```

## Edit 5 — Sweep remaining exhaustive matches

Run:

```bash
cargo check -p zeron-ui -p zeron-engine -p zeron 2>&1
```

For every `non-exhaustive patterns: OpenCode not covered`:

- Prefer copying the nearest Hermes/Pi arm.
- For agent-account activate/login matches that only support Claude/Codex,
  leave OpenCode in the existing `other => Err(...)` path (no new login flow).

## Verify

```bash
cargo check -p zeron-ui -p zeron-engine -p zeron
cargo test -p zeron-ui harness -- --nocapture
```

## Done when

- Settings shows a sensible OpenCode description + CLI name `opencode`.
- Picker/accounts use `OPENCODE_MARK`.
- No compile errors on `HarnessId` matches.
- `PROVIDERS` unchanged (length 2).
