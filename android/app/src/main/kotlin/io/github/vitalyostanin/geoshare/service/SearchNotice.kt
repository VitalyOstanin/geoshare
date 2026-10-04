package io.github.vitalyostanin.geoshare.service

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.map.stands
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.Duration
import java.time.Instant

/**
 * How often the notification that says a search is standing is shown
 * again while it does, in case the process was killed and restarted
 * with its own memory of having shown it lost along with it.
 */
private val SEARCH_NOTICE_REFRESH: Duration = Duration.ofMinutes(1)

/**
 * Shows the notification that says this very phone is being looked for,
 * for as long as the search stands, and takes it down once it does not.
 *
 * A search ending is nothing arriving from the database: nothing deletes
 * the document at that moment, and this is what would otherwise leave the
 * notification up past the end everybody else's screen already reads.
 * Shown again once a minute while it stands, in case the process is killed
 * and restarted with the notification's own state lost with it.
 *
 * One notification per group, tagged with its project: a search ending in
 * one group must not take down the one still standing in another.
 */
suspend fun keepSearchNotification(context: Context, phoneId: String, connection: GroupConnection) {
    ownSearch(phoneId, connection).collectLatest { search ->
        if (search == null) {
            cancelSearchNotification(context, connection.projectId)

            return@collectLatest
        }

        val openedByName =
            memberDisplayName(search.openedBy, connection)
                ?: unnamedMember(context, search.openedBy)
        val group = groupNameForNotice(context, connection)

        while (search.stands(Instant.now())) {
            // A member who refused notifications still gets the banner over
            // the map; the service goes on either way.
            if (devicePermissions(context).notificationsGranted) {
                context.getSystemService<NotificationManager>()?.notify(
                    connection.projectId,
                    SEARCH_NOTIFICATION_ID,
                    searchNotification(context, search, openedByName, group),
                )
            }

            val left = Duration.between(Instant.now(), search.endsAt)

            delay(minOf(left, SEARCH_NOTICE_REFRESH).toMillis().coerceAtLeast(0))
        }

        cancelSearchNotification(context, connection.projectId)
    }
}
