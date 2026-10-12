package io.github.vitalyostanin.geoshare.permission

/**
 * Whether to ask the member, now, to let the device leave the application
 * alone.
 *
 * A device that stops the application stops answering the other members: the
 * fast collecting they ask for arrives over a subscription, and a subscription
 * needs a process that is still running. The request is therefore worth making,
 * but it is not worth making twice: a member who said no has said no, and the
 * row on the settings screen is where they can change their mind.
 *
 * Asked only while sharing is on, because a member who is not sharing has
 * nothing for the device to interrupt.
 */
fun batteryExemptionWanted(
    sharing: Boolean,
    /** Whether the device already leaves the application out of its battery saving. */
    unrestricted: Boolean,
    /** Whether the member has been shown the request before. */
    askedBefore: Boolean,
): Boolean = sharing && !unrestricted && !askedBefore
