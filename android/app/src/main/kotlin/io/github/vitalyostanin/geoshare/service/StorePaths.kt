package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Where everything this group writes lives, and what its fields are called.
 *
 * Written out once rather than in each of the seven files that reach into the
 * database. A path spelled by hand in a writer and again in a reader is a pair
 * that can be changed on one side alone: nothing fails to compile, nothing
 * fails a test, and the first sign of it is a card with nothing in it.
 *
 * The names match the page, which keeps its own copy in `web/public/lib`, and
 * the rules, which name the same fields again in `firebase/firestore.rules`.
 */
fun groupDocument(store: FirebaseFirestore): DocumentReference =
    store.collection("groups").document(GROUP)

fun positionsOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("positions")

fun membersOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("members")

fun watchOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("watch")

/**
 * What a member gives out: the phones they carry and whom they let watch.
 *
 * Apart from the profile because the profile is read by everybody in the
 * group, and neither of these is the group's business: only the member and the
 * readers they named can see them.
 */
fun grantsOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("grants")

/**
 * The invitations of the group: one document per code, named by the code.
 *
 * Nobody reads this collection, the application included. The rules read it
 * while they weigh somebody joining, and a rule reads without the caller's
 * permission; a client that could read it would hold every code of the group.
 */
fun invitationsOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("invitations")

/**
 * Where the person joining puts the code they are holding.
 *
 * A rule cannot be told anything except through the database: the write it
 * weighs is the roster, and a code has nowhere to ride in it.
 */
fun joinsOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("joins")

/** The fields of the group document itself. */
object GroupField {
    const val MEMBERS = "members"
    const val ADMINS = "admins"
}

/** The fields of an invitation. */
object InvitationField {
    const val CREATED_BY = "createdBy"
    const val EXPIRES_AT = "expiresAt"
}

/** The one field of a claim to be holding a code. */
object JoinField {
    const val CODE = "code"
}

/** The fields of a position document, as the writer and the reader both call them. */
object PositionField {
    const val MEMBER_ID = "memberId"
    const val DEVICE_NAME = "deviceName"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val TAKEN_AT = "takenAt"
    const val WRITTEN_AT = "writtenAt"
    const val MODE = "mode"
    const val BATTERY_PERCENT = "batteryPercent"
    const val CHARGING = "charging"
}

/** The fields of a member's profile. */
object ProfileField {
    const val DISPLAY_NAME = "displayName"
    const val PHOTO_URL = "photoUrl"
    const val UPDATED_AT = "updatedAt"
}

/** The fields of what a member gives out. */
object GrantField {
    const val DEVICES = "devices"
    const val SHARED_WITH = "sharedWith"
}

/** The fields of a claim to be looking. */
object WatchField {
    const val MEMBER_ID = "memberId"
    const val SINCE = "since"
}
