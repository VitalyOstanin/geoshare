// A charge level written the way a language writes a percentage: "64%" in
// English, with a space before the sign in Russian.

/** The percentage in the given language. */
export function percentText(percent, language) {
    return new Intl.NumberFormat(language, { style: 'percent' }).format(percent / 100);
}
