package io.github.vitalyostanin.geoshare.update

import android.content.Context
import io.github.vitalyostanin.geoshare.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * One check at a time for the whole process. The screen and the sharing
 * service start their checks on their own, and both read the time of the last
 * check before the request and write it after; without the lock two checks
 * started while one is due would both pass and both ask GitHub. Under the lock
 * the second one sees the time the first one wrote and asks nothing.
 */
private val checkLock = Mutex()

/**
 * Asks GitHub for a newer release if the last check is old enough, and tells
 * the member once per version. Called at every start of the screen and daily
 * by the sharing service; [isCheckDue] keeps those calls from adding up to
 * more than one request a day.
 */
suspend fun checkForUpdateIfDue(context: Context) {
    val app = context.applicationContext

    checkForUpdate(
        record = UpdateStore(app),
        current = BuildConfig.VERSION_NAME,
        now = Instant.now(),
        fetch = { fetchLatestRelease() },
        notify = { version -> showUpdateNotification(app, version) },
    )
    UpdateBoard.refresh(UpdateStore(app), BuildConfig.VERSION_NAME)
}

/** What a check the member asked for came to. */
sealed interface ManualCheck {
    /** A release newer than the app is out. */
    data class Newer(val release: LatestRelease) : ManualCheck

    /** GitHub answered and there is nothing newer than the app. */
    data object UpToDate : ManualCheck

    /** No answer: offline, rate-limited or any other status. */
    data object Failed : ManualCheck
}

/**
 * The check behind "Check for updates": asks GitHub now, whatever the time
 * of the last check, and keeps the answer in the store as the daily one does.
 * No notification, since the member is looking at the answer.
 */
suspend fun checkForUpdateNow(context: Context): ManualCheck {
    val store = UpdateStore(context.applicationContext)
    val result = checkNow(store, BuildConfig.VERSION_NAME, Instant.now()) { fetchLatestRelease() }

    UpdateBoard.refresh(store, BuildConfig.VERSION_NAME)

    return result
}

/** [checkForUpdateNow] with its edges exposed so a test can supply the answer and the clock. */
internal suspend fun checkNow(
    record: UpdateRecord,
    current: String,
    now: Instant,
    fetch: suspend () -> ReleaseCheck,
): ManualCheck = checkLock.withLock {
    val release = when (val result = fetch()) {
        ReleaseCheck.Unavailable -> return@withLock ManualCheck.Failed
        ReleaseCheck.NothingToInstall -> null
        is ReleaseCheck.Found -> result.release
    }
    val newer = release?.takeIf { isNewerVersion(it.version, current) }

    record.lastCheck = now
    record.found = newer

    if (newer == null) ManualCheck.UpToDate else ManualCheck.Newer(newer)
}

/**
 * [checkForUpdateIfDue] with its edges exposed so a test can supply the
 * answer, the clock and the notification.
 *
 * With [UpdateRecord.autoCheck] off nothing is asked and nothing is shown.
 * An [ReleaseCheck.Unavailable] answer leaves the time of the last check
 * alone, so the next start tries again instead of waiting out the day. A
 * release is remembered when it is newer than [current] and forgotten
 * otherwise (the member updated, or the release was withdrawn), and the
 * version is recorded as announced only when [notify] reports it was shown.
 * Whether the check is due is read under [checkLock], so a check that waited
 * for another one sees its answer instead of asking again.
 */
internal suspend fun checkForUpdate(
    record: UpdateRecord,
    current: String,
    now: Instant,
    fetch: suspend () -> ReleaseCheck,
    notify: (version: String) -> Boolean,
): Unit = checkLock.withLock {
    if (!record.autoCheck || !isCheckDue(record.lastCheck, now)) return@withLock

    val release = when (val result = fetch()) {
        ReleaseCheck.Unavailable -> return@withLock
        ReleaseCheck.NothingToInstall -> null
        is ReleaseCheck.Found -> result.release
    }

    record.lastCheck = now
    record.found = release?.takeIf { isNewerVersion(it.version, current) }

    if (release != null && shouldNotify(record.notifiedVersion, release.version, current) &&
        notify(release.version)
    ) {
        record.notifiedVersion = release.version
    }
}
