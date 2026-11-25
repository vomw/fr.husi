# AGENTS.md

Read [CONTRIBUTING.md](./CONTRIBUTING.md) before writing code.

## Android build commands

First-time setup:

```
./run lib source
make assets
```

| Target | Purpose |
|---|---|
| `make libcore_android` | Build the Go core into `composeApp/libs/libcore.aar`. |
| `make apk` / `make apk_debug` | Build Foss release/debug Android APKs. |
| `make aboutlibraries` | Regenerate Android and Go OSS-license metadata. |
| `make plugin PLUGIN=<name>` | Build an Android plugin APK. |
| `make assets` | Download geoip/geosite into app resources. |
| `make icon` | Regenerate Android icons from `art/`. |
| `make lint_go` | Lint the Android Go target. |
| `make test_go` | Run Go tests. |
| `make lint_android` | Run Android NewApi lint. |
| `make proto` | Re-vendor sing-box schema and regenerate Go gRPC stubs. |

`BUILD_PLUGIN=none` excludes plugin modules for app-only Gradle builds.

## Workflow requirements

### gomobile export surface

Package `libcore` is what gomobile binds. An exported interface there whose methods gomobile cannot
bind (proto slices, function values) fails `make libcore_android` with "proxy … does not implement".
Nothing Kotlin does not call belongs in `libcore`'s exported surface; put shared Go-side types in a
sibling package.

### Proto workflow

- `daemon/started_service.proto` is vendored from the pinned sing-box by `make proto`; never edit it by hand.
- `KEEP_STARTED_SERVICE_RPCS` in `buildScript/proto.sh` is the upstream RPC allowlist.
- Add Husi messages under `husi/v1/`, then run `make proto`.
- Go stubs are regenerated for `husi/v1` only; duplicating the daemon proto in the binary panics the protobuf registry.

### Room database migrations

`database/SagerDatabase` uses an explicit `AutoMigration` chain plus custom migration specs in
`database/Migrations.kt`. Add an entry whenever the schema changes.

### Icon pipeline

- `art/icon.svg` is the full mark and `art/icon-small.svg` is the small-size variant.
- `buildScript/icon.py` accepts only `<path>` elements in input SVGs.
- Never hand-edit generated Android icon files; edit SVG then run `make icon`.

### Android two-process model

The app runs UI and `:bg` processes. Only `:bg` loads libcore (`Seq.setContext`, `Libcore.initCore`,
`boxService.start()`); the UI process reaches the core through `CoreClient`. The binder
(`SagerConnection`) is lifecycle-only and data flows over gRPC on `<filesDir>/api.sock`. Background
main-process work wraps core calls in `withBackgroundProcess`.

### Android signing

A `FossRelease` build without a keystore calls `exitProcess(0)` in `setupAppCommon`; this is intentional.
