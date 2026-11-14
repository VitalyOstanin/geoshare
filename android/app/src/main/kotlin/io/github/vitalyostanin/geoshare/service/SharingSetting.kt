package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.managed.ManagedConfiguration

private const val FILE = "sharing"
private const val KEY_ENABLED = "enabled"

/**
 * Whether the member wants their position shared.
 *
 * Kept outside the session on purpose: the answer is needed after a restart,
 * before anything has had a chance to ask Firebase who is signed in.
 *
 * On a managed device the device policy controller may have said it instead
 * (ADR-0043), and then its word is what every reader gets: the switch, the
 * start after a restart and the service alike. The member's own answer is
 * still written and kept, so that it is back in force the moment the
 * configuration stops saying anything.
 */
class SharingSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sharingWanted(
            ManagedConfiguration.current.sharingEnabled,
            store.readBoolean(KEY_ENABLED, false),
        )
        set(value) = store.edit().putBoolean(KEY_ENABLED, value).apply()
}

/** The controller's answer when it gave one, and the member's otherwise. */
internal fun sharingWanted(managed: Boolean?, member: Boolean): Boolean = managed ?: member

/**
 * What a change of the managed configuration asks of the service: start it,
 * stop it, or leave it as it is.
 *
 * Only a configuration that says something about sharing moves the service.
 * One that stopped saying anything leaves it running or stopped as it was,
 * since the member's own answer may well be the same, and the next start
 * reads it anyway.
 */
internal enum class ManagedSharingStep { Start, Stop, Nothing }

internal fun managedSharingStep(managed: Boolean?, refusal: StartRefusal?): ManagedSharingStep =
    when {
        managed == false -> ManagedSharingStep.Stop
        managed == true && refusal == null -> ManagedSharingStep.Start
        else -> ManagedSharingStep.Nothing
    }
