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
    groupDocumentOf(store, CurrentGroup.name)

/**
 * The same document of a group this phone is not in yet.
 *
 * Founding one and joining one are the two writes made before the phone has
 * moved: both have to reach a group by name, and the name is only worth
 * remembering once the write has been allowed. Everything else goes through
 * the group this phone is in.
 */
fun groupDocumentOf(store: FirebaseFirestore, group: String): DocumentReference =
    store.collection("groups").document(group)

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
 * The phones of each member and what each last said about itself: when it was
 * in touch, in which mode, how charged and whether on a cable.
 *
 * Apart from the positions because none of it is a place, and a phone whose
 * answers are all being held back as forged is still in touch and still on its
 * cable. Apart from the grants because an administrator reads it, and whom a
 * member shares with is not theirs to see.
 */
fun phonesOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("devices")

/**
 * What each member calls the phones and the people of the group, for
 * themselves: one document per member, read and written by that member alone.
 */
fun labelsOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("labels")

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
fun joinsOf(store: FirebaseFirestore): CollectionReference = joinsIn(groupDocument(store))

/**
 * The same collection under a group this phone has not joined yet, which is
 * the only group a claim to hold a code is ever written into.
 */
fun joinsIn(group: DocumentReference): CollectionReference = group.collection("joins")

/**
 * The places a false signal is known to put phones at, as this group knows
 * them.
 *
 * In the database rather than in the build: which places those are is a fact
 * about one household's surroundings, and a build carrying them would hand
 * them to everybody who downloads it. Everybody in the group reads the
 * collection, and only an administrator writes it.
 */
fun falsePlacesOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("falsePlaces")

/**
 * The group's searches: one document per phone the group is looking for,
 * addressed by `{uid}.{deviceId}` the same as a position or a siren.
 *
 * Everybody in the group reads the collection, because everybody's map marks
 * the phone being looked for; only an administrator writes it, and the member
 * whose phone it is may delete it themselves, once they are found.
 */
fun searchesOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("searches")

/**
 * The group's sirens: one document per phone, addressed by `{uid}.{deviceId}`
 * the same as a position.
 *
 * An administrator may call a siren on any phone in the group; a member may
 * call one only on their own. Reading a document is likewise limited to
 * whoever may call it, so the collection as a whole is never listened to —
 * only the one document a phone or a caller's screen cares about.
 */
fun sirensOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("sirens")

/**
 * What the group has settled about how its phones judge an answer.
 *
 * One document per subject rather than fields on the group document: the
 * roster is written by a rule that allows two named shapes and nothing else,
 * and every setting added to it would be a third.
 */
fun settingsOf(store: FirebaseFirestore): CollectionReference =
    groupDocument(store).collection("settings")

/** What counts as going round in circles, as this group has settled it. */
fun circlingSetting(store: FirebaseFirestore): DocumentReference =
    settingsOf(store).document("circling")

/** The fields of the group document itself. */
object GroupField {
    const val MEMBERS = "members"
    const val ADMINS = "admins"

    /** Who owns the group, where it is not named after them. */
    const val OWNER = "owner"

    /** What people call the group (ADR-0016); absent until it is given one. */
    const val NAME = "name"
}

/** The fields of an invitation. */
object InvitationField {
    const val CREATED_BY = "createdBy"
    const val EXPIRES_AT = "expiresAt"

    /** The address the rules let in on this invitation (`invitationShape`, ADR-0018). */
    const val EMAIL = "email"
}

/** The one field of a claim to be holding a code. */
object JoinField {
    const val CODE = "code"
}

/** The fields of a position document, as the writer and the reader both call them. */
object PositionField {
    const val MEMBER_ID = "memberId"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val TAKEN_AT = "takenAt"
    const val WRITTEN_AT = "writtenAt"
    const val MODE = "mode"
    const val BATTERY_PERCENT = "batteryPercent"
    const val CHARGING = "charging"

    /**
     * How wide the provider said the answer is, in metres.
     *
     * Written because a position is a claim of two parts, and a map showing
     * only the first says the same thing about an answer from the Wi-Fi in the
     * flat and an answer from a tower a kilometre and a half wide.
     */
    const val ACCURACY_METRES = "accuracyMetres"

