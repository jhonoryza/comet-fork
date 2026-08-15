# Task 04 — Harness tests

**Goal:** Lock the OpenCode descriptor surface and keep shared ACP fixture
coverage.

**Twin:** Hermes/Pi tests in `crates/harness/tests/acp.rs` and
`crates/harness/tests/shell_env_resolution.rs`.

## Edit 1 — descriptor surface test

File: `crates/harness/tests/acp.rs`

Near `hermes_and_pi_descriptor_surfaces_match_registry_expectations`, add:

```rust
#[test]
fn opencode_descriptor_surface_matches_registry_expectations() {
    let opencode = AcpHarness::opencode();
    assert_eq!(opencode.id(), HarnessId::OpenCode);
    assert_eq!(opencode.display_name(), "OpenCode");
    assert!(opencode.supports_steering());
    assert_eq!(opencode.steering_mode(), SteeringMode::TurnBoundary);
    assert!(opencode.reasoning_levels().is_empty());
}
```

## Edit 2 — fixture matrix

In the same file, find the table that builds harnesses with
`with_executable(fixture_path())` (pairs like `("hermes", …)`, `("pi", …)`,
`("cursor", …)`). Add:

```rust
        ("opencode", AcpHarness::opencode().with_executable(fixture_path())),
```

Also add OpenCode to any other **non-ignored** enumeration of all ACP factories
in this file (search for `AcpHarness::pi()` lists). Skip ignored `real_*` tests
unless they already list every harness — if they do, append
`("opencode", AcpHarness::opencode)` / `AcpHarness::opencode()` for consistency.

## Edit 3 — static fallback test

Add (mirror of the Pi probe-failure test):

```rust
#[tokio::test]
async fn opencode_models_fall_back_to_static_catalog_when_probe_fails() {
    let harness = AcpHarness::opencode().with_executable("/nonexistent/never-an-opencode");
    let models = harness.models().await.expect("static fallback");
    let ids: Vec<&str> = models.iter().map(|m| m.id.as_str()).collect();
    assert_eq!(ids, vec!["default"], "{models:?}");
}
```

## Edit 4 — login-shell PATH resolution

File: `crates/harness/tests/shell_env_resolution.rs`

1. Create a fake binary next to the others:

```rust
    write_executable(&shell_bin.join("opencode"), "#!/bin/sh\nexit 0\n");
```

2. Clear the override env with the others:

```rust
        std::env::remove_var("OPENCODE_EXECUTABLE");
```

3. Assert resolution:

```rust
    let opencode = AcpHarness::opencode()
        .launch_program()
        .expect("opencode resolves via login-shell PATH");
    assert_eq!(opencode, shell_bin.join("opencode"), "{opencode:?}");
```

## Edit 5 — quiet survey (if present)

File: `crates/harness/tests/real_quiet_survey.rs`

If it lists `("hermes", AcpHarness::hermes), …`, append
`("opencode", AcpHarness::opencode),`. This file is typically ignored live —
still keep the list complete.

## Verify

```bash
cargo test -p zeron-harness --test acp opencode
cargo test -p zeron-harness --test shell_env_resolution
```

Do **not** run `--ignored` live surveys in this task.

## Done when

- Descriptor assertions pass.
- Fixture matrix includes OpenCode.
- Static fallback returns `["default"]`.
- Shell-env test resolves `opencode` from the fake login PATH.
