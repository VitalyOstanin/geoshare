// The browser half of tools/landing-hero.sh, run in the Playwright image:
// the page on the stand signed in as Mark, in both themes, and then the
// header drawn from hero.html around it and the two device frames.
//
//   node shoot.mjs <directory>
//
// The directory holds hero.html, the phone and tablet frames, and receives
// page-<theme>.png and hero-<theme>.png.
import { chromium } from 'playwright';

const [, , dir] = process.argv;
const standUrl = process.env.STAND_URL ?? 'http://127.0.0.1:5000/';
const themes = ['light', 'dark'];

const browser = await chromium.launch({
    args: ['--no-proxy-server', '--allow-file-access-from-files'],
});

for (const theme of themes) {
    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 },
        colorScheme: theme,
        locale: 'en-GB',
        timezoneId: 'Europe/Tallinn',
    });
    const page = await context.newPage();
    await page.goto(standUrl);
    await page.evaluate(async () => {
        // The page's own copy of the Firebase library, whatever its version.
        const source = await (await fetch('/lib/store.js')).text();
        const vendor = source.match(/vendor\/firebase-[\d.]+\/firebase-auth\.js/)[0];
        const store = await import('/lib/store.js');
        const { signInWithEmailAndPassword } = await import(`/lib/${vendor}`);
        await signInWithEmailAndPassword(store.auth, 'mark@geoshare.test', 'geoshare');
    });
    // Polled from here: a predicate that imports is a promise, which
    // waitForFunction takes for true at once.
    const deadline = Date.now() + 30_000;
    for (;;) {
        const placed = await page.evaluate(async () => {
            const { state } = await import('/page/state.js');
            return state.view?.map
                ? state.positions.filter((p) => Number.isFinite(p.latitude) && Number.isFinite(p.longitude)).length
                : 0;
        });
        if (placed >= 4) {
            break;
        }
        if (Date.now() > deadline) {
            throw new Error(`only ${placed} positions on the page after 30 s`);
        }
        await page.waitForTimeout(500);
    }
    // Every marker in sight, clear of the tablet laid over the lower left.
    await page.evaluate(async () => {
        const { state } = await import('/page/state.js');
        const placed = state.positions.filter((p) => Number.isFinite(p.latitude) && Number.isFinite(p.longitude));
        const lngs = placed.map((p) => p.longitude);
        const lats = placed.map((p) => p.latitude);
        state.view.map.fitBounds(
            [[Math.min(...lngs), Math.min(...lats)], [Math.max(...lngs), Math.max(...lats)]],
            { padding: { top: 110, left: 140, right: 200, bottom: 470 }, animate: false },
        );
    });
    await page.waitForTimeout(3000);
    await page.mouse.move(700, 450);
    await page.screenshot({ path: `${dir}/page-${theme}.png` });
    console.error(`==> page, ${theme}`);
    await context.close();
}

const stage = await browser.newPage({ viewport: { width: 1600, height: 900 } });
for (const theme of themes) {
    await stage.goto(`file://${dir}/hero.html?t=${theme}`);
    await stage.waitForFunction(() => [...document.images].every((i) => i.complete && i.naturalWidth > 0));
    await stage.locator('#stage').screenshot({ path: `${dir}/hero-${theme}.png`, omitBackground: true });
    console.error(`==> hero, ${theme}`);
}

await browser.close();
