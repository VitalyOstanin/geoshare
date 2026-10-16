package io.github.vitalyostanin.geoshare.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

private fun release(version: String, size: Long = 12L * 1024 * 1024) = LatestRelease(
    version = version,
    pageUrl = "https://github.com/x/geoshare/releases/tag/v$version",
    publishedAt = Instant.parse("2026-10-17T10:00:00Z"),
    apk = ApkAsset(
        "https://github.com/x/geoshare/releases/download/v$version/g.apk",
        size,
        "0".repeat(64),
    ),
)

class UpdateBlockStateTest {
    @Test
    fun `a stored release newer than the app opens the block as available`() {
        val state = initialUpdateState(release("0.2.0"), "0.1.0")

        assertEquals(UpdateBlockState.Available(release("0.2.0")), state)
    }

    @Test
    fun `a stored release the app has caught up with opens the block idle`() {
        assertEquals(UpdateBlockState.Idle(), initialUpdateState(release("0.2.0"), "0.2.0-debug"))
        assertEquals(UpdateBlockState.Idle(), initialUpdateState(null, "0.1.0"))
    }

    @Test
    fun `each answer of a manual check has its state`() {
        assertEquals(
            UpdateBlockState.Available(release("0.2.0")),
            afterManualCheck(ManualCheck.Newer(release("0.2.0"))),
        )
        assertEquals(
            UpdateBlockState.Idle(UpdateNote.Latest),
            afterManualCheck(ManualCheck.UpToDate),
        )
        assertEquals(
            UpdateBlockState.Idle(UpdateNote.CheckFailed),
            afterManualCheck(ManualCheck.Failed),
        )
    }

    @Test
    fun `a download starts at zero of the announced size`() {
        val state = startDownload(release("0.2.0", size = 100)) as UpdateBlockState.Downloading

        assertEquals(0L, state.received)
        assertEquals(100L, state.total)
    }

    @Test
    fun `a damaged file and a failed download are told apart`() {
        val r = release("0.2.0")

        assertEquals(
            UpdateBlockState.Available(r, UpdateNote.Damaged),
            afterFailedDownload(r, ApkDownload.Corrupt),
        )
        assertEquals(
            UpdateBlockState.Available(r, UpdateNote.Failed),
            afterFailedDownload(r, ApkDownload.Failed),
        )
        assertTrue(
            afterFailedDownload(r, ApkDownload.Done(File("x"))) is UpdateBlockState.Available,
        )
    }

    @Test
    fun `only a device that cannot install reports a failure after the hand-over`() {
        val r = release("0.2.0")

        assertEquals(UpdateBlockState.Available(r), afterInstallStart(r, InstallStart.Started))
        assertEquals(
            UpdateBlockState.Available(r),
            afterInstallStart(r, InstallStart.NeedsPermission),
        )
        assertEquals(
            UpdateBlockState.Available(r, UpdateNote.Failed),
            afterInstallStart(r, InstallStart.Unavailable),
        )
    }

    @Test
    fun `cancelling returns to the offer without a note`() {
        assertEquals(UpdateBlockState.Available(release("0.2.0")), afterCancel(release("0.2.0")))
    }

    @Test
    fun `sizes round to whole megabytes and never show zero`() {
        assertEquals(12L, megabytes(12L * 1024 * 1024 + 1000))
        assertEquals(7L, megabytes(7L * 1024 * 1024 - 1000))
        assertEquals(1L, megabytes(10))
    }

    @Test
    fun `the fraction is clamped and zero without a size`() {
        assertEquals(0.5f, downloadFraction(50, 100), 0f)
        assertEquals(1f, downloadFraction(150, 100), 0f)
        assertEquals(0f, downloadFraction(5, 0), 0f)
    }
}
