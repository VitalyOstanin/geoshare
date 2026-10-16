#!/usr/bin/env bash
# Copies the libraries the page serves itself out of the npm packages pinned
# in web/package.json, into web/public/lib/vendor/<package>-<version>/.
#
#   tools/vendor-web.sh
#
# The page loads no script from another server (ADR-0035). Each library sits
# beside it in a directory named after its version, which is what lets the
# hosting keep lib/vendor in a browser's cache for a year: a new version is a
# new address. Dependabot bumps the pins; the page's tests then fail until
# this has been run and lib/vendor committed with the bump.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly NPM_VOLUME="${NPM_VOLUME:-geoshare-npm}"
readonly MODULES_VOLUME="${MODULES_VOLUME:-geoshare-web-modules}"

ensure_firebase_image

echo "==> copying the pinned libraries into web/public/lib/vendor" >&2

# The packages are installed into a volume rather than web/node_modules, which
# would put over two hundred megabytes in the working copy for three directories.
# npm ci checks every package against the integrity package-lock.json holds.
# Without the proxy for the reason tools/rules.sh gives. The volume leaves its
# mount point behind in the working copy, failed run or not.
trap 'rmdir "${REPO_ROOT}/web/node_modules" 2>/dev/null || true' EXIT
timeout 10m podman run --rm -i --http-proxy=false "${limit_args[@]}" \
    -v "${REPO_ROOT}:/src:z" -v "${NPM_VOLUME}:/npm" \
    -v "${MODULES_VOLUME}:/src/web/node_modules" -w /src/web \
    "${FIREBASE_IMAGE}" \
    bash -s <<'EOF'
set -euo pipefail

npm ci --ignore-scripts --no-audit --no-fund

version() {
    node -p "require('./node_modules/$1/package.json').version"
}

readonly VENDOR="public/lib/vendor"
rm -rf "${VENDOR}"
mkdir -p "${VENDOR}"

# Firebase: the same browser builds gstatic serves. The other two import
# firebase-app.js by its gstatic address; here it is the file beside them.
# The package carries no licence text, so it comes from the release's tag.
v="$(version firebase)"
dir="${VENDOR}/firebase-${v}"
mkdir "${dir}"
cp node_modules/firebase/firebase-app.js "${dir}/"
for name in auth firestore; do
    sed "s#from\"https://www.gstatic.com/firebasejs/${v}/firebase-app.js\"#from\"./firebase-app.js\"#" \
        "node_modules/firebase/firebase-${name}.js" > "${dir}/firebase-${name}.js"
done
if grep -l 'from"https://' "${dir}"/*.js; then
    echo "a Firebase build still imports from another server" >&2
    exit 1
fi
node -e '
    fetch(process.argv[1]).then(async (response) => {
        if (!response.ok) throw new Error(`${response.status} for ${process.argv[1]}`);
        process.stdout.write(await response.text());
    });
' "https://raw.githubusercontent.com/firebase/firebase-js-sdk/firebase%40${v}/LICENSE" \
    > "${dir}/LICENSE.txt"

# MapLibre GL JS: the module build, its worker and its styles. The worker is
# started from the address of the library, so the two stay together.
#
# Both scripts are brought down to the syntax of the oldest browser the page
# is for: MapLibre 6 is written with class static blocks, and a browser
# without them (Chrome before 94) refuses the whole module before a line of
# the page runs. The few newer built-ins it calls are put at the head of each
# (tools/maplibre-polyfills.js); the code is otherwise MapLibre's own.
readonly OLDEST_BROWSER="chrome86"
v="$(version maplibre-gl)"
dir="${VENDOR}/maplibre-gl-${v}"
mkdir "${dir}"
cp node_modules/maplibre-gl/dist/maplibre-gl.css "${dir}/"
for name in maplibre-gl maplibre-gl-worker; do
    node_modules/.bin/esbuild "node_modules/maplibre-gl/dist/${name}.mjs" \
        --target="${OLDEST_BROWSER}" --format=esm --legal-comments=inline \
        --banner:js="$(cat /src/tools/maplibre-polyfills.js)" \
        --log-level=warning --outfile="${dir}/${name}.mjs"
done
if grep -l 'static{' "${dir}"/*.mjs; then
    echo "MapLibre still holds a class static block" >&2
    exit 1
fi
cp node_modules/maplibre-gl/LICENSE.txt "${dir}/LICENSE.txt"

# qr: the encoder alone, which is the package's index.js and imports nothing.
v="$(version qr)"
dir="${VENDOR}/qr-${v}"
mkdir "${dir}"
{
    printf '/**\n * qr %s, the encoder only (index.js of the npm package "qr").\n' "${v}"
    printf ' * https://github.com/paulmillr/qr — MIT OR Apache-2.0, used under MIT.\n */\n'
    cat node_modules/qr/index.js
} > "${dir}/qr.mjs"
cp node_modules/qr/LICENSE-MIT "${dir}/LICENSE.txt"

find "${VENDOR}" -type f | sort
EOF
