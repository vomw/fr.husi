#!/usr/bin/env bash

# Each `# renovate:` comment tells Renovate where the value below it comes from;
# the matching custom manager lives in .github/renovate.json.

# renovate: datasource=golang-version depName=go
GO_VERSION="1.27.3"
# renovate: datasource=github-releases depName=rust-lang/rust
RUST_VERSION="1.99.1"
JAVA_VERSION="25"
# NDK version as named by sdkmanager under $ANDROID_HOME/ndk/.
# Renovate has no datasource for it; keep androidApp/build.gradle.kts `ndkVersion` equal.
ANDROID_NDK_VERSION="30.0.16248370"
PROTOC_VERSION="36.1"

# renovate: datasource=github-releases depName=Dreamacro/maxmind-geoip
GEOIP_VERSION="20261012"

# renovate: datasource=github-releases depName=v2fly/domain-list-community
GEOSITE_VERSION="20261019074502"
