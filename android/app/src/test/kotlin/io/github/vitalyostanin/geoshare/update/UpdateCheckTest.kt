package io.github.vitalyostanin.geoshare.update

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

private val NOW = Instant.parse("2026-10-17T12:00:00Z")

private fun release(version: String) = LatestRelease(
    version = version,
    pageUrl = "https://github.com/x/geoshare/releases/tag/v$version",
    publishedAt = Instant.parse("2026-10-17T10:00:00Z"),
    apk = ApkAsset(
        "https://github.com/x/geoshare/releases/download/v$version/g.apk",
        10,
        "0".repeat(64),
    ),
)

private class FakeRecord(
    override var lastCheck: Instant? = null,
    override var notifiedVersion: String? = null,
    override var found: LatestRelease? = null,
    override var autoCheck: Boolean = true,
) : UpdateRecord

class UpdateCheckTest {
    private val shown = mutableListOf<String>()

    private fun run(
        record: UpdateRecord,
        answer: ReleaseCheck,
        now: Instant = NOW,
        shownOk: Boolean = true,
    ) = runBlocking {
        checkForUpdate(
            record = record,
            current = "0.1.0",
            now = now,
            fetch = { answer },
            notify = {
                shown += it
                shownOk
            },
        )
    }

    @Test
    fun `with the automatic check off nothing is asked and nothing is announced`() {
        val record = FakeRecord(autoCheck = false)
        var asked = false

        runBlocking {
            checkForUpdate(
                record = record,
                current = "0.1.0",
                now = NOW,
                fetch = {
                    asked = true
                    ReleaseCheck.Found(release("0.2.0"))
                },
                notify = {
                    shown += it
                    true
                },
            )
        }

        assertEquals(false, asked)
        assertEquals(emptyList<String>(), shown)
        assertNull(record.found)
        assertNull(record.lastCheck)
    }

    @Test
    fun `a manual check asks even with the automatic one off and keeps the answer`() {
        val record = FakeRecord(autoCheck = false, lastCheck = NOW)

        val result = runBlocking {
            checkNow(record, "0.1.0", NOW.plus(Duration.ofMinutes(1))) {
                ReleaseCheck.Found(release("0.2.0"))
            }
        }

        assertEquals(ManualCheck.Newer(release("0.2.0")), result)
        assertEquals("0.2.0", record.found?.version)
    }

    @Test
    fun `a manual check that gets no answer leaves the record alone`() {
        val record = FakeRecord(found = release("0.2.0"))

        val result = runBlocking { checkNow(record, "0.1.0", NOW) { ReleaseCheck.Unavailable } }

        assertEquals(ManualCheck.Failed, result)
        assertEquals("0.2.0", record.found?.version)
    }

    @Test
    fun `a manual check that finds nothing newer forgets the stored release`() {
        val record = FakeRecord(found = release("0.2.0"))

        val result = runBlocking {
            checkNow(record, "0.2.0", NOW) { ReleaseCheck.Found(release("0.2.0")) }
        }

        assertEquals(ManualCheck.UpToDate, result)
        assertNull(record.found)
    }

    @Test
    fun `a newer release is remembered and announced once`() {
        val record = FakeRecord()

        run(record, ReleaseCheck.Found(release("0.2.0")))

        assertEquals(listOf("0.2.0"), shown)
        assertEquals("0.2.0", record.notifiedVersion)
        assertEquals("0.2.0", record.found?.version)
        assertEquals(NOW, record.lastCheck)
    }

    @Test
    fun `the same version found by a later check is not announced again`() {
        val record = FakeRecord()

        run(record, ReleaseCheck.Found(release("0.2.0")))
        run(record, ReleaseCheck.Found(release("0.2.0")), NOW.plus(CHECK_INTERVAL))

        assertEquals(listOf("0.2.0"), shown)
        assertEquals(NOW.plus(CHECK_INTERVAL), record.lastCheck)
    }

    @Test
    fun `a still newer version is announced in its turn`() {
        val record = FakeRecord()

        run(record, ReleaseCheck.Found(release("0.2.0")))
        run(record, ReleaseCheck.Found(release("0.3.0")), NOW.plus(CHECK_INTERVAL))

        assertEquals(listOf("0.2.0", "0.3.0"), shown)
    }

    @Test
    fun `an unavailable answer leaves the time of the last check alone`() {
        val earlier = NOW.minus(Duration.ofDays(2))
        val record = FakeRecord(lastCheck = earlier, found = release("0.2.0"))

        run(record, ReleaseCheck.Unavailable)

        assertEquals(earlier, record.lastCheck)
        assertEquals("0.2.0", record.found?.version)
        assertEquals(emptyList<String>(), shown)
    }

    @Test
    fun `a check that is not due asks nothing`() {
        val record = FakeRecord(lastCheck = NOW.minus(Duration.ofHours(1)))
        var asked = false

        runBlocking {
            checkForUpdate(record, "0.1.0", NOW, {
                asked = true
                ReleaseCheck.Unavailable
            }, { true })
        }

        assertEquals(false, asked)
    }

    @Test
    fun `the remembered release is cleared when nothing newer exists`() {
        val record = FakeRecord(
            lastCheck = NOW.minus(Duration.ofDays(2)),
            found = release("0.2.0"),
        )

        run(record, ReleaseCheck.Found(release("0.1.0")))

        assertNull(record.found)
        assertEquals(NOW, record.lastCheck)
        assertEquals(emptyList<String>(), shown)
    }

    @Test
    fun `the remembered release is cleared when GitHub has nothing to install`() {
        val record = FakeRecord(lastCheck = null, found = release("0.2.0"))

        run(record, ReleaseCheck.NothingToInstall)

        assertNull(record.found)
        assertEquals(NOW, record.lastCheck)
    }

    @Test
    fun `a notification that could not be shown is tried again next time`() {
        val record = FakeRecord()

        run(record, ReleaseCheck.Found(release("0.2.0")), shownOk = false)
        assertNull(record.notifiedVersion)

        run(record, ReleaseCheck.Found(release("0.2.0")), NOW.plus(CHECK_INTERVAL))

        assertEquals(listOf("0.2.0", "0.2.0"), shown)
        assertEquals("0.2.0", record.notifiedVersion)
    }

    // DEV-13: the screen and the sharing service start their checks on their
    // own; two of them started while a check is due must still ask GitHub once.
    @Test
    fun `two checks started together ask and announce once`() {
        val record = FakeRecord()
        val answer = CompletableDeferred<ReleaseCheck>()
        var asked = 0

        runBlocking {
            val checks = List(2) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    checkForUpdate(
                        record = record,
                        current = "0.1.0",
                        now = NOW,
                        fetch = {
                            asked++
                            answer.await()
                        },
                        notify = {
                            shown += it
                            true
                        },
                    )
                }
            }

            answer.complete(ReleaseCheck.Found(release("0.2.0")))
            checks.forEach { it.join() }
        }

        assertEquals(1, asked)
        assertEquals(listOf("0.2.0"), shown)
        assertEquals(NOW, record.lastCheck)
    }
}
