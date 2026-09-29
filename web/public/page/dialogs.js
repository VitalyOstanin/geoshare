// The small windows: help, confirming a write, naming a member or a phone.

import { ageText, positionAge } from '../lib/age.js';
import { outsideTheBox } from '../lib/mode-help.js';
import { labelText } from '../lib/labels.js';
import { WORDS } from '../lib/words.js';
import { ADMINISTRATOR, standingIn } from '../lib/membership.js';
import { writeLabel } from '../lib/store.js';
import { elements, state } from './state.js';
import { knownNote } from './link-ui.js';
import { closeOnClickOutside, tryGroupWrite } from './group-panel.js';
import { ALARM_ICON, BIN_ICON, SPEAKER_ICON } from './siren-ui.js';

/**
 * Opens the help of one screen: each of its buttons drawn the way the screen
 * draws it — [look] is 'plain', 'alarm' or 'filled' — and what it does.
 * [mark] is the markup of the icon, or an svg of the page to copy.
 */
function showHelp(title, entries) {
    elements.helpTitle.textContent = title;
    elements.helpClose.textContent = WORDS.helpClose;
    elements.helpList.replaceChildren(
        ...entries.flatMap(({ mark, look, text }) => {
            const shown = document.createElement('span');
            const said = document.createElement('span');

            shown.className = `help-look ${look}`;

            if (typeof mark === 'string') {
                shown.innerHTML = mark;
            } else {
                const copy = mark.cloneNode(true);

                copy.removeAttribute('hidden');
                shown.append(copy);
            }

            said.textContent = text;

            return [shown, said];
        }),
    );

    if (!elements.help.open) {
        elements.help.showModal();
    }
}

/** The siren and the search, as a row of the panel or of My devices draws them. */
function alarmHelp() {
    return [
        { mark: SPEAKER_ICON, look: 'alarm', text: WORDS.helpSiren },
        { mark: ALARM_ICON, look: 'alarm', text: WORDS.helpSearch },
        { mark: ALARM_ICON, look: 'filled', text: WORDS.helpSearchStanding },
    ];
}

elements.phonesHelp.addEventListener('click', () => {
    const running = standingIn(state.roster, state.member.memberId) === ADMINISTRATOR;
    // Only whoever runs the group has the search in these rows.
    const alarms = alarmHelp().filter((entry) => running || entry.mark !== ALARM_ICON);

    showHelp(WORDS.helpTitleScreen, [
        ...alarms,
        { mark: BIN_ICON, look: 'alarm', text: WORDS.helpRemove },
    ]);
});
elements.adminHelp.addEventListener('click', () => showHelp(WORDS.helpTitleScreen, alarmHelp()));
elements.mapHelp.addEventListener('click', () => {
    const follow = (mode) => elements.followButton.querySelector(`svg[data-mode="${mode}"]`);
    const theme = (name) => document.querySelector(`#theme-choice [data-theme="${name}"] svg`);
    const entries = [
        { mark: follow('none'), look: 'plain', text: WORDS.helpFollowNone },
        { mark: follow('one'), look: 'plain', text: WORDS.helpFollowOne },
        { mark: follow('all'), look: 'plain', text: WORDS.helpFollowAll },
    ];

    // Only where the browser has a full screen to give, as with the button.
    if (!elements.fullScreenButton.hidden) {
        entries.push({
            mark: elements.fullScreenButton.querySelector('svg[data-full="off"]'),
            look: 'plain',
            text: WORDS.helpFullScreen,
        });
    }

    entries.push(
        { mark: theme('light'), look: 'plain', text: WORDS.helpThemeLight },
        { mark: theme('dark'), look: 'plain', text: WORDS.helpThemeDark },
        { mark: theme('system'), look: 'plain', text: WORDS.helpThemeSystem },
    );
    showHelp(WORDS.helpTitleMap, entries);
});
elements.helpClose.addEventListener('click', () => elements.help.close());
closeOnClickOutside(elements.help, () => elements.help.close());

/** The write waiting on the confirming window, or null while it is shut. */
let confirmedWrite = null;

export function askConfirm({ title, note = '', go }, write) {
    confirmedWrite = write;
    elements.confirmTitle.textContent = title;
    elements.confirmNote.textContent = note;
    elements.confirmNote.hidden = note === '';
    elements.confirmGo.textContent = go;
    elements.confirmCancel.textContent = WORDS.searchCancel;

    if (!elements.confirm.open) {
        elements.confirm.showModal();
    }
}

