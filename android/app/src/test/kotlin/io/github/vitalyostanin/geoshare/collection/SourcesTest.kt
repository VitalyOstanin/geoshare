package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesTest {
    @Test
    fun `a device that was asked for nothing in particular counts everything`() {
        FoundBy.entries.forEach { source ->
            assertTrue(source.name, usesWhatWasFoundBy(source, emptySet()))
        }
    }

    @Test
    fun `a switch turned off stops the answers it covers from counting`() {
        val leftOut = setOf(SourceChoice.Unheard.name)

        assertFalse(usesWhatWasFoundBy(FoundBy.Unheard, leftOut))
    }

    // One switch for both, because both are a place worked out from the towers
    // and the Wi-Fi: what separates them is who did the arithmetic and whose
    // name the answer came under, and neither is a reason to keep one.
    @Test
    fun `the towers switch covers the assistance answers as well`() {
        val leftOut = setOf(SourceChoice.OtherRadios.name)

        assertFalse(usesWhatWasFoundBy(FoundBy.OtherRadios, leftOut))
        assertFalse(usesWhatWasFoundBy(FoundBy.Assistance, leftOut))
    }

    @Test
    fun `turning one switch off leaves the others counting`() {
        val leftOut = setOf(SourceChoice.OtherRadios.name)

        assertTrue(usesWhatWasFoundBy(FoundBy.Unheard, leftOut))
        assertTrue(usesWhatWasFoundBy(FoundBy.Pretended, leftOut))
        assertTrue(usesWhatWasFoundBy(FoundBy.Unsaid, leftOut))
    }

    // The setting a neighbourhood with a transmitter in it actually calls
    // for: the satellite answer is the one being lied to, and the towers go
    // on saying roughly the right district.
    @Test
    fun `the satellites can be switched off like anything else`() {
        val leftOut = setOf(SourceChoice.Satellites.name)

        assertFalse(usesWhatWasFoundBy(FoundBy.Satellites, leftOut))
        assertTrue(usesWhatWasFoundBy(FoundBy.OtherRadios, leftOut))
    }

    @Test
    fun `every source falls under exactly one switch`() {
        FoundBy.entries.forEach { source ->
            assertEquals(
                source.name,
                1,
                SourceChoice.entries.count { source in it.covers },
            )
        }
    }

    // Everything off is a device with nothing to place itself by, and it is
    // allowed: the button says how many are left, so it cannot be done by
    // accident and forgotten.
    @Test
    fun `every switch off leaves nothing counting`() {
        val leftOut = SourceChoice.entries.map { it.name }.toSet()

        FoundBy.entries.forEach { source ->
            assertFalse(source.name, usesWhatWasFoundBy(source, leftOut))
        }
    }

    // A name from another build matches nothing, and a position whose source
    // nobody switched off counts rather than being quietly dropped.
    @Test
    fun `a name nothing answers to leaves every source counting`() {
        FoundBy.entries.forEach { source ->
            assertTrue(source.name, usesWhatWasFoundBy(source, setOf("Whatever")))
        }
    }
}
