package io.github.vitalyostanin.geoshare.map

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CoordinatesTextTest {
    private val saved: Locale = Locale.getDefault()

    @Before
    fun russian() = Locale.setDefault(Locale.forLanguageTag("ru"))

    @After
    fun restore() = Locale.setDefault(saved)

    @Test
    fun `coordinates keep the decimal point under a Russian locale`() {
        assertEquals("55.75580, 37.61730", coordinatesText(55.7558, 37.6173))
    }
}
