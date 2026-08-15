# Task 01 — Add `HarnessId::OpenCode`

**Goal:** Introduce the wire identity so later crates can compile against it.

**Twin:** existing variants in `crates/proto/src/agent.rs`.

## Edit 1 — enum variant

File: `crates/proto/src/agent.rs`

Inside `enum HarnessId`, **after** `Pi` and **before** `Mock`, insert:

```rust
    /// OpenCode coding agent, driven over ACP (`opencode acp`).
    #[serde(rename = "opencode")]
    OpenCode,
```

Why `#[serde(rename = "opencode")]`: plain kebab-case would emit `"open-code"`.
The wire id must be the single token `"opencode"` (CLI / product name).

## Edit 2 — serde unit test

In the same file's `#[cfg(test)]` module (near `harness_id_uses_kebab_case`),
add:

```rust
    #[test]
    fn opencode_harness_id_wire_is_opencode() {
        assert_eq!(
            serde_json::to_string(&HarnessId::OpenCode).unwrap(),
            "\"opencode\""
        );
        let back: HarnessId = serde_json::from_str("\"opencode\"").unwrap();
        assert_eq!(back, HarnessId::OpenCode);
    }
```

## Fix compile breaks from exhaustive matches (required in this task)

Adding a variant will fail `cargo check` on exhaustive `match` arms. Fix every
compiler error **now** with the minimal arm (copy the Hermes/Pi arm and rename).

Known exhaustive sites (search if the compiler names others):

| File | Function / site | Add arm |
| --- | --- | --- |
| `crates/engine/src/agent_accounts.rs` | `harness_slug` | `HarnessId::OpenCode => "opencode"` |
| `crates/ui/src/settings/harnesses.rs` | `blurb` | see task 06 copy (use a temporary string if needed) |
| `crates/ui/src/settings/harnesses.rs` | `cli_name` | `HarnessId::OpenCode => "opencode"` |
| `crates/ui/src/pickers.rs` | `harness_brand_icon` | temporarily reuse `PI_MARK` or `HERMES_MARK` until task 05 |

For UI icon matches that do not yet have `OPENCODE_MARK`, **temporarily** map
`OpenCode` to `crate::icons::HERMES_MARK` (or any existing mark). Task 05/06
will replace that with the real mark. Do not leave `_` catch-alls that hide
future harnesses if the match was previously exhaustive.

**Do not** add OpenCode to `PROVIDERS` in accounts settings.

## Verify

```bash
cargo test -p zeron-proto opencode_harness_id_wire_is_opencode
cargo check -p zeron-proto -p zeron-harness -p zeron-engine -p zeron-ui
```

All must succeed before task 02.

## Done when

- Wire round-trip is `"opencode"`.
- Workspace crates that match on `HarnessId` compile.
