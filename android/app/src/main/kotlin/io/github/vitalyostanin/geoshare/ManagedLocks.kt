package io.github.vitalyostanin.geoshare

import io.github.vitalyostanin.geoshare.managed.ManagedSettings

/*
 * What the controlling application's lock flags take away from the member (ADR-0043).
 * Kept apart from the screens so that "who may do what" is decided in one place, and so that a
 * locked control and the code behind it cannot disagree: the screen disables the control, and
 * whatever the control would have started asks the same question again.
 *
 * Without a configuration every flag is off, so each answer is yes and nothing changes.
 */

/** Whether the member may switch sharing off, here or from the notification. */
fun canStopSharing(settings: ManagedSettings): Boolean = !settings.lockSharing

/** Whether the member may leave a group on this device. */
fun canLeaveGroup(settings: ManagedSettings): Boolean = !settings.lockGroup

/** Whether the member may sign out or switch to another account. */
fun canChangeAccount(settings: ManagedSettings): Boolean = !settings.lockAccount
