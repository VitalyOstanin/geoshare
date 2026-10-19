package io.github.vitalyostanin.geoshare.update

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val BODY = ByteArray(200_000) { (it % 251).toByte() }

/** How long the race test waits for a progress report to reach its write. */
private const val RACE_WAIT_MS = 500L

private fun release(size: Long = BODY.size.toLong(), digest: ByteArray = BODY): LatestRelease =
    LatestRelease(
        version = "0.2.0",
        pageUrl = "https://github.com/x/geoshare/releases/tag/v0.2.0",
        publishedAt = null,
        apk = ApkAsset(
            url = "https://github.com/x/geoshare/releases/download/v0.2.0/geoshare.apk",
            size = size,
            sha256 = MessageDigest.getInstance("SHA-256").digest(digest)
                .joinToString("") { "%02x".format(it) },
        ),
    )

class ApkDownloadTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun download(
        release: LatestRelease,
        body: ByteArray = BODY,
        progress: (Long, Long) -> Unit = { _, _ -> },
    ): ApkDownload = runBlocking {
        downloadApk(release, folder.root, progress) { ByteArrayInputStream(body) }
    }

    @Test
    fun `an intact download is kept under the name of its version`() {
        val result = download(release()) as ApkDownload.Done

        assertEquals("geoshare-0.2.0.apk", result.file.name)
        assertTrue(result.file.readBytes().contentEquals(BODY))
    }

    @Test
    fun `progress ends at the whole size`() {
        val seen = mutableListOf<Long>()

        download(release()) { received, total ->
            assertEquals(BODY.size.toLong(), total)
            seen += received
        }

        assertEquals(BODY.size.toLong(), seen.last())
        assertEquals(seen.sorted(), seen)
    }

    @Test
    fun `a body that does not match its digest is deleted and reported corrupt`() {
        val altered = BODY.copyOf().also { it[10] = (it[10] + 1).toByte() }

        assertEquals(ApkDownload.Corrupt, download(release(), body = altered))
        assertEquals(0, folder.root.listFiles().orEmpty().size)
    }

    @Test
    fun `a body longer than announced is deleted and reported corrupt`() {
        val result = download(release(size = BODY.size - 100L))

        assertEquals(ApkDownload.Corrupt, result)
        assertEquals(0, folder.root.listFiles().orEmpty().size)
    }

    @Test
    fun `a body shorter than announced is deleted and reported corrupt`() {
        assertEquals(ApkDownload.Corrupt, download(release(size = BODY.size + 100L)))
        assertEquals(0, folder.root.listFiles().orEmpty().size)
    }

    @Test
    fun `a failing connection is a failure and leaves no file`() {
        val result = runBlocking {
            downloadApk(release(), folder.root, { _, _ -> }) { throw IOException("reset") }
        }

        assertEquals(ApkDownload.Failed, result)
        assertEquals(0, folder.root.listFiles().orEmpty().size)
    }

    @Test
    fun `files of earlier updates are removed before a new download`() {
        val old = File(folder.root, "geoshare-0.1.0.apk").apply { writeText("old") }

        download(release())

        assertFalse(old.exists())
    }

    @Test
    fun `an intact file of this release is reused without opening the network`() {
        val kept = File(folder.root, "geoshare-0.2.0.apk").apply { writeBytes(BODY) }
        val other = File(folder.root, "geoshare-0.1.0.apk").apply { writeText("old") }

        val result = runBlocking {
            downloadApk(release(), folder.root, { _, _ -> }) { error("must not download") }
        }

        assertEquals(ApkDownload.Done(kept), result)
        assertFalse(other.exists())
    }

    @Test
    fun `a file of this release with a wrong digest is downloaded again`() {
        val altered = BODY.copyOf().also { it[10] = (it[10] + 1).toByte() }

        File(folder.root, "geoshare-0.2.0.apk").writeBytes(altered)

        val result = download(release()) as ApkDownload.Done

        assertTrue(result.file.readBytes().contentEquals(BODY))
    }

    @Test
    fun `a cancelled download leaves no partial file`() {
        val midway = CompletableDeferred<Unit>()
        val never = object : InputStream() {
            private var sent = 0

            override fun read(): Int = error("single bytes are not read")

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (sent > 0) {
                    midway.complete(Unit)
                    Thread.sleep(50)
                }
                sent += 1
                buffer.fill(1, offset, offset + length)
                return length
            }
        }

        runBlocking {
            val job = async(Dispatchers.IO) {
                downloadApk(release(size = Long.MAX_VALUE), folder.root, { _, _ -> }) { never }
            }

            midway.await()
            job.cancelAndJoin()
        }

        assertEquals(0, folder.root.listFiles().orEmpty().size)
    }

    // DEV-13: the settings write the update block from onProgress and, on
    // "Cancel", cancel the download and write the cancelled state on the
    // same thread. A report already past ensureActive on the IO thread used
    // to land after that and leave the block downloading for good.
    @Test
    fun `progress reported after cancelling does not overwrite what the caller set`() {
        val reached = CountDownLatch(1)
        val resume = CountDownLatch(1)
        var shown = "idle"
        val progress = { _: Long, _: Long ->
            reached.countDown()
            resume.await()
            shown = "downloading"
        }

        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                downloadApk(release(), folder.root, progress) { ByteArrayInputStream(BODY) }
            }

            // With progress on the IO thread the report is now parked just
            // before its write; on the caller's thread it cannot run before
            // this block lets go of the thread, so the wait only times out.
            reached.await(RACE_WAIT_MS, TimeUnit.MILLISECONDS)
            job.cancel()
            shown = "cancelled"
            resume.countDown()
            job.join()
        }

        assertEquals("cancelled", shown)
        assertEquals(0, folder.root.listFiles().orEmpty().size)
    }
}
