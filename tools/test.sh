#!/usr/bin/env bash
# The unit tests, the ones that need no device.
set -euo pipefail

exec "$(dirname "${BASH_SOURCE[0]}")/gradle.sh" testDebugUnitTest "$@"
