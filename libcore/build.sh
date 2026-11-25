#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -ne 1 ] || [ "$1" != "--android" ]; then
    echo "Usage: ./build.sh --android" >&2
    exit 1
fi

readonly ANDROID_MIN_API=24
readonly BUILD_TAGS="with_quic,with_wireguard,with_openconnect,with_openvpn,with_tailscale,with_utls,with_naive_outbound,badlinkname,tfogo_checklinkname0"

# gomobile only recognizes SDK platforms named android-<N>: an SDK that holds
# nothing but minor releases such as android-37.2 looks empty to it. Build a
# shadow SDK that links every entry of the real one, except that platforms/
# holds only the newest platform at or above the minimum API, under its major
# number. Prints the shadow SDK path.
make_gomobile_android_home() {
    local sdk="${ANDROID_HOME:-}"
    local best_dir=""
    local best_version=""
    local platform_dir
    local version
    local entry
    local shadow

    for platform_dir in "$sdk"/platforms/android-*; do
        [ -f "$platform_dir/android.jar" ] || continue
        version="${platform_dir##*/android-}"
        [[ "$version" =~ ^[0-9]+(\.[0-9]+)?$ ]] || continue
        [ "${version%%.*}" -ge "$ANDROID_MIN_API" ] || continue
        if [ -z "$best_version" ] || [ "$(printf '%s\n%s\n' "$best_version" "$version" | sort -V | tail -n 1)" = "$version" ]; then
            best_dir="$platform_dir"
            best_version="$version"
        fi
    done
    if [ -z "$best_dir" ]; then
        echo "No Android SDK platform with API >= $ANDROID_MIN_API under ${sdk:-\$ANDROID_HOME}/platforms" >&2
        return 1
    fi

    shadow="$(mktemp -d)"
    for entry in "$sdk"/*; do
        [ "${entry##*/}" = "platforms" ] && continue
        ln -s "$entry" "$shadow/${entry##*/}"
    done
    mkdir "$shadow/platforms"
    ln -s "$best_dir" "$shadow/platforms/android-${best_version%%.*}"
    echo "$shadow"
}

print_sha256() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1"
    else
        shasum -a 256 "$1"
    fi
}

go install tool

box_version="$(go list -m -f '{{.Version}}' github.com/sagernet/sing-box)"
if [ -z "$box_version" ]; then
    echo "Unable to determine sing-box version from go.mod" >&2
    exit 1
fi

readonly COMMON_LDFLAGS="-X github.com/sagernet/sing-box/constant.Version=${box_version} -s -w -buildid= -checklinkname=0"

rm -f libcore.aar libcore-sources.jar
gomobile_android_home="$(make_gomobile_android_home)"
trap 'rm -rf "${gomobile_android_home:?}"' EXIT

ANDROID_HOME="$gomobile_android_home" gomobile bind \
    -target=android \
    -androidapi "$ANDROID_MIN_API" \
    -v \
    -trimpath \
    -buildvcs=false \
    -javapkg=fr.husi \
    -ldflags="$COMMON_LDFLAGS" \
    -tags="$BUILD_TAGS" \
    .

readonly project_dir="../composeApp/libs"
mkdir -p "$project_dir"
cp -f libcore.aar "$project_dir"
echo ">> Installed $(realpath "$project_dir/libcore.aar")"
print_sha256 libcore.aar
