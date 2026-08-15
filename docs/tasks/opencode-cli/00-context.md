# Task 00 — Context (read only)

**Goal:** Understand enough to implement OpenCode without redesigning the
harness layer. **No file edits in this task.**

## Read these files (in order)

1. `docs/tasks/opencode-cli/README.md` — product facts table.
2. `docs/research/acp.md` — how ACP harnesses work in this repo.
3. `crates/harness/src/acp/mod.rs` — find `fn hermes_spec()` and
   `fn hermes_install_paths()` and `AcpHarness::hermes()`. Those three blocks
   are the template.
4. `crates/engine/src/registry.rs` — find the Hermes `register_lazy` block.
5. `crates/proto/src/agent.rs` — `enum HarnessId`.
6. `docs/PARITY.md` — §4 Harness table (you will add a row in task 07).

## What you must not do

- Do not add a bespoke stream-json / HTTP adapter. OpenCode speaks ACP.
- Do not add credential-swap / agent-accounts support.
- Do not enable OpenCode by default (`default_enabled` stays Claude+Codex).
- Do not pin an `npx` package; native CLI only.

## Checkpoint

You can point to Hermes as the twin and recite:

- launch = `opencode` + `acp`
- env = `OPENCODE_EXECUTABLE`
- wire id = `opencode`
- steering = turn boundary

Then proceed to task 01.
