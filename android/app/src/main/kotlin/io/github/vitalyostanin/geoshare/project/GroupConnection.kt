package io.github.vitalyostanin.geoshare.project

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Everything one group is reached through: its project, the group's name in
 * it, and the Firebase instance started for that project.
 *
 * One per group rather than one per process, because each group is a project
 * of its own (ADR-0005): a device in two groups talks to two databases and
 * holds two sessions at once, and every read and write has to say which of
 * them it means.
 *
 * Compared by identity on purpose. Joining the group a device already wrote
 * into changes what the rules allow without changing a single field here,
 * and the screens still have to take their subscriptions out again: a
 * subscription refused before the device was let in stays refused. Each time
 * [Connections] publishes, every connection is a new object, and that is the
 * change the screens follow.
 */
class GroupConnection(val settings: ProjectSettings, val group: String, val app: FirebaseApp) {
    val projectId: String
        get() = settings.projectId

    /** The session in this group's project; each project signs in on its own (ADR-0002). */
    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance(app)

    val store: FirebaseFirestore
        get() = FirebaseFirestore.getInstance(app)

    /** The group's own document, which every other path of the group hangs off. */
    val groupDocument: DocumentReference
        get() = store.collection("groups").document(group)

    override fun toString(): String = "GroupConnection($projectId, $group)"
}
