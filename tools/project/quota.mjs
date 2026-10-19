// The calendar of the free plan and the table `show --usage` prints. Firestore's
// daily quota resets around midnight Pacific time, so the quota day is worked
// out in Los Angeles; the moments in the table are shown in the local time of
// whoever runs the script. Pure, so that the arithmetic across a change of
// daylight saving time is tested; usage.mjs does the asking.

const LA = 'America/Los_Angeles';

export const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

// The calendar day and wall time of a moment in the given zone.
function wallIn(zone, moment) {
    const parts = new Intl.DateTimeFormat('en-US', {
        timeZone: zone, hourCycle: 'h23',
        year: 'numeric', month: 'numeric', day: 'numeric',
        hour: 'numeric', minute: 'numeric', second: 'numeric',
    }).formatToParts(moment);

    return Object.fromEntries(parts.map((p) => [p.type, Number(p.value)]));
}

function offsetOf(moment) {
    const w = wallIn(LA, moment);

    return Date.UTC(w.year, w.month - 1, w.day, w.hour, w.minute, w.second) - moment.getTime();
}

// The calendar day in Los Angeles at the given moment.
export function pacificDay(moment) {
    const { year, month, day } = wallIn(LA, moment);

    return { year, month, day };
}

// Midnight in Los Angeles of the given calendar day; a day past the end of
// the month rolls over, as Date.UTC does. The offset is taken twice, the
// second time at the moment the first guess lands on, so that a day that
// starts in one offset and ends in the other still comes out right.
export function midnight(year, month, day) {
    const wall = Date.UTC(year, month - 1, day);
    let moment = wall - offsetOf(new Date(wall));

    moment = wall - offsetOf(new Date(moment));

    return new Date(moment);
}

// A moment to the second, as Cloud Monitoring takes it and the report gives it.
export function stamp(moment) {
    return moment.toISOString().replace(/\.\d+Z$/, 'Z');
}

const pad = (n) => String(n).padStart(2, '0');

export const localDay = (m) => `${m.getFullYear()}-${pad(m.getMonth() + 1)}-${pad(m.getDate())}`;
const localTime = (m) => `${pad(m.getHours())}:${pad(m.getMinutes())}`;

// A number of bytes in the largest unit that keeps it above one, up to GiB.
export function size(value) {
    for (const unit of ['B', 'KiB', 'MiB', 'GiB']) {
        if (value < 1024 || unit === 'GiB') {
            return unit === 'B' ? `${value.toFixed(0)} ${unit}` : `${value.toFixed(1)} ${unit}`;
        }
        value /= 1024;
    }
}

// How much of a limit is used; a limit Cloud Monitoring did not report reads
// as unknown rather than as a division by zero.
export function share(entry) {
    return entry.limit ? `${(100 * entry.used / entry.limit).toFixed(1)}%` : '?';
}

// The table for a person to read, one string per line: today's use of every
// limit, then, when earlier quota days were asked for, a row for each of them
// held against the daily limits.
export function reportLines(report, limits) {
    const { firestore, hosting, earlierDays } = report;
    const quotaDay = new Date(report.quotaDayFrom);
    const since = `${localDay(quotaDay)} ${localTime(quotaDay)}`;
    const rows = [
        [`Firestore reads since ${since}`, firestore.reads, String],
        ['Firestore writes', firestore.writes, String],
        ['Firestore deletes', firestore.deletes, String],
        ['Firestore stored data', firestore.storedBytes, size],
        ['Hosting transfer this month', hosting.sentBytesThisMonth, size],
        ['Hosting storage', hosting.storedBytes, size],
    ];
    const out = rows.map(([what, entry, shown]) =>
        `${what.padEnd(42)} ${shown(entry.used).padStart(11)} of ` +
        `${shown(entry.limit).padStart(11)}  ${share(entry).padStart(6)}`);

    if (earlierDays.length) {
        out.push('');
        out.push(`${'quota day from'.padEnd(18)} ${'reads'.padStart(13)} ` +
            `${'writes'.padStart(13)} ${'deletes'.padStart(13)}`);

        for (const day of earlierDays) {
            const start = new Date(day.from);
            const cells = ['reads', 'writes', 'deletes'].map((kind) =>
                `${day[kind]} ${(100 * day[kind] / limits[kind]).toFixed(0)}%`);
            const label = `${WEEKDAYS[start.getDay()]} ${localDay(start)} ${localTime(start)}`;

            out.push(`${label.padEnd(18)} ${cells[0].padStart(13)} ${cells[1].padStart(13)} ` +
                `${cells[2].padStart(13)}`);
        }
    }

    return out;
}
