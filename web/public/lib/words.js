// The words the page is speaking, settled once.
//
// Every module imports WORDS and uses it as it stands: the language is decided
// before any of them run, and changing it loads the page again rather than
// redrawing what is on screen. A redraw would have to reach the markup, the
// cards, the bubbles and the help at once, and missing one corner of that is
// worse than the moment a reload costs.

import { EN } from './words/en.js';
import { RU } from './words/ru.js';
import { spokenLanguage } from './language.js';

/** Every language the page has words for, by its tag. */
export const DICTIONARIES = { en: EN, ru: RU };

/** The one it is speaking now. */
export const SPEAKING = spokenLanguage();

export const WORDS = DICTIONARIES[SPEAKING];
