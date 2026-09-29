// The page's styles as the browser reads them: every stylesheet the page
// links, in the order it links them, as one text.
import { readFile } from 'node:fs/promises';

export async function readStyles() {
    const html = await readFile(new URL('../public/index.html', import.meta.url), 'utf8');
    const links = [...html.matchAll(/<link rel="stylesheet" href="([^"]+)"/g)].map(
        (found) => found[1],
    );
    const texts = await Promise.all(
        links.map((href) => readFile(new URL(`../public/${href}`, import.meta.url), 'utf8')),
    );

    return texts.join('\n');
}
