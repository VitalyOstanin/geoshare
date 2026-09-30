// The words the join page speaks, picked the same way as the main page's:
// the viewer's stored answer, or the first language their browser asks for.
// The two pages share the storage key, so a choice made on one is honoured
// on the other, but not the dictionary — see lib/words/join-en.js for why.

import { JOIN_EN } from './words/join-en.js';
import { JOIN_RU } from './words/join-ru.js';
import { spokenLanguage } from './language.js';

/** Every language the join page has words for, by its tag. */
export const JOIN_DICTIONARIES = { en: JOIN_EN, ru: JOIN_RU };

/** The one it is speaking now. */
export const JOIN_WORDS = JOIN_DICTIONARIES[spokenLanguage()];
