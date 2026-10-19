// The results tools/project.sh prints on standard output under --json, one
// object per command. The step texts and console links come in from the
// shell, which keeps the wording once for the human-readable messages too.

// What every result of `create` and `show` names: the project, its site and
// the two addresses on that site the project owner is given.
function siteFields(projectId, site) {
    return {
        projectId,
        site,
        connectLink: `https://${site}.web.app/join`,
        settingsUrl: `https://${site}.web.app/geoshare.json`,
    };
}

// The result of `create`: both console steps are always left, since the
// project it made cannot have sign-in with Google on yet.
export function createResult({ projectId, site, steps }) {
    return {
        ...siteFields(projectId, site),
        manualSteps: steps,
        done: false,
    };
}

// The result of `show`: the Google step only while sign-in with Google is
// still off, and the free plan's usage when it was asked for.
export function showResult({ projectId, site, googleOn, googleStep, usage }) {
    const result = {
        ...siteFields(projectId, site),
        manualSteps: googleOn ? [] : [googleStep],
        done: googleOn,
    };

    if (usage) {
        result.usage = usage;
    }

    return result;
}

export function updateResult({ projectId, site }) {
    return { projectId, site, deployed: true };
}

// Google keeps a project shut down for 30 days before it is gone; the moment
// is given to the second, as the other times this script prints are.
export function deleteResult({ projectId, now }) {
    const until = new Date(now + 30 * 24 * 3600 * 1000);

    return {
        projectId,
        deleteRequested: true,
        restorableUntil: until.toISOString().replace(/\.\d+Z$/, 'Z'),
    };
}
