package io.github.vitalyostanin.geoshare.project

import android.content.Context
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.readString
import org.json.JSONException
import org.json.JSONObject

private const val KEY_PENDING_INVITATION = "pending_invitation"
private const val TAG = "geoshare.project"

private const val FIELD_GROUP_ID = "groupId"
private const val FIELD_CODE = "code"
private const val FIELD_TO = "to"
private const val FIELD_PROJECT = "project"
private const val FIELD_BY = "by"
private const val FIELD_REFUSED = "refused"

/**
 * An [Invitation]'s group and code, kept from [ConnectState.Done] until the
 * sign-in to [projectId] that follows it, so that sign-in can spend it the way
 * a group invitation is spent everywhere else. [invitedAddress] is the
 * shortened address the link names, kept so that an account which is plainly
 * not the one invited is told so before anything is written (ADR-0019).
 * [inviter] is who the link said invited, the person to ask for a new link
 * should this one be refused.
 *
 * [refused] marks one the rules turned down: it is not tried again, but kept
 * so the map can go on saying so until the group is dropped or another link
 * is opened.
 */
data class PendingInvitation(
    val groupId: String,
    val code: String,
    val invitedAddress: String? = null,
    /** The project it invites to; null when kept by a build that knew a single project. */
    val projectId: String? = null,
    val inviter: String? = null,
    val refused: Boolean = false,
) {
    fun toJson(): String = JSONObject()
        .put(FIELD_GROUP_ID, groupId)
        .put(FIELD_CODE, code)
        .apply { invitedAddress?.let { put(FIELD_TO, it) } }
        .apply { projectId?.let { put(FIELD_PROJECT, it) } }
        .apply { inviter?.let { put(FIELD_BY, it) } }
        .apply { if (refused) put(FIELD_REFUSED, true) }
        .toString()

    /**
     * Whether a sign-in to [projectId] is the one to spend this on: a device
     * in several groups signs in to each on its own, and the code is good in
     * one project only.
     */
    fun isFor(projectId: String): Boolean = this.projectId == null || this.projectId == projectId

    /**
     * Whether [signedInEmail] is plainly not the account this was made out
     * to: its shortened form differs from the one the link carried. Only a
     * caption is compared — an account that passes here is still weighed by
     * the rules against the full address. Nothing to compare either way
     * means no verdict, and the rules answer alone.
     */
    fun meantForAnotherAccount(signedInEmail: String?): Boolean {
        val invited = invitedAddress ?: return false
        val email = signedInEmail ?: return false

        return !ConnectLink.invitedAs(email).equals(invited, ignoreCase = true)
    }

    companion object {
        fun parse(json: String): PendingInvitation? {
            val document = try {
                JSONObject(json)
            } catch (malformed: JSONException) {
                return null
            }

            val groupId = document.optString(FIELD_GROUP_ID)
            val code = document.optString(FIELD_CODE)
            val invitedAddress = document.optString(FIELD_TO).ifBlank { null }
            val projectId = document.optString(FIELD_PROJECT).ifBlank { null }
            val inviter = document.optString(FIELD_BY).ifBlank { null }
            val refused = document.optBoolean(FIELD_REFUSED, false)

            return if (groupId.isBlank() || code.isBlank()) {
                null
            } else {
                PendingInvitation(groupId, code, invitedAddress, projectId, inviter, refused)
            }
        }
    }
}

/**
 * The [PendingInvitation] [ConnectState.Done] leaves for the sign-in to its
 * project to spend, or none for an owner's own link — [Invitation.groupId] and
 * [Invitation.code] are what a join needs, [Invitation.invitedAddress] what
 * tells a wrong account apart before joining, and [Invitation.inviter] whom a
 * refusal sends the person back to; the group's name is the connection's own
 * by then.
 */
fun pendingInvitationFor(done: ConnectState.Done): PendingInvitation? = done.invitation?.let {
    PendingInvitation(
        it.groupId,
        it.code,
        it.invitedAddress,
        done.settings.projectId,
        inviter = it.inviter,
    )
}

/**
 * An invitation to [projectId]'s group the rules refused: used, withdrawn or
 * run out — the three are not told apart, since the invited account may not
 * read the invitation (ADR-0018). [inviter] is whom to ask for a new link.
 */
data class RefusedJoin(val projectId: String, val inviter: String?)

/** [RefusedJoin] for [pending], when it is one the rules turned down. */
fun refusedJoinOf(pending: PendingInvitation?, projectId: String): RefusedJoin? = pending?.takeIf {
    it.refused && it.isFor(projectId)
}?.let { RefusedJoin(projectId, it.inviter) }

/**
 * Where a [PendingInvitation] waits between [ConnectState.Done] writing it
 * and the sign-in to its project reading it back — [Connections.files]' own
 * file, beside the list of groups it was written together with.
 */
object PendingInvitationStore {
    /** Keeps [invitation] for the next start, or clears what was kept when there is none. */
    fun save(context: Context, invitation: PendingInvitation?) {
        val editor = Connections.files(context).edit()

        if (invitation == null) {
            editor.remove(KEY_PENDING_INVITATION)
        } else {
            editor.putString(KEY_PENDING_INVITATION, invitation.toJson())
        }

        // Committed rather than applied, as the list of groups it is written
        // beside is: what follows may be the process being cut short, before
        // an asynchronous write would be guaranteed to reach disk.
        editor.commit()
    }

    /** Reads back what [save] kept, without clearing it — [clear] does that once it is spent. */
    fun read(context: Context): PendingInvitation? {
        val kept = Connections.files(context)
            .readString(KEY_PENDING_INVITATION, null) ?: return null
        val invitation = PendingInvitation.parse(kept)

        if (invitation == null) {
            AppLog.w(TAG, "the pending invitation kept on the phone could not be read")
        }

        return invitation
    }

    /** Drops the pending invitation once it has been spent or refused outright. */
    fun clear(context: Context) {
        Connections.files(context).edit().remove(KEY_PENDING_INVITATION).apply()
    }
}
