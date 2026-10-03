package io.github.vitalyostanin.geoshare.service

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.membership.INVITATION_LASTS
import io.github.vitalyostanin.geoshare.membership.Invitation
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.newInvitationCode
import io.github.vitalyostanin.geoshare.membership.rosterFounded
import io.github.vitalyostanin.geoshare.membership.rosterWithout
import io.github.vitalyostanin.geoshare.project.PendingInvitation
import io.github.vitalyostanin.geoshare.project.PendingInvitationStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

private const val TAG = "geoshare.group"

/**
 * The roster, as the group document holds it.
 *
 * A subscription rather than a read, because it changes under the people
 * looking at it: somebody joins on a code while the screen is open, and an
 * administrator removing a member has to see them go rather than reopen the
 * screen to find out whether the write landed.
 *
 * Nothing arrives until the document does. A group that has not been made
 * carries no roster, and that is a state of its own: it is what tells the
 * founder they may make one.
 */
fun groupRoster(store: FirebaseFirestore = FirebaseFirestore.getInstance()): Flow<Roster?> =
    callbackFlow {
        val subscription =
            groupDocument(store).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)

                    return@addSnapshotListener
                }

                trySend(snapshot?.takeIf { it.exists() }?.let(::rosterOf))
            }

        awaitClose { subscription.remove() }
        // A refusal reads the same way as a group that was never made, which
        // is what it amounts to for this phone: whoever is looking is offered
        // the two ways into a group rather than left with a roster they are
        // no longer on.
    }.comingBack("roster", forgetting = { null })

private fun rosterOf(snapshot: DocumentSnapshot): Roster = Roster(
    members = snapshot.get(GroupField.MEMBERS).asIds(),
    admins = snapshot.get(GroupField.ADMINS).asIds(),
    // Whoever the document names as its owner, or else the account the group
    // is named after: the rules read the founder the same way.
    founder = snapshot.get(GroupField.OWNER) as? String ?: snapshot.id,
    name = snapshot.get(GroupField.NAME) as? String,
)

/**
 * A field that ought to be a list of account ids, read as one.
 *
 * Anything else is read as nothing rather than thrown: the screens then say
 * there is no group instead of failing, and what is actually in the document
 * is a question for the console.
 */
private fun Any?.asIds(): List<String> = (this as? List<*>).orEmpty().filterIsInstance<String>()

/**
 * Makes the group, with the founder alone in it.
 *
 * The rules refuse a group made around anybody else, so there is nothing to
 * choose about who is on it: what the founder hands out afterwards is a code.
 * The id it gets is the account that made it, which is what keeps one account
 * from making them without end; [name] is a caption on top of that, given if
 * the founder typed one (ADR-0016).
 *
 * The phone is moved into the new group only once the write has been allowed.
 * Moving it first would leave a phone whose founding was refused writing into
 * a group that does not exist, with every refusal afterwards saying nothing
 * about why.
 */
suspend fun foundGroup(
    context: Context,
    uid: String,
    name: String? = null,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val roster = rosterFounded(uid)
    // The rules refuse the field at all once it is blank (nameFits), so it is
    // left out rather than written empty: a group without a name and one
    // named the empty string would otherwise read the same everywhere but
    // here.
    val fields =
        if (name.isNullOrBlank()) {
            roster.asDocument()
        } else {
            roster.asDocument() + (GroupField.NAME to name)
        }

    groupDocumentOf(store, uid).set(fields).await()
    CurrentGroup.moveTo(context, uid)

    AppLog.i(TAG, "the group has been founded")
}

/**
 * Gives the group a name, or changes the one it has.
 *
 * A write of its own rather than a part of [foundGroup]: the rules weigh it
 * separately (`renaming` in the rules) from the roster it goes with, so this
 * one touches only the field it changes and leaves members and admins as
 * they are.
 */
suspend fun renameGroup(name: String, store: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    groupDocument(store).update(GroupField.NAME, name).await()

    AppLog.i(TAG, "the group has been renamed")
}

/**
 * What making an invitation ([makeInvitation]) hands back to whoever asked for
 * it: everything a link to it is built from, rather than the link itself,
 * which is the caller's to build once it also knows what the screen is
 * showing this member as (`ConnectLink.invitationLink` wants a caption
 * neither the invitation document nor this call holds).
 */
data class MadeInvitation(
    val groupId: String,
    val code: String,
    val expiresAt: Instant,
    val email: String,
)

