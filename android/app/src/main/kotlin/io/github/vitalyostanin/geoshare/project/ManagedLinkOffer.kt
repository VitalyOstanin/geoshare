package io.github.vitalyostanin.geoshare.project

/**
 * Which connection link, if any, the managed configuration asks the device to follow now.
 *
 * Only a link that names a group the device is not in yet is worth handing to the connection flow:
 * for one it is already in the flow would have nothing to add. A link already offered in this
 * process is not offered again, so a member who went through it or dismissed it is not sent back
 * to it every time the configuration is read. Text that is no [ConnectLink] offers nothing — the
 * controller's console may hold anything.
 *
 * Returns the link as the controller wrote it, ready for [ConnectFlow.linkArrived].
 */
fun managedLinkToOffer(
    configured: String?,
    connectedSites: Collection<String>,
    alreadyOffered: Set<String>,
): String? {
    val text = configured?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val link = ConnectLink.parse(text) ?: return null

    if (link.site in connectedSites || text in alreadyOffered) {
        return null
    }

    return text
}

/**
 * The links the managed configuration has already put in front of the member since the process
 * started. Kept in memory on purpose: after a restart the decision is made afresh, and a link
 * whose group has been joined by then is skipped because the group is listed.
 */
object ManagedLinkOffers {
    private val offered = mutableSetOf<String>()

    /** The link to hand to the flow now, noted as offered; null when there is none. */
    @Synchronized
    fun next(configured: String?, connectedSites: Collection<String>): String? {
        val link = managedLinkToOffer(configured, connectedSites, offered)

        if (link != null) {
            offered += link
        }

        return link
    }
}
