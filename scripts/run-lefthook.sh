#!/usr/bin/env sh

set -eu

hook_name="${1:?hook name is required}"
repo_root="$(git rev-parse --show-toplevel)"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    # Git for Windows (including Sublime Merge) can run the tracked hook, but
    # the project toolchain lives in WSL.
    if ! wsl.exe -e bash -lc 'command -v lefthook >/dev/null 2>&1'; then
      echo "Lefthook is required in WSL for repository checks. Install it from https://lefthook.dev/install/." >&2
      exit 1
    fi
    wsl.exe -e bash -lc "cd \"\$(wslpath -u '$repo_root')\" && lefthook run '$hook_name'"
    ;;
  *)
    if ! command -v lefthook >/dev/null 2>&1; then
      echo "Lefthook is required for repository checks. Install it from https://lefthook.dev/install/." >&2
      exit 1
    fi
    exec lefthook run "$hook_name"
    ;;
esac
