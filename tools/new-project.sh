#!/usr/bin/env bash
# Sets up a Firebase project of one's own for geoshare, from this machine.
#
#   tools/new-project.sh create <project-id> [--location <region>] [--name <display name>]
#                               [--sha-from <existing-project-id>] [--json]
#   tools/new-project.sh configs <project-id> [--wait] [--json]
#
# Step by step for whoever runs it: docs/new-project.md; the console step for
# the project owner: docs/console-steps.md.
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
#   3. `configs` checks that a project is ready: the Android app is
#      registered and knows a signing key, and sign-in with Google is on.
#      --wait has it poll for the last one instead of reporting it as a
#      manual step. It also writes firebase-config.js next to it, for a
#      working copy pointed at this project.
#
# --json is for a program or an agent running this rather than somebody at a
# terminal. Both commands keep their running commentary, but on standard
# error instead — the firebase command line's own commentary moves there too,
# under the same --non-interactive that already turns its prompts into
# failures — and add one line of JSON on standard output once they are done:
#
#   {
#     "projectId": "...", "site": "...",
#     "connectLink": "https://<site>.web.app/join",
#     "settingsUrl": "https://<site>.web.app/geoshare.json",
#     "manualSteps": [{"what": "...", "url": "..."}, ...],
#     "done": false
#   }
#
# manualSteps lists what is still left for a human in the console. `create`
# cannot check either console step, so it always lists both: turning on
# sign-in with Google, and the redirect URI its OAuth client needs. `configs`
# can check the first one itself and lists it only while it is still off; the
# redirect URI step stays off its list either way, since nothing here reads it
# back — a done of true from `configs` means sign-in with Google is confirmed
# on, not that the redirect URI was ever set. done is false while any step
# manualSteps lists is outstanding.
#
# The configuration files of this working copy are left alone: they name the
# project it already talks to, and a script that quietly pointed a working
# copy at another project would be a surprise at the next build. `configs`
# writes its copy of the page's configuration under build/new-project/<id>/
# instead; copy it into place when that is what is wanted.
#
# Needs a signed-in firebase command line (`firebase login`, or
# `firebase login --no-localhost` from a shell without a browser of its own,
# such as Google Cloud Shell) — the one sign-in in a browser the whole setup
# asks for. Without it the script fails right away instead of waiting on a
# prompt --non-interactive told the firebase command line not to show.
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
    # --non-interactive is global to the firebase command line and every
    # subcommand honours it, so it goes on every call through here rather
    # than being repeated at each call site: a prompt this script cannot
    # answer becomes a failure instead of a hang.
    npx --yes "firebase-tools@${FIREBASE_TOOLS_VERSION}" --non-interactive "$@"
}

usage() {
    sed -n '4,6p' "${BASH_SOURCE[0]}" | sed 's/^# //' >&2
    exit 2
}

