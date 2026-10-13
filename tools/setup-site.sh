#!/usr/bin/env bash
# Builds the site of the setup page (ADR-0033) into build/setup-site.
#
#   tools/setup-site.sh --client-id <id> [--sha <fingerprint>...]
#
# The setup page deploys to a group's site the files of a group's page, so the
# site holds them under group/ beside the setup page, which is its index:
#
#   setup-files.json  the paths a group's site gets, and the hosting section of
#                     firebase.json for the version (rewrites and headers);
#   setup-config.json the web OAuth client, the package and the fingerprints
#                     the group's Android app is registered with, the place of
#                     the database;
#   firestore.rules   the rules the page deploys.
#
# The site's own firebase.json is written beside them, with a policy that lets
# the setup page load Google Identity Services and talk to the APIs it calls.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly OUT="${REPO_ROOT}/build/setup-site"
readonly PACKAGE_NAME="io.github.vitalyostanin.geoshare"
readonly LOCATION="europe-north1"

client_id=""
sha_given=()

while (( $# )); do
    case "$1" in
        --client-id) client_id="$2"; shift 2 ;;
        --sha) sha_given+=("$2"); shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [[ -z "${client_id}" ]]; then
    echo "usage: tools/setup-site.sh --client-id <id> [--sha <fingerprint>...]" >&2
    exit 2
fi

# Without --sha, the projects the page sets up accept the published builds.
if (( ${#sha_given[@]} == 0 )); then
    mapfile -t sha_given < <(grep -v '^#' "${REPO_ROOT}/android/release-fingerprints.txt")
fi

echo "==> building ${OUT#"${REPO_ROOT}"/}" >&2
rm -rf "${OUT}"
mkdir -p "${OUT}/public/group"
cp -r "${REPO_ROOT}/web/public/." "${OUT}/public/group/"
# A working copy's own project settings are not the setup page's business, and
# firebase.json keeps the example out of a group's site.
rm -f "${OUT}/public/group/firebase-config.js" "${OUT}/public/group/geoshare.json" \
    "${OUT}/public/group/setup-config.example.json" "${OUT}/public/group/firebase-config.example.js"
# The setup page needs the same modules and styles, and is the site's index.
cp -r "${OUT}/public/group/." "${OUT}/public/"
rm -r "${OUT}/public/group/setup.html" "${OUT}/public/group/setup-privacy.html" \
    "${OUT}/public/group/setup-app.js" \
    "${OUT}/public/group/lib/setup-project.js" "${OUT}/public/group/lib/setup-hosting.js" \
    "${OUT}/public/group/lib/qr-svg.js" "${OUT}/public/group/lib/vendor/qr-"*
cp "${OUT}/public/setup.html" "${OUT}/public/index.html"
cp "${REPO_ROOT}/firebase/firestore.rules" "${OUT}/public/firestore.rules"

SETUP_CLIENT_ID="${client_id}" SETUP_PACKAGE="${PACKAGE_NAME}" SETUP_LOCATION="${LOCATION}" \
    python3 - "${OUT}" "${REPO_ROOT}/firebase.json" "${sha_given[@]}" <<'EOF'
import json, os, pathlib, sys

out = pathlib.Path(sys.argv[1])
public = out / "public"
hosting = json.loads(pathlib.Path(sys.argv[2]).read_text())["hosting"]
fingerprints = sys.argv[3:]

# Everything a group's page is made of; the setup page fetches each from
# group/<path> and deploys it as <path>.
group = public / "group"
paths = sorted(
    "/" + str(path.relative_to(group))
    for path in group.rglob("*")
    if path.is_file() and not any(part.startswith(".") for part in path.relative_to(group).parts)
)

(public / "setup-files.json").write_text(json.dumps({"paths": paths, "hosting": hosting}, indent=2) + "\n")
(public / "setup-config.json").write_text(json.dumps({
    "clientId": os.environ["SETUP_CLIENT_ID"],
    "packageName": os.environ["SETUP_PACKAGE"],
    "location": os.environ["SETUP_LOCATION"],
    "fingerprints": fingerprints,
    "consoleUrl": "https://console.firebase.google.com/",
}, indent=2) + "\n")

# The group's policy, widened for the setup page alone: Google Identity
# Services is served from accounts.google.com, and the page calls the
# Firebase, Firestore, Service Usage, Rules and Hosting APIs, the upload host
# of Hosting among them.
policy = next(h["value"] for rule in hosting["headers"] for h in rule["headers"]
              if h["key"] == "Content-Security-Policy")
policy = policy.replace("script-src 'self'", "script-src 'self' https://accounts.google.com", 1)
policy = policy.replace("style-src 'self'", "style-src 'self' https://accounts.google.com", 1)
policy = policy.replace("connect-src 'self'", "connect-src 'self' https://accounts.google.com", 1)
site_hosting = json.loads(json.dumps(hosting))
site_hosting.pop("target", None)
site_hosting["public"] = "public"
for rule in site_hosting["headers"]:
    for header in rule["headers"]:
        if header["key"] == "Content-Security-Policy":
            header["value"] = policy
# /join belongs to a group's site, not to this one.
site_hosting["rewrites"] = []
(out / "firebase.json").write_text(json.dumps({"hosting": site_hosting}, indent=2) + "\n")

print(f"==> {len(paths)} files for a group's site", file=sys.stderr)
EOF

echo "==> deploy: firebase deploy --only hosting --project geoshare-setup --config ${OUT#"${REPO_ROOT}"/}/firebase.json" >&2
