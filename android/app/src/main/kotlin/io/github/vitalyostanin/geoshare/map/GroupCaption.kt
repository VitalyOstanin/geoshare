package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.cutToLimit
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.trimText

/** How long a group's name may be, the rules' own limit (ADR-0016). */
const val GROUP_NAME_LIMIT = 60

/**
 * What is written for a group's name: trimmed, held to the length the rules
 * check, and null once that leaves nothing.
 *
 * The same shape [labelText] gives a device or a person's name, so a field
 * left blank founds a group with none of its own rather than one named the
 * empty string, which the rules would refuse anyway (`nameFits`).
 */
fun groupNameText(typed: String): String? =
    cutToLimit(trimText(typed), GROUP_NAME_LIMIT).ifEmpty { null }

/**
 * What the group is called, on the panel and on the founding screen.
 *
 * A name it was given (ADR-0016) is shown as it stands. A group with none is
 * called after whoever founded it — read the same way any other member's name
 * is, by their profile where there is one or else the short form of their id
 * ([shortName]) — with the caption's shape (ADR-0019) coming from the
 * resources, which this file does not read: [unnamedPattern] takes it already
 * localized, the way [memberName] takes its own caption.
 */
fun groupCaption(
    roster: Roster,
    profiles: Map<String, MemberProfile>,
    /** "%1$s's group" or its equivalent; %1$s takes the founder's name. */
    unnamedPattern: String,
): String {
    val name = roster.name?.let { trimText(it) }

    if (!name.isNullOrEmpty()) {
        return name
    }

    val founderId = roster.founder.orEmpty()
    val founderName = profiles[founderId]?.displayName ?: shortName(founderId)

    return String.format(unnamedPattern, founderName)
}
