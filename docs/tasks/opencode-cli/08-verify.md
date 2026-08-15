# Task 08 — Verify end-to-end (checklist)

**Goal:** Prove the feature is wired without requiring a live OpenCode login.

Run from the repo root. Fix failures before declaring done.

## Required commands

```bash
# Identity
cargo test -p zeron-proto opencode_harness_id_wire_is_opencode

# Spec + fixture coverage
cargo test -p zeron-harness --test acp opencode
cargo test -p zeron-harness --test shell_env_resolution

# Icons
cargo test -p zeron-ui every_registered_icon_loads_and_parses

# Exhaustiveness / link
cargo check -p zeron-harness -p zeron-engine -p zeron-ui -p zeron
```

## Manual product checks (headed app, if available)

1. Settings → Agents: OpenCode row appears; CLI hint mentions `opencode`.
2. With OpenCode **disabled** (default): composer harness rail omits it (same
   as other opt-in agents).
3. Enable OpenCode on a machine where `opencode` is on PATH: row is not dimmed;
   picker shows OpenCode + mark.
4. Without the CLI: row is dimmed / install hint visible; toggle inert or
   engine rejects enable per existing rules.
5. Accounts page: still only Claude Code + Codex provider cards.

## Optional live smoke (human / powerful model only)

Only if `opencode` is installed and authenticated:

```bash
cargo test -p zeron-harness --test acp -- --ignored --nocapture real_all_harnesses
```

Expect OpenCode to appear in the matrix if task 04 added it. Do not block the
task pack on live auth failures.

## Final acceptance table

| Check | Pass? |
| --- | --- |
| Wire id `"opencode"` | |
| `AcpHarness::opencode` exists, args `acp` | |
| Registry lazy slot present | |
| Not in `default_enabled` | |
| Tests for descriptor + static `default` model | |
| `OPENCODE_MARK` asset loads | |
| UI blurb / cli_name / icon arms | |
| Not in `PROVIDERS` | |
| PARITY + acp research notes updated | |

When every required command passes and the acceptance table is complete, the
OpenCode CLI support task pack is finished.
