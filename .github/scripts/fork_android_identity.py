#!/usr/bin/env python3
"""Namespace a disposable Android CI checkout so it can coexist with Husi."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


PACKAGE_NAME_PATTERN = re.compile(r"^(PACKAGE_NAME=)([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z][a-zA-Z0-9_]*)+)(\r?\n?)$", re.MULTILINE)
APP_NAME_PATTERN = re.compile(r'(<string\s+name="app_name">)([^<]+)(</string>)')
RESOURCE_DIRECTORIES = (
    Path("composeApp/src/androidMain/res"),
    Path("composeApp/src/commonMain/composeResources"),
)


def fail(message: str) -> None:
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(1)


def replace_package_name(text: str, suffix: str) -> tuple[str, str, str]:
    matches = list(PACKAGE_NAME_PATTERN.finditer(text))
    if len(matches) != 1:
        fail("husi.properties must contain exactly one valid PACKAGE_NAME entry")

    match = matches[0]
    original = match.group(2)
    forked = original if original.endswith(f".{suffix}") else f"{original}.{suffix}"
    return f"{text[:match.start(2)]}{forked}{text[match.end(2):]}", original, forked


def replace_app_label(text: str, suffix: str, path: Path) -> tuple[str, str]:
    matches = list(APP_NAME_PATTERN.finditer(text))
    if len(matches) != 1:
        fail(f"{path} must contain exactly one non-empty app_name string")

    match = matches[0]
    label = match.group(2)
    forked_label = label if label.endswith(f".{suffix}") else f"{label}.{suffix}"
    return f"{text[:match.start(2)]}{forked_label}{text[match.end(2):]}", forked_label


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--suffix", required=True, help="Android-ID/label suffix without the leading dot")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not re.fullmatch(r"[a-z][a-z0-9_]*", args.suffix):
        fail("suffix must be a lowercase Android identifier segment")

    root = Path(__file__).resolve().parents[2]
    properties_path = root / "husi.properties"
    manifest_path = root / "androidApp/src/main/AndroidManifest.xml"
    gradle_path = root / "androidApp/build.gradle.kts"
    properties = properties_path.read_text(encoding="utf-8")
    updated_properties, original_id, forked_id = replace_package_name(properties, args.suffix)

    if re.search(rf'^\s*namespace\s*=\s*"{re.escape(forked_id)}"', gradle_path.read_text(encoding="utf-8"), re.MULTILINE):
        fail("the Gradle namespace must remain the source package, not the forked application ID")

    manifest = manifest_path.read_text(encoding="utf-8")
    for authority in ("${applicationId}.cache", "${applicationId}.androidx-startup"):
        if authority not in manifest:
            fail(f"AndroidManifest.xml must derive provider authority {authority} from applicationId")
    if "${applicationId}.permission.SERVICE" not in manifest:
        fail("AndroidManifest.xml must derive the service permission from applicationId")

    label_changes: list[tuple[Path, str]] = []
    resource_defaults: set[Path] = set()
    for directory in RESOURCE_DIRECTORIES:
        resource_dir = root / directory
        if not resource_dir.is_dir():
            fail(f"missing resource directory: {directory}")
        for path in sorted(resource_dir.glob("values*/strings.xml")):
            source = path.read_text(encoding="utf-8")
            if not APP_NAME_PATTERN.search(source):
                continue
            updated, label = replace_app_label(source, args.suffix, path.relative_to(root))
            label_changes.append((path, label))
            if path.parent.name == "values":
                resource_defaults.add(resource_dir)
            if not args.dry_run and updated != source:
                path.write_text(updated, encoding="utf-8")

    if set(root / directory for directory in RESOURCE_DIRECTORIES) != resource_defaults:
        fail("the default Android and shared resources must each define app_name")

    if not args.dry_run and updated_properties != properties:
        properties_path.write_text(updated_properties, encoding="utf-8")

    print(f"Android application ID: {original_id} -> {forked_id}")
    print(f"Updated {len(label_changes)} Android/shared app labels with .{args.suffix}")
    print("Gradle namespace and Kotlin package are intentionally unchanged.")


if __name__ == "__main__":
    main()
