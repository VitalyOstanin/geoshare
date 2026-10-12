// What this browser keeps for the next visit, and nothing more.
//
// A browser told to keep no site data throws on the attempt rather than
// answering with nothing, so every read and write here goes through the two
// functions below: a refusal is never the viewer's concern, only the next
// visit is worse off. The store is a parameter so the tests can hand in their
// own.

/** What is kept under [key], or null where nothing is or nothing can be. */
export function readKept(key, store = globalThis.localStorage) {
    try {
        return store.getItem(key);
    } catch {
        return null;
    }
}

/** Keeps [value] under [key], or forgets it for null; false where the browser refused. */
export function keep(key, value, store = globalThis.localStorage) {
    try {
        if (value === null) {
            store.removeItem(key);
        } else {
            store.setItem(key, value);
        }

        return true;
    } catch {
        return false;
    }
}
