package io.github.vitalyostanin.geoshare.control

import io.github.vitalyostanin.geoshare.managed.TrustedController
import io.github.vitalyostanin.geoshare.service.StartRefusal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.vitalyostanin.geoshare.control.SharingControlProtocol as Protocol

/** The pure parts of the control conversation; the binding around them needs a device. */
// DEV-15: following the device policy controller of a managed device (ADR-0043).
class SharingControlProtocolTest {
    private val digest = "ab".repeat(32)
    private val other = "cd".repeat(32)
    private val list = listOf(TrustedController("org.example.control", digest))

    private val status = Protocol.Status(
        sharingWanted = true,
        groupConnected = true,
        signedIn = false,
        locationGranted = true,
        backgroundLocationGranted = false,
        notificationsGranted = true,
        serviceRunning = false,
    )

    private fun roundTrip(reply: Protocol.Reply) =
        Protocol.parseReply(Protocol.resultOf(reply), Protocol.fields(reply))

    @Test
    fun `the numbers another application is compiled against do not move`() {
        assertEquals(
            listOf(1, 2, 3, 100),
            listOf(
                Protocol.WHAT_START,
                Protocol.WHAT_STOP,
                Protocol.WHAT_STATUS,
                Protocol.WHAT_REPLY,
            ),
        )
        assertEquals(
            listOf(0, 1, 2),
            listOf(Protocol.RESULT_OK, Protocol.RESULT_REFUSED, Protocol.RESULT_ERROR),
        )
    }

    @Test
    fun `a request number maps to its request and an unknown one to nothing`() {
        assertEquals(Protocol.Request.Start, Protocol.requestOf(Protocol.WHAT_START))
        assertEquals(Protocol.Request.Stop, Protocol.requestOf(Protocol.WHAT_STOP))
        assertEquals(Protocol.Request.Status, Protocol.requestOf(Protocol.WHAT_STATUS))
        assertNull(Protocol.requestOf(0))
        assertNull(Protocol.requestOf(Protocol.WHAT_REPLY))
        assertNull(Protocol.requestOf(-7))
    }

    @Test
    fun `every kind of reply survives the round trip`() {
        val replies = listOf(
            Protocol.Reply.Ok,
            Protocol.Reply.State(status),
            Protocol.Reply.Refused(),
            Protocol.Reply.Error(Protocol.ERROR_START_REFUSED_BY_PLATFORM),
        ) + StartRefusal.entries.map { Protocol.Reply.Refused(it) }
        for (reply in replies) assertEquals(reply, roundTrip(reply))
    }

    @Test
    fun `a refusal of an untrusted caller carries no reason`() {
        assertTrue(Protocol.fields(Protocol.Reply.Refused()).isEmpty())
        assertEquals(Protocol.RESULT_REFUSED, Protocol.resultOf(Protocol.Reply.Refused()))
    }

    @Test
    fun `a refusal names the condition by its enum name`() {
        val fields = Protocol.fields(Protocol.Reply.Refused(StartRefusal.TurnedOff))
        assertEquals(mapOf(Protocol.KEY_REFUSAL to "TurnedOff"), fields)
    }

    @Test
    fun `a reply with an unknown number, a missing flag or an unknown refusal is not understood`() {
        assertNull(Protocol.parseReply(9, emptyMap()))
        assertNull(Protocol.parseReply(Protocol.RESULT_ERROR, emptyMap()))
        val partial = Protocol.fields(Protocol.Reply.State(status)) - Protocol.KEY_SIGNED_IN
        assertNull(Protocol.parseReply(Protocol.RESULT_OK, partial))
        assertEquals(
            Protocol.Reply.Refused(),
            Protocol.parseReply(Protocol.RESULT_REFUSED, mapOf(Protocol.KEY_REFUSAL to "Gone")),
        )
    }

    @Test
    fun `a caller with one matching signer is trusted`() {
        assertTrue(Protocol.callerTrusted(list, mapOf("org.example.control" to listOf(digest))))
    }

    @Test
    fun `a caller with several certificates is trusted when one of them is listed`() {
        val packages = mapOf("org.example.control" to listOf(other, digest.uppercase()))
        assertTrue(Protocol.callerTrusted(list, packages))
    }

    @Test
    fun `a caller whose package is listed but whose certificate is not is refused`() {
        assertFalse(Protocol.callerTrusted(list, mapOf("org.example.control" to listOf(other))))
    }

    @Test
    fun `a caller with the right certificate under another name is refused`() {
        assertFalse(Protocol.callerTrusted(list, mapOf("org.example.other" to listOf(digest))))
    }

    @Test
    fun `a caller with no packages, no certificates or a malformed one is refused`() {
        assertFalse(Protocol.callerTrusted(list, emptyMap()))
        assertFalse(Protocol.callerTrusted(list, mapOf("org.example.control" to emptyList())))
        assertFalse(
            Protocol.callerTrusted(
                list,
                mapOf("org.example.control" to listOf("ab", "zz".repeat(32))),
            ),
        )
    }

    @Test
    fun `nobody is trusted when the configuration lists no controller`() {
        assertFalse(
            Protocol.callerTrusted(emptyList(), mapOf("org.example.control" to listOf(digest))),
        )
    }

    @Test
    fun `any package of the caller's uid may vouch for it`() {
        val packages = mapOf(
            "org.example.stranger" to listOf(other),
            "org.example.control" to listOf(digest),
        )
        assertTrue(Protocol.callerTrusted(list, packages))
    }
}
