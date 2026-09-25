import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
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

// The same three forms for the answers a phone threw away, which is a count
// shown on a card while a spoof is running.
test('a count of left-out answers takes the ending the count asks for', () => {
    assert.equal(RU.leftOutAnswers(1), '1 позиция');
    assert.equal(RU.leftOutAnswers(2), '2 позиции');
    assert.equal(RU.leftOutAnswers(5), '5 позиций');
    assert.equal(RU.leftOutAnswers(21), '21 позиция');
    assert.equal(RU.leftOutAnswers(11), '11 позиций');
});

// The count of phones in the panel that runs the group, where the verb agrees
// with the count as well as the noun.
test('a count of phones shared agrees in both number and verb', () => {
    assert.equal(RU.adminPhonesShared(1), '1 телефон доступен вам');
    assert.equal(RU.adminPhonesShared(2), '2 телефона доступны вам');
    assert.equal(RU.adminPhonesShared(5), '5 телефонов доступны вам');
    assert.equal(RU.adminPhonesShared(21), '21 телефон доступен вам');
    assert.equal(EN.adminPhonesShared(1), '1 phone shared with you');
    assert.equal(EN.adminPhonesShared(2), '2 phones shared with you');
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

test('the words of the markup are in the dictionaries', () => {
    for (const key of ['signInExplanation', 'signInAction', 'signOut', 'appearance',
        'themeLight', 'themeDark', 'themeSystem', 'language', 'languageSystem']) {
        assert.ok(EN[key], `English has no ${key}`);
        assert.ok(RU[key], `Russian has no ${key}`);
    }
});
