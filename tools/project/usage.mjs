// How much of the free plan a project has used, from Cloud Monitoring, for
// `tools/project.sh show --usage`. Everything comes from the environment the
// script sets: TOKEN, the access token, rather than an argument, so that it
// does not show in the list of processes; PROJECT; FORMAT, "json" for the
// report as one line of JSON and anything else for the table; DAYS, the
// number of earlier quota days to add; READS, WRITES, DELETES and STORED, the
// free plan's limits.
import { WEEKDAYS, localDay, midnight, pacificDay, reportLines, stamp } from './quota.mjs';

const project = process.env.PROJECT;
const token = process.env.TOKEN;
const limits = {
    reads: Number(process.env.READS),
    writes: Number(process.env.WRITES),
    deletes: Number(process.env.DELETES),
    stored: Number(process.env.STORED),
};
const now = new Date(Math.floor(Date.now() / 1000) * 1000);
const today = pacificDay(now);
const quotaDay = midnight(today.year, today.month, today.day);

async function series(metric, start, aligner, period, end = now) {
    const query = new URLSearchParams({
        'filter': `metric.type = "${metric}"`,
        'interval.startTime': stamp(start),
        'interval.endTime': stamp(end),
        'aggregation.alignmentPeriod': `${Math.max(period, 60)}s`,
        'aggregation.perSeriesAligner': aligner,
        'aggregation.crossSeriesReducer': 'REDUCE_SUM',
    });
    const response = await fetch(
        `https://monitoring.googleapis.com/v3/projects/${project}/timeSeries?${query}`,
        { headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(60_000) },
    );

    if (!response.ok) {
        throw new Error(`Cloud Monitoring answered HTTP ${response.status} for ${metric}`);
    }

    const found = (await response.json()).timeSeries ?? [];

    // Newest first; a metric nothing has been counted in yet has no series.
    return found.flatMap((one) => one.points).map((point) => Number(Object.values(point.value)[0]));
}

async function counted(metric, start = quotaDay, end = now) {
    const period = Math.trunc((end - start) / 1000);
    const values = await series(metric, start, 'ALIGN_SUM', period, end);

    return Math.trunc(values.reduce((sum, value) => sum + value, 0));
}

async function latest(metric) {
    const values = await series(metric, new Date(now - 2 * 24 * 3600 * 1000), 'ALIGN_MAX', 3600);

    return values.length ? Math.trunc(values[0]) : 0;
}

function progress(line) {
    // Each question to Cloud Monitoring takes a moment, and a dozen of them in
    // a row would otherwise read as the script having stopped.
    process.stderr.write(`    ${line}\n`);
}

// Each earlier quota day asked for on its own, from one Pacific midnight to
// the next, so that no window straddles a reset.
async function earlierDays() {
    const earlier = [];

    for (let back = Number(process.env.DAYS); back > 0; back--) {
        const start = midnight(today.year, today.month, today.day - back);
        const end = midnight(today.year, today.month, today.day - back + 1);

        progress(`Firestore on ${WEEKDAYS[start.getDay()]} ${localDay(start)}`);
        earlier.push({
            from: stamp(start),
            reads: await counted('firestore.googleapis.com/document/read_count', start, end),
            writes: await counted('firestore.googleapis.com/document/write_count', start, end),
            deletes: await counted('firestore.googleapis.com/document/delete_count', start, end),
        });
    }

    return earlier;
}

async function firestoreToday() {
    progress('Firestore today');

    return {
        reads: { used: await counted('firestore.googleapis.com/document/read_count'),
            limit: limits.reads },
        writes: { used: await counted('firestore.googleapis.com/document/write_count'),
            limit: limits.writes },
        deletes: { used: await counted('firestore.googleapis.com/document/delete_count'),
            limit: limits.deletes },
        storedBytes: {
            used: await latest('firestore.googleapis.com/storage/data_and_index_storage_bytes'),
            limit: limits.stored,
        },
    };
}

async function hostingNow() {
    progress('Hosting');

    return {
        sentBytesThisMonth: {
            used: await latest('firebasehosting.googleapis.com/network/monthly_sent'),
            limit: await latest('firebasehosting.googleapis.com/network/monthly_sent_limit'),
        },
        storedBytes: { used: await latest('firebasehosting.googleapis.com/storage/total_bytes'),
            limit: await latest('firebasehosting.googleapis.com/storage/limit') },
    };
}

async function main() {
    const earlier = await earlierDays();
    const firestore = await firestoreToday();
    const hosting = await hostingNow();
    const report = {
        projectId: project,
        quotaDayFrom: stamp(quotaDay),
        firestore,
        earlierDays: earlier,
        hosting,
    };

    if (process.env.FORMAT === 'json') {
        console.log(JSON.stringify(report));

        return;
    }

    for (const line of reportLines(report, limits)) {
        console.log(line);
    }
}

main().catch((error) => {
    console.error(error.message);
    process.exit(1);
});
