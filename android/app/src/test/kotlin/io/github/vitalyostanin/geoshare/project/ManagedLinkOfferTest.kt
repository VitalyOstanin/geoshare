package io.github.vitalyostanin.geoshare.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val LINK_TEXT = "https://geoshare-example-abc123.web.app/join"

// DEV-15: following the device policy controller of a managed device (ADR-0043).
class ManagedLinkOfferTest {
    @Test
    fun `a link to a group not connected is offered as written`() {
        assertEquals(LINK_TEXT, managedLinkToOffer(" $LINK_TEXT ", emptyList(), emptySet()))
    }

    @Test
    fun `a link to a connected site is not offered`() {
        assertNull(managedLinkToOffer(LINK_TEXT, listOf("geoshare-example-abc123"), emptySet()))
    }

    @Test
    fun `a link already offered is not offered again`() {
        assertNull(managedLinkToOffer(LINK_TEXT, emptyList(), setOf(LINK_TEXT)))
    }

    @Test
    fun `nothing configured or text that is no link offers nothing`() {
        assertNull(managedLinkToOffer(null, emptyList(), emptySet()))
        assertNull(managedLinkToOffer("  ", emptyList(), emptySet()))
        assertNull(managedLinkToOffer("https://example.com/join", emptyList(), emptySet()))
    }

    @Test
    fun `the process-wide offers hand a link out once`() {
        val link = "https://geoshare-example-once00.web.app/join"

        assertEquals(link, ManagedLinkOffers.next(link, emptyList()))
        assertNull(ManagedLinkOffers.next(link, emptyList()))
    }
}
