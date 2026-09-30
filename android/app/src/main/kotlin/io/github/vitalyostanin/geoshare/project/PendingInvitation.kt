package io.github.vitalyostanin.geoshare.project

import android.content.Context
import io.github.vitalyostanin.geoshare.log.AppLog
import org.json.JSONException
import org.json.JSONObject

private const val KEY_PENDING_INVITATION = "pending_invitation"
private const val TAG = "geoshare.project"

private const val FIELD_GROUP_ID = "groupId"
private const val FIELD_CODE = "code"

/**
 * An [Invitation]'s group and code, kept across the restart
 * [ConnectState.Done] leads to (ADR-0001), so the sign-in that follows it can
 * spend it the way a group invitation is spent everywhere else. The captions
 * ([Invitation.inviter], [Invitation.groupName], [Invitation.invitedAddress])
 * were shown once already, on the confirmation screen that has already been
 * accepted by the time this is written, and are not needed again.
 */
data class PendingInvitation(val groupId: String, val code: String) {
    fun toJson(): String = JSONObject()
        .put(FIELD_GROUP_ID, groupId)
        .put(FIELD_CODE, code)
        .toString()

    companion object {
        fun parse(json: String): PendingInvitation? {
            val document = try {
                JSONObject(json)
            } catch (malformed: JSONException) {
                return null
            }

            val groupId = document.optString(FIELD_GROUP_ID)
            val code = document.optString(FIELD_CODE)

            return if (groupId.isBlank() || code.isBlank()) {
                null
            } else {
                PendingInvitation(groupId, code)
            }
        }
    }
}

/**
 * The [PendingInvitation] [ConnectState.Done] leaves for the restart to
 * spend, or none for an owner's own link — [Invitation.groupId] and
 * [Invitation.code] are the only two fields a join needs, and the captions
 * have nothing left to say once the confirmation screen that showed them is
 * gone.
 */
fun pendingInvitationFor(done: ConnectState.Done): PendingInvitation? =
    done.invitation?.let { PendingInvitation(it.groupId, it.code) }

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
