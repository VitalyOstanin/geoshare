#!/usr/bin/env bash
# Draws every mermaid diagram in the documents, so that one the renderer
# cannot parse fails here rather than on the page that shows it. The
# markdown linter reads a fence as plain text and cannot tell.
#
#   tools/mermaid-check.sh [file.md ...]   # default: every tracked document
#
# Run from md-lint.sh. The renderer is fetched by npx inside the container, as
# the linters are, and drives the Chromium the Playwright image already
# carries, so nothing is downloaded for the browser. The image is the one the
# landing page's pictures are taken in; the firebase image has no browser.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

# MERMAID_ROOT points the check at a tree other than the repository, for trying
# it on a document made to fail; the files are then named on the command line.
root="${MERMAID_ROOT:-${REPO_ROOT}}"
cd "${root}"

if (($# > 0)); then
    files=("$@")
elif git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    mapfile -t files < <(git ls-files '*.md')
else
    # A copy of the tree without its history, as a build machine gets it: the
    # documents are looked for on disk, past what the linter leaves out too.
    mapfile -t files < <(find . -name '*.md' \
        -not -path '*/node_modules/*' -not -path '*/build/*' \
        -not -path './web/public/lib/vendor/*' \
        -not -path './docs/__check/*' -not -path './docs/superpowers/*' \
        -not -path './shots/*' | sed 's|^\./||' | sort)
fi

# Only the documents with a diagram are handed over; the renderer has nothing
# to say about the rest.
with_diagrams=()
for file in "${files[@]}"; do
    if grep -q '^[[:space:]]*```mermaid' "${file}"; then
        with_diagrams+=("${file}")
    fi
done

if ((${#with_diagrams[@]} == 0)); then
    echo "no mermaid diagrams to check"
    exit 0
fi

# Chromium runs as root in the container, which it refuses without
# --no-sandbox. Each document is rendered into a scratch directory, and a
# parse error makes the renderer exit non-zero.
exec podman run --rm --http-proxy=false "${limit_args[@]}" \
    -v "${root}:/src:z,ro" -v "${NPM_VOLUME:-geoshare-npm}:/npm" -w /src \
    -e npm_config_cache=/npm -e PUPPETEER_SKIP_DOWNLOAD=1 \
    "mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-noble" \
    bash -c '
        set -euo pipefail
        chrome=$(ls -d /ms-playwright/chromium-*/chrome-linux*/chrome | head -n 1)
        printf "{\"executablePath\":\"%s\",\"args\":[\"--no-sandbox\"]}" "${chrome}" \
            > /tmp/puppeteer.json
        npm install --silent --no-audit --no-fund --prefix /tmp/mmdc \
            "@mermaid-js/mermaid-cli@$0" > /dev/null
        status=0
        for file in "${@}"; do
            echo "==> ${file}"
            /tmp/mmdc/node_modules/.bin/mmdc -q -p /tmp/puppeteer.json \
                -i "${file}" -o /tmp/out.md || status=1
        done
        exit "${status}"
    ' "${MERMAID_CLI_VERSION}" "${with_diagrams[@]}"
