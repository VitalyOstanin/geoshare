#!/usr/bin/env bash
# Writes the Firebase configuration the page needs, out of the project itself.
#
#   tools/web-config.sh
#
# FIREBASE_PROJECT names another project than the working copy's default,
# FIREBASE_SITE the Hosting site the page is served from when it is not named
# after the project, FIREBASE_ACCOUNT the Google account to ask as when it is
# not the firebase command line's default one, and WEB_CONFIG_TARGET another
# file to write; tools/project.sh uses all four.
#
# The result names the project, and the project is not the repository's
# business — the file is ignored by git, the same way the Android one is. Run
# this once per working copy, and again if the web app is ever re-registered.
set -euo pipefail

# The same place every other script here gets the repository root and the
# versions from: a copy of either is a copy that stops matching.
# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly TARGET="${WEB_CONFIG_TARGET:-${REPO_ROOT}/web/public/firebase-config.js}"

project_args=()

if [[ -n "${FIREBASE_PROJECT:-}" ]]; then
    project_args=(--project "${FIREBASE_PROJECT}")
fi

if [[ -n "${FIREBASE_ACCOUNT:-}" ]]; then
    project_args+=(--account "${FIREBASE_ACCOUNT}")
fi

# The one web app of the project. Passed by platform rather than by id, so that
# this keeps working if the app is registered again under a new one.
npx --yes "firebase-tools@${FIREBASE_TOOLS_VERSION}" apps:sdkconfig WEB "${project_args[@]}" --json |
    FIREBASE_SITE="${FIREBASE_SITE:-}" python3 -c '
import json, os, sys

raw = sys.stdin.read()
data = json.loads(raw[raw.index("{"):])
config = json.loads(data["result"]["fileContents"])

# Sign-in goes through the domain the page is served from rather than the
# firebaseapp.com one the project names: a browser that keeps the storage of
# each site apart (Firefox, Safari, Chrome from 115) loses a redirect sign-in
# that passes through a frame on another domain. Hosting serves the sign-in
# handler on every site; the page is on the one named after the project unless
# FIREBASE_SITE says otherwise.
config["authDomain"] = (os.environ["FIREBASE_SITE"] or config["projectId"]) + ".web.app"

print("// Written by tools/web-config.sh out of the Firebase project itself.")
print("// Not in the repository: it names the project.")
print("export const firebaseConfig = " + json.dumps(config, indent=4) + ";")
' > "${TARGET}"

echo "==> wrote ${TARGET#"${REPO_ROOT}"/}"