    /**
     * How the phone arrived at the answer, as one of the words of `FoundBy`.
     *
     * The phone's own reading of it rather than the name of the provider: what
     * arrives under the name of the satellites is half the time an assistance
     * service answering over the network, and a reader shown the provider's
     * name would be told the opposite of what happened. The entry the phone
     * wrote is the key, and each client says it in its own language.
     */
    const val FOUND_BY = "foundBy"

    /**
     * Whether the receiver had not settled when this answer was taken.
     *
     * Written only where it is true, the way the width is written only where
     * there is one: a phone whose receiver has settled says nothing, and a
     * reader that finds nothing shows nothing. The field is what keeps the
     * tighter rules of those minutes from being the whole of the answer — an
     * answer of a receiver coming up that passes them is still an answer of a
     * receiver coming up, and a card that showed it like any other would be
     * putting the same weight on both.
     */
    const val SETTLING = "settling"

    /**
     * How many answers have been left out since the last position was written.
     *
     * The field that tells a phone being lied to from a phone that is off. A
     * phone under a transmitter writes nothing — every answer it gets is
     * thrown out by one rule or another — and from the outside that is exactly
     * what a phone in a drawer looks like: a marker quietly getting older. The
     * count says the phone is there, working, and being talked over.
     */
    const val LEFT_OUT_COUNT = "leftOutCount"

    /**
     * Why the last answer was left out, as one of the words of `JumpReason` or
     * the word for a source the member has switched off.
     *
     * The key rather than a sentence, for the same reason as [FOUND_BY]: each
     * client says it in its own language, and a word from a newer build is
     * shown as it came rather than mistranslated.
     */
    const val LEFT_OUT_REASON = "leftOutReason"

    /** When the run of left-out answers began, by the server's clock. */
    const val LEFT_OUT_SINCE = "leftOutSince"
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

/** The fields of one phone's entry in its member's document of phones. */
object PhoneField {
    const val MODEL = "model"
    const val NAME = "name"
    const val ONLINE_AT = "onlineAt"
    const val MODE = "mode"
    const val BATTERY_PERCENT = "batteryPercent"
    const val CHARGING = "charging"
}

/** The fields of a member's own names for the others. */
object LabelField {
    const val PHONES = "devices"
    const val MEMBERS = "members"
}

/** The fields of a place a false signal is known to use. */
object FalsePlaceField {
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val RADIUS_METRES = "radiusMetres"
    const val NAMED = "named"
}

/**
 * The fields of the circling setting, each one a limit of the rule that reads
 * the shape of a run of answers. Every one of them may be absent, and what is
 * absent keeps the value the build carries.
 */
object CirclingField {
    const val ANSWERS = "answers"
    const val WITHIN_SECONDS = "withinSeconds"
    const val ROUNDNESS = "roundness"
    const val WIDEST_METRES = "widestMetres"
    const val TIGHTEST_METRES = "tightestMetres"
    const val STEADINESS = "steadiness"
    const val TURNED_DEGREES = "turnedDegrees"
    const val SLOWEST_MPS = "slowestMps"
}

/** The fields of a claim to be looking. */
object WatchField {
    const val MEMBER_ID = "memberId"
    const val SINCE = "since"
}

/** The fields of a search: the group looking for one phone of a member. */
object SearchField {
    const val OPENED_BY = "openedBy"
    const val STARTED_AT = "startedAt"
    const val RENEWED_BY = "renewedBy"
    const val RENEWED_AT = "renewedAt"
    const val HOURS = "hours"

    /** The phone's name as the administrator saw it when they opened or renewed the search. */
    const val PHONE_NAME = "phoneName"
}

/** The fields of a siren: a call that rings one phone in the group. */
object SirenField {
    const val CALLED_BY = "calledBy"
    const val CALLED_AT = "calledAt"
    const val HEARD_AT = "heardAt"
    const val STOPPED_BY = "stoppedBy"
    const val STOPPED_AT = "stoppedAt"
}
