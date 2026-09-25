package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val FILE = "group"
private const val KEY_NAME = "name"
private const val TAG = "geoshare.group"

/**
 * The group this phone writes into, and where its name is kept.
 *
 * It used to be a constant of the build, and could be while every installation
 * belonged to one household. A group is now named after the account that made
 * it, and an account is not known when the build is made, so the name is an
 * answer this phone remembers rather than one it was compiled with.
 *
 * Remembered outside the session, beside the sharing switch and for the same
 * reason: the service starts after a restart, before anything has asked
 * Firebase who is signed in, and it has to know where to write before it can
 * ask anybody anything.
 *
 * Held in memory as well, because the paths are built in seven files that have
 * no context to read a setting with, and reading a file on each of them would
 * be a read per position written. The value in memory is set before the first
 * of those paths can be built, which is what [start] is for.
 */
object CurrentGroup {
    private val remembered = MutableStateFlow(Writing(INHERITED, 0))

    /** The name to build a path out of. */
    val name: String
        get() = remembered.value.name

    /**
     * Which group this phone is writing into, as something to follow.
     *
     * The screens read every one of their subscriptions out of a path built
     * around the name, and a subscription taken out before this phone was let
     * in goes on being refused afterwards: the database drops a refused
     * listener, and the one taken out in its place waits longer each time. So
     * the screens follow this and take their subscriptions out again whenever
     * it changes — which is on joining a group as well as on moving to one,
     * because joining the group the phone already wrote into changes what the
     * rules allow without changing the name.
     */
    val writings: StateFlow<Writing> = remembered.asStateFlow()

    /** Reads back which group this phone joined, before anything reaches the database. */
    fun start(context: Context) {
        val saved = settingOf(context).getString(KEY_NAME, null) ?: INHERITED

        remembered.value = Writing(saved, remembered.value.turn + 1)

        AppLog.i(TAG, "writing into the group $saved")
    }

    /**
     * Says this phone is now in another group, and remembers it for the next
     * start.
     *
     * The two happen together on purpose: a phone that joined a group and
     * forgot it by the morning would go on writing into the group it left, and
     * the rules would refuse every write without anything on the screen saying
     * why.
     */
    fun moveTo(context: Context, group: String) {
        settingOf(context).edit().putString(KEY_NAME, group).apply()
        remembered.value = Writing(group, remembered.value.turn + 1)

        AppLog.i(TAG, "now writing into the group $group")
    }

    private fun settingOf(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * The group this phone writes into, and how many times it has been settled.
     *
     * The count is there because the name alone does not say everything that
     * matters: a phone that has just been let into the group it was already
     * writing into has the same name and a different answer from the rules.
     *
     * Named for the writing rather than for a standing, because a standing is
     * already where an account stands with respect to a group — a different
     * question, answered in `membership`.
     */
    data class Writing(val name: String, val turn: Long)
}

/**
 * The group of the household that started before groups had names of their
 * own.
 *
 * Kept as what a phone assumes when it has joined nothing: those phones are
 * already writing into it, their group document was made before the rule that
 * names a founder, and an update that moved them elsewhere would take the
 * household off its own map.
 */
const val INHERITED: String = "family"
