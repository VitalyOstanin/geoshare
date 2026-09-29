#!/usr/bin/env bash
# Sets up a Firebase project of one's own for geoshare, from this machine.
#
#   tools/new-project.sh create <project-id> [--location <region>] [--name <display name>]
#                               [--sha-from <existing-project-id>]
#   tools/new-project.sh configs <project-id>
#
# Two steps with one step in the Firebase console between them, because the
# console is the only place left that turns on sign-in with Google: the
# provider needs an OAuth client, and Google shut down the API that created
# one outside the console on 19 March 2026.
#
#   1. `create` makes the project, registers the Android app and the page,
#      adds the fingerprints of the keys the application is signed with,
#      creates the database, and deploys the rules and the page, with the
#      application's settings beside it as geoshare.json.
#   2. In the console: Authentication, Sign-in method, Google, Enable, Save.
#      The console makes the OAuth client on its own.
#   3. `configs` fetches the two configuration files, now carrying that client,
#      and writes them next to each other under build/new-project/<id>/.
#
# The configuration files of this working copy are left alone: they name the
# project it already talks to, and a script that quietly pointed a working
# copy at another project would be a surprise at the next build. Copy the two
# files into place when that is what is wanted; the last lines say where.
#
# Needs a signed-in firebase command line (`firebase login`), which is the one
# sign-in in a browser the whole setup asks for.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

# The package the application is built under; the Android app registered here
# has to match it, or sign-in refuses the application.
readonly PACKAGE_NAME="io.github.vitalyostanin.geoshare"

# Where the database of the project this repository started on lives. Any
# region is inside the free plan; this one is close to the family it was made
# for.
readonly DEFAULT_LOCATION="europe-north1"

firebase() {
    npx --yes "firebase-tools@${FIREBASE_TOOLS_VERSION}" "$@"
}

usage() {
    sed -n '4,6p' "${BASH_SOURCE[0]}" | sed 's/^# //' >&2
    exit 2
}

# The id of the one app of a platform, out of the listing's JSON. The listing
# is read rather than the id kept from `apps:create`, so that `configs` works
# on a project whose apps were registered by hand as well.
app_id() {
    local project="$1" platform="$2"

    firebase apps:list "${platform}" --project "${project}" --json |
        python3 -c '
import json, sys

raw = sys.stdin.read()
apps = json.loads(raw[raw.index("{"):])["result"]

if len(apps) != 1:
    sys.exit(f"expected one app, found {len(apps)}")

print(apps[0]["appId"])
'
}

# The SHA-1 and SHA-256 of every key the builds are signed with, as
# `gradle signingReport` prints them — or, with --sha-from, the ones an
# existing project already accepts. The second is the one to use when the
# builds are signed on another machine than this: the debug key lives in the
# build container's volume, and this machine's may not be the one that signed
# the application on the phones.
fingerprints() {
    local from="$1"

    if [[ -n "${from}" ]]; then
        firebase apps:android:sha:list "$(app_id "${from}" ANDROID)" --project "${from}" --json |
            python3 -c '
import json, sys

raw = sys.stdin.read()

for entry in json.loads(raw[raw.index("{"):])["result"]:
    print(entry["shaHash"])
'
        return
    fi

    "${REPO_ROOT}/tools/signing.sh" |
        awk '/^SHA1:|^SHA-256:/ { print $2 }' |
        sort -u
}

# How many apps of a platform the project has.
app_count() {
    local project="$1" platform="$2"

    firebase apps:list "${platform}" --project "${project}" --json |
        python3 -c '
import json, sys

raw = sys.stdin.read()
print(len(json.loads(raw[raw.index("{"):])["result"]))
'
}

# Turns on a Google API in the project. A project made by projects:create
# starts with the Firestore API off, and creating the database fails with 403
# until it is on; the firebase command line has no command for it, so this
# asks Service Usage directly with the token the command line keeps, which
# every command it runs keeps fresh.
enable_api() {
    local project="$1" api="$2"
    local token

    firebase projects:list > /dev/null
    token="$(python3 -c '
import json, os

path = os.path.expanduser("~/.config/configstore/firebase-tools.json")
print(json.load(open(path))["tokens"]["access_token"])
')"

    curl -sf -X POST -H "Authorization: Bearer ${token}" -H "Content-Type: application/json" \
        "https://serviceusage.googleapis.com/v1/projects/${project}/services/${api}:enable" \
        > /dev/null

    # The switch takes a while to reach the service behind it.
    local attempt

    for attempt in $(seq 1 30); do
        if curl -sf -H "Authorization: Bearer ${token}" \
            "https://serviceusage.googleapis.com/v1/projects/${project}/services/${api}" |
            grep -q '"state": "ENABLED"'; then
            return
        fi

        echo "    waiting for ${api} (${attempt})"
        sleep 10
    done

    echo "${api} is still not on" >&2
    exit 1
}

