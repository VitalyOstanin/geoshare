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
# The site's root gets only what the setup page loads, which the script below
# works out from the page itself.
rm -r "${OUT}/public/group/setup.html" "${OUT}/public/group/setup-privacy.html" \
    "${OUT}/public/group/setup-app.js" \
    "${OUT}/public/group/lib/setup-project.js" "${OUT}/public/group/lib/setup-hosting.js" \
    "${OUT}/public/group/lib/owner-rules.js" "${OUT}/public/group/lib/setup-token.js" \
    "${OUT}/public/group/styles/setup.css" \
    "${OUT}/public/group/lib/qr-svg.js" "${OUT}/public/group/lib/vendor/qr-"*
cp "${REPO_ROOT}/firebase/firestore.rules" "${OUT}/public/firestore.rules"

SETUP_CLIENT_ID="${client_id}" SETUP_PACKAGE="${PACKAGE_NAME}" SETUP_LOCATION="${LOCATION}" \
    node - "${OUT}" "${REPO_ROOT}/firebase.json" "${REPO_ROOT}/web/public" "${sha_given[@]}" <<'EOF'
const fs = require("fs");
const path = require("path");

const [out, firebaseJson, source, ...fingerprints] = process.argv.slice(2);
const pub = path.join(out, "public");
const hosting = JSON.parse(fs.readFileSync(firebaseJson, "utf8")).hosting;

// The site's root holds the setup page and what it loads, found by following
// the page's links and the imports of its modules, so that the list cannot
// fall behind the code. The map, the join page and their modules stay under
// group/: with a Google token on this origin, code the page never uses would
// be code that can run there. A path the page names and the source lacks stops
// the build.
const pending = ["setup.html", "setup-privacy.html"];
const rooted = new Set();
const missing = [];

while (pending.length > 0) {
    const rel = pending.pop();

    if (rooted.has(rel)) {
        continue;
    }
    if (!fs.existsSync(path.join(source, rel))) {
        missing.push(rel);
        continue;
    }
    rooted.add(rel);

    const text = fs.readFileSync(path.join(source, rel), "utf8");
    const found = [];

    if (rel.endsWith(".html")) {
        for (const m of text.matchAll(/\b(?:href|src)="([^"]+)"/g)) {
            found.push(m[1]);
        }
    } else if (rel.endsWith(".js")) {
        for (const m of text.matchAll(/\bimport\s*(?:[^'"()]*?\bfrom\s*)?["']([^"']+)["']/g)) {
            found.push(m[1]);
        }
        for (const m of text.matchAll(/\bimport\(\s*["']([^"']+)["']\s*\)/g)) {
            found.push(m[1]);
        }
    } else if (rel.endsWith(".css")) {
        for (const m of text.matchAll(/(?:@import\s+|url\()\s*["']?([^"')\s]+)/g)) {
            found.push(m[1]);
        }
    }
    for (const ref of found) {
        // Other origins, anchors and "./" (the site's own index) are not files here.
        if (/^([a-z]+:|#|\/\/)/i.test(ref) || ref.endsWith("/")) {
            continue;
        }
        const file = path.posix.join(path.posix.dirname(rel), ref.split(/[?#]/)[0]);

        pending.push(path.posix.normalize(file));
    }
}
if (missing.length > 0) {
    console.error(`setup page loads files that are not in web/public: ${missing.join(", ")}`);
    process.exit(1);
}
for (const rel of rooted) {
    fs.mkdirSync(path.dirname(path.join(pub, rel)), { recursive: true });
    fs.copyFileSync(path.join(source, rel), path.join(pub, rel));
}
// A vendored library travels with its licence, which nothing imports.
for (const dir of new Set([...rooted].map((rel) => path.posix.dirname(rel)))) {
    if (!dir.includes("vendor/")) {
        continue;
    }
    for (const name of fs.readdirSync(path.join(source, dir))) {
        if (/^(LICEN[CS]E|NOTICE|COPYING)/i.test(name)) {
            fs.copyFileSync(path.join(source, dir, name), path.join(pub, dir, name));
        }
    }
}
fs.copyFileSync(path.join(pub, "setup.html"), path.join(pub, "index.html"));
console.error(`==> ${rooted.size} files at the site's root`);

// Everything a group's page is made of; the setup page fetches each from
// group/<path> and deploys it as <path>.
const group = path.join(pub, "group");
const paths = [];

(function walk(dir, rel) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        if (entry.name.startsWith(".")) {
            continue;
        }
        const relPath = `${rel}/${entry.name}`;

        if (entry.isDirectory()) {
            walk(path.join(dir, entry.name), relPath);
        } else if (entry.isFile()) {
            paths.push(relPath);
        }
    }
})(group, "");
paths.sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));

const write = (file, value) => fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);

write(path.join(pub, "setup-files.json"), { paths, hosting });
write(path.join(pub, "setup-config.json"), {
    clientId: process.env.SETUP_CLIENT_ID,
    packageName: process.env.SETUP_PACKAGE,
    location: process.env.SETUP_LOCATION,
    fingerprints,
    consoleUrl: "https://console.firebase.google.com/",
});

// The group's policy, adjusted for the setup page alone: Google Identity
// Services is served from accounts.google.com, and the page calls the
// Firebase, Firestore, Service Usage, Rules and Hosting APIs, the upload host
// of Hosting among them. The page never loads the Google API loader that the
// map page's sign-in uses, so apis.google.com is dropped from script-src.
// style-src drops 'unsafe-inline': setup.html keeps its styles in
// styles/setup.css, and the Google Identity Services guide asks for no more
// than https://accounts.google.com/gsi/style there. Trusted Types are not
// required: the guide says nothing about the library working under
// require-trusted-types-for, and loadGis() assigns a plain string to
// script.src, which the requirement would block.
let policy = hosting.headers.flatMap((rule) => rule.headers)
    .find((h) => h.key === "Content-Security-Policy").value;

policy = policy.replace(
    "script-src 'self' https://apis.google.com",
    "script-src 'self' https://accounts.google.com",
);
policy = policy.replace(
    "style-src 'self' 'unsafe-inline'",
    "style-src 'self' https://accounts.google.com/gsi/style",
);
policy = policy.replace("connect-src 'self'", "connect-src 'self' https://accounts.google.com");

const siteHosting = structuredClone(hosting);

delete siteHosting.target;
siteHosting.public = "public";
for (const rule of siteHosting.headers) {
    for (const header of rule.headers) {
        if (header.key === "Content-Security-Policy") {
            header.value = policy;
        }
    }
}
// The files under group/ are data for the setup page to fetch and deploy, not
// pages of this site. A sandbox without allow-scripts keeps them from running
// should anyone open one. The rule comes last and repeats the whole policy,
// since a header of the same name from a later rule replaces an earlier one.
siteHosting.headers.push({
    source: "/group/**",
    headers: [{ key: "Content-Security-Policy", value: `${policy}; sandbox` }],
});
// /join belongs to a group's site, not to this one.
siteHosting.rewrites = [];
write(path.join(out, "firebase.json"), { hosting: siteHosting });

console.error(`==> ${paths.length} files for a group's site`);
EOF

echo "==> deploy: firebase deploy --only hosting --project geoshare-setup --config ${OUT#"${REPO_ROOT}"/}/firebase.json" >&2
