#!/usr/bin/env bash
# Writes the Firebase configuration the page needs, out of the project itself.
#
#   tools/web-config.sh
#
# The result names the project, and the project is not the repository's
# business — the file is ignored by git, the same way the Android one is. Run
# this once per working copy, and again if the web app is ever re-registered.
set -euo pipefail

# The same place every other script here gets the repository root and the
# versions from: a copy of either is a copy that stops matching.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly TARGET="${REPO_ROOT}/web/public/firebase-config.js"

# The one web app of the project. Passed by platform rather than by id, so that
# this keeps working if the app is registered again under a new one.
npx --yes "firebase-tools@${FIREBASE_TOOLS_VERSION}" apps:sdkconfig WEB --json |
    python3 -c '
import json, sys

raw = sys.stdin.read()
data = json.loads(raw[raw.index("{"):])
config = json.loads(data["result"]["fileContents"])

# Sign-in goes through the domain the page is served from rather than the
# firebaseapp.com one the project names: a browser that keeps the storage of
# each site apart (Firefox, Safari, Chrome from 115) loses a redirect sign-in
# that passes through a frame on another domain. The page is the default
# Hosting site of the project, and Hosting serves the sign-in handler there.
config["authDomain"] = config["projectId"] + ".web.app"

print("// Written by tools/web-config.sh out of the Firebase project itself.")
print("// Not in the repository: it names the project.")
print("export const firebaseConfig = " + json.dumps(config, indent=4) + ";")
' > "${TARGET}"

echo "==> wrote ${TARGET#"${REPO_ROOT}"/}"
