package io.github.vitalyostanin.geoshare.membership

/** What leaving a group can do for the person signed in. */
enum class Leave {
    /**
     * Take themselves off the roster, with everything they wrote into the
     * group, and then drop the group from this device.
     */
    ROSTER,

    /**
     * Only drop the group from this device: they run it, and nobody would be
     * left to. They stay on its roster, and their other devices stay in it.
     */
    DEVICE_ONLY,

    /**
     * Only drop the group from this device: they are not on its roster any
     * more, or it could not be read, so there is nothing to take off it.
     */
    NOT_ON_ROSTER,
}

/**
 * What leaving the group [roster] describes can do for [uid].
 *
 * Somebody who runs the group cannot leave it to nobody: a group nobody runs
 * is a group nobody can add to or take from, and the console is the only way
 * back. The rules refuse it for a named administrator; the founder runs the
 * group without being named, and the app holds them to the same, which the
 * rules alone would not. Somebody no longer on the roster, or whose roster
 * cannot be read, has nothing to take off it.
 */
fun howToLeave(roster: Roster?, uid: String): Leave {
    if (roster == null || uid !in roster.members) {
        return Leave.NOT_ON_ROSTER
    }

    if (!runsTheGroup(roster, uid)) {
        return Leave.ROSTER
    }

    val left = rosterWithout(roster, uid)

    return if (left.members.any { runsTheGroup(left, it) }) Leave.ROSTER else Leave.DEVICE_ONLY
}
