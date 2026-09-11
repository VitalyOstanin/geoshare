package io.github.vitalyostanin.geoshare.permission

/**
 * One thing to ask the member for.
 *
 * They are asked one at a time and in this order: the platform refuses a
 * background location request that arrives before the ordinary one, and a
 * member who is shown three dialogs at once tends to answer no to all three.
 */
enum class PermissionStep {
    /** Without it the position cannot be read at all. */
    Location,

    /** Without it the notification of a running service is not shown. */
    Notifications,

    /**
     * Without it sharing stops as soon as the application leaves the screen.
     * Asked last, because on Android 11 and later it is granted on a settings
     * screen rather than in a dialog.
     */
    BackgroundLocation,
}

data class PermissionState(
    val locationGranted: Boolean,
    val notificationsGranted: Boolean,
    val backgroundLocationGranted: Boolean,
)

/** What to ask for next, or null when nothing is left to ask. */
fun nextPermissionStep(state: PermissionState): PermissionStep? = when {
    !state.locationGranted -> PermissionStep.Location
    !state.notificationsGranted -> PermissionStep.Notifications
    !state.backgroundLocationGranted -> PermissionStep.BackgroundLocation
    else -> null
}
