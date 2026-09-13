// The theme, put on the element before the first paint.
//
// Not a module and not part of app.js: both run after the page is painted, and
// the white flash on the way into a dark map is what the setting exists to
// avoid. The key is spelled out here as well as in lib/theme.js because this
// file cannot import anything.
try {
    const theme = localStorage.getItem('geoshare.theme');

    if (theme === 'light' || theme === 'dark') {
        document.documentElement.dataset.theme = theme;
    }
} catch {
    // A browser keeping no site data has no answer to read, and the system's
    // own is what the page falls back to anyway.
}
