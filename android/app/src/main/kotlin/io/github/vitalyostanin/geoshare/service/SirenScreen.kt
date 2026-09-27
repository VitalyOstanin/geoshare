package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService

/**
 * Lights the screen for as long as the siren sounds: the phone found in a
 * drawer shows what is making the noise and the button that stops it, without
 * anybody having to press the power button first.
 *
 * The flag that wakes the screen is deprecated since API 33 in favour of an
 * activity's own turnScreenOn, which needs an activity over the locked screen
 * — the very thing the siren's notification keeps away from.
 */
@Suppress("DEPRECATION")
fun lightScreen(context: Context, seconds: Long): PowerManager.WakeLock? = context
    .getSystemService<PowerManager>()
    ?.newWakeLock(
        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "geoshare:siren",
    )?.apply { acquire(seconds * 1000) }

/** Lets the screen go dark again on its usual timeout. */
fun PowerManager.WakeLock.letGo() {
    if (isHeld) {
        release()
    }
}
