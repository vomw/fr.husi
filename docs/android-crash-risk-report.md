# Android crash-risk review

Reviewed: current working tree, including the Android-only conversion and the
connected-state bar changes. This is a static review: findings are ranked by
whether an exception can escape an Android lifecycle callback, Compose event,
or unhandled coroutine. It cannot prove that a device-specific path is
reachable without instrumentation tests and crash telemetry.

## Findings

| ID | Status | Severity | Trigger | Failure |
|---|---|---|---|---|
| CR-0 | Fixed — source verified | Critical — confirmed | Creating `GroupScreenViewModel` while its first group/count flow emits synchronously. | Main-thread NPE while locking an uninitialized `Mutex`; the Group screen crashes. |
| CR-1 | Fixed — source verified | High | A rule is deleted while its editor route remains in the navigation back stack, or state restoration opens a removed rule. | Null-pointer exception while creating `RouteSettingsViewModel`. |
| CR-2 | Fixed — source verified | High | Importing a syntactically valid backup containing corrupt legacy settings parcel data. | An uncaught exception from a global coroutine; the import remains marked as running and may terminate the process through the uncaught-exception handler. |
| CR-3 | Fixed — source verified | Medium | A package is removed, disabled, or becomes unavailable while the App Manager or VPN scanner processes a cached `PackageInfo`. | Null-pointer exception or package-resource exception in the UI/background scan. |
| CR-4 | Fixed — source verified | Medium | Android recreates or otherwise re-enters a VPN consent launch without the contract's in-memory cached intent. | Null-pointer exception in `StartService.createIntent`. |
| CR-5 | Fixed — source verified | Low | The settings composable is hosted by a non-`Activity` `Context` (for example, a test, preview, or unusual context wrapper). | Null-pointer exception after changing the theme. |
| CR-6 | Fixed — source verified | Low | The export callback runs after the backing exported value was cleared or replaced. | Null-pointer exception while writing the selected backup file. |
| CR-7 | Fixed — source verified | Low | A profile screen is restored with a route/type mismatch after database migration, data corruption, or an invalid deep link. | `ClassCastException` in a profile settings composable. |

## CR-0: Group screen launches a coroutine before its mutex is initialized

**Status:** fixed — source verified. The supplied
`husi_crash67249205038270596.log` confirmed this issue on Android 15
(Redmi Note 8, app 2.3.4 / version code 551).

The fatal exception is on the main thread:

```text
java.lang.NullPointerException: Attempt to invoke interface method
'kotlinx.coroutines.sync.Mutex.lock(...)' on a null object reference
    at fr.husi.ui.GroupScreenViewModel.reloadGroups(...:71)
    at fr.husi.ui.GroupScreenViewModel.<init>(...:27)
```

**Root cause:** `GroupScreenViewModel` declares its `init` block before
`hiddenGroupAccess` and `hiddenGroup`:

- `composeApp/src/commonMain/kotlin/fr/husi/ui/GroupScreenViewModel.kt:60-87`
  starts collecting the groups/counts flows and calls `reloadGroups`.
- `GroupScreenViewModel.kt:90-92` initializes the `Mutex` used by
  `reloadGroups`.
- `GroupScreenViewModel.kt:94-110` immediately dereferences that mutex.

`viewModelScope.launch` may start immediately on the main dispatcher. When the
database flows already have a value, the collection can reach `reloadGroups`
before Kotlin has initialized the properties declared after the `init` block.
The log's constructor-to-collector stack proves that ordering occurred in the
field build that crashed.

**Fix:** move `deleteTimer`, `hiddenGroupAccess`, and `hiddenGroup` above the
`init` block. This makes all state used by either collector available before a
coroutine can be scheduled. As extra hardening, use a named initialization
function invoked after all property declarations rather than relying on source
order.

**Regression test:** add a `GroupScreenViewModel` test with hot/immediately
emitting group and count flows. Construct the ViewModel repeatedly and assert
that it publishes its first `GroupUiState` without an uncaught exception.

## CR-1: stale route editor dereferences a missing rule

**Status:** fixed — source verified. Rule loading is lifecycle-owned and a missing rule displays a
message before leaving the stale editor route.

**Evidence:** `composeApp/src/commonMain/kotlin/fr/husi/ui/RouteSettingsViewModel.kt:135-141`
loads an existing entity with:

```kotlin
runBlocking { SagerDatabase.rulesDao.getById(id) }!!
```

The editor route accepts an ID, while deletion and navigation are independent
operations. A stale saved navigation entry, a concurrent delete, or a restore
after a backup import can therefore turn an expected missing row into an NPE
during ViewModel construction.

**Fix:** load asynchronously, treat a missing entity as a normal result, show a
"rule no longer exists" message, and pop the editor route. Do not use
`runBlocking` on the ViewModel creation path. Add a test that deletes a rule
before constructing/recreating the editor for its ID.

## CR-2: legacy settings import trusts parcel payloads and uses an unowned coroutine

**Status:** fixed — source verified. Import work is lifecycle-owned, validates legacy entries,
recycles Parcels in `finally`, reports errors, and clears its busy state in `finally`.

**Evidence:**

- `composeApp/src/androidMain/kotlin/fr/husi/ui/tools/LegacySettingsImport.android.kt:12-23`
  force-casts JSON, Base64-decodes every entry, and unmarshals it into a Parcel
  without validating the entry or protecting `Parcel.recycle()` with `finally`.
- `composeApp/src/commonMain/kotlin/fr/husi/ui/tools/BackupViewModel.kt:289-347`
  calls that importer from `finishInput`.
