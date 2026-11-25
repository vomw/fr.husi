# CLAUDE.md

All guidance is in these files:

@AGENTS.md

@CONTRIBUTING.md

## Build and test through the Makefile

Never hand-roll a `./gradlew` / `go` / `golangci-lint` invocation when a `make` target already
covers it. The Makefile is the Android build orchestration layer and encodes plugin exclusions and
the required task names.

| Instead of                                     | Run                     |
|------------------------------------------------|-------------------------|
| `:composeApp:compileAndroidMain` (compile check)   | `make apk_debug`        |
| `cd libcore && go test ./...`                  | `make test_go`          |
| `golangci-lint run` (per GOOS)                 | `make lint_go`          |
| `golangci-lint fmt`                            | `make fmt_go`           |
| `:composeApp:lintAndroidMain` (NewApi check)   | `make lint_android`     |
| everything at once                             | `make test`             |

The target list is the root `Makefile` and the table in AGENTS.md — check there **first**.
Reach for `./gradlew` only for something no target expresses.

## Read the matching skill before editing

The `husi-*` skills in `.agents/skills/` record UI and testing conventions that the code alone
does not show. Invoke the matching skill before the first edit, not after the change is written:

| Before touching                                                              | Skill                        |
|------------------------------------------------------------------------------|------------------------------|
| `Scaffold`, `topBar`, `CapsuleTopBar`, tabs / `Tab` rows, search bars, Haze   | `husi-topbar`                |
| `DropdownMenuPopup` or a `more_vert` menu in topbar `actions`                 | `husi-actions-dropdown-menu` |
| Settings or profile-editor preference rows, `preferenceGroup`, `MaskedIcon`   | `husi-preference-ui`         |
| Anything under `commonTest/`, or a DI seam for testability                     | `husi-testing`               |

## Cloud work

If the skills start with `husi-` isn't visible, run this command to get an overview:

```shell
grep -r description .agents/skills/ --include="SKILL.md"
```

