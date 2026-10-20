// What the JavaScript half is held to.
//
// The page has no build step and no dependencies, so the rules here are about
// one thing: keeping the two halves of the JavaScript in the repository — the
// page under web/ and the emulator scripts under firebase/ — written the same
// way. They had drifted into opposite quoting conventions, which is the sort
// of difference that makes a file look foreign to whoever opens it next.
import { realpathSync } from 'node:fs';
import { createRequire } from 'node:module';

// The linter is fetched by npx beside @eslint/js and the plugins
// (tools/js-lint.sh), not installed under this directory, so they are found
// next to the running eslint rather than by a bare import from here.
const besideEslint = createRequire(realpathSync(process.argv[1]));
const js = besideEslint('@eslint/js');
// Published as an ES module only, so require() hands back its namespace.
const stylisticModule = besideEslint('@stylistic/eslint-plugin');
const stylistic = stylisticModule.default ?? stylisticModule;
const { importX, createNodeResolver } = besideEslint('eslint-plugin-import-x');
const promise = besideEslint('eslint-plugin-promise');

export default [
    js.configs.recommended,
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
        plugins: { '@stylistic': stylistic },
        rules: {
            // The formatting rules left ESLint itself in 8.53 for this plugin,
            // which keeps their names and options.
            '@stylistic/quotes': ['error', 'single', { avoidEscape: true }],
            '@stylistic/semi': ['error', 'always'],
            '@stylistic/comma-dangle': ['error', 'always-multiline'],
            '@stylistic/indent': ['error', 4, { SwitchCase: 1 }],
            '@stylistic/max-len': ['error', { code: 100, ignoreUrls: true, ignoreStrings: true }],
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
        // The page is split into modules that import from one another, and a
        // path that names no file or a name the module does not export only
        // shows once a browser loads them. Circles are left alone: the page's
        // parts import app.js back for its state and handlers, which the
        // modules reach only once everything is loaded.
        files: ['web/**/*.js', 'web/**/*.mjs', 'firebase/**/*.mjs', 'tools/project/**/*.mjs'],
        plugins: { 'import-x': importX },
        settings: { 'import-x/resolver-next': [createNodeResolver()] },
        rules: {
            // The rules tests import packages installed by their own run, not
            // by the lint, so only paths of the repository are checked. The
            // page's firebase-config.js is written per project and never
            // committed, so a fresh checkout has nothing to resolve it to.
            'import-x/no-unresolved': [
                'error',
                { ignore: ['^@firebase/', '^firebase/', '/firebase-config\\.js$'] },
            ],
            'import-x/named': 'error',
        },
    },
    {
        // A promise nobody awaits fails without a word unless it is caught, and
        // a then() inside a then() hides which of them a failure came from.
        files: ['**/*.js', '**/*.mjs'],
        plugins: { promise },
        rules: {
            'promise/catch-or-return': ['error', { allowFinally: true }],
            'promise/no-nesting': 'error',
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
                TextDecoder: 'readonly',
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
    {
        // The JavaScript half of tools/project.sh, run by node on the machine
        // that looks after a group's project.
        files: ['tools/project/**/*.mjs'],
        languageOptions: {
            globals: {
                AbortSignal: 'readonly',
                console: 'readonly',
                fetch: 'readonly',
                process: 'readonly',
                URL: 'readonly',
                URLSearchParams: 'readonly',
            },
        },
    },
];
