#!/usr/bin/env bash
# Takes the setup page's first two steps for the landing page, full size, in
# both languages and both themes, from the page as it is online.
#
# A 760x560 window at three device pixels to the CSS pixel: 2280x1680, more
# than the largest copy the landing page offers. The pictures go beside the
# application's in shots/<date>/ (see tools/landing-shots.sh), and
# tools/landing-images.sh makes the page's pictures of them.
#
#   tools/landing-steps.sh
#   SHOTS_DIR=shots/retake tools/landing-steps.sh
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly OUT="${SHOTS_DIR:-${REPO_ROOT}/shots/$(date +%F)}"
readonly SETUP_URL="${SETUP_URL:-https://geoshare-setup.web.app/}"

mkdir -p "${OUT}/en" "${OUT}/ru"

# The browsers come with the image; the library of the same version is
# fetched into the container, which is thrown away afterwards. The host's
# network, so that a proxy on its loopback is still reachable.
podman run --rm --network host --ipc host \
    -v "${OUT}:/out:z" -e "SETUP_URL=${SETUP_URL}" \
    "mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-noble" \
    bash -c "cd /tmp && npm install --silent --no-audit --no-fund playwright@${PLAYWRIGHT_VERSION} > /dev/null && node --input-type=module -e \"\$0\"" '
import { chromium } from "playwright";

const browser = await chromium.launch();
for (const [language, locale] of [["en", "en-GB"], ["ru", "ru-RU"]]) {
    for (const theme of ["light", "dark"]) {
        const context = await browser.newContext({
            locale,
            colorScheme: theme,
            viewport: { width: 760, height: 560 },
            deviceScaleFactor: 3,
        });
        const page = await context.newPage();
        await page.goto(process.env.SETUP_URL, { waitUntil: "networkidle" });
        await page.locator("#step-1").waitFor();
        await page.screenshot({ path: `/out/${language}/step-project-${theme}.png` });
        await page.click("#s1-next");
        await page.locator("#step-2").waitFor();
        await page.screenshot({ path: `/out/${language}/step-setup-${theme}.png` });
        console.log(`==> ${language}, ${theme}`);
        await context.close();
    }
}
await browser.close();
'
