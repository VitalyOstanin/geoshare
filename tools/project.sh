#!/usr/bin/env bash
# A group's own Firebase project for geoshare, from this machine: making it,
# looking at it, keeping it up to date and removing it.
#
#   tools/project.sh create <project-id> [--location <region>] [--name <display name>]
#                           [--sha-from <existing-project-id> | --sha <fingerprint>...] [--json]
#   tools/project.sh show <project-id> [--wait] [--usage] [--days <n>] [--json]
#   tools/project.sh update <project-id> [--json]
#   tools/project.sh delete <project-id> --yes [--json]
#   any of them with [--account <google-address>]
#
# The project is named on every run, so one machine looks after several
# projects — a family's, a trip's, one to try things on — without a working
# copy per project. --account picks which of the Google accounts the firebase
# command line is signed in to does the asking (`firebase login:add` signs in
# another one); without it the command line's default account does, as it
# always has.
#
# Step by step for whoever runs it: docs/new-project.md; the console step for
# the project owner: docs/console-steps.md.
#
# Making a project takes two runs with one step in the Firebase console
# between them, because the console is the only place left that turns on
# sign-in with Google: the provider needs an OAuth client, and Google shut
# down the API that created one outside the console on 19 March 2026.
#
#   1. `create` makes the project, registers the Android app and the page,
#      adds the fingerprints of the keys the application is signed with,
#      creates the database, and deploys the rules and the page, with the
#      application's settings beside it as geoshare.json.
#   2. In the console: Authentication, Sign-in method, Google, Enable, Save.
#      The console makes the OAuth client on its own.
#   3. `show` checks that a project is ready: the Android app is registered
#      and knows a signing key, and sign-in with Google is on. --wait has it
#      poll for the last one instead of reporting it as a manual step. It also
#      writes firebase-config.js under build/project/<id>/, for a working copy
#      pointed at this project. --usage adds how much of the free plan the
#      project has used today, and --days <n> the n quota days before it.
#
# `update` deploys this working copy's rules and page to a project that is
# already there, with its settings file; `create` run again does the same
# along with everything else it skips. `delete` asks Google to shut the
# project down: it is unusable at once and gone for good after 30 days, and
# can be restored until then. It does nothing without --yes.
#
# --json is for a program or an agent running this rather than somebody at a
# terminal. Every command keeps its running commentary, but on standard error
# instead — the firebase command line's own commentary moves there too, under
# the same --non-interactive that already turns its prompts into failures —
# and adds its result as JSON on standard output once it is done. For
# `create` and `show`:
#
#   {
#     "projectId": "...", "site": "...",
#     "connectLink": "https://<site>.web.app/join",
#     "settingsUrl": "https://<site>.web.app/geoshare.json",
#     "manualSteps": [{"what": "...", "url": "..."}, ...],
#     "done": false
#   }
#
# with "usage" added by `show --usage`. manualSteps lists what is still left
# for a human in the console. `create` cannot check either console step, so
# it always lists both: turning on sign-in with Google, and the redirect URI
# its OAuth client needs. `show` can check the first one itself and lists it
# only while it is still off; the redirect URI step stays off its list either
# way, since nothing here reads it back — a done of true from `show` means
# sign-in with Google is confirmed on, not that the redirect URI was ever
# set. done is false while any step manualSteps lists is outstanding.
#
# The configuration files of this working copy are left alone: they name the
# project it already talks to, and a script that quietly pointed a working
# copy at another project would be a surprise at the next build.
#
# Needs a signed-in firebase command line (`firebase login`, or
# `firebase login --no-localhost` from a shell without a browser of its own,
# such as Google Cloud Shell) — the one sign-in in a browser the whole setup
# asks for. Without it the script fails right away instead of waiting on a
# prompt --non-interactive told the firebase command line not to show.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

# npm's own notices ("npm notice run npx") on every start of the firebase
# command line, here and in tools/web-config.sh, say nothing to whoever reads
# the output.
export npm_config_loglevel=error

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
    npx --yes "firebase-tools@${FIREBASE_TOOLS_VERSION}" --non-interactive "${account_args[@]}" "$@"
}

