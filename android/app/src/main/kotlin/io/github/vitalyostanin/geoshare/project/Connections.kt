package io.github.vitalyostanin.geoshare.project

import android.content.Context
import com.google.firebase.FirebaseApp
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.readString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val FILE = "connections"
private const val KEY_LIST = "list"

/**
 * A kept list that could not be read, set aside before anything overwrites
 * it: joining a group writes the list anew, and the groups in the old one
 * would otherwise be gone for good rather than waiting for a build that reads
 * them.
 */
internal const val KEY_UNREADABLE_LIST = "list_unreadable"
private const val KEY_DEFAULT_PROJECT = "default_project"
private const val TAG = "geoshare.connections"

/** Where a build that knew a single project kept it, read once to carry it over. */
private const val OLD_PROJECT_FILE = "family_project"
private const val OLD_PROJECT_KEY = "settings"
private const val OLD_GROUP_FILE = "group"
private const val OLD_GROUP_KEY = "name"

/**
 * The groups this device is in, each with the Firebase instance it is reached
 * through, and the one the screens show.
 *
 * One build serves every group, so it carries no project of its own: what a
 * device reaches is decided by the settings connection links download and hand
 * here to keep. Kept outside the session and read back before anything else
 * touches Firebase, because a service started at boot reaches for the
 * database before a screen has had the chance to ask.
 *
 * Every group's project gets an instance named after it, started in this
 * process without a restart (ADR-0026). The one exception is the project a
 * device was connected to before it could be in more than one group: that
 * project keeps the default instance, which is where its stored sign-in lives,
 * so that the update does not sign anybody out. Nothing reaches it as the
 * default, though — every caller goes through its [GroupConnection].
 */
object Connections {
    private val listed = MutableStateFlow<List<GroupConnection>>(emptyList())
    private val chosen = MutableStateFlow<GroupConnection?>(null)
    private var kept = ConnectionList(emptyList(), null)

    /**
     * Told about every instance the moment it is started, before anything has
     * used it: the debug build points it at the emulators there, which the
     * library refuses once the instance has been used.
     */
    var onStarted: ((GroupConnection) -> Unit)? = null

    /** Every group this device is in, in the order they were joined. */
    val all: StateFlow<List<GroupConnection>> = listed.asStateFlow()

    /**
     * The group the screens show, or null while the device is in none.
     *
     * The screens take every subscription out of a path built around this,
     * and a subscription taken out before the device was let in goes on being
     * refused afterwards: the database drops a refused listener, and the one
     * taken out in its place waits longer each time. So they follow this and
     * take their subscriptions out again whenever it changes — which is on
     * joining a group as well as on moving to another, because joining the
     * group the device already wrote into changes what the rules allow
     * without changing the name; a new value is published for that too.
     */
    val selected: StateFlow<GroupConnection?> = chosen.asStateFlow()

    /** Whether this device is in any group at all. */
    val connected: Boolean
        get() = listed.value.isNotEmpty()

    /** The group of [projectId], or null when this device reaches no such project. */
    fun of(projectId: String): GroupConnection? = listed.value.firstOrNull {
        it.projectId ==
            projectId
    }

    /**
     * Reads back the groups kept on a previous run, or what a build that knew
     * a single project kept, and starts an instance for each.
     *
     * Called once, before anything else in the application touches Firebase.
     */
    fun start(context: Context) {
        val stored = files(context).readString(KEY_LIST, null)
        val list =
            if (stored != null) {
                ConnectionList.parse(stored).also {
                    if (it == null) setAside(context, stored)
                }
            } else {
                migrated(context)
            }

        if (list != null) {
            publish(context, list)
        }
    }

    /**
     * Adds a group, or moves the device to another group of a project it
     * already reaches, and shows it.
     *
     * Written with `commit` rather than `apply`: what follows may be the
     * device being switched off or the process being cut short, and the list
     * is what the next start reads before anything else.
     */
    fun connect(context: Context, settings: ProjectSettings, group: String) =
        publish(context, kept.with(Connected(settings, group)))

    /**
     * Says the device is now in [group] of a project it already reaches —
     * founding a group, or joining one — and remembers it for the next start.
     *
     * The two happen together on purpose: a device that joined a group and
     * forgot it by the morning would go on writing into the group it left,
     * and the rules would refuse every write without anything on the screen
     * saying why.
     */
    fun moveTo(context: Context, projectId: String, group: String) {
        val settings = kept.connections.firstOrNull { it.settings.projectId == projectId }?.settings

        if (settings == null) {
            AppLog.w(TAG, "asked to move within $projectId, which this device does not reach")

            return
        }

        publish(context, kept.with(Connected(settings, group)))
        AppLog.i(TAG, "now in the group $group of $projectId")
    }

