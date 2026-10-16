import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { test } from 'node:test';

import { EN } from '../public/lib/words/en.js';
import { RU } from '../public/lib/words/ru.js';

/** What a word is: a line to show, or a line built around something counted. */
const shapeOf = (dictionary) => Object.fromEntries(
    Object.entries(dictionary).map(([key, word]) => [
        key,
        typeof word === 'function' ? `function/${word.length}` : typeof word,
    ]),
);

test('both languages say the same things', () => {
    assert.deepEqual(Object.keys(RU).sort(), Object.keys(EN).sort());
});

test('a word built around a count is one in both languages, of the same arity', () => {
    assert.deepEqual(shapeOf(RU), shapeOf(EN));
});

test('nothing is left untranslated by accident', () => {
    for (const [key, word] of Object.entries(RU)) {
        if (typeof word !== 'string' || key.startsWith('language')) {
            continue;
        }

        assert.notEqual(word, EN[key], `${key} is still in English`);
    }
});

// Russian counts in three forms rather than two, and the form follows the last
// digits rather than the number: 1 and 21 take one ending, 2 and 22 another,
// 5 and 11 a third.
test('a counted member takes the ending the count asks for', () => {
    assert.equal(RU.members(1), '1 участник');
    assert.equal(RU.members(2), '2 участника');
    assert.equal(RU.members(5), '5 участников');
    assert.equal(RU.members(21), '21 участник');
    assert.equal(RU.members(11), '11 участников');
});

// The same three forms for the answers a device threw away, which is a count
// shown on a card while a spoof is running.
test('a count of left-out answers takes the ending the count asks for', () => {
    assert.equal(RU.leftOutAnswers(1), '1 позиция');
    assert.equal(RU.leftOutAnswers(2), '2 позиции');
    assert.equal(RU.leftOutAnswers(5), '5 позиций');
    assert.equal(RU.leftOutAnswers(21), '21 позиция');
    assert.equal(RU.leftOutAnswers(11), '11 позиций');
});

// The count of devices in the panel that runs the group, where the verb agrees
// with the count as well as the noun.
test('a count of devices shared agrees in both number and verb', () => {
    assert.equal(RU.adminPhonesShared(1), '1 устройство доступно вам');
    assert.equal(RU.adminPhonesShared(2), '2 устройства доступны вам');
    assert.equal(RU.adminPhonesShared(5), '5 устройств доступны вам');
    assert.equal(RU.adminPhonesShared(21), '21 устройство доступно вам');
    assert.equal(EN.adminPhonesShared(1), '1 device shared with you');
    assert.equal(EN.adminPhonesShared(2), '2 devices shared with you');
});

test('an age takes the ending its unit and count ask for', () => {
    assert.equal(RU.ago(1, 'minutes'), '1 минуту назад');
    assert.equal(RU.ago(3, 'minutes'), '3 минуты назад');
    assert.equal(RU.ago(20, 'minutes'), '20 минут назад');
    assert.equal(RU.ago(1, 'hours'), '1 час назад');
    assert.equal(RU.ago(2, 'hours'), '2 часа назад');
    assert.equal(RU.ago(6, 'days'), '6 дней назад');
});

test('members out of reach agree with the count in both number and verb', () => {
    assert.equal(RU.moreRefused(2), 'Ещё 2 участника сейчас не делятся с вами.');
    assert.equal(RU.moreRefused(21), 'Ещё 21 участник сейчас не делится с вами.');
});

// The markup is painted before any module runs, so it carries its own words;
// the dictionary carries them too, and the page says them again in whichever
// language it settled on. An element the page cannot find says nothing at all.
test('every word the markup is painted with has a place to be said again', async () => {
    const page = await readFile(new URL('../public/index.html', import.meta.url), 'utf8');

    for (const id of ['sign-in-explanation', 'sign-in-button', 'sign-out-button',
        'theme-label', 'language-label']) {
        assert.ok(page.includes(`id="${id}"`), `the page has no ${id}`);
    }
});

// Elements whose words are meant to stay as they are in every language.
const LEFT_AS_WRITTEN = new Map([
    // Each language is named in its own words, so a viewer looking for theirs finds it.
    ['[data-language=ru]', 'Russian, named in Russian'],
    ['[data-language=en]', 'English, named in English'],
]);

/** The name `state.js` gives an element of the markup: sign-in-button, signInButton. */
const keyOf = (id) => id.replace(/-(\w)/g, (_, letter) => letter.toUpperCase());

/**
 * Every element of the markup that carries words a viewer can read or hear: some
 * text of its own (a mark such as a cross counts: it is read aloud through an
 * aria-label), or an aria-label or a title. An element with an id
 * is found by it; the theme and language buttons have none, only a data attribute.
 */
function wordsOfMarkup(page) {
    const found = [];

    for (const match of page.matchAll(/<(\w+)([^>]*)>([^<]*)/g)) {
        const [, , attributes, text] = match;
        const id = /\sid="([^"]+)"/.exec(attributes)?.[1];
        const data = /\sdata-(theme|language)="([^"]+)"/.exec(attributes);
        const labelled = /\s(?:aria-label|title)="[^"]*\p{L}/u.test(attributes);

        if (!id && !data) {
            continue;
        }

        if (labelled || /\S/.test(text)) {
            found.push(id
                ? { selector: `#${id}`, id }
                : { selector: `[data-${data[1]}=${data[2]}]`, kind: data[1] });
        }
    }

    return found;
}

// Index.html is painted in English before anything runs. An element of it with
// words and no place that says them again keeps English for a viewer reading
// Russian, which nothing else on the page would show.
test('every element of the markup with words is said again in the page language', async () => {
    const page = await readFile(new URL('../public/index.html', import.meta.url), 'utf8');
    const names = (await readdir(new URL('../public/page/', import.meta.url)))
        .map((name) => `page/${name}`);
    const said = (await Promise.all(['app.js', ...names].map(
        (name) => readFile(new URL(`../public/${name}`, import.meta.url), 'utf8'),
    ))).join('\n');
    const found = wordsOfMarkup(page);

    assert.ok(found.length > 10, 'the markup was not read');

    for (const { selector, id, kind } of found) {
        if (LEFT_AS_WRITTEN.has(selector)) {
            continue;
        }

        const buttons = kind === 'theme' ? 'themeButtons' : 'languageButtons';
        const place = id ? keyOf(id) : buttons;

        const says = [
            `elements\\.${place}\\.(?:textContent|title)\\s*=`,
            `elements\\.${place}\\.setAttribute\\('aria-label'`,
            `of elements\\.${place}\\)`,
        ].join('|');

        assert.match(said, new RegExp(says),
            `${selector} has words in index.html that nothing says again`);
    }
});

test('the words of the markup are in the dictionaries', () => {
    for (const key of ['signInExplanation', 'signInAction', 'signOut', 'appearance',
        'themeLight', 'themeDark', 'themeSystem', 'language', 'languageSystem']) {
        assert.ok(EN[key], `English has no ${key}`);
        assert.ok(RU[key], `Russian has no ${key}`);
    }
});
