// The menus over the map: signing in, following, the account menu and settings.

import { FOLLOW_ALL, FOLLOW_NONE, FOLLOW_ONE } from '../lib/following.js';
import {
    awaitingFollowTarget,
    chooseFollow,
    followMode,
    stopAwaitingFollowTarget,
} from '../lib/map.js';
import { applyTheme, readTheme, writeTheme } from '../lib/theme.js';
import { readLanguage, writeLanguage } from '../lib/language.js';
import { SPEAKING, WORDS } from '../lib/words.js';
import { leave, signIn, signInReturned } from '../lib/store.js';
import { elements, state } from './state.js';
import { drawGroup } from './group-panel.js';

elements.signInButton.addEventListener('click', async () => {
    elements.signInError.textContent = '';

    try {
        await signIn();
    } catch (error) {
        console.warn('sign-in failed', error);
        elements.signInError.textContent = signInTrouble(error);
    }
});

/**
 * What to say when signing in did not happen.
 *
 * The answers the phone gives, less the one for a viewer who changed their
 * mind: leaving Google's page brings them back with no error at all. What is
 * left is the network not being there, this build pointed at a project that
 * does not want it, and something else.
 */
function signInTrouble(error) {
    if (error.code === 'auth/network-request-failed') {
        return WORDS.signInOffline;
    }

    if (error.code === 'auth/unauthorized-domain' || error.code === 'auth/operation-not-allowed') {
        return WORDS.signInMisconfigured;
    }

    // The code is what tells one unexpected failure from another, and the
    // viewer who reads it out is often the only way it reaches anybody.
    return error.code ? `${WORDS.signInFailed} (${error.code})` : WORDS.signInFailed;
}

// A sign-in that went to Google and came back failed is said here, on the
// page it came back to.
signInReturned().catch((error) => {
    elements.signInError.textContent = signInTrouble(error);
});

elements.signOutButton.addEventListener('click', () => leave());

// How long the name of a newly chosen mode stands beside its mark.
export const FOLLOW_ANNOUNCE_MS = 2500;

// How long the page waits for a member to be picked after "follow a member"
// was asked for with nobody to follow. Long enough to find somebody in the
// strip, short enough that the strip does not stay outlined all afternoon.
const FOLLOW_WAIT_MS = 15000;

elements.followButton.addEventListener('click', (event) => {
    event.stopPropagation();
    showMenu(false);
    showFollowMenu(elements.followMenu.hidden);
});

elements.followMenu.addEventListener('click', (event) => event.stopPropagation());

for (const button of elements.followMenuButtons) {
    // Each entry carries the same mark as the button does in that mode.
    const marks = elements.followButton.querySelectorAll('svg');
    const mark = [...marks].find((each) => each.dataset.mode === button.dataset.mode);

    button.prepend(mark.cloneNode(true));
    button.querySelector('svg').removeAttribute('hidden');

    // chooseFollow moves the map itself and calls the map's own
    // onCardChange, which is render; the wait it may start is timed here.
    button.addEventListener('click', () => {
        showFollowMenu(false);

        clearTimeout(state.followWaitEnds);

        if (!chooseFollow(state.view, button.dataset.mode)) {
            state.followWaitEnds = setTimeout(
                () => stopAwaitingFollowTarget(state.view),
                FOLLOW_WAIT_MS,
            );
        }
    });
}

/**
 * Escape closes one thing at a time, the last opened first: a window closes
 * itself — the browser does that for a modal dialog, topmost first — then a
 * menu, then the card on the map (see `lib/map.js`). Whatever this closes is
 * marked handled, so that the card under a menu stays for the next press.
 */
document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape' || event.defaultPrevented || document.querySelector('dialog[open]')) {
        return;
    }

    if (!elements.accountMenu.hidden) {
        showMenu(false);
        elements.accountButton.focus();
        event.preventDefault();
    } else if (!elements.followMenu.hidden) {
        showFollowMenu(false);
        elements.followButton.focus();
        event.preventDefault();
    } else if (state.view !== null && awaitingFollowTarget(state.view)) {
        stopAwaitingFollowTarget(state.view);
        event.preventDefault();
    }
});

export function showFollowMenu(open) {
    if (open) {
        sayFollowMenu();
    }

    elements.followMenu.hidden = !open;
    elements.followButton.setAttribute('aria-expanded', String(open));
}

/** The menu's words, with the current mode marked and the member named. */
function sayFollowMenu() {
    const mode = followMode(state.view);
    const titles = {
        [FOLLOW_NONE]: WORDS.followMenuNone,
        [FOLLOW_ONE]: state.followTargetName
            ? WORDS.followMenuOneNamed(state.followTargetName)
            : WORDS.followMenuOne,
        [FOLLOW_ALL]: WORDS.followMenuAll,
    };
    const notes = {
        [FOLLOW_NONE]: WORDS.followMenuNoneNote,
        [FOLLOW_ONE]: WORDS.followMenuOneNote,
        [FOLLOW_ALL]: WORDS.followMenuAllNote,
    };

    for (const button of elements.followMenuButtons) {
        button.querySelector('.follow-item-title').textContent = titles[button.dataset.mode];
        button.querySelector('.follow-item-note').textContent = notes[button.dataset.mode];
        button.setAttribute('aria-checked', String(button.dataset.mode === mode));
    }

    elements.followHint.textContent = WORDS.followMenuHint;
}

elements.accountButton.addEventListener('click', (event) => {
    event.stopPropagation();
    showMenu(elements.accountMenu.hidden);
});

