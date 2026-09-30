// Wires the join page: works out what the link says, shows it, and puts the
// two buttons to work. No sign-in and no database here, and nothing from
// Firebase — before somebody signs in, this page and the application both
// know only what the link and geoshare.json carry, and this page does not
// even read geoshare.json.

import { JOIN_WORDS } from './lib/join-words.js';
import {
    RELEASES_URL,
    buildIntentUrl,
    isAndroid,
    joinCaption,
    parseInvitation,
    siteFromHost,
} from './lib/join.js';

const invitation = parseInvitation(window.location.hash);
const { title, invitedFor } = joinCaption(JOIN_WORDS, invitation);

document.getElementById('join-title').textContent = title;
document.getElementById('join-text').textContent =
    JOIN_WORDS.joinText(siteFromHost(window.location.host));

const invitedForElement = document.getElementById('join-invited-for');

if (invitedFor !== null) {
    invitedForElement.textContent = invitedFor;
    invitedForElement.hidden = false;
}

const openButton = document.getElementById('join-open');
const downloadButton = document.getElementById('join-download');
const saidElement = document.getElementById('join-said');

openButton.textContent = JOIN_WORDS.joinOpen;
downloadButton.textContent = JOIN_WORDS.joinDownload;
downloadButton.href = RELEASES_URL;

if (isAndroid(navigator.userAgent)) {
    // A real <a href>, not something the script opens itself: Chrome only
    // launches an application from a link a person taps.
    openButton.href = buildIntentUrl(window.location.host, invitation, RELEASES_URL);
} else {
    // Nothing to launch on a browser that is not Android's, so the link
    // goes to the clipboard instead, to be carried to a phone that has one.
    openButton.addEventListener('click', async (event) => {
        event.preventDefault();

        try {
            await navigator.clipboard.writeText(window.location.href);
            saidElement.textContent = JOIN_WORDS.joinLinkCopied;
            saidElement.hidden = false;
        } catch {
            // A browser that refuses the clipboard leaves the link where it
            // was opened from — the address bar — to be copied by hand.
        }
    });
}
