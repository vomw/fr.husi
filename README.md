# Husi (虎兕)

Husi is an Android proxy-set-based VPN and proxy client.

## Build

Read [CONTRIBUTING.md](CONTRIBUTING.md), then initialize submodules and Android assets:

```shell
./run lib source
make assets
```

The Android build requires JDK 25, Android NDK `30.0.16248370`, and the Go version pinned in
[`buildScript/init/version.sh`](buildScript/init/version.sh). Build the gomobile core and a release APK:

```shell
make libcore_android
make aboutlibraries
make apk
```

The APKs are written under `androidApp/build/outputs/apk/`. Use `make apk_debug` for a debug build.

### Signing locally

Put `release.keystore` at the repository root and write these values to `local.properties`:

```properties
KEYSTORE_PASS=<keystore password>
ALIAS_NAME=<key alias>
ALIAS_PASS=<key password>
```

### GitHub Actions production APKs

Run **Android production APKs** manually to create signed `FossRelease` APKs for `arm64-v8a`,
`armeabi-v7a`, `x86_64`, and `x86`. Each artifact filename is:

```
<application-id>-<version-name>-<version-code>-<abi>.apk
```

Configure these repository secrets:

- `ANDROID_KEYSTORE_BASE64`: single-line Base64 of `release.keystore`. In PowerShell:

  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Clipboard
  ```

- `LOCAL_PROPERTIES`: the literal `KEYSTORE_PASS`, `ALIAS_NAME`, and `ALIAS_PASS` properties above.

Do not commit a keystore or passwords. The workflow configures Android SDK, NDK, Java, Go, and route
asset versions itself.

### Plugins

Android plugin APKs are built with:

```shell
make plugin PLUGIN=<hysteria2|juicity|naive|mieru|shadowquic>
```

## Install

[![Get it on Obtainium](docs/assets/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22fr.husi%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fxchacha20-poly1305%2Fhusi%22%2C%22author%22%3A%22xchacha20-poly1305%22%2C%22name%22%3A%22husi%22%7D)

## Contributing and localization

See [CONTRIBUTING.md](CONTRIBUTING.md). Translations are managed through
[Weblate](https://hosted.weblate.org/engage/husi/).
