package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AskingTest {
    private val everything = setOf(FUSED_PROVIDER, GPS_PROVIDER, NETWORK_PROVIDER)
    private val noBlend = setOf(GPS_PROVIDER, NETWORK_PROVIDER)
    private val noSky = setOf(SourceChoice.Satellites.name)
    private val noRadios = setOf(SourceChoice.OtherRadios.name)

    @Test
    fun `play services are asked while every source is on`() {
        assertEquals(Asked.PlayServices, whomToAsk(Accuracy.High, true, emptySet(), everything))
        assertEquals(
            "a switch that is not a source changes nothing",
            Asked.PlayServices,
            whomToAsk(Accuracy.Balanced, true, setOf(SourceChoice.Pretended.name), everything),
        )
    }

    @Test
    fun `a mode that asks for nothing asks nobody`() {
        assertNull(whomToAsk(Accuracy.None, true, emptySet(), everything))
        assertNull(whomToAsk(Accuracy.None, false, emptySet(), everything))
    }

    // The towers switched off used to be asked all the same and thrown away
    // on arrival; now only the receiver is asked, even with Play Services.
    @Test
    fun `a source switched off is not asked at all`() {
        assertEquals(
            Asked.Platform(listOf(GPS_PROVIDER)),
            whomToAsk(Accuracy.Balanced, true, noRadios, everything),
        )
        assertEquals(
            Asked.Platform(listOf(NETWORK_PROVIDER)),
            whomToAsk(Accuracy.High, true, noSky, everything),
        )
        assertNull(whomToAsk(Accuracy.High, true, noSky + noRadios, everything))
    }

    @Test
    fun `without play services the platform's own blend comes first`() {
        assertEquals(
            Asked.Platform(listOf(FUSED_PROVIDER)),
            whomToAsk(Accuracy.Balanced, false, emptySet(), everything),
        )
    }

    @Test
    fun `without a blend the fast mode asks the receiver and the network`() {
        assertEquals(
            Asked.Platform(listOf(GPS_PROVIDER, NETWORK_PROVIDER)),
            whomToAsk(Accuracy.High, false, emptySet(), noBlend),
        )
    }

    @Test
    fun `without a blend the slow modes leave the receiver asleep`() {
        assertEquals(
            Asked.Platform(listOf(NETWORK_PROVIDER)),
            whomToAsk(Accuracy.Balanced, false, emptySet(), noBlend),
        )
        assertEquals(
            Asked.Platform(listOf(NETWORK_PROVIDER)),
            whomToAsk(Accuracy.LowPower, false, emptySet(), noBlend),
        )
    }

    // A tablet with Wi-Fi and no receiver, or a phone whose network provider
    // is missing: whatever is there is asked, and nothing that is not.
    @Test
    fun `only providers the phone has are asked`() {
        assertEquals(
            Asked.Platform(listOf(GPS_PROVIDER)),
            whomToAsk(Accuracy.Balanced, false, emptySet(), setOf(GPS_PROVIDER)),
        )
        assertEquals(
            Asked.Platform(listOf(NETWORK_PROVIDER)),
            whomToAsk(Accuracy.High, false, emptySet(), setOf(NETWORK_PROVIDER)),
        )
        assertNull(whomToAsk(Accuracy.High, false, noRadios, setOf(NETWORK_PROVIDER)))
        assertNull(whomToAsk(Accuracy.High, false, emptySet(), emptySet()))
    }
}