usage() {
    sed -n '5,10p' "${BASH_SOURCE[0]}" | sed 's/^# //' >&2
    exit 2
}

# Fails fast, with a message naming the fix, rather than letting the first
# call below run into "have you run firebase login?" from somewhere deep in a
# helper function. login:list reads the local token file only, so this costs
# nothing on the network.
require_login() {
    # Asking for the projects is both the check and the refresh: it fails when
    # nobody is signed in or the sign-in was revoked, and on success it leaves
    # a fresh access token in the firebase command line's own file.
    if firebase projects:list > /dev/null 2>&1; then
        remember_access_token
        return
    fi

    local reason="nobody is signed in to the firebase command line"
    local login=(login)

    if [[ -n "${account}" ]]; then
        # An account other than the default one is signed in on its own with
        # login:add, and signed in again the same way once it stops working.
        reason="the firebase command line has no working sign-in for ${account}"
        login=(login:add "${account}")
    elif firebase login:list --json 2> /dev/null | grep -q '"result"'; then
        reason="the firebase command line's sign-in no longer works"
        login+=(--reauth)
    fi

    # Google Cloud Shell has no browser of its own to come back to.
    if [[ -n "${CLOUD_SHELL:-}" ]]; then
        login+=(--no-localhost)
    fi

    # Signing in needs a person at a browser. Somebody at a terminal is helped
    # through it here and now; a program reading --json gets the reason and
    # the command instead, since nobody would see the page it opens.
    if (( json_mode )) || [[ ! -t 0 || ! -t 2 ]]; then
        echo "${reason}; run \`npx firebase-tools@${FIREBASE_TOOLS_VERSION} ${login[*]}\`" \
            "as the project owner, then this again" >&2
        exit 1
    fi

    echo "${reason}. Signing in now: a page in the browser asks for the Google" \
        "account that owns the project." >&2
    npx --yes "firebase-tools@${FIREBASE_TOOLS_VERSION}" "${login[@]}"

    if ! firebase projects:list > /dev/null 2>&1; then
        echo "still not signed in; nothing was changed" >&2
        exit 1
    fi

    remember_access_token
}
# The id of the one app of a platform, out of the listing's JSON. The listing
# is read rather than the id kept from `apps:create`, so that `show` works
# on a project whose apps were registered by hand as well.
app_id() {
    local project="$1" platform="$2"
    local ids

    ids="$(apps_of "${project}" "${platform}")"

    if [[ "$(grep -c . <<< "${ids}" || true)" -ne 1 ]]; then
        echo "expected one ${platform} app in ${project}, found:" \
            "$(grep -c . <<< "${ids}" || true)" >&2
        exit 1
    fi

    echo "${ids}"
}
# The SHA-1 and SHA-256 of every key the builds are signed with, as
# `gradle signingReport` prints them — or, with --sha-from, the ones an
# existing project already accepts. The second is the one to use when the
# builds are signed on another machine than this: the debug key lives in the
# build container's volume, and this machine's may not be the one that signed
# the application on the phones.
#
# --sha names them outright, once per fingerprint, for a project made under
# another account than the one that can read the project they are known in.
fingerprints() {
    local from="$1"

    if (( ${#sha_given[@]} )); then
        printf '%s\n' "${sha_given[@]}"
        return
    fi

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

    apps_of "${project}" "${platform}" | grep -c . || true
}

# The app ids of one platform, one per line, from the Firebase Management API.
apps_of() {
    local project="$1" platform="$2"
    local kind="androidApps"

    [[ "${platform}" == "WEB" ]] && kind="webApps"

    api_get "https://firebase.googleapis.com/v1beta1/projects/${project}/${kind}" |
        python3 -c '
import json, sys

for app in json.load(sys.stdin).get("apps", []):
    print(app["appId"])
'
}
# The fingerprints the Android app already knows: sign-in checks the calling
# application against these, so an app with none of them yet cannot sign
# anybody in. `show` reads this to confirm they are there; `create` writes
# them below instead of reading them.
#
# Read from the app's own list rather than from the Android OAuth client in
# its configuration: that client exists for one pair of package and key across
# all of Google, so in every project but the first the key is registered and
# the client never appears.
android_fingerprints() {
    local project="$1" android="$2"

    api_get "https://firebase.googleapis.com/v1beta1/projects/${project}/androidApps/${android}/sha" |
        python3 -c '
import json, sys

for entry in json.load(sys.stdin).get("certificates", []):
    print(entry["shaHash"])
'
}

# The Firebase command line's own access token, kept fresh at
# ~/.config/configstore/firebase-tools.json by every command it runs, and
# remembered after the first read. Every Google API this script asks
# directly — for what the firebase command line has no command for, and for
# the readings `show` makes many of — goes through it.
firebase_access_token() {
    # An access token lasts an hour; one older than three quarters of that is
    # refreshed the same way require_login() got it, so that a long `create`
    # or a 20-minute --wait does not run out halfway.
    if [[ -z "${ACCESS_TOKEN:-}" ]] || (( SECONDS - ACCESS_TOKEN_AT > 2700 )); then
        firebase projects:list > /dev/null
        remember_access_token
    fi

    echo "${ACCESS_TOKEN}"
}

# Reads the access token the firebase command line has just refreshed, once,
# so that every Google API asked below uses it without starting the command
# line again: each start of it costs seconds before any work is done.
remember_access_token() {
    ACCESS_TOKEN="$(ACCOUNT="${account}" python3 -c '
import json, os

path = os.path.expanduser("~/.config/configstore/firebase-tools.json")
store = json.load(open(path))
wanted = os.environ["ACCOUNT"].lower()

# The default account sits at the top of the file, every one added with
# login:add in additionalAccounts beside it, each with tokens of its own.
accounts = [store] + store.get("additionalAccounts", [])
match = [a for a in accounts if not wanted or a["user"]["email"].lower() == wanted]
print(match[0]["tokens"]["access_token"])
')"
    ACCESS_TOKEN_AT="${SECONDS}"
}

# Stops with a message naming the account when the project cannot be reached:
# it does not exist, or the account asking is not one of its members. Without
# this every later reading fails on its own, and the first one to say anything
# says something beside the point.
require_access() {
    local project="$1"

    if api_get "https://firebase.googleapis.com/v1beta1/projects/${project}" > /dev/null; then
        return
    fi

    local who="${account}"

    [[ -n "${who}" ]] || who="the firebase command line's default account"

    echo "${who} cannot reach" \
        "${project}: there is no such project, or that account has no access to it" \
        "(--account picks another)" >&2
    exit 1
}

# The authorization header for curl, read by it from a file descriptor: a
# token on curl's command line is visible to every user of the machine in the
# list of processes, and printf is a builtin that runs as no process at all.
bearer() {
    printf 'Authorization: Bearer %s\n' "$1"
}

# GETs a Google API with the remembered token and prints the body. Reading
# through the APIs the firebase command line itself calls takes a fraction of
# a second; the same through the command line takes several.
api_get() {
    local url="$1"
    local token

    token="$(firebase_access_token)"
    curl -sf -H @<(bearer "${token}") "${url}"
}
# Turns on a Google API in the project. A project made by projects:create
# starts with the Firestore API off, and creating the database fails with 403
# until it is on; the firebase command line has no command for it, so this
# asks Service Usage directly instead.
enable_api() {
    local project="$1" api="$2"
    local token

    token="$(firebase_access_token)"

    curl -sf -X POST -H @<(bearer "${token}") -H "Content-Type: application/json" \
        "https://serviceusage.googleapis.com/v1/projects/${project}/services/${api}:enable" \
        > /dev/null

    # The switch takes a while to reach the service behind it.
    local attempt

    for attempt in $(seq 1 30); do
        if curl -sf -H @<(bearer "${token}") \
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
    response="$(curl -s -w $'\n%{http_code}' -H @<(bearer "${token}") "${url}")"
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
# Split out of hosting_site() so that `show` and `update` can ask the same question
# without the power to create one.
existing_site() {
    local project="$1"

    api_get "https://firebasehosting.googleapis.com/v1beta1/projects/${project}/sites" |
        python3 -c '
import json, sys

sites = json.load(sys.stdin).get("sites", [])
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
            "what": "Authentication, Sign-in method, Google, Enable, type the name "
                    "of the group as the public-facing name, pick a support "
                    "address, Save",
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

# Deploys this working copy's rules and page to the project, with the
# application's settings beside the page as geoshare.json.
deploy_site() {
    local project="$1" android="$2" site_id="$3"

    # The page and the rules go out from a copy of their own under build/: the
    # page in the working tree carries this working copy's firebase-config.js,
    # and deployed as it is, the new project's page would talk to the old
    # project. The copy gets a configuration of the new project's own, a
    # firebase.json pointing at it, and a .firebaserc of its own for the "page"
    # target, which leaves the working copy's .firebaserc alone.
    local out="${REPO_ROOT}/build/project/${project}"
    local site="${out}/site"

    echo "==> page configuration for the copy"
    rm -rf "${site}"
    # The same layout as the repository, so firebase.json is taken unchanged.
    mkdir -p "${site}/web"
    cp -r "${REPO_ROOT}/web/public" "${site}/web/public"
    rm -f "${site}/web/public/firebase-config.js"
    cp -r "${REPO_ROOT}/firebase" "${site}/firebase"
    cp "${REPO_ROOT}/firebase.json" "${site}/firebase.json"
    FIREBASE_PROJECT="${project}" FIREBASE_SITE="${site_id}" FIREBASE_ACCOUNT="${account}" \
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
}

# Every step checks what is already there, so a run that stopped half way can
# be started again with the same command.
create() {
    local project="$1" location="$2" name="$3" sha_from="$4"

    echo "==> project ${project}"
    if firebase projects:list --json | grep -q "\"projectId\": \"${project}\""; then
        echo "    already there"
    elif api_get "https://cloudresourcemanager.googleapis.com/v1/projects/${project}" > /dev/null; then
        # A run that made the Google Cloud project and then failed to add
        # Firebase to it — for one, on an account that has not yet accepted
        # the Firebase terms — leaves the project behind, and making it again
        # is refused because its id is taken.
        echo "    a Google Cloud project without Firebase; adding it"
        firebase projects:addfirebase "${project}"
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

    deploy_site "${project}" "${android}" "${site_id}"

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

  tools/project.sh show ${project}
EOF

    echo
    echo "Once those two steps are done, phones can connect through:"
    print_connect_link "${site_id}"

    if (( json_mode )); then
        create_result_json "${project}" "${site_id}" >&3
    fi
}

show() {
    local project="$1" wait_google="$2" with_usage="$3"
    local out="${REPO_ROOT}/build/project/${project}"

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
    (cd "${REPO_ROOT}" && FIREBASE_PROJECT="${project}" FIREBASE_SITE="${site_id}" FIREBASE_ACCOUNT="${account}" \
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

  Authentication, Sign-in method, Google, Enable, type the name of the group as
  the public-facing name, pick a support address, Save. Then run this command
  again, or with --wait to have it wait here instead.
EOF
    fi

    local usage_json=""

    if (( with_usage )); then
        echo
        if (( json_mode )); then
            usage_json="$(usage_report "${project}" json)"
        else
            usage_report "${project}" text
        fi
    fi

    # done here means the one thing this function can check for itself, sign-in
    # with Google, is confirmed on — not that every console step ever happened.
    # The redirect URI step from create's second console link is never read
    # back, by this function or anything else, so it stays out of manualSteps
    # and out of done either way.
    if (( json_mode )); then
        PROJECT="${project}" SITE="${site_id}" GOOGLE_ON="${google_on}" USAGE="${usage_json}" \
            python3 -c '
import json, os

project = os.environ["PROJECT"]
site = os.environ["SITE"]
google_on = os.environ["GOOGLE_ON"] == "1"

manual_steps = [] if google_on else [{
    "what": "Authentication, Sign-in method, Google, Enable, type the name "
            "of the group as the public-facing name, pick a support "
            "address, Save",
    "url": f"https://console.firebase.google.com/project/{project}"
           "/authentication/providers",
}]

result = {
    "projectId": project,
    "site": site,
    "connectLink": f"https://{site}.web.app/join",
    "settingsUrl": f"https://{site}.web.app/geoshare.json",
    "manualSteps": manual_steps,
    "done": google_on,
}

if os.environ["USAGE"]:
    result["usage"] = json.loads(os.environ["USAGE"])

print(json.dumps(result, indent=2))
' >&3
    fi
}

# Puts this working copy's rules and page on a project made earlier: what a
# change to either needs to reach a group, without the steps of `create` that
# only ever happen once.
update() {
    local project="$1"

    echo "==> Android app"
    if [[ "$(app_count "${project}" ANDROID)" -eq 0 ]]; then
        echo "no Android app registered in ${project} yet; run \`create\` first" >&2
        exit 1
    fi

    local android
    android="$(app_id "${project}" ANDROID)"

    echo "==> Hosting site"
    local site_id
    site_id="$(existing_site "${project}")"

    if [[ -z "${site_id}" ]]; then
        echo "no Hosting site in ${project} yet; run \`create\` first" >&2
        exit 1
    fi
    echo "    ${site_id}"

    deploy_site "${project}" "${android}" "${site_id}"

    if (( json_mode )); then
        PROJECT="${project}" SITE="${site_id}" python3 -c '
import json, os

print(json.dumps({"projectId": os.environ["PROJECT"], "site": os.environ["SITE"], "deployed": True}))
' >&3
    fi
}

# Asks Google to shut the project down. It is unusable at once, kept for 30
# days in which its owner can bring it back, and then deleted with everything
# in it: https://docs.cloud.google.com/resource-manager/docs/delete-restore-projects
# The firebase command line has no command for it, so Resource Manager is
# asked directly, the way enable_api() asks Service Usage.
delete_project() {
    local project="$1"
    local token response status

    if (( ! confirmed )); then
        echo "delete removes ${project} and everything in it — the group's places," \
            "members and page; run it again with --yes to go ahead" >&2
        exit 2
    fi

    token="$(firebase_access_token)"
    response="$(curl -s -w $'\n%{http_code}' -X DELETE -H @<(bearer "${token}") \
        "https://cloudresourcemanager.googleapis.com/v3/projects/${project}")"
    status="${response##*$'\n'}"

    if [[ "${status}" != "200" ]]; then
        echo "deleting ${project}: HTTP ${status}" >&2
        echo "${response%$'\n'*}" >&2
        exit 1
    fi

    cat <<EOF
${project} is shut down: unusable now, deleted for good in 30 days. Until
then its owner can bring it back:

  https://console.cloud.google.com/cloud-resource-manager?pendingDeletion=true
EOF

    if (( json_mode )); then
        PROJECT="${project}" python3 -c '
import json, os
from datetime import datetime, timedelta, timezone

until = datetime.now(timezone.utc).replace(microsecond=0) + timedelta(days=30)
print(json.dumps({
    "projectId": os.environ["PROJECT"],
    "deleteRequested": True,
    "restorableUntil": until.strftime("%Y-%m-%dT%H:%M:%SZ"),
}))
' >&3
    fi
}

# The free plan's limits the project's usage is held against. Firestore's are
# from https://firebase.google.com/docs/firestore/quotas and are not published
# as metrics; Hosting's are, and are read alongside its usage instead.
readonly FREE_READS_PER_DAY=50000
readonly FREE_WRITES_PER_DAY=20000
readonly FREE_DELETES_PER_DAY=20000
readonly FREE_STORED_BYTES=$((1024 * 1024 * 1024))

# How much of the free plan the project has used: Firestore's reads, writes
# and deletes since the daily quota last reset — around midnight Pacific
# time, by the same documentation — its stored data, and Hosting's transfer
# this month and its storage. Read from Cloud Monitoring with the same
# sign-in as everything else here, so that the project owner, or whoever runs
# this for them, can see what is left without opening the console.
usage_report() {
    local project="$1" format="$2"
    local token

    token="$(firebase_access_token)"

    echo "==> free plan usage, from Cloud Monitoring" >&2

    TOKEN="${token}" PROJECT="${project}" FORMAT="${format}" DAYS="${days}" \
        READS="${FREE_READS_PER_DAY}" WRITES="${FREE_WRITES_PER_DAY}" \
        DELETES="${FREE_DELETES_PER_DAY}" STORED="${FREE_STORED_BYTES}" \
        python3 - <<'EOF'
import json, os, sys, urllib.parse, urllib.request
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

project = os.environ["PROJECT"]
token = os.environ["TOKEN"]
now = datetime.now(timezone.utc).replace(microsecond=0)
quota_day = (
    now.astimezone(ZoneInfo("America/Los_Angeles"))
    .replace(hour=0, minute=0, second=0)
    .astimezone(timezone.utc)
)


def stamp(moment):
    return moment.strftime("%Y-%m-%dT%H:%M:%SZ")


def series(metric, start, aligner, period, end=None):
    end = end or now
    query = urllib.parse.urlencode({
        "filter": f'metric.type = "{metric}"',
        "interval.startTime": stamp(start),
        "interval.endTime": stamp(end),
        "aggregation.alignmentPeriod": f"{max(period, 60)}s",
        "aggregation.perSeriesAligner": aligner,
        "aggregation.crossSeriesReducer": "REDUCE_SUM",
    })
    request = urllib.request.Request(
        f"https://monitoring.googleapis.com/v3/projects/{project}/timeSeries?{query}",
        headers={"Authorization": f"Bearer {token}"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        found = json.load(response).get("timeSeries", [])

    # Newest first; a metric nothing has been counted in yet has no series.
    points = [point["value"] for one in found for point in one["points"]]
    return [float(next(iter(value.values()))) for value in points]


def counted(metric, start=None, end=None):
    start = start or quota_day
    period = int(((end or now) - start).total_seconds())
    return int(sum(series(metric, start, "ALIGN_SUM", period, end)))


def progress(line):
    # Each question to Cloud Monitoring takes a moment, and a dozen of them in
    # a row would otherwise read as the script having stopped.
    print(f"    {line}", file=sys.stderr, flush=True)


# Each earlier quota day asked for on its own, from one Pacific midnight to
# the next, so that no window straddles a reset.
earlier = []
for back in range(int(os.environ["DAYS"]), 0, -1):
    start = (quota_day.astimezone(ZoneInfo("America/Los_Angeles")) - timedelta(days=back)).replace(
        hour=0, minute=0, second=0).astimezone(timezone.utc)
    end = (start.astimezone(ZoneInfo("America/Los_Angeles")) + timedelta(days=1)).replace(
        hour=0, minute=0, second=0).astimezone(timezone.utc)
    progress(f"Firestore on {start.astimezone().strftime('%a %Y-%m-%d')}")
    earlier.append({
        "from": stamp(start),
        "reads": counted("firestore.googleapis.com/document/read_count", start, end),
        "writes": counted("firestore.googleapis.com/document/write_count", start, end),
        "deletes": counted("firestore.googleapis.com/document/delete_count", start, end),
    })


def latest(metric):
    values = series(metric, now - timedelta(days=2), "ALIGN_MAX", 3600)
    return int(values[0]) if values else 0


progress("Firestore today")
firestore = {
    "reads": {"used": counted("firestore.googleapis.com/document/read_count"),
              "limit": int(os.environ["READS"])},
    "writes": {"used": counted("firestore.googleapis.com/document/write_count"),
               "limit": int(os.environ["WRITES"])},
    "deletes": {"used": counted("firestore.googleapis.com/document/delete_count"),
                "limit": int(os.environ["DELETES"])},
    "storedBytes": {"used": latest("firestore.googleapis.com/storage/data_and_index_storage_bytes"),
                    "limit": int(os.environ["STORED"])},
}

progress("Hosting")
hosting = {
    "sentBytesThisMonth": {"used": latest("firebasehosting.googleapis.com/network/monthly_sent"),
                           "limit": latest("firebasehosting.googleapis.com/network/monthly_sent_limit")},
    "storedBytes": {"used": latest("firebasehosting.googleapis.com/storage/total_bytes"),
                    "limit": latest("firebasehosting.googleapis.com/storage/limit")},
}

report = {
    "projectId": project,
    "quotaDayFrom": stamp(quota_day),
    "firestore": firestore,
    "earlierDays": earlier,
    "hosting": hosting,
}

if os.environ["FORMAT"] == "json":
    print(json.dumps(report))
    sys.exit()


def size(value):
    for unit in ("B", "KiB", "MiB", "GiB"):
        if value < 1024 or unit == "GiB":
            return f"{value:.0f} {unit}" if unit == "B" else f"{value:.1f} {unit}"
        value /= 1024


def share(entry):
    return f"{100 * entry['used'] / entry['limit']:.1f}%" if entry["limit"] else "?"


since = quota_day.astimezone().strftime("%Y-%m-%d %H:%M")
rows = [
    (f"Firestore reads since {since}", report["firestore"]["reads"], str),
    ("Firestore writes", report["firestore"]["writes"], str),
    ("Firestore deletes", report["firestore"]["deletes"], str),
    ("Firestore stored data", report["firestore"]["storedBytes"], size),
    ("Hosting transfer this month", report["hosting"]["sentBytesThisMonth"], size),
    ("Hosting storage", report["hosting"]["storedBytes"], size),
]

for what, entry, shown in rows:
    print(f"{what:<42} {shown(entry['used']):>11} of {shown(entry['limit']):>11}  {share(entry):>6}")

if earlier:
    print()
    print(f"{'quota day from':<18} {'reads':>13} {'writes':>13} {'deletes':>13}")
    for day in earlier:
        start = datetime.strptime(day["from"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
        cells = [
            f"{day[kind]} {100 * day[kind] / int(os.environ[kind.upper()]):.0f}%"
            for kind in ("reads", "writes", "deletes")
        ]
        print(f"{start.astimezone().strftime('%a %Y-%m-%d %H:%M'):<18} {cells[0]:>13} {cells[1]:>13} {cells[2]:>13}")
EOF
}

[[ $# -ge 2 ]] || usage

command="$1"
project="$2"
shift 2

location="${DEFAULT_LOCATION}"
name="geoshare"
sha_from=""
sha_given=()
json_mode=0
wait_google=0
days=0
with_usage=0
confirmed=0
account=""
account_args=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --location) location="$2"; shift 2 ;;
        --name) name="$2"; shift 2 ;;
        --sha-from) sha_from="$2"; shift 2 ;;
        --sha) sha_given+=("$2"); shift 2 ;;
        --json) json_mode=1; shift ;;
        --wait) wait_google=1; shift ;;
        --days) days="$2"; with_usage=1; shift 2 ;;
        --usage) with_usage=1; shift ;;
        --yes) confirmed=1; shift ;;
        --account) account="$2"; account_args=(--account "$2"); shift 2 ;;
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
    show) require_login; require_access "${project}"; show "${project}" "${wait_google}" "${with_usage}" ;;
    update) require_login; require_access "${project}"; update "${project}" ;;
    delete) require_login; require_access "${project}"; delete_project "${project}" ;;
    *) usage ;;
esac