# Fails fast, with a message naming the fix, rather than letting the first
# call below run into "have you run firebase login?" from somewhere deep in a
# helper function. login:list reads the local token file only, so this costs
# nothing on the network.
require_login() {
    if ! firebase login:list --json | grep -q '"result"'; then
        echo "not logged in to the firebase command line; run \`firebase login\`" \
            "(or \`firebase login --no-localhost\` in Cloud Shell) first" >&2
        exit 1
    fi
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

# The fingerprints the Android app already knows, straight out of its own
# configuration: sign-in checks the calling application against these, so an
# app with none of them yet cannot sign anybody in. `configs` reads this to
# confirm they are there; `create` writes them below instead of reading them.
android_fingerprints() {
    local project="$1" android="$2"

    firebase apps:sdkconfig ANDROID "${android}" --project "${project}" --json |
        python3 -c '
import json, sys

raw = sys.stdin.read()
config = json.loads(json.loads(raw[raw.index("{"):])["result"]["fileContents"])
client = config["client"][0]

for entry in client.get("oauth_client", []):
    if entry.get("client_type") == 1:
        print(entry["android_info"]["certificate_hash"])
'
}

# The Firebase command line's own access token, kept fresh at
# ~/.config/configstore/firebase-tools.json by every command it runs; the
# projects:list call is only there to make it run one before the token is
# read. Shared by enable_api() and google_sign_in_enabled() below, the two
# places this script asks a Google API directly rather than through the
# firebase command line, which has no command for either.
firebase_access_token() {
    firebase projects:list > /dev/null
    python3 -c '
import json, os

path = os.path.expanduser("~/.config/configstore/firebase-tools.json")
print(json.load(open(path))["tokens"]["access_token"])
'
}

# Turns on a Google API in the project. A project made by projects:create
# starts with the Firestore API off, and creating the database fails with 403
# until it is on; the firebase command line has no command for it, so this
# asks Service Usage directly instead.
enable_api() {
    local project="$1" api="$2"
    local token

    token="$(firebase_access_token)"

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

# Whether the Google provider is already on in the project, read from the
# Identity Toolkit admin API the same way enable_api() above reads Service
# Usage: the firebase command line has no command for this either, and a
# phone would otherwise only find out by trying to sign in. A project where
# the provider was never touched in the console answers with an error rather
# than a plain "enabled": false, so only a 404 — no configuration yet — is
# kept quiet; anything else is worth a line on standard error, since it might
# be a token or permission problem rather than the provider being off.
google_sign_in_enabled() {
    local project="$1"
    local token url response status body enabled

    token="$(firebase_access_token)"
    url="https://identitytoolkit.googleapis.com/admin/v2/projects/${project}"
    url="${url}/defaultSupportedIdpConfigs/google.com"
    response="$(curl -s -w $'\n%{http_code}' -H "Authorization: Bearer ${token}" "${url}")"
    status="${response##*$'\n'}"
    body="${response%$'\n'*}"

    if [[ "${status}" != "200" ]]; then
        if [[ "${status}" != "404" ]]; then
            echo "checking sign-in with Google in ${project}: HTTP ${status}" >&2
        fi
        return 1
    fi

    enabled="$(python3 -c '
import json, sys

# JSON leaves out a false boolean, so a provider that was switched off
# again comes back without the field.
print(json.loads(sys.stdin.read()).get("enabled", False))
' <<< "${body}")"

    [[ "${enabled}" == "True" ]]
}

# The Hosting site already made for the project, empty if it has none yet.
# Split out of hosting_site() so that `configs` can ask the same question
# without the power to create one.
existing_site() {
    local project="$1"

    firebase hosting:sites:list --project "${project}" --json |
        python3 -c '
import json, sys

raw = sys.stdin.read()
sites = json.loads(raw[raw.index("{"):])["result"].get("sites", [])
default = [s for s in sites if s.get("type") == "DEFAULT_SITE"]
chosen = (default or sites)[:1]
print(chosen[0]["name"].rsplit("/", 1)[1] if chosen else "")
'
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

    site="$(existing_site "${project}")"

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
# as geoshare.json. One build serves every group, so the build carries no
# project: the application reads these from the group's site when it is
# pointed at it, and starts Firebase with them. None of it is a secret — the
# page's firebase-config.js on the same site carries the same kind of values;
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

# The link the project owner opens on their own phone to connect the
# application, plus a QR code of it when qrencode is on PATH — scanning one
# beats typing a *.web.app address on a phone's keyboard. ANSIUTF8 sets its
# black and white with its own escape codes rather than the terminal's, so
# the code reads the same on a light or a dark terminal; plain UTF8 borrows
# the terminal's own colours and comes out inverted on a dark one.
print_connect_link() {
    local site="$1"

    echo
    echo "Connect link: https://${site}.web.app/join"

    if command -v qrencode > /dev/null; then
        qrencode -t ANSIUTF8 "https://${site}.web.app/join"
    else
        echo "(install qrencode to also print this as a QR code)"
    fi
}

# The manual steps left after `create`, and the JSON --json ends with once it
# is done. Built from the same two console steps the human-readable message
# below spells out, so the two never drift apart.
create_result_json() {
    local project="$1" site="$2"

    PROJECT="${project}" SITE="${site}" python3 -c '
import json, os

project = os.environ["PROJECT"]
site = os.environ["SITE"]

print(json.dumps({
    "projectId": project,
    "site": site,
    "connectLink": f"https://{site}.web.app/join",
    "settingsUrl": f"https://{site}.web.app/geoshare.json",
    "manualSteps": [
        {
            "what": "Authentication, Sign-in method, Google, Enable, pick a "
                    "support address, Save",
            "url": f"https://console.firebase.google.com/project/{project}"
                   "/authentication/providers",
        },
        {
            "what": "Open \"Web client (auto created by Google Service)\", "
                    f"add https://{site}.web.app/__/auth/handler under "
                    "Authorized redirect URIs, Save",
            "url": f"https://console.cloud.google.com/auth/clients?project={project}",
        },
    ],
    "done": False,
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

    echo
    echo "Once those two steps are done, phones can connect through:"
    print_connect_link "${site_id}"

    if (( json_mode )); then
        create_result_json "${project}" "${site_id}" >&3
    fi
}

configs() {
    local project="$1" wait_google="$2"
    local out="${REPO_ROOT}/build/new-project/${project}"

    echo "==> Android app"
    if [[ "$(app_count "${project}" ANDROID)" -eq 0 ]]; then
        echo "no Android app registered in ${project} yet; run \`create\` first" >&2
        exit 1
    fi

    local android
    android="$(app_id "${project}" ANDROID)"
    local hashes
    hashes="$(android_fingerprints "${project}" "${android}")"

    if [[ -z "${hashes}" ]]; then
        echo "the Android app in ${project} has no signing key fingerprint yet;" \
            "run \`create\` again, or add one in the console" >&2
        exit 1
    fi
    echo "    registered, $(wc -l <<< "${hashes}") signing key(s) known"

    echo "==> Hosting site"
    local site_id
    site_id="$(existing_site "${project}")"

    if [[ -z "${site_id}" ]]; then
        echo "no Hosting site in ${project} yet; run \`create\` first" >&2
        exit 1
    fi
    echo "    ${site_id}"

    echo "==> page configuration"
    mkdir -p "${out}"
    (cd "${REPO_ROOT}" && FIREBASE_PROJECT="${project}" FIREBASE_SITE="${site_id}" \
        WEB_CONFIG_TARGET="${out}/firebase-config.js" "${REPO_ROOT}/tools/web-config.sh")

    echo "==> sign-in with Google"

    local google_on=0

    if google_sign_in_enabled "${project}"; then
        google_on=1
        echo "    already on"
    elif (( wait_google )); then
        echo "    not on yet, waiting up to 20 minutes"

        local attempt

        for attempt in $(seq 1 80); do
            sleep 15

            if google_sign_in_enabled "${project}"; then
                google_on=1
                break
            fi

            echo "    still waiting (${attempt})"
        done

        if (( ! google_on )); then
            echo "sign-in with Google is still off in ${project} after 20 minutes" >&2
            exit 1
        fi

        echo "    on now"
    else
        echo "    not on yet"
    fi

    if (( google_on )); then
        cat <<EOF

${project} is ready to sign phones in: the Android app is registered and knows
a signing key, sign-in with Google is on, and the page serves from
https://${site_id}.web.app/. Written to ${out#"${REPO_ROOT}"/}/firebase-config.js,
for a working copy pointed at this project rather than the one it already
talks to.
EOF

        print_connect_link "${site_id}"
    else
        cat <<EOF

${project} still needs one step before it can sign phones in:

  https://console.firebase.google.com/project/${project}/authentication/providers

  Authentication, Sign-in method, Google, Enable, pick a support address,
  Save. Then run this command again, or with --wait to have it wait here
  instead.
EOF
    fi

    # done here means the one thing this function can check for itself, sign-in
    # with Google, is confirmed on — not that every console step ever happened.
    # The redirect URI step from create's second console link is never read
    # back, by this function or anything else, so it stays out of manualSteps
    # and out of done either way.
    if (( json_mode )); then
        PROJECT="${project}" SITE="${site_id}" GOOGLE_ON="${google_on}" python3 -c '
import json, os

project = os.environ["PROJECT"]
site = os.environ["SITE"]
google_on = os.environ["GOOGLE_ON"] == "1"

manual_steps = [] if google_on else [{
    "what": "Authentication, Sign-in method, Google, Enable, pick a "
            "support address, Save",
    "url": f"https://console.firebase.google.com/project/{project}"
           "/authentication/providers",
}]

print(json.dumps({
    "projectId": project,
    "site": site,
    "connectLink": f"https://{site}.web.app/join",
    "settingsUrl": f"https://{site}.web.app/geoshare.json",
    "manualSteps": manual_steps,
    "done": google_on,
}, indent=2))
' >&3
    fi
}

[[ $# -ge 2 ]] || usage

command="$1"
project="$2"
shift 2

location="${DEFAULT_LOCATION}"
name="geoshare"
sha_from=""
json_mode=0
wait_google=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --location) location="$2"; shift 2 ;;
        --name) name="$2"; shift 2 ;;
        --sha-from) sha_from="$2"; shift 2 ;;
        --json) json_mode=1; shift ;;
        --wait) wait_google=1; shift ;;
        *) usage ;;
    esac
done

# fd 3 keeps the real standard output for the one line --json promises;
# everything else this script or the firebase command line would otherwise
# print moves to standard error by way of fd 1, which now points there.
# Command substitution and pipes are unaffected: `$(...)` and `a | b` each open
# a fresh pipe for the writer's standard output regardless of what it was
# redirected to beforehand, which is how the helper functions above keep
# working whether or not this redirect is in effect.
if (( json_mode )); then
    exec 3>&1 1>&2
fi

case "${command}" in
    create) require_login; create "${project}" "${location}" "${name}" "${sha_from}" ;;
    configs) require_login; configs "${project}" "${wait_google}" ;;
    *) usage ;;
esac
