package io.github.vitalyostanin.geoshare.service

import android.content.Context

private const val FILE = "background-work"
private const val KEY_ASKED = "battery-asked"
private const val KEY_STARTUP = "startup-set"
private const val KEY_MOVEMENT = "movement-asked"

/**
 * What the phone has been asked about letting the application keep running.
 *
 * Three answers live here. The first is whether the battery request has been
 * shown at all: it is shown once, because a request the member has already
 * refused is not made better by arriving again. The second is the member's own
 * word that the manufacturer's startup list is set, which is the only way that
 * list can be known — nothing on the phone will say. The third is the same
 * once-only record for the request to be told about movement.
 */
class BackgroundWorkSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var batteryAsked: Boolean
        get() = store.getBoolean(KEY_ASKED, false)
        set(value) = store.edit().putBoolean(KEY_ASKED, value).apply()

    var startupSet: Boolean
        get() = store.getBoolean(KEY_STARTUP, false)
        set(value) = store.edit().putBoolean(KEY_STARTUP, value).apply()

    var movementAsked: Boolean
        get() = store.getBoolean(KEY_MOVEMENT, false)
        set(value) = store.edit().putBoolean(KEY_MOVEMENT, value).apply()
}