# The Hosting site the page is served from, made if the project has none.
#
# A project made from 15 October 2026 on gets no site of its own: Hosting
# makes one only when something first asks for it, and a deploy to a site that
# is not there fails with "Site Not Found". The site is named after the
# project where that name is free; a subdomain of web.app another project has
# taken already gets the suffix instead.
hosting_site() {
    local project="$1" web_app="$2"
    local site

    site="$(firebase hosting:sites:list --project "${project}" --json |
        python3 -c '
import json, sys

raw = sys.stdin.read()
sites = json.loads(raw[raw.index("{"):])["result"].get("sites", [])
default = [s for s in sites if s.get("type") == "DEFAULT_SITE"]
chosen = (default or sites)[:1]
print(chosen[0]["name"].rsplit("/", 1)[1] if chosen else "")
')"

    if [[ -n "${site}" ]]; then
        echo "    ${site} already there" >&2
        echo "${site}"
        return
    fi

    for site in "${project}" "${project}-geoshare"; do
        if firebase hosting:sites:create "${site}" --app "${web_app}" \
            --project "${project}" >&2; then
            echo "${site}"
            return
        fi
    done

    echo "no free name for the Hosting site of ${project}" >&2
    exit 1
}

# What the application needs to talk to the project, served next to the page
# as geoshare.json. One build serves every family, so the build carries no
# project: the application reads these from the family's site when it is
# pointed at it, and starts Firebase with them. None of it is a secret — the
# same values sit in every copy of google-services.json and firebase-config.js;
# what guards the data is the rules.
app_settings() {
    local project="$1" android="$2" site_id="$3"

    firebase apps:sdkconfig ANDROID "${android}" --project "${project}" --json |
        SITE_ID="${site_id}" python3 -c '
import json, os, sys

raw = sys.stdin.read()
config = json.loads(json.loads(raw[raw.index("{"):])["result"]["fileContents"])
client = config["client"][0]

print(json.dumps({
    "version": 1,
    "projectId": config["project_info"]["project_id"],
    "applicationId": client["client_info"]["mobilesdk_app_id"],
    "apiKey": client["api_key"][0]["current_key"],
    "gcmSenderId": config["project_info"]["project_number"],
    "site": os.environ["SITE_ID"],
}, indent=2))
'
}

