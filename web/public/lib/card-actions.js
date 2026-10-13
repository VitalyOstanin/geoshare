// Who is offered the round buttons on the card of a device: the same rule the
// panel that runs the group and "My devices" go by, kept apart from them so
// that the card, which has neither, asks the same question.

/**
 * Whether the viewer may reset the place of, or ring, the device on a card.
 *
 * One's own devices always: this page is not a device, so none of them is the
 * one in hand. Somebody else's only for whoever runs the group.
 *
 * @param viewerId the signed-in member, or null before there is one
 * @param ownerId the member the device belongs to
 * @param runsTheGroup whether the viewer is an administrator of the group
 */
export function mayActOnDevice(viewerId, ownerId, runsTheGroup) {
    if (!viewerId || !ownerId) {
        return false;
    }

    return viewerId === ownerId || runsTheGroup === true;
}
