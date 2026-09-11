#!/usr/bin/env bash
# Rewrites what ktlint can fix on its own. What it cannot is left for the
# lint run to report.
set -euo pipefail

exec "$(dirname "${BASH_SOURCE[0]}")/gradle.sh" ktlintFormat "$@"