# Every step checks what is already there, so a run that stopped half way can
# be started again with the same command.
create() {
    local project="$1" location="$2" name="$3" sha_from="$4"

    echo "==> project ${project}"
    if firebase projects:list --json | grep -q "\"projectId\": \"${project}\""; then
        echo "    already there"
    else
        firebase projects:create "${project}" --display-name "${name}"
    fi

    echo "==> Android app ${PACKAGE_NAME}"
    if [[ "$(app_count "${project}" ANDROID)" -gt 0 ]]; then
        echo "    already there"
    else
        firebase apps:create ANDROID "${name}" --package-name "${PACKAGE_NAME}" \
            --project "${project}"
    fi

    echo "==> page"
    if [[ "$(app_count "${project}" WEB)" -gt 0 ]]; then
        echo "    already there"
    else
        firebase apps:create WEB "${name}" --project "${project}"
    fi

    local android
    android="$(app_id "${project}" ANDROID)"

    echo "==> Hosting site"
    local site_id
    site_id="$(hosting_site "${project}" "$(app_id "${project}" WEB)")"

    # Sign-in with Google checks the application against these; without them
    # the first sign-in on a phone fails with a developer error.
    echo "==> signing key fingerprints"
    local hash
    local hashes
    local known
    hashes="$(fingerprints "${sha_from}")"
    known="$(firebase apps:android:sha:list "${android}" --project "${project}" --json)"

    if [[ -z "${hashes}" ]]; then
        echo "no fingerprints in the signing report; add them in the console later" >&2
    fi

    for hash in ${hashes}; do
        if grep -qi "${hash}" <<< "${known}"; then
            echo "    ${hash:0:8}… already there"
        else
            firebase apps:android:sha:create "${android}" "${hash}" --project "${project}"
        fi
    done

    echo "==> Firestore API"
    enable_api "${project}" firestore.googleapis.com

    echo "==> database in ${location}"
    if firebase firestore:databases:list --project "${project}" --json |
        grep -q '/databases/(default)'; then
        echo "    already there"
    else
        # A freshly switched on API still answers 403 for a few minutes.
        local attempt

        for attempt in $(seq 1 20); do
            if firebase firestore:databases:create "(default)" --location "${location}" \
                --project "${project}"; then
                break
            fi

            [[ "${attempt}" -lt 20 ]] || exit 1
            echo "    the database is not ready to be made yet, again in 30 s (${attempt})"
            sleep 30
        done
    fi

    # The page and the rules go out from a copy of their own under build/: the
    # page in the working tree carries this working copy's firebase-config.js,
    # and deployed as it is, the new project's page would talk to the old
    # project. The copy gets a configuration of the new project's own, a
    # firebase.json pointing at it, and a .firebaserc of its own for the "page"
    # target, which leaves the working copy's .firebaserc alone.
    local out="${REPO_ROOT}/build/new-project/${project}"
    local site="${out}/site"

    echo "==> page configuration for the copy"
    rm -rf "${site}"
    # The same layout as the repository, so firebase.json is taken unchanged.
    mkdir -p "${site}/web"
    cp -r "${REPO_ROOT}/web/public" "${site}/web/public"
    rm -f "${site}/web/public/firebase-config.js"
    cp -r "${REPO_ROOT}/firebase" "${site}/firebase"
    cp "${REPO_ROOT}/firebase.json" "${site}/firebase.json"
    FIREBASE_PROJECT="${project}" FIREBASE_SITE="${site_id}" \
        WEB_CONFIG_TARGET="${site}/web/public/firebase-config.js" \
        "${REPO_ROOT}/tools/web-config.sh"

    echo "==> application settings"
    app_settings "${project}" "${android}" "${site_id}" > "${site}/web/public/geoshare.json"

    echo "==> rules and page"
    (
        cd "${site}"
        firebase target:apply hosting page "${site_id}" --project "${project}"
        firebase deploy --only firestore:rules,hosting --project "${project}"
    )

    cat <<EOF

Done with the part that runs from here. Two steps are left in the consoles:

  1. https://console.firebase.google.com/project/${project}/authentication/providers

     Get started (the first time), then Google, Enable, pick the support
     address, Save. The console makes the OAuth client for the page.

  2. https://console.cloud.google.com/auth/clients?project=${project}

     Open "Web client (auto created by Google Service)", add
     https://${site_id}.web.app/__/auth/handler under Authorized redirect URIs,
     Save. The page signs in through its own domain rather than
     firebaseapp.com (see tools/web-config.sh), and the client made in step 1
     only lists the firebaseapp.com one; without this the page's sign-in ends
     in redirect_uri_mismatch.

Then run:

  tools/new-project.sh configs ${project}
EOF
}

configs() {
    local project="$1"
    local out="${REPO_ROOT}/build/new-project/${project}"

    mkdir -p "${out}"

    echo "==> Android configuration"
    firebase apps:sdkconfig ANDROID "$(app_id "${project}" ANDROID)" --project "${project}" \
        --out "${out}/google-services.json"

    # Without the OAuth client the application builds and then cannot sign
    # in, which is a failure far from its cause; said here instead.
    if ! grep -q '"client_type": 3' "${out}/google-services.json"; then
        echo "google-services.json has no web client yet: is Google sign-in enabled?" >&2
        exit 1
    fi

    echo "==> page configuration"
    (cd "${REPO_ROOT}" && FIREBASE_PROJECT="${project}" \
        WEB_CONFIG_TARGET="${out}/firebase-config.js" "${REPO_ROOT}/tools/web-config.sh")

    cat <<EOF

Written to ${out#"${REPO_ROOT}"/}/. To point this working copy at the new project:

  cp ${out#"${REPO_ROOT}"/}/google-services.json android/app/
  cp ${out#"${REPO_ROOT}"/}/firebase-config.js web/public/
  firebase use ${project}
EOF
}

[[ $# -ge 2 ]] || usage

command="$1"
project="$2"
shift 2

location="${DEFAULT_LOCATION}"
name="geoshare"
sha_from=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --location) location="$2"; shift 2 ;;
        --name) name="$2"; shift 2 ;;
        --sha-from) sha_from="$2"; shift 2 ;;
        *) usage ;;
    esac
done

case "${command}" in
    create) create "${project}" "${location}" "${name}" "${sha_from}" ;;
    configs) configs "${project}" ;;
    *) usage ;;
esac
