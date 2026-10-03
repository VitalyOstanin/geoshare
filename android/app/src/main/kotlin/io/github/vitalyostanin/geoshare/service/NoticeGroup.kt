package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.map.groupCaption
import io.github.vitalyostanin.geoshare.map.memberProfiles
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration

/** How long a notification waits for its group's name before going without it. */
private val GROUP_NAME_WAIT: Duration = Duration.ofSeconds(3)

/**
 * What a notification calls the group it is about, or null when it need not
 * say: a device in one group has nothing to tell apart, and a notification
 * held up by a group that does not answer is worse than one without its name.
 */
suspend fun groupNameForNotice(context: Context, connection: GroupConnection): String? {
    if (Connections.all.value.size < 2) {
        return null
    }

    return withTimeoutOrNull(GROUP_NAME_WAIT.toMillis()) {
        groupCaption(
            groupRoster(connection).filterNotNull().first(),
            memberProfiles(connection).first(),
            context.getString(R.string.group_name_unnamed),
        )
    }
}
