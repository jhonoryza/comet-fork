# Task 03 — Engine registry lazy slot

**Goal:** Surface OpenCode in `ListHarnesses` without spawning until first use.

**Twin:** Hermes `register_lazy` block in `crates/engine/src/registry.rs`.

## Edit

File: `crates/engine/src/registry.rs`

Inside `pub fn default_registry()` (or whatever builds the production catalog),
**after** the Pi `register_lazy` block and **before** `registry` is returned,
add:

```rust
    // OpenCode over ACP (`opencode acp`), same lazy pattern: the static
    // descriptor mirrors AcpHarness::opencode() exactly. No steering extension
    // (turn boundaries) and no static effort ladder — models/effort come from
    // live ACP discovery when the CLI is present.
    registry.register_lazy(
        HarnessDescriptor {
            id: HarnessId::OpenCode,
            name: "OpenCode".into(),
            supports_steering: true,
            steering_mode: SteeringMode::TurnBoundary,
            reasoning_levels: Vec::new(),
            installed: true,
            enabled: None,
        },
        Box::new(|| zeron_harness::AcpHarness::opencode().installed()),
        Box::new(|| Ok(Arc::new(zeron_harness::AcpHarness::opencode()) as Arc<dyn Harness>)),
    );
```

## Do not change

- `default_enabled()` — must remain `[ClaudeCode, Codex]` only.
- Mock registration.
- Existing Hermes/Pi/Cursor/Grok slots (except inserting OpenCode beside them).

## Verify

```bash
cargo check -p zeron-engine
cargo test -p zeron-engine registry -- --nocapture
```

(If no tests match the filter, `cargo check -p zeron-engine` is enough.)

## Done when

- Default registry lists eight production harnesses + mock (OpenCode included).
- OpenCode is **not** in `default_enabled()`.
