# Task 07 — Docs / parity notes

**Goal:** Record OpenCode in research + parity so future work knows it exists.

## Edit 1 — Parity checklist

File: `docs/PARITY.md`

In **§4 Harness**, after the Pi row, add:

```markdown
| OpenCode (ACP) | done | Shared `AcpHarness` spec; `opencode acp` (native ACP server), turn-boundary steering, no static effort ladder; models from live ACP discovery with a `default` static fallback. Opt-in via Settings → Agents (not default-enabled). |
```

Only add the row if the implementation from tasks 01–06 is actually present.
If something is still missing, write `partial` and list the gap in the Notes
column instead of lying with `done`.

## Edit 2 — ACP research note

File: `docs/research/acp.md`

In the Decision section (near Hermes + Pi), append a short bullet:

```markdown
- **OpenCode registered**: `AcpHarness::opencode()` runs OpenCode's native ACP
  server (`opencode acp`; install via `curl -fsSL https://opencode.ai/install | bash`
  or `npm i -g opencode-ai`; `OPENCODE_EXECUTABLE` overrides). No npm ACP
  adapter package. No `_session/steering` assumed → turn-boundary steering;
  empty static reasoning ladder; static model fallback is a single `default`
  pass-through (user providers live in OpenCode config). Not part of zeron
  agent-account credential swap.
```

## Edit 3 — Optional feature inventory

If `docs/research/feature-inventory.md` lists harness ids
(`claude-code | codex | cursor | …`), append `opencode` to that list in the
same style. Skip this edit if the file has no harness id enumeration.

## Do not

- Rewrite `ARCHITECTURE.md` harness blurb unless it still claims only
  Claude/Codex as production adapters and a one-line mention is cheap —
  prefer a single clause, not a redesign.

## Done when

- PARITY §4 mentions OpenCode.
- `docs/research/acp.md` mentions `AcpHarness::opencode()`.