- `composeApp/src/commonMain/kotlin/fr/husi/ktx/Asyncs.kt:15-16` implements
  `runOnDefaultDispatcher` with `GlobalScope.launch`, so the work has neither a
  ViewModel lifecycle nor a local exception handler.

A backup can pass the outer JSON/version check but still contain a bad Base64
string, a non-primitive item, or an invalid/truncated Parcel. Those exceptions
escape `finishInput`; `isImporting` is never reset on failure.

**Fix:** perform imports in `viewModelScope.launch`, wrap the whole import in
`try/catch/finally`, return a user-visible error, and always clear
`isImporting`. Validate `JsonPrimitive`/Base64 input and use
`Parcel.obtain().use`-equivalent `try/finally` recycling. Add malformed legacy
settings fixtures (bad Base64, wrong JSON shape, truncated Parcel).

## CR-3: package scans dereference mutable package-manager snapshots

**Status:** fixed — source verified. Incomplete or unavailable package entries are skipped and
per-package label, icon, APK, and scan failures do not abort the remaining scan.

**Evidence:**

- `composeApp/src/androidMain/kotlin/fr/husi/ui/AbstractAppList.kt:129-190` and
  `AppManagerViewModel.kt:73-164` repeatedly use `packageInfo.applicationInfo!!`.
- `composeApp/src/androidMain/kotlin/fr/husi/ui/tools/VPNScannerScreenViewModel.kt:101-151`
  uses the same assertion for icon loading and APK access.

Package install/remove/disable events can occur after `PackageCache` takes its
snapshot and while either scan is running. `PackageInfo.applicationInfo` is
nullable in the platform API; additionally, loading a label/icon or opening the
APK can fail after an uninstall. The App Manager path has no per-package
exception boundary, so one stale entry can cancel the scan and surface an
uncaught exception.

**Fix:** snapshot only complete entries (`applicationInfo ?: continue`) and
wrap each package's label/icon/APK work in `runCatching`. Skip unavailable
packages, log at debug/warn level, and continue scanning. Add a package-source
abstraction/fake that returns a package without `applicationInfo` and one that
throws `NameNotFoundException` during icon access.

## CR-4: VPN consent contract depends on volatile cached state

**Status:** fixed — source verified. The contract no longer retains a consent `Intent`; it obtains
a fresh intent when Android creates the consent launch.

**Evidence:** `composeApp/src/androidMain/kotlin/fr/husi/ui/VpnRequestActivity.kt:58-75` stores
the system consent `Intent` in `cachedIntent`, then `createIntent` executes
`cachedIntent!!`.

This only works if the Activity Result contract instance that receives
`getSynchronousResult` is the same instance that immediately receives
`createIntent`. Activity recreation, pending-result restoration, or an
unexpected duplicate launch violates that implicit contract and crashes at the
assertion.

**Fix:** remove the mutable cache. In `createIntent`, call `VpnService.prepare`
again and return its result; if it is unexpectedly null, return an explicit
failed/cancelled result through a purpose-built contract or finish safely. Add
an instrumentation test covering recreation while the VPN consent sheet is
pending.

## CR-5: theme changes assume an Activity context

**Status:** fixed — source verified. Theme application now recreates an Activity only when one is
available from the current context.

**Evidence:** `composeApp/src/androidMain/kotlin/fr/husi/ui/SettingsScreenPlatform.android.kt:115`
uses `context.findActivity<Activity>()!!.recreate()`.

`findActivity` intentionally returns nullable (`ktx/AndroidUtils.kt:69-76`), so
the assertion is inconsistent with its API. Production navigation normally
provides an Activity, but test, preview, or wrapped contexts can make this a
direct crash.

**Fix:** use `context.findActivity<Activity>()?.recreate()` and keep the theme
change even if no host Activity is available. Add a Compose test using a
non-Activity context wrapper.

## CR-6: backup export callback reads a non-null value twice

**Status:** fixed — source verified. The file-saver callback uses the payload captured for that
specific export request and treats a missing payload as cancellation.

**Evidence:** `composeApp/src/commonMain/kotlin/fr/husi/ui/tools/BackupScreen.kt:147-166`
launches the file-saver callback and later evaluates `uiState.exported!!`.
The value is independently cleared by `postExport`; lifecycle changes or a
second export can make the callback observe null.

**Fix:** capture the exported text in the `LaunchedEffect` and pass it to a
callback/state object tied to that request. In the callback, treat a missing
payload as cancellation rather than asserting non-null.

## CR-7: profile settings rely on unchecked UI-state casts

**Status:** fixed — source verified. The shared profile editor scaffold catches a stale editor-state
cast and renders a recoverable error instead of allowing a Compose `ClassCastException` to escape.

**Evidence:** examples include
`uiState as WireGuardUiState` in
`composeApp/src/commonMain/kotlin/fr/husi/ui/profile/WireGuardSettingsScreen.kt:63`,
`VMessUiState` in `VMessSettingsScreen.kt:58`, and `VLESSUiState` in
`VLESSSettingsScreen.kt:52`.

The casts are valid for the normal in-app route, but a stale saved route or an
invalid profile type creates a synchronous Compose `ClassCastException`.

**Fix:** make the scaffold generic over the concrete state type or use a safe
cast with an error UI and navigation recovery. Add restoration tests for a
deleted/changed profile type.

## Recommended order

1. Fix CR-0 immediately: it is a confirmed main-thread crash on normal Group
   screen creation.
2. Fix CR-1 and CR-2 next: they are reachable from persisted/user-supplied
   state and have no recovery path today.
3. Harden CR-3 before supporting package-change-heavy devices or long scans.
4. Remove the volatile Activity Result state in CR-4.
5. Address CR-5 through CR-7 as defensive hardening alongside Compose and
   navigation tests.
