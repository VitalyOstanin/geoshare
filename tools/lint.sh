#!/usr/bin/env bash
# Formatting and the Android lint, the pair a change has to pass before it is
# pushed.
set -euo pipefail

exec "$(dirname "${BASH_SOURCE[0]}")/gradle.sh" ktlintCheck lintDebug "$@"
