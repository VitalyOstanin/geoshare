#!/usr/bin/env bash
# Fails when a package in the npm lock files carries a licence outside the
# allowed list below, or none at all.
#
# The project is MIT, and everything the page and the rules tests pull in is
# a permissive licence, so the list is the set of those, spelled as the
# lock files spell them. A package under a copyleft or unknown licence shows
# up as a new name here and has to be looked at by hand before the list
# grows. Read off the lock files alone, like `npm audit` in CI; no tool is
# downloaded.
set -euo pipefail

cd "$(dirname "$0")/.."

allowed='["0BSD","Apache-2.0","BSD-2-Clause","BSD-3-Clause","ISC","MIT","(MIT OR Apache-2.0)"]'

status=0
for lock in web/package-lock.json firebase/tests/package-lock.json; do
  bad=$(jq -r --argjson allowed "$allowed" '
    .packages | to_entries[] | select(.key != "")
    | select((.value.license // "none") as $l | ($allowed | index($l)) | not)
    | "\(.key): \(.value.license // "none")"' "$lock")
  if [ -n "$bad" ]; then
    echo "$lock: licence outside the allowed list:" >&2
    echo "$bad" >&2
    status=1
  fi
done
exit $status
