package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderNoteTest {
    // The note a fix carried while the device was being handed a false road:
    // the receiver was hearing satellites, loudly, and the position it worked
    // out of them was a kilometre-wide circle in a forest.
    private val spoofed = "HDACC=0.0|SourceType=0|maxCn0=27|meanCn0=24|satellites=8"

    // The same device, minutes earlier, answering with the place it was really
    // at: an assistance service said so, and nothing was heard at the time.
    private val assisted = "QUICKGPS=true|SourceType=128|maxCn0=0|meanCn0=0|satellites=4"

    @Test
    fun `the fields of a note are read out of it`() {
        val note = noteOf(spoofed)

        assertEquals(0, note.sourceType)
        assertEquals(8, note.satellites)
        assertEquals(27.0, note.maxLoudness!!, 0.001)
        assertEquals(24.0, note.meanLoudness!!, 0.001)
        assertNull(note.quickGps)
    }

    @Test
    fun `satellites being heard is the answer resting on them`() {
        assertEquals(Answered.Satellites, answeredBy(noteOf(spoofed)))
        assertTrue(restsOnSatellites(spoofed))
    }

    // The point of the whole file: this arrived under the name `gps` like the
    // one above, and is worth nothing like it.
    @Test
    fun `an assistance answer is not a satellite one however it is named`() {
        assertEquals(Answered.Assistance, answeredBy(noteOf(assisted)))
        assertFalse(restsOnSatellites(assisted))
    }

    @Test
    fun `the provider saying so outright settles it`() {
        assertEquals(Answered.Assistance, answeredBy(noteOf("QUICKGPS=true|maxCn0=31")))
    }

    @Test
    fun `satellites counted and none of them heard is not a measurement`() {
        assertEquals(Answered.Unheard, answeredBy(noteOf("SourceType=0|maxCn0=0|satellites=6")))
        assertFalse(restsOnSatellites("SourceType=0|maxCn0=0|satellites=6"))
    }

    // A label with no measurement behind it is a label. The case is real: the
    // device that wrote the assistance note above wrote `SourceType=0` on it.
    @Test
    fun `a note without a loudness says nothing either way`() {
        assertEquals(Answered.Unsaid, answeredBy(noteOf("SourceType=0|satellites=6")))
    }

    @Test
    fun `a phone that attached nothing is not accused of anything`() {
        assertEquals(Answered.Unsaid, answeredBy(noteOf(null)))
        assertEquals(Answered.Unsaid, answeredBy(noteOf("")))
        assertFalse(restsOnSatellites(null))
    }

    @Test
    fun `what a provider is not asked is how it spells its keys`() {
        assertEquals(Answered.Assistance, answeredBy(noteOf("quickgps=TRUE|maxCn0=31")))
        assertEquals(Answered.Satellites, answeredBy(noteOf("MAXCN0=31|sourcetype=0")))
    }

    @Test
    fun `whatever else is in the note is stepped over`() {
        val note = noteOf("someFlag|=orphan|maxCn0=27|SourceType=not a number")

        assertNull(note.sourceType)
        assertEquals(27.0, note.maxLoudness!!, 0.001)
        assertEquals(Answered.Satellites, answeredBy(note))
    }

    @Test
    fun `a value with its own separators in it survives the split`() {
        val note = noteOf("maxCn0=27|note=a=b=c")

        assertEquals(27.0, note.maxLoudness!!, 0.001)
    }
}
