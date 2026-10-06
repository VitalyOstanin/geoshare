package io.github.vitalyostanin.geoshare.membership

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan

/**
 * The roster as a screen sees it, and whether the database has answered at
 * all since the screen came up.
 *
 * The two come from one subscription: taken from two, the answer could
 * arrive before the roster it carries, and the screen would count a member
 * outside the group for that moment.
 */
data class RosterSeen(val roster: Roster?, val answered: Boolean) {
    /**
     * Whether [uid] is known to stand outside the group: the database has
     * answered, and either there is no group or the roster leaves them out.
     * Silence is not counted, so a device offline is never told it is out.
     */
    fun outside(uid: String): Boolean = answered &&
        standingIn(roster, uid).let { it == Standing.Ungrouped || it == Standing.Outsider }
}

/**
 * Every [RosterRead] folded into what a screen shows: a refusal reads as no
 * group, and a dropped connection leaves the last roster as true as it was.
 */
fun rosterSeen(reads: Flow<RosterRead>): Flow<RosterSeen> =
    reads.scan(RosterSeen(roster = null, answered = false)) { seen, read ->
        when (read) {
            is RosterRead.Read -> RosterSeen(read.roster, answered = true)
            RosterRead.Refused -> RosterSeen(roster = null, answered = true)
            RosterRead.Unreachable -> seen
        }
    }.distinctUntilChanged()
