#!/usr/bin/env bash

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

restore_file=""
cleanup() {
  if [[ -n "$restore_file" ]]; then
    cp "$restore_file" local.properties
    rm -f "$restore_file"
  fi
}
trap cleanup EXIT

# Android Studio on Windows writes a Windows SDK path to local.properties.
# When the same checkout is used from WSL, translate it for Gradle temporarily.
if [[ -f local.properties ]] && command -v wslpath >/dev/null 2>&1; then
  sdk_path="$(sed -n 's/^sdk\.dir=//p' local.properties | head -n 1)"
  sdk_path="${sdk_path//$'\r'/}"
  sdk_path="${sdk_path//\\:/\:}"
  if [[ "$sdk_path" =~ ^[A-Za-z]:[\\/] ]]; then
    wsl_sdk_path="$(wslpath -u "$sdk_path")"
    if [[ -d "$wsl_sdk_path" ]]; then
      restore_file="$(mktemp)"
      cp local.properties "$restore_file"
      sed -i "s|^sdk\.dir=.*|sdk.dir=$wsl_sdk_path|" local.properties
    fi
  fi
fi

./gradlew "$@"
