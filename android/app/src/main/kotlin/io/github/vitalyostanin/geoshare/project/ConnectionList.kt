package io.github.vitalyostanin.geoshare.project

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val FIELD_CONNECTIONS = "connections"
private const val FIELD_SELECTED = "selected"
private const val FIELD_SETTINGS = "settings"
private const val FIELD_GROUP = "group"

/**
 * The group of the household that started before groups had names of their
 * own.
 *
 * Kept as what a device assumes when it has joined nothing: those devices are
 * already writing into it, their group document was made before the rule that
 * names a founder, and an update that moved them elsewhere would take the
 * household off its own map.
 */
const val INHERITED: String = "family"

/** One group this device is in, as it is kept: the project, and the group's name in it. */
data class Connected(val settings: ProjectSettings, val group: String)

/**
 * The groups this device is in, and which of them the screens show.
 *
 * Keyed by project: a project holds one group for this device (ADR-0005), so
 * joining another group of a project already listed moves the device within
 * that project rather than adding a second entry for it.
 *
 * Only data, with nothing of Firebase in it, so that what is kept and how it
 * changes can be settled apart from starting anything.
 */
data class ConnectionList(val connections: List<Connected>, val selected: String?) {
    /** The list with [connection] in it, in place of any group of the same project, and selected. */
    fun with(connection: Connected): ConnectionList {
        val projectId = connection.settings.projectId
        val listed = connections.any { it.settings.projectId == projectId }
        val next =
            if (listed) {
                connections.map { if (it.settings.projectId == projectId) connection else it }
            } else {
                connections + connection
            }

        return ConnectionList(next, projectId)
    }

    /**
     * The list without the group of [projectId]. The selection stays where it
     * was unless that was the group removed, and then moves to the first one
     * left — the screens always show some group while there is one.
     */
    fun without(projectId: String): ConnectionList = ConnectionList(
        connections.filterNot {
            it.settings.projectId == projectId
        },
        selected,
    ).settled()

    /** The list with [projectId] selected, or unchanged when no group of it is listed. */
    fun selecting(projectId: String): ConnectionList =
        if (listed(projectId)) copy(selected = projectId) else this

    fun toJson(): String = JSONObject()
        .put(
            FIELD_CONNECTIONS,
            JSONArray(
                connections.map { connection ->
                    JSONObject()
                        .put(FIELD_SETTINGS, JSONObject(connection.settings.toJson()))
                        .put(FIELD_GROUP, connection.group)
                },
            ),
        )
        .put(FIELD_SELECTED, selected ?: JSONObject.NULL)
        .toString()

    private fun listed(projectId: String?) = connections.any { it.settings.projectId == projectId }

    private fun settled(): ConnectionList = if (listed(
            selected,
        )
    ) {
        this
    } else {
        copy(selected = connections.firstOrNull()?.settings?.projectId)
    }

    companion object {
        /**
         * Reads a kept list, or null when it is not one this build can start
         * with.
         *
         * One unreadable entry refuses the whole list rather than dropping
         * that group: a device that silently left a group would go on being
         * listed in it, and nothing on the screen would say why its position
         * stopped arriving there.
         */
        fun parse(json: String): ConnectionList? {
            val document =
                try {
                    JSONObject(json)
                } catch (malformed: JSONException) {
                    return null
                }
            val entries = document.optJSONArray(FIELD_CONNECTIONS) ?: return null
            val connections =
                (0 until entries.length()).map { index ->
                    val entry = entries.optJSONObject(index) ?: return null
                    val settings =
                        entry.optJSONObject(FIELD_SETTINGS)?.let {
                            ProjectSettings.parse(it.toString())
                        }
                            ?: return null
                    val group =
                        entry.optString(FIELD_GROUP).takeIf { it.isNotBlank() } ?: return null

                    Connected(settings, group)
                }
            val selected = if (document.isNull(
                    FIELD_SELECTED,
                )
            ) {
                null
            } else {
                document.optString(FIELD_SELECTED)
            }

            return ConnectionList(connections, selected).settled()
        }

        /**
         * What a build that knew a single project kept, as a list of one: its
         * settings, and the group it wrote into — or the inherited group, for
         * a device that never named one.
         */
        fun migrated(settingsJson: String, group: String?): ConnectionList? {
            val settings = ProjectSettings.parse(settingsJson) ?: return null

            return ConnectionList(
                listOf(Connected(settings, group ?: INHERITED)),
                settings.projectId,
            )
        }
    }
}
