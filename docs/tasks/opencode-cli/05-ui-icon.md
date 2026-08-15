# Task 05 — OpenCode brand mark icon

**Goal:** Add a monochrome SVG mark and register it like Hermes/Pi.

**Twin:** `crates/ui/assets/icons/pi-mark.svg` + `OPENCODE`-style entry in
`crates/ui/src/icons.rs`.

## Edit 1 — SVG asset

Create file: `crates/ui/assets/icons/opencode-mark.svg`

Use this exact SVG (16×16 logical, `currentColor`, simple geometric mark — do
not download remote logos):

```svg
<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
  <path fill="currentColor" d="M8 1.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13Zm0 2a4.5 4.5 0 1 1 0 9 4.5 4.5 0 0 1 0-9Z"/>
  <path fill="currentColor" d="M8 5.25a2.75 2.75 0 1 0 0 5.5 2.75 2.75 0 0 0 0-5.5Z"/>
</svg>
```

Rules:

- Must contain `<svg` and `viewBox`.
- Prefer `currentColor` so the UI can tint it (same as Pi/OpenAI marks).
- Keep the file small (<2KB).

## Edit 2 — register constant

File: `crates/ui/src/icons.rs`

In the icon table next to `(PI_MARK, "pi-mark")`, add:

```rust
    (OPENCODE_MARK, "opencode-mark"),
```

The macro/`include` pattern already used for other marks must pick up
`icons/opencode-mark.svg` automatically — follow the existing registration
style exactly (do not invent a new loader).

## Edit 3 — replace temporary icon arms from task 01

Wherever task 01 mapped `HarnessId::OpenCode` to `HERMES_MARK` / `PI_MARK`,
switch to `crate::icons::OPENCODE_MARK` with `None` tint (monochrome), same as
Pi/Grok:

- `crates/ui/src/pickers.rs` → `harness_brand_icon`
- `crates/ui/src/settings/accounts.rs` → `provider_icon` closure (add an
  explicit arm; do not leave OpenCode falling into Claude's brand orange)

## Verify

```bash
cargo test -p zeron-ui every_registered_icon_loads_and_parses
cargo check -p zeron-ui
```

## Done when

- Asset exists and parses as SVG with `viewBox`.
- `OPENCODE_MARK` is registered.
- OpenCode no longer reuses another product's mark in UI matches.