    /** Shows the group of [projectId]; nothing else changes. */
    fun select(context: Context, projectId: String) = publish(context, kept.selecting(projectId))

    /**
     * Forgets the group of [projectId]; its session is the caller's to sign
     * out first.
     *
     * The instance it was reached through is left running until the process
     * ends. The screens and the service let go of their subscriptions only
     * once they see the list without it — on the next frame, in their own
     * coroutines — and an instance deleted under a subscription still held
     * fails it. Joining the project again in the same run takes the same
     * instance back.
     */
    fun remove(context: Context, projectId: String) {
        publish(context, kept.without(projectId))
        SeenOnRoster.forget(context, projectId)
        GroupNames.forget(context, projectId)
        RemovedHere.forget(context, projectId)
        SignInStarted.forget(context, projectId)

        AppLog.i(TAG, "no longer in $projectId")
    }

    /** The file the kept list lives in, beside which [PendingInvitationStore] keeps its own key. */
    internal fun files(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun setAside(context: Context, stored: String) {
        AppLog.w(TAG, "the groups kept on the device could not be read; setting them aside")

        // The first unreadable list is the one worth keeping: a later one is
        // only what was written over it.
        if (files(context).contains(KEY_UNREADABLE_LIST)) {
            return
        }

        if (!files(context).edit().putString(KEY_UNREADABLE_LIST, stored).commit()) {
            AppLog.e(TAG, "the unreadable groups could not be set aside")
        }
    }

    private fun publish(context: Context, list: ConnectionList) {
        if (!files(context).edit().putString(KEY_LIST, list.toJson()).commit()) {
            // The device goes on with the list it has in memory; the next start
            // reads whatever the file still holds.
            AppLog.e(TAG, "the groups could not be written down")
        }

        kept = list

        val connections = list.connections.map { started(context, it) }

        listed.value = connections
        chosen.value = connections.firstOrNull { it.projectId == list.selected }
    }

    /**
     * The connection for [connected], starting its project's instance unless
     * this process already has.
     *
     * The instance stays when the group within the project changes: only the
     * name of the group moves, and a new instance would throw the session
     * away with it.
     */
    private fun started(context: Context, connected: Connected): GroupConnection {
        val settings = connected.settings
        val name = instanceName(context, settings.projectId)
        val running = FirebaseApp.getApps(context).firstOrNull { it.name == name }
        val app =
            running ?: if (name == FirebaseApp.DEFAULT_APP_NAME) {
                FirebaseApp.initializeApp(context, settings.toOptions())
            } else {
                FirebaseApp.initializeApp(context, settings.toOptions(), name)
            }
        val connection = GroupConnection(settings, connected.group, app)

        if (running == null) {
            AppLog.i(TAG, "talking to the project ${settings.projectId}")
            onStarted?.invoke(connection)
        }

        return connection
    }

    /**
     * The instance a project is reached through: the default one for the
     * project it was first made for, and one named after the project for any
     * other. The first project ever started here claims the default, which is
     * the one a build that knew a single project kept its sign-in in.
     */
    private fun instanceName(context: Context, projectId: String): String {
        val files = files(context)
        val owner = files.readString(KEY_DEFAULT_PROJECT, null)

        if (owner == null) {
            files.edit().putString(KEY_DEFAULT_PROJECT, projectId).commit()

            return FirebaseApp.DEFAULT_APP_NAME
        }

        return if (owner == projectId) FirebaseApp.DEFAULT_APP_NAME else projectId
    }

    /**
     * What a build that knew a single project kept, as a list of one.
     *
     * The old files are left where they are: a device taken back to that
     * build finds them still there.
     */
    private fun migrated(context: Context): ConnectionList? {
        val settings =
            context.getSharedPreferences(OLD_PROJECT_FILE, Context.MODE_PRIVATE)
                .readString(OLD_PROJECT_KEY, null) ?: return null
        val group =
            context.getSharedPreferences(OLD_GROUP_FILE, Context.MODE_PRIVATE)
                .readString(OLD_GROUP_KEY, null)
        val list = ConnectionList.migrated(settings, group)

        if (list == null) {
            AppLog.w(TAG, "the settings an earlier build kept could not be read")
        } else {
            AppLog.i(TAG, "carried over the group an earlier build kept")
        }

        return list
    }
}