// Anywhere else means "not that": the menu is a corner of the map, and the map
// is what the viewer came for.
document.addEventListener('click', () => {
    showMenu(false);
    showFollowMenu(false);
});

// Inside the menu is not anywhere else: the theme buttons live there, and a
// menu that shuts on the first press hides the change it was pressed for.
elements.accountMenu.addEventListener('click', (event) => event.stopPropagation());

/**
 * What the page is painted in. Kept here rather than in the account: it
 * answers for the screen the map is read on, not for the member reading it.
 */
export let theme = readTheme();

applyTheme(theme);
showTheme();

for (const button of elements.themeButtons) {
    button.addEventListener('click', () => {
        theme = button.dataset.theme;

        applyTheme(theme);
        writeTheme(theme);
        showTheme();
    });
}

function showTheme() {
    for (const button of elements.themeButtons) {
        button.setAttribute('aria-pressed', String(button.dataset.theme === theme));
    }
}

export function showMenu(open) {
    // The roster is drawn when it changes, but which of its names lead to a
    // card depends on the positions, which change all the time: drawn again
    // as the menu opens rather than on every position.
    if (open) {
        drawGroup();
    }

    elements.accountMenu.hidden = !open;
    elements.accountButton.setAttribute('aria-expanded', String(open));
}

/**
 * The language the page is read in. The answer is the viewer's, the language
 * it comes out as is theirs or their browser's, and the two are not the same:
 * "system" is an answer that follows the browser rather than a language.
 */
export const language = readLanguage();

sayMarkup();
showLanguage();

for (const button of elements.languageButtons) {
    button.addEventListener('click', () => {
        if (button.dataset.language === language) {
            return;
        }

        writeLanguage(button.dataset.language);

        // Loading the page again rather than redrawing it: every word on
        // screen comes from the dictionary, including the ones the markup was
        // painted with, and a redraw that misses one corner is worse than the
        // moment this costs. Nothing is lost — the map is drawn from the
        // documents, and the subscriptions open again on their own.
        window.location.reload();
    });
}

elements.settingsButton.addEventListener('click', () => {
    showMenu(false);
    elements.settings.showModal();
});
elements.settingsClose.addEventListener('click', () => elements.settings.close());

/**
 * The map on the whole screen, and back. Offered only where the browser has a
 * full screen to give: a phone's browser that has none would leave a button
 * that does nothing. The mark follows the browser rather than the press, since
 * the browser leaves full screen on its own gestures too.
 */
export const FULL_SCREEN = document.fullscreenEnabled === true;

elements.fullScreenButton.addEventListener('click', () => {
    const asked = document.fullscreenElement
        ? document.exitFullscreen()
        : document.documentElement.requestFullscreen();

    // A refusal leaves the page as it was, and the mark says so already.
    asked.catch(() => {});
});
document.addEventListener('fullscreenchange', () => showFullScreen());

export function showFullScreen() {
    const full = document.fullscreenElement !== null;
    const words = full ? WORDS.fullScreenLeave : WORDS.fullScreen;

    for (const mark of elements.fullScreenButton.querySelectorAll('svg')) {
        mark.hidden = (mark.dataset.full === 'on') !== full;
    }

    elements.fullScreenButton.setAttribute('aria-pressed', String(full));
    elements.fullScreenButton.setAttribute('aria-label', words);
    elements.fullScreenButton.title = words;
}

/**
 * Says the words of index.html in the language the page settled on.
 *
 * The markup is written in English and so is painted in English; this is what
 * a viewer reading in the other language sees instead, as soon as the modules
 * have run. The alternative — markup with no words in it — starts the page
 * blank and fills it in, which is worse for everybody to save a moment for
 * some.
 */
function sayMarkup() {
    const themes = {
        light: WORDS.themeLight,
        dark: WORDS.themeDark,
        system: WORDS.themeSystem,
    };
    const languages = {
        ru: WORDS.languageRussian,
        en: WORDS.languageEnglish,
        system: WORDS.languageSystem,
    };

    document.documentElement.lang = SPEAKING;

    elements.signInExplanation.textContent = WORDS.signInExplanation;
    elements.signInButton.textContent = WORDS.signInAction;
    elements.signInWaiting.textContent = WORDS.signInWaiting;
    elements.signOutButton.textContent = WORDS.signOut;
    elements.namingSave.textContent = WORDS.nameSave;
    elements.namingReset.textContent = WORDS.nameReset;
    elements.namingCancel.textContent = WORDS.nameCancel;
    elements.themeLabel.textContent = WORDS.appearance;
    elements.languageLabel.textContent = WORDS.language;
    elements.settingsButton.textContent = WORDS.settings;
    elements.settingsTitle.textContent = WORDS.settings;
    elements.settingsClose.textContent = WORDS.close;
    elements.mapHelp.title = WORDS.helpOpen;
    elements.mapHelp.setAttribute('aria-label', WORDS.helpOpen);
    // The group section says its own words, since what stands there depends on
    // the roster rather than on the markup alone.
    drawGroup();

    // The theme buttons are marks: their words are what a screen reader says
    // and what a pointer resting on one shows.
    for (const button of elements.themeButtons) {
        button.setAttribute('aria-label', themes[button.dataset.theme]);
        button.title = themes[button.dataset.theme];
    }

    for (const button of elements.languageButtons) {
        button.textContent = languages[button.dataset.language];
    }
}

function showLanguage() {
    for (const button of elements.languageButtons) {
        button.setAttribute('aria-pressed', String(button.dataset.language === language));
    }
}
