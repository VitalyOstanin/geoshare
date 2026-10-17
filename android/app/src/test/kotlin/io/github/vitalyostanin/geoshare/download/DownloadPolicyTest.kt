package io.github.vitalyostanin.geoshare.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DownloadPolicyTest {
    @Test
    fun `out of the box Wi-Fi and the mobile network at home go and roaming asks`() {
        assertEquals(
            DownloadVerdict.Go,
            downloadVerdict(NetworkKind.WiFi, DEFAULT_DOWNLOAD_NETWORKS),
        )
        assertEquals(
            DownloadVerdict.Go,
            downloadVerdict(NetworkKind.Mobile, DEFAULT_DOWNLOAD_NETWORKS),
        )
        assertEquals(
            DownloadVerdict.AskFirst,
            downloadVerdict(NetworkKind.Roaming, DEFAULT_DOWNLOAD_NETWORKS),
        )
    }

    @Test
    fun `no network waits whatever is allowed`() {
        assertEquals(
            DownloadVerdict.Wait,
            downloadVerdict(NetworkKind.None, DownloadNetwork.entries.toSet()),
        )
    }

    // Switching a network off at home means "not over this one", not "ask me":
    // only roaming is asked about, as the member was promised.
    @Test
    fun `a network switched off at home waits rather than asks`() {
        val wifiOnly = setOf(DownloadNetwork.WiFi)

        assertEquals(DownloadVerdict.Wait, downloadVerdict(NetworkKind.Mobile, wifiOnly))
        assertEquals(
            DownloadVerdict.Wait,
            downloadVerdict(NetworkKind.WiFi, setOf(DownloadNetwork.Mobile)),
        )
    }

    @Test
    fun `roaming switched on goes without asking`() {
        assertEquals(
            DownloadVerdict.Go,
            downloadVerdict(
                NetworkKind.Roaming,
                DEFAULT_DOWNLOAD_NETWORKS + DownloadNetwork.Roaming,
            ),
        )
    }

    @Test
    fun `not now holds for a day`() {
        val declined = Instant.parse("2026-10-02T09:00:00Z")

        assertTrue(mayAskAgain(null, declined))
        assertFalse(mayAskAgain(declined, declined.plus(Duration.ofHours(23))))
        assertTrue(mayAskAgain(declined, declined.plus(Duration.ofDays(1))))
    }

    // The member pressed "Update", so a network they allowed just goes; one
    // they switched off, at home or in roaming, is asked about rather than
    // waited on, and with no network there is nothing to ask.
    @Test
    fun `an update asks only over a network switched off`() {
        val wifiOnly = setOf(DownloadNetwork.WiFi)

        assertFalse(asksBeforeUpdate(NetworkKind.WiFi, DEFAULT_DOWNLOAD_NETWORKS))
        assertFalse(asksBeforeUpdate(NetworkKind.Mobile, DEFAULT_DOWNLOAD_NETWORKS))
        assertTrue(asksBeforeUpdate(NetworkKind.Roaming, DEFAULT_DOWNLOAD_NETWORKS))
        assertTrue(asksBeforeUpdate(NetworkKind.Mobile, wifiOnly))
        assertTrue(asksBeforeUpdate(NetworkKind.WiFi, setOf(DownloadNetwork.Mobile)))
        assertFalse(
            asksBeforeUpdate(
                NetworkKind.Roaming,
                DEFAULT_DOWNLOAD_NETWORKS + DownloadNetwork.Roaming,
            ),
        )
        assertFalse(asksBeforeUpdate(NetworkKind.None, emptySet()))
    }
}
