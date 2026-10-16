package io.github.vitalyostanin.geoshare.update

private const val BYTES_PER_MEGABYTE = 1024L * 1024L

/** A line the block shows under its button after something went wrong or came out. */
enum class UpdateNote {
    /** The check found nothing newer. */
    Latest,

    /** The check got no answer. */
    CheckFailed,

    /** The download did not match the size or digest GitHub announced. */
    Damaged,

    /** The download or the hand-over to the installer did not work. */
    Failed,
}

/** What the update block of the settings screen shows. */
sealed interface UpdateBlockState {
    /** No newer release is known: a button to look, and the outcome of the last look. */
    data class Idle(val note: UpdateNote? = null) : UpdateBlockState

    /** A check the member asked for is running. */
    data object Checking : UpdateBlockState

    /** [release] is out: its description and the buttons; [note] tells how the last try went. */
    data class Available(val release: LatestRelease, val note: UpdateNote? = null) :
        UpdateBlockState

    /** [release] is coming down: [received] of [total] bytes so far. */
    data class Downloading(val release: LatestRelease, val received: Long, val total: Long) :
        UpdateBlockState
}

/** The state the block opens in: the stored release if it is still newer than [current]. */
fun initialUpdateState(found: LatestRelease?, current: String): UpdateBlockState =
    found?.takeIf { isNewerVersion(it.version, current) }
        ?.let { UpdateBlockState.Available(it) }
        ?: UpdateBlockState.Idle()

/** The state after a check the member asked for. */
fun afterManualCheck(result: ManualCheck): UpdateBlockState = when (result) {
    is ManualCheck.Newer -> UpdateBlockState.Available(result.release)
    ManualCheck.UpToDate -> UpdateBlockState.Idle(UpdateNote.Latest)
    ManualCheck.Failed -> UpdateBlockState.Idle(UpdateNote.CheckFailed)
}

/** The state when the download of [release] starts. */
fun startDownload(release: LatestRelease): UpdateBlockState =
    UpdateBlockState.Downloading(release, received = 0, total = release.apk.size)

/**
 * The state after a download that did not end in a file: the member may try
 * again, with a line saying what went wrong. A finished one has no state of
 * its own, since the installer takes over; see [afterInstallStart].
 */
fun afterFailedDownload(release: LatestRelease, result: ApkDownload): UpdateBlockState =
    UpdateBlockState.Available(
        release,
        when (result) {
            ApkDownload.Corrupt -> UpdateNote.Damaged
            else -> UpdateNote.Failed
        },
    )

/**
 * The state once the file was handed over. Started and NeedsPermission both
 * leave the block as it was (the system's own screen is what the member
 * sees, and for the permission they press "Update" again on coming back);
 * only a device that cannot install says so.
 */
fun afterInstallStart(release: LatestRelease, result: InstallStart): UpdateBlockState =
    UpdateBlockState.Available(
        release,
        if (result == InstallStart.Unavailable) UpdateNote.Failed else null,
    )

/** The state when the member cancels a download. */
fun afterCancel(release: LatestRelease): UpdateBlockState = UpdateBlockState.Available(release)

/** Bytes as the whole megabytes the block shows, never less than one. */
fun megabytes(bytes: Long): Long =
    ((bytes + BYTES_PER_MEGABYTE / 2) / BYTES_PER_MEGABYTE).coerceAtLeast(1)

/** How far a download has got, 0 to 1; 0 while the size is unknown. */
fun downloadFraction(received: Long, total: Long): Float =
    if (total <= 0) 0f else (received.toFloat() / total).coerceIn(0f, 1f)
