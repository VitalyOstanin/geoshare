// What the JavaScript half is held to.
//
// The page has no build step and no dependencies, so the rules here are about
// one thing: keeping the two halves of the JavaScript in the repository — the
// page under web/ and the emulator scripts under firebase/ — written the same
// way. They had drifted into opposite quoting conventions, which is the sort
// of difference that makes a file look foreign to whoever opens it next.
export default [
    {
        // Written by tools/web-config.sh out of the Firebase project itself,
        // and not in the repository at all. Beside it, the map library as its
        // authors published it: minified, not ours to hold to a style.
        ignores: ['web/public/firebase-config.js', 'web/public/lib/vendor/**'],
    },
    {
        files: ['**/*.js', '**/*.mjs'],
        languageOptions: {
            ecmaVersion: 2024,
            sourceType: 'module',
        },
        rules: {
            quotes: ['error', 'single', { avoidEscape: true }],
            semi: ['error', 'always'],
            'comma-dangle': ['error', 'always-multiline'],
            indent: ['error', 4, { SwitchCase: 1 }],
            'max-len': ['error', { code: 100, ignoreUrls: true, ignoreStrings: true }],
            'no-unused-vars': ['error', { argsIgnorePattern: '^unused' }],
            eqeqeq: ['error', 'always'],
            'prefer-const': 'error',
            'no-var': 'error',
            // The page is split into modules that import from one another, and a
            // name used without its import would only fail once the code runs.
            'no-undef': 'error',
            // One way of writing a long number: from five digits on, with
            // separators, 15_000 rather than 15000.
            'no-restricted-syntax': ['error', {
                selector: 'Literal[raw=/^[0-9]{5}/]',
                message: 'Write a number of five digits or more with separators, as 15_000.',
            }],
        },
    },
    {
        // The page's own code, held to functions that fit on one screen: the
        // longest of them were split at 60 lines of code, blank lines and
        // comments not counted, and this keeps them from growing back.
        files: ['web/public/**/*.js'],
        rules: {
            'max-lines-per-function': ['error', { max: 60, skipBlankLines: true, skipComments: true }],
        },
    },
    {
        // Written to be read by a browser, which brings its own globals.
        files: ['web/**/*.js'],
        languageOptions: {
            globals: {
                crypto: 'readonly',
                document: 'readonly',
                localStorage: 'readonly',
                matchMedia: 'readonly',
                navigator: 'readonly',
                Node: 'readonly',
                setInterval: 'readonly',
                clearInterval: 'readonly',
                setTimeout: 'readonly',
                clearTimeout: 'readonly',
                window: 'readonly',
                console: 'readonly',
                URL: 'readonly',
                URLSearchParams: 'readonly',
                ResizeObserver: 'readonly',
                atob: 'readonly',
                Blob: 'readonly',
                CompressionStream: 'readonly',
                Response: 'readonly',
                fetch: 'readonly',
            },
        },
    },
    {
        // Run by node, against the emulators.
        files: ['firebase/**/*.mjs', 'web/tests/**/*.mjs'],
        languageOptions: {
            globals: {
                console: 'readonly',
                fetch: 'readonly',
                process: 'readonly',
                setTimeout: 'readonly',
                URL: 'readonly',
                btoa: 'readonly',
            },
        },
    },
];
