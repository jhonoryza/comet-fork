# OpenCode CLI harness — task pack

Add **OpenCode** as a production harness, the same way **Hermes** and **Cursor**
were added: a thin `AcpAgentSpec` on the shared `AcpHarness`, native ACP via
`opencode acp` (no adapter npm package).

These tasks are written for a **cheap/fast coding model**. Each file is one
small, ordered unit of work. Do **not** skip ahead. Do **not** invent new
architecture. Copy the named twin (Hermes) unless a task says otherwise.

## Product facts (do not re-research)

| Fact | Value |
| --- | --- |
| Product name (UI) | `OpenCode` |
| Wire `HarnessId` JSON | `"opencode"` |
| Rust enum variant | `OpenCode` with `#[serde(rename = "opencode")]` |
| CLI binary | `opencode` |
| ACP launch args | `["acp"]` |
| Env override | `OPENCODE_EXECUTABLE` |
| npm fallback | **none** (`npx_package: None`) — same as Hermes/Cursor |
| Steering | `SteeringMode::TurnBoundary` (no `_session/steering` assumed) |
| Reasoning ladder | empty `reasoning_levels: &[]` until discovery proves otherwise |
| Static model fallback | one pass-through row `id: "default"` (same idea as Pi) |
| Agent accounts / credential swap | **out of scope** — do **not** add to `PROVIDERS` |
| Default enabled set | unchanged — OpenCode stays opt-in (Settings → Agents) |

Closest twin in this repo: **`AcpHarness::hermes()`** + Hermes registry slot +
Hermes UI blurb/icon arms.

External docs (already summarized; do not block on fetching):

- ACP: `opencode acp` — <https://opencode.ai/docs/acp/> (also open-code.ai)
- Install: `curl -fsSL https://opencode.ai/install | bash` or
  `npm i -g opencode-ai` (package is `opencode-ai`, binary is `opencode`)
- Default install dirs: `$HOME/.opencode/bin`, `$HOME/.local/bin`, `$HOME/bin`,
  Homebrew `/opt/homebrew/bin`, `/usr/local/bin`

## Task order (strict)

| # | File | Outcome |
| --- | --- | --- |
| 0 | [`00-context.md`](00-context.md) | Read-only orientation (no code changes) |
| 1 | [`01-proto-harness-id.md`](01-proto-harness-id.md) | `HarnessId::OpenCode` + serde test |
| 2 | [`02-acp-spec.md`](02-acp-spec.md) | `opencode_spec()` + `AcpHarness::opencode()` |
| 3 | [`03-engine-registry.md`](03-engine-registry.md) | Lazy registry slot |
| 4 | [`04-harness-tests.md`](04-harness-tests.md) | Descriptor + fixture + shell-env tests |
| 5 | [`05-ui-icon.md`](05-ui-icon.md) | SVG asset + `OPENCODE_MARK` |
| 6 | [`06-ui-surface.md`](06-ui-surface.md) | Picker icon, Settings blurbs, slug, accounts icon |
| 7 | [`07-docs-parity.md`](07-docs-parity.md) | `PARITY.md` + `docs/research/acp.md` notes |
| 8 | [`08-verify.md`](08-verify.md) | Compile + targeted tests checklist |

## Definition of done

- `cargo test -p zeron-proto harness_id` passes.
- `cargo test -p zeron-harness --test acp opencode` passes (non-ignored).
- `cargo check -p zeron-engine -p zeron-ui -p zeron` succeeds (exhaustive
  matches compile).
- Settings → Agents can list OpenCode; composer picker shows the mark when
  enabled + installed.
- No new agent-account login flow.
- No changes to Claude/Codex/Cursor/Grok/Hermes/Pi specs beyond adding
  OpenCode beside them.

## Rules for the implementing model

1. One task file per session/turn when possible.
2. Prefer **copy-paste from Hermes**, then rename identifiers.
3. If `rustc` reports a non-exhaustive `match` on `HarnessId`, fix that arm in
   the **same** task that introduced the compile break — do not leave broken
   builds between tasks 1–6.
4. Do not run ignored live CLI tests unless the human asks
   (`--ignored real_*`).
5. Do not commit unless the human asks.
