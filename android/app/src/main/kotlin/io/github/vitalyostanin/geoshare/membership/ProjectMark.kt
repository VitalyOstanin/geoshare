package io.github.vitalyostanin.geoshare.membership

/**
 * What the project's mark (`project/group`) says to a device that is in no
 * group of it.
 *
 * Three answers rather than two, because a read can fail: the screens offer
 * to found a group for [Free] and for [Unknown] alike, and tell the device the
 * project is spoken for only on [Taken].
 */
sealed interface ProjectMark {
    /** The project has a group, [group] being its id. */
    data class Taken(val group: String) : ProjectMark

    /** The mark was read and is not there. */
    data object Free : ProjectMark

    /** The mark could not be read, or is not what the rules write. */
    data object Unknown : ProjectMark
}

/**
 * [ProjectMark] for a mark document that [exists] and holds [group] in its
 * field, as the read returned them.
 *
 * A mark with anything but a non-empty id in it is not one the rules would
 * have let in, and is no reason to withhold the offer: it reads as unknown.
 */
fun projectMarkOf(exists: Boolean, group: Any?): ProjectMark = when {
    !exists -> ProjectMark.Free
    group is String && group.isNotEmpty() -> ProjectMark.Taken(group)
    else -> ProjectMark.Unknown
}

/** Whether the screens say the project is taken instead of offering a group of its own. */
fun ProjectMark.isTaken(): Boolean = this is ProjectMark.Taken
