// The window a problem is reported from, opened from the help: what the viewer
// writes, the part of the page's log that goes with it, and the link that
// opens GitHub's form for a new issue with both in it.

import { browserOf } from '../lib/fault.js';
import { pageLog } from '../lib/page-log.js';
import { buildIssue } from '../lib/report.js';
import { WORDS } from '../lib/words.js';
import { elements } from './state.js';
import { closeOnClickOutside } from './group-panel.js';

/** The browser and system, as an issue names them. */
function browser() {
    return browserOf(navigator.userAgent).join(' · ');
}

/** Fills the link and the shown tail from what is written now. */
function draw() {
    const log = pageLog();
    const draft = buildIssue(elements.reportWhat.value, browser(), log);

    elements.reportOpen.href = draft.url;
    elements.reportTail.textContent = log.length > 0 ? draft.tail : WORDS.reportEmpty;
    // Opened at its end: the last lines are the ones about the trouble.
    elements.reportTail.scrollTop = elements.reportTail.scrollHeight;
}

function openReport() {
    elements.help.close();
    elements.reportTitle.textContent = WORDS.reportTitle;
    elements.reportWhatLabel.textContent = WORDS.reportWhat;
    elements.reportSentLabel.textContent = WORDS.reportSent;
    elements.reportWarning.textContent = WORDS.reportWarning;
    elements.reportOpen.textContent = WORDS.reportOpenIssue;
    elements.reportCopy.textContent = WORDS.reportCopyLog;
    elements.reportCopy.hidden = pageLog().length === 0;
    elements.reportClose.textContent = WORDS.reportClose;
    elements.reportWhat.value = '';

    elements.report.showModal();
    draw();
    elements.reportWhat.focus();
}

elements.helpReport.textContent = WORDS.reportOpen;
elements.helpReport.addEventListener('click', openReport);
elements.reportWhat.addEventListener('input', draw);

// The whole log, not only the tail the address has room for: it goes where
// the viewer pastes it, an issue's comment or a message to the group's owner.
elements.reportCopy.addEventListener('click', async () => {
    try {
        await navigator.clipboard.writeText(pageLog().join('\n'));
        elements.reportCopy.textContent = WORDS.reportCopied;
    } catch (error) {
        console.warn('copying the log:', error);
    }
});
elements.reportClose.addEventListener('click', () => elements.report.close());
closeOnClickOutside(elements.report, () => elements.report.close());
