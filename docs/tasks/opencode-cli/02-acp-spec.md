# Task 02 — `AcpHarness::opencode()` spec

**Goal:** Register OpenCode as another `AcpAgentSpec` on the shared ACP harness.

**Twin:** copy `hermes_install_paths` + `hermes_spec` + `AcpHarness::hermes`
in `crates/harness/src/acp/mod.rs`, then rename.

## Edit 1 — module docs

At the top of `crates/harness/src/acp/mod.rs`, the crate docs list agents.
Add OpenCode next to Hermes/Pi/Cursor (one short clause):
`AcpHarness::opencode` (`opencode acp`).

## Edit 2 — install path helper

Paste **immediately after** `hermes_install_paths` (before `hermes_spec` is
fine too; keep helpers next to their specs). Use this exact body:

```rust
fn opencode_install_paths() -> Vec<PathBuf> {
    let mut dirs = Vec::new();
    if let Some(home) = std::env::var_os("HOME").map(PathBuf::from) {
        // Official install script fallback + XDG-ish locations.
        dirs.push(home.join(".opencode").join("bin").join("opencode"));
        dirs.push(home.join(".local").join("bin").join("opencode"));
        dirs.push(home.join("bin").join("opencode"));
        dirs.push(home.join(".npm-global").join("bin").join("opencode"));
    }
    dirs.push(PathBuf::from("/opt/homebrew/bin/opencode"));
    dirs.push(PathBuf::from("/usr/local/bin/opencode"));
    dirs
}
```

## Edit 3 — `opencode_spec`

Paste after `hermes_spec` (or after `pi_spec`). Exact shape:

```rust
fn opencode_spec() -> AcpAgentSpec {
    AcpAgentSpec {
        id: HarnessId::OpenCode,
        display_name: "OpenCode",
        executable: "opencode",
        env_override: "OPENCODE_EXECUTABLE",
        // Native ACP server — no adapter package in between.
        args: &["acp"],
        npx_package: None,
        extra_paths: opencode_install_paths,
        cli_executable: "opencode",
        cli_extra_paths: opencode_install_paths,
        install_hint: "opencode (searched PATH, the login shell's PATH, ~/.opencode/bin, \
             ~/.local/bin, ~/bin, ~/.npm-global/bin, /opt/homebrew/bin, /usr/local/bin, and \
             fnm/nvm/volta/pnpm/bun install dirs; install with \
             `curl -fsSL https://opencode.ai/install | bash` or \
             `npm install -g opencode-ai`; set OPENCODE_EXECUTABLE to override)",
        // Models come from the user's OpenCode providers/config. Probe discovery
        // wins; this pass-through row is only the offline/static fallback.
        models: || {
            vec![Model {
                id: "default".into(),
                label: "OpenCode default".into(),
                description: Some(
                    "Uses the model configured in OpenCode (`opencode` / opencode.json)"
                        .into(),
                ),
                reasoning_levels: Vec::new(),
                options: Vec::new(),
            }]
        },
        // No `_session/steering` extension assumed: steers deliver at turn boundaries.
        steering_mode: SteeringMode::TurnBoundary,
        // No static effort ladder; live `thought_level` (if advertised) fills in.
        reasoning_levels: &[],
        prompt_transform: identity_transform,
        effort_values: default_effort_values,
        ladder_extras: &[],
    }
}
```

## Edit 4 — constructor

Next to `AcpHarness::hermes` / `pi`:

```rust
    /// OpenCode (`opencode acp`) — OpenCode's native ACP server.
    pub fn opencode() -> Self {
        Self::with_spec(opencode_spec())
    }
```

## Do not change

- Shared JSON-RPC / session/prompt / permission auto-accept paths.
- Cursor-only notification helpers.
- Claude/Codex catalogs.

## Verify

```bash
cargo check -p zeron-harness
```

Optional smoke (no CLI required):

```bash
cargo test -p zeron-harness --lib -- acp::tests 2>/dev/null || true
```

## Done when

- `AcpHarness::opencode().id() == HarnessId::OpenCode` would hold (tested in task 04).
- Spec uses `args: &["acp"]`, `npx_package: None`, empty reasoning ladder.
