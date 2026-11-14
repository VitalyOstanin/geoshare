package io.github.vitalyostanin.geoshare.control

import io.github.vitalyostanin.geoshare.managed.TrustedController
import io.github.vitalyostanin.geoshare.managed.trusts
import io.github.vitalyostanin.geoshare.service.StartRefusal

/**
 * The words of the control conversation, kept apart from [SharingControlService] so a JVM test can
 * reach them: a [android.os.Message] carries two integers and a bundle across the process
 * boundary, and every number and key below is part of what another application is compiled
 * against. A number or a key is never reused for another meaning.
 *
 * A request is the message's `what`. A reply is a message whose `what` is [WHAT_REPLY], whose
 * `arg1` is one of the `RESULT_` numbers and whose bundle holds what [fields] lists.
 */
object SharingControlProtocol {
    /** Request: ask for the position to be shared, as far as the device allows it. */
    const val WHAT_START = 1

    /** Request: stop sharing; allowed even when the member's own switch is locked. */
    const val WHAT_STOP = 2

    /** Request: say how things stand, changing nothing. */
    const val WHAT_STATUS = 3

    /** Every reply carries this; the request it answers is not echoed, there is one at a time. */
    const val WHAT_REPLY = 100

    /** The request was carried out; a status request carries the state in the bundle. */
    const val RESULT_OK = 0

    /** The request is understood, but the device is not in a state to carry it out. */
    const val RESULT_REFUSED = 1

    /** The request was not carried out for a reason named in the bundle. */
    const val RESULT_ERROR = 2

    /** Bundle key (string): the [StartRefusal] name, on a refusal that has a reason. */
    const val KEY_REFUSAL = "refusal"

    /** Bundle key (string): what went wrong, on an error. */
    const val KEY_ERROR = "error"

    /** Bundle key (boolean), status: the member's switch, or the controller's, is on. */
    const val KEY_SHARING_WANTED = "sharing_wanted"

    /** Bundle key (boolean), status: a group is connected. */
    const val KEY_GROUP_CONNECTED = "group_connected"

    /** Bundle key (boolean), status: signed in to at least one group. */
    const val KEY_SIGNED_IN = "signed_in"

    /** Bundle key (boolean), status: the location permission is granted. */
    const val KEY_LOCATION_GRANTED = "location_granted"

    /** Bundle key (boolean), status: the background location permission is granted. */
    const val KEY_BACKGROUND_LOCATION_GRANTED = "background_location_granted"

    /** Bundle key (boolean), status: the notification permission is granted. */
    const val KEY_NOTIFICATIONS_GRANTED = "notifications_granted"

    /** Bundle key (boolean), status: the sharing service is running. */
    const val KEY_SERVICE_RUNNING = "service_running"

    /** Error: the platform would not let the service start from the background. */
    const val ERROR_START_REFUSED_BY_PLATFORM = "start_refused_by_platform"

    /** What a request asks for. */
    enum class Request { Start, Stop, Status }

    /** The request [what] stands for, or null for a number nobody defined. */
    fun requestOf(what: Int): Request? = when (what) {
        WHAT_START -> Request.Start
        WHAT_STOP -> Request.Stop
        WHAT_STATUS -> Request.Status
        else -> null
    }

    /** How things stand on the device, as a status request reports it. */
    data class Status(
        val sharingWanted: Boolean,
        val groupConnected: Boolean,
        val signedIn: Boolean,
        val locationGranted: Boolean,
        val backgroundLocationGranted: Boolean,
        val notificationsGranted: Boolean,
        val serviceRunning: Boolean,
    )

    /** What the service answers with. */
    sealed interface Reply {
        /** The request was carried out. */
        data object Ok : Reply

        /** The answer to a status request. */
        data class State(val status: Status) : Reply

        /**
         * Refused. [reason] is null for a caller that is not trusted: the refusal says nothing
         * about why, so a probing application learns nothing about the device.
         */
        data class Refused(val reason: StartRefusal? = null) : Reply

        data class Error(val what: String) : Reply
    }

    /** The `arg1` a reply travels with. */
    fun resultOf(reply: Reply): Int = when (reply) {
        Reply.Ok, is Reply.State -> RESULT_OK
        is Reply.Refused -> RESULT_REFUSED
        is Reply.Error -> RESULT_ERROR
    }

    /** The bundle entries a reply travels with; the service copies them into a real bundle. */
    fun fields(reply: Reply): Map<String, Any> = when (reply) {
        Reply.Ok -> emptyMap()

        is Reply.State -> mapOf(
            KEY_SHARING_WANTED to reply.status.sharingWanted,
            KEY_GROUP_CONNECTED to reply.status.groupConnected,
            KEY_SIGNED_IN to reply.status.signedIn,
            KEY_LOCATION_GRANTED to reply.status.locationGranted,
            KEY_BACKGROUND_LOCATION_GRANTED to reply.status.backgroundLocationGranted,
            KEY_NOTIFICATIONS_GRANTED to reply.status.notificationsGranted,
            KEY_SERVICE_RUNNING to reply.status.serviceRunning,
        )

        is Reply.Refused -> reply.reason?.let { mapOf(KEY_REFUSAL to it.name) } ?: emptyMap()

        is Reply.Error -> mapOf(KEY_ERROR to reply.what)
    }

    /**
     * The reply a controller rebuilds from [result] and [fields]; the other side of [resultOf]
     * and [fields], here so that the round trip can be tested. Null for a number nobody
     * defined.
     */
    fun parseReply(result: Int, fields: Map<String, Any?>): Reply? = when (result) {
        RESULT_OK -> if (fields.containsKey(KEY_SERVICE_RUNNING)) {
            statusOf(fields)?.let { Reply.State(it) }
        } else {
            Reply.Ok
        }

        RESULT_REFUSED -> Reply.Refused(
            (fields[KEY_REFUSAL] as? String)?.let { name ->
                StartRefusal.entries.firstOrNull { it.name == name }
            },
        )

        RESULT_ERROR -> (fields[KEY_ERROR] as? String)?.let { Reply.Error(it) }

        else -> null
    }

    private fun statusOf(fields: Map<String, Any?>): Status? {
        fun flag(key: String) = fields[key] as? Boolean
        return Status(
            sharingWanted = flag(KEY_SHARING_WANTED) ?: return null,
            groupConnected = flag(KEY_GROUP_CONNECTED) ?: return null,
            signedIn = flag(KEY_SIGNED_IN) ?: return null,
            locationGranted = flag(KEY_LOCATION_GRANTED) ?: return null,
            backgroundLocationGranted = flag(KEY_BACKGROUND_LOCATION_GRANTED) ?: return null,
            notificationsGranted = flag(KEY_NOTIFICATIONS_GRANTED) ?: return null,
            serviceRunning = flag(KEY_SERVICE_RUNNING) ?: return null,
        )
    }

    /**
     * Whether any of the caller's [packages] is on the trusted [list] with a certificate that
     * signs it. Each package comes with the SHA-256 digests (hex) of the certificates the
     * platform reports for it: a package with several signers, or one whose signing key was
     * rotated, has several, and one match is enough. Applications sharing a uid share
     * permissions and a signature, so the listed one being among them is enough to know who is
     * calling. A caller with no packages, or none with a certificate, is not trusted.
     */
    fun callerTrusted(
        list: List<TrustedController>,
        packages: Map<String, List<String>>,
    ): Boolean = packages.any { (name, digests) -> digests.any { trusts(list, name, it) } }
}