elements.confirmGo.addEventListener('click', () => {
    const write = confirmedWrite;

    elements.confirm.close();

    if (write) {
        tryGroupWrite(write);
    }
});
elements.confirmCancel.addEventListener('click', () => elements.confirm.close());
elements.confirm.addEventListener('close', () => {
    confirmedWrite = null;
});

/**
 * A phone, when it was last in touch, and its charge: after its name where the
 * line stands for the member, and without it where the name is the line above.
 */
export function phoneText(phone, now, named = true) {
    if (phone.onlineAt === null) {
        return named ? WORDS.adminPhoneSilent(phone.name) : WORDS.phoneSilent;
    }

    const age = ageText(positionAge(phone.onlineAt, now));
    const said = [named ? WORDS.adminPhoneOnline(phone.name, age) : WORDS.phoneOnline(age)];

    if (phone.batteryPercent !== null) {
        said.push(`${phone.batteryPercent}%`);
    }

    if (phone.charging) {
        said.push(WORDS.adminPhoneCharging);
    }

    if (knownNote() !== '') {
        said.push(knownNote());
    }

    return said.join(', ');
}

/**
 * The line under a name: who they are to this group, and what this viewer may
 * see of them.
 *
 * Said in one line rather than in marks beside the name, because a mark has to
 * be learnt and this is read once a month.
 */
export function aboutThem(line) {
    const said = [];

    if (line.isYou) {
        said.push(WORDS.adminYou);
    }

    if (line.runsTheGroup) {
        said.push(WORDS.groupRunsIt);
    }

    said.push(
        line.phonesShared === 0 ? WORDS.adminNotShared : WORDS.adminPhonesShared(line.phonesShared),
    );

    return said.join(', ');
}

/**
 * Asks for a name for a phone or a person, for this viewer alone.
 *
 * `kind` is the map the name goes into — `devices` or `members` — and `key`
 * what it is filed under there: the phone's full address, or the account.
 */
export function askName(kind, key) {
    const current = (kind === 'devices' ? state.labels.phones : state.labels.members).get(key);

    state.naming = { kind, key };
    elements.namingTitle.textContent = kind === 'devices' ? WORDS.namePhone : WORDS.namePerson;
    sayNaming(WORDS.nameForYou, false);
    elements.namingTyped.value = current ?? '';
    elements.namingReset.hidden = current === undefined;

    if (!elements.naming.open) {
        elements.naming.showModal();
    }
}

export function closeNaming() {
    state.naming = null;

    if (elements.naming.open) {
        elements.naming.close();
    }
}

/**
 * Writes the name, or takes it away for null, and leaves the answer to the
 * subscription: the name shows up once the database has it, the same way it
 * does when it was given on a phone.
 *
 * The window stays until the database has answered. Closed at once, a refused
 * write left nothing on the screen: the page showed the new name for as long
 * as the write was pending, dropped it on the refusal, and the reason went
 * only to the console.
 */
async function saveName(name) {
    const naming = state.naming;

    if (!naming) {
        return;
    }

    sayNaming(WORDS.nameSaving, true);

    try {
        await writeLabel(state.member.memberId, naming.kind, naming.key, name);

        if (state.naming === naming) {
            closeNaming();
        }
    } catch (error) {
        console.error('labels:', error);

        if (state.naming === naming) {
            sayNaming(WORDS.nameFailed(error?.code ?? error?.message ?? String(error)), false);
        }
    }
}

/** What the naming window says under its title, and whether it is busy saving. */
function sayNaming(note, busy) {
    elements.namingNote.textContent = note;
    elements.namingNote.classList.toggle('naming-failed', !busy && note !== WORDS.nameForYou);
    elements.namingSave.disabled = busy;
    elements.namingReset.disabled = busy;
    elements.namingTyped.disabled = busy;
}

elements.namingSave.addEventListener('click', () => saveName(labelText(elements.namingTyped.value)));
elements.namingReset.addEventListener('click', () => saveName(null));
elements.namingTyped.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
        event.preventDefault();
        saveName(labelText(elements.namingTyped.value));
    }
});
elements.namingCancel.addEventListener('click', closeNaming);
elements.naming.addEventListener('close', () => {
    state.naming = null;
});
// The escape key that closes this window is not also meant for the card
// behind it, which would close on the same press.
elements.naming.addEventListener('keydown', (event) => event.stopPropagation());
// A click beside the window closes it, as it does for the panel.
elements.naming.addEventListener('click', (event) => {
    if (event.target === elements.naming
        && outsideTheBox(elements.naming.getBoundingClientRect(), event)) {
        closeNaming();
    }
});
