package io.github.vitalyostanin.geoshare.permission

/**
 * Whether to ask the member, now, to let the phone tell us when it is moving.
 *
 * Asked the way the battery exemption is asked: once, while sharing is on, and
 * never again. A member who said no has said no, and the request is worth
 * making only because the answer buys them a mode that follows them on the way
 * somewhere without following them at home.
 *
 * Asked after the three the service cannot run without, so that a refusal here
 * cannot be mistaken for a refusal there.
 */
fun movementPermissionWanted(
    sharing: Boolean,
    /** Whether the phone already lets us hear about movement. */
    granted: Boolean,
    /** Whether the member has been shown the request before. */
    askedBefore: Boolean,
): Boolean = sharing && !granted && !askedBefore
