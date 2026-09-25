package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.log.AppLog

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
    @Volatile
    private var remembered: String = INHERITED

    /** The name to build a path out of. */
    val name: String
        get() = remembered

    /** Reads back which group this phone joined, before anything reaches the database. */
    fun start(context: Context) {
        remembered = settingOf(context).getString(KEY_NAME, null) ?: INHERITED

        AppLog.i(TAG, "writing into the group $remembered")
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
        remembered = group

        AppLog.i(TAG, "now writing into the group $group")
    }

    private fun settingOf(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
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
