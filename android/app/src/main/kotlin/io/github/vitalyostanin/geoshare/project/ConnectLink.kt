package io.github.vitalyostanin.geoshare.project

import java.net.URLDecoder

/** How long a caption taken off an invitation link is kept. */
private const val CAPTION_MAX_LENGTH = 60

/**
 * The site and the `/join` path of a connection link. Firebase Hosting
 * answers to both hosts for the same site; [ConnectLink.parse] accepts
 * either one.
 *
 * The site id rule below is a simplification of Firebase Hosting's own:
 * lowercase letters, digits and hyphens, starting with a letter (assumption
 * — Firebase does not publish the exact grammar, only examples).
 */
private val LINK_PATTERN = Regex(
    """https://([a-z][a-z0-9-]*)\.(?:web\.app|firebaseapp\.com)/(\S*)""",
)

/**
 * A connection link: what the project owner's site hands out on its own
 * (ADR-0001) and what an invitation adds after the `#` (ADR-0015,
 * ADR-0017, ADR-0018).
 *
 * [site] is the Firebase Hosting site id read out of the link's host,
 * whichever of the two hosts was used. [settingsUrl] always names
 * `web.app`, since that is the host `geoshare.json` is fetched from
 * regardless of which one the link carried.
 */
data class ConnectLink(val site: String, val invitation: Invitation?) {
    val settingsUrl: String
        get() = "https://$site.web.app/geoshare.json"

    companion object {
        /**
         * Reads a connection link out of [text], which may be exactly the
         * link or a longer message it was pasted into — the first match is
         * used and the rest of the text is ignored.
         *
         * Returns null for anything that is not this project's link: a
         * scheme other than https, a different host, a path other than
         * `/join`, or a site id Firebase Hosting would not have issued. A
         * fragment naming neither a group nor a code is read as an owner's
         * link, since that is what one looks like; a fragment naming one
         * but not the other is refused rather than turned into a link with
         * no invitation, which would join the group unannounced instead of
         * failing visibly.
         */
        fun parse(text: String): ConnectLink? {
            val match = LINK_PATTERN.find(text) ?: return null
            val (site, rest) = match.destructured
            val hashIndex = rest.indexOf('#')
            val path = if (hashIndex >= 0) rest.substring(0, hashIndex) else rest
            val fragment = if (hashIndex >= 0) rest.substring(hashIndex + 1) else ""

            if (path != "join" && path != "join/") {
                return null
            }

            val fields = fieldsOf(fragment)

            if ("g" !in fields && "c" !in fields) {
                return ConnectLink(site, invitation = null)
            }

            val invitation = invitationOf(fields) ?: return null

            return ConnectLink(site, invitation)
        }

        private fun fieldsOf(fragment: String): Map<String, String> =
            fragment.split("&").mapNotNull { pair ->
                val eq = pair.indexOf('=')

                if (eq < 0) return@mapNotNull null

                val value = decode(pair.substring(eq + 1)) ?: return@mapNotNull null

                pair.substring(0, eq) to value
            }.toMap()

        private fun invitationOf(fields: Map<String, String>): Invitation? {
            val groupId = fields["g"]
            val code = fields["c"]

            if (groupId.isNullOrBlank() || code.isNullOrBlank()) {
                return null
            }

            return Invitation(
                groupId = groupId,
                code = code,
                inviter = caption(fields["by"]),
                groupName = caption(fields["n"]),
                invitedAddress = caption(fields["to"]),
            )
        }

        private fun decode(value: String): String? = try {
            URLDecoder.decode(value, "UTF-8")
        } catch (malformed: IllegalArgumentException) {
            null
        }

        /**
         * A caption is only ever shown, never checked against anything
         * (ADR-0017, ADR-0018), so a blank one is dropped rather than kept
         * as an empty string, and an implausibly long one — the link can be
         * edited by whoever holds it — is cut down to size.
         */
        private fun caption(value: String?): String? {
            val trimmed = value?.trim()

            return if (trimmed.isNullOrEmpty()) null else trimmed.take(CAPTION_MAX_LENGTH)
        }
    }
}

/**
 * The invitation half of a [ConnectLink]: who may use it (ADR-0015) and,
 * for the page and the confirmation screen alone, who it says sent it and
 * to where (ADR-0017, ADR-0018). [inviter], [groupName] and
 * [invitedAddress] are captions only — the code and the group id are what
 * the rules act on.
 */
data class Invitation(
    val groupId: String,
    val code: String,
    val inviter: String?,
    val groupName: String?,
    val invitedAddress: String?,
)
