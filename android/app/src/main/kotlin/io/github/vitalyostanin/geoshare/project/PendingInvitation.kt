package io.github.vitalyostanin.geoshare.project

import android.content.Context
import io.github.vitalyostanin.geoshare.log.AppLog
import org.json.JSONException
import org.json.JSONObject

private const val KEY_PENDING_INVITATION = "pending_invitation"
private const val TAG = "geoshare.project"

private const val FIELD_GROUP_ID = "groupId"
private const val FIELD_CODE = "code"
private const val FIELD_TO = "to"

/**
 * An [Invitation]'s group and code, kept across the restart
 * [ConnectState.Done] leads to (ADR-0001), so the sign-in that follows it can
 * spend it the way a group invitation is spent everywhere else.
 * [invitedAddress] is the shortened address the link names, kept so that an
 * account which is plainly not the one invited is told so before anything is
 * written (ADR-0019); the other captions were shown once already and are not
 * needed again.
 */
data class PendingInvitation(
    val groupId: String,
    val code: String,
    val invitedAddress: String? = null,
) {
    fun toJson(): String = JSONObject()
        .put(FIELD_GROUP_ID, groupId)
        .put(FIELD_CODE, code)
        .apply { invitedAddress?.let { put(FIELD_TO, it) } }
        .toString()

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

        return !ConnectLink.shortenedAddress(email).equals(invited, ignoreCase = true)
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

            return if (groupId.isBlank() || code.isBlank()) {
                null
            } else {
                PendingInvitation(groupId, code, invitedAddress)
            }
        }
    }
}

/**
 * The [PendingInvitation] [ConnectState.Done] leaves for the restart to
 * spend, or none for an owner's own link — [Invitation.groupId] and
 * [Invitation.code] are what a join needs, and [Invitation.invitedAddress]
 * what tells a wrong account apart before joining; the other captions have
 * nothing left to say once the confirmation screen that showed them is gone.
 */
fun pendingInvitationFor(done: ConnectState.Done): PendingInvitation? =
    done.invitation?.let { PendingInvitation(it.groupId, it.code, it.invitedAddress) }

/**
 * Where a [PendingInvitation] waits between [ConnectState.Done] writing it
 * and the sign-in after the restart reading it back —
 * [FamilyProject.settingOf]'s own file, since both the write and the read
 * belong to the same restart.
 */
object PendingInvitationStore {
    /** Keeps [invitation] for the next start, or clears what was kept when there is none. */
    fun save(context: Context, invitation: PendingInvitation?) {
        val editor = FamilyProject.settingOf(context).edit()

        if (invitation == null) {
            editor.remove(KEY_PENDING_INVITATION)
        } else {
            editor.putString(KEY_PENDING_INVITATION, invitation.toJson())
        }

        // Committed rather than applied for the same reason as
        // FamilyProject.save, which this is written right beside: the
        // process that calls this ends a moment later, before an
        // asynchronous write would be guaranteed to reach disk.
        editor.commit()
    }

    /** Reads back what [save] kept, without clearing it — [clear] does that once it is spent. */
    fun read(context: Context): PendingInvitation? {
        val kept = FamilyProject.settingOf(context)
            .getString(KEY_PENDING_INVITATION, null) ?: return null
        val invitation = PendingInvitation.parse(kept)

        if (invitation == null) {
            AppLog.w(TAG, "the pending invitation kept on the phone could not be read")
        }

        return invitation
    }

    /** Drops the pending invitation once it has been spent or refused outright. */
    fun clear(context: Context) {
        FamilyProject.settingOf(context).edit().remove(KEY_PENDING_INVITATION).apply()
    }
}
