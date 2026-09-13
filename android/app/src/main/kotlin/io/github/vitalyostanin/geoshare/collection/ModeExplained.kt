package io.github.vitalyostanin.geoshare.collection

import androidx.annotation.StringRes
import io.github.vitalyostanin.geoshare.R

/** One line of the help: a mode, when it happens and what the phone does in it. */
data class ModeExplained(
    val mode: CollectionMode,
    @param:StringRes val name: Int,
    @param:StringRes val whenItHappens: Int,
    @param:StringRes val whatItDoes: Int,
)

/**
 * The five ways of collecting, explained in the order the help reads them:
 * hardest working first.
 *
 * Not the order of the enumeration, which is the order the entries happened to
 * be written in. A member opening this has just seen one name on a card and
 * wants to know where it stands among the others, and "how much is this phone
 * doing" is the axis that answers that.
 */
fun modesExplained(): List<ModeExplained> = listOf(
    ModeExplained(
        CollectionMode.Viewing,
        R.string.mode_viewing,
        R.string.mode_help_when_viewing,
        R.string.mode_help_asks_viewing,
    ),
    ModeExplained(
        CollectionMode.Moving,
        R.string.mode_moving,
        R.string.mode_help_when_moving,
        R.string.mode_help_asks_moving,
    ),
    ModeExplained(
        CollectionMode.Standby,
        R.string.mode_standby,
        R.string.mode_help_when_standby,
        R.string.mode_help_asks_standby,
    ),
    ModeExplained(
        CollectionMode.Night,
        R.string.mode_night,
        R.string.mode_help_when_night,
        R.string.mode_help_asks_night,
    ),
    ModeExplained(
        CollectionMode.Idle,
        R.string.mode_idle,
        R.string.mode_help_when_idle,
        R.string.mode_help_asks_idle,
    ),
)