/**
 * Makes an invitation, addressed to one account, and returns what a link to it
 * is built from.
 *
 * The code is the name of the document, so nothing has to be read back to know
 * it, and nothing in the document says what it is: a reader who could list the
 * collection would hold every code, which is why nobody may read it at all.
 *
 * [email] is who may spend the code (`invitedAccount` in the rules, ADR-0015):
 * saved as it was typed, save for the spaces around it, since the rules
 * compare it to the signed-in account's own address without regard to case.
 */
suspend fun makeInvitation(
    uid: String,
    email: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): MadeInvitation {
    val code = newInvitationCode()
    val expiresAt = Instant.now().plus(INVITATION_LASTS)
    val address = email.trim()

    invitationsOf(store)
        .document(code)
        .set(
            mapOf(
                InvitationField.CREATED_BY to uid,
                InvitationField.EXPIRES_AT to Timestamp(Date.from(expiresAt)),
                InvitationField.EMAIL to address,
            ),
        ).await()

    AppLog.i(TAG, "an invitation has been made, good until $expiresAt")

    return MadeInvitation(CurrentGroup.name, code, expiresAt, address)
}

/**
 * Joins a group on an invitation: the code, the roster and the spending of the
 * code, in one batch.
 *
 * One batch because the rules weigh the three together: the roster grows only
 * if the code the claim names is gone afterwards, and the code goes only if
 * the roster grows. That is what makes a code admit one person once.
 *
 * Which group is not this phone's to assume: the invitation says so, and every
 * write is addressed by that name rather than by the group the phone is in
 * — which, until this succeeds, is a different one.
 *
 * The roster is not read, and cannot be: the group document is closed to
 * everybody who is not on it, which is everybody about to join. So the server
 * is asked to append rather than told the whole list, and the rule ends up
 * comparing the same two lists it would have compared anyway.
 */
suspend fun joinGroup(
    context: Context,
    uid: String,
    invitation: Invitation,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val group = groupDocumentOf(store, invitation.group)

    store
        .batch()
        .set(joinsIn(group).document(uid), mapOf(JoinField.CODE to invitation.code))
        .update(group, GroupField.MEMBERS, FieldValue.arrayUnion(uid))
        .delete(invitationsIn(group).document(invitation.code))
        .commit()
        .await()

    CurrentGroup.moveTo(context, invitation.group)

    AppLog.i(TAG, "joined a group on a code")
}

/**
 * Takes a member off the roster, and off the administrators with it.
 *
 * Leaving them among the administrators would leave somebody outside the group
 * still able to say who is in it, and the rules refuse such a write whole
 * rather than perform half of it.
 */
suspend fun removeFromGroup(
    roster: Roster,
    uid: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    // The two lists alone: a whole document written back would drop the
    // founder and the name of the group, and the rules refuse a write that
    // touches either.
    groupDocument(store).update(rosterWithout(roster, uid).asDocument()).await()

    AppLog.i(TAG, "a member has been taken off the roster")
}

/**
 * Hands the running of the group to a member, or takes it back.
 *
 * One element added or taken rather than the list written whole, so that two
 * administrators changing different members at once do not undo each other.
 */
suspend fun setRunning(
    uid: String,
    runs: Boolean,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val change = if (runs) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)

    groupDocument(store).update(GroupField.ADMINS, change).await()

    AppLog.i(TAG, if (runs) "a member has been handed the group" else "a member has given it back")
}

/**
 * Spends the pending invitation a restart carried over (`ConnectState.Done`,
 * [PendingInvitationStore]), through the same write [joinGroup] performs for
 * a code typed into the group section — there is no invitation-specific path
 * of its own.
 *
 * Cleared once the answer is final either way: a refusal from the rules
 * ([refusedOutright]) means the code has run out or was never good, and
 * trying it again on the next sign-in would only refuse it again. A network
 * failure says nothing about the code itself, so [pending] is left for the
 * next sign-in to find and try again.
 */
suspend fun consumePendingInvitation(
    context: Context,
    uid: String,
    pending: PendingInvitation,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    try {
        joinGroup(context, uid, Invitation(pending.groupId, pending.code), store)
        PendingInvitationStore.clear(context)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (refusal: Exception) {
        if (refusedOutright(refusal)) {
            PendingInvitationStore.clear(context)
        }

        throw refusal
    }
}

private fun Roster.asDocument(): Map<String, Any> =
    mapOf(GroupField.MEMBERS to members, GroupField.ADMINS to admins)
