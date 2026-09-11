#!/usr/bin/env bash
# Writes the Firebase configuration the page needs, out of the project itself.
#
#   tools/web-config.sh
#
# The result names the project, and the project is not the repository's
# business — the file is ignored by git, the same way the Android one is. Run
# this once per working copy, and again if the web app is ever re-registered.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT
readonly TARGET="${REPO_ROOT}/web/public/firebase-config.js"

# The one web app of the project. Passed by platform rather than by id, so that
# this keeps working if the app is registered again under a new one.
npx --yes firebase-tools@15.30.0 apps:sdkconfig WEB --json |
    python3 -c '
import json, sys

raw = sys.stdin.read()
data = json.loads(raw[raw.index("{"):])
config = json.loads(data["result"]["fileContents"])

print("// Written by tools/web-config.sh out of the Firebase project itself.")
print("// Not in the repository: it names the project.")
print("export const firebaseConfig = " + json.dumps(config, indent=4) + ";")
' > "${TARGET}"

echo "==> wrote ${TARGET#"${REPO_ROOT}"/}"
