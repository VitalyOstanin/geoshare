package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CircleLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

/**
 * The false places and the circling setting are typed by hand in the console,
 * past every rule; a field of the wrong type costs that field, never the list.
 */
class ConsoleFieldsTest {
    private val place =
        mapOf<String, Any?>(
            FalsePlaceField.LATITUDE to 59.437,
            FalsePlaceField.LONGITUDE to 24.7536,
            FalsePlaceField.RADIUS_METRES to 4_000L,
            FalsePlaceField.NAMED to "the harbour",
        )

    @Test
    fun `a place typed right is read, a whole number of metres included`() {
        val read = placeOf("p1", place)

        assertEquals(59.437, read?.latitude)
        assertEquals(4_000.0, read?.radiusMetres)
        assertEquals("the harbour", read?.named)
    }

    @Test
    fun `a latitude typed as a string leaves the place out instead of throwing`() {
        assertNull(placeOf("p1", place + (FalsePlaceField.LATITUDE to "59.437")))
    }

    @Test
    fun `a name that is not text gives way to the document id`() {
        assertEquals("p1", placeOf("p1", place + (FalsePlaceField.NAMED to 7L))?.named)
    }

    @Test
    fun `a place out of range is left out`() {
        assertNull(placeOf("p1", place + (FalsePlaceField.RADIUS_METRES to 90_000.0)))
    }

    @Test
    fun `a circling field typed as a string keeps the build's value`() {
        val read =
            limitsOf(
                mapOf(
                    CirclingField.ANSWERS to "5",
                    CirclingField.WITHIN_SECONDS to 90L,
                ),
            )

        assertEquals(CircleLimits().answers, read.answers)
        assertEquals(Duration.ofSeconds(90), read.within)
    }

    @Test
    fun `no fields at all are the build's limits`() {
        assertEquals(CircleLimits(), limitsOf(emptyMap()))
    }
}
