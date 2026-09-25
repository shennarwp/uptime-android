#!/usr/bin/env bash

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

git config core.hooksPath .githooks

if ! command -v lefthook >/dev/null 2>&1; then
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
      if ! wsl.exe -e bash -lc 'command -v lefthook >/dev/null 2>&1'; then
        echo "Install Lefthook in WSL before enabling hooks: https://lefthook.dev/install/" >&2
        exit 1
      fi
      ;;
    *)
      echo "Install Lefthook before enabling hooks: https://lefthook.dev/install/" >&2
      exit 1
      ;;
  esac
fi

chmod +x .githooks/pre-commit .githooks/pre-push scripts/run-lefthook.sh
echo "Git hooks enabled from .githooks"
