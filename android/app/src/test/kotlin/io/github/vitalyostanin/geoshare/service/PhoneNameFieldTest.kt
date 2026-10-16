package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNameFieldTest {
    @Test
    fun `a device name cut inside an emoji does not end in half of it`() {
        val half = "a".repeat(99)

        assertEquals(
            mapOf(SearchField.PHONE_NAME to half),
            phoneNameField(half + "\uD83D\uDE00"),
        )
    }

    @Test
    fun `a blank device name is kept as nothing`() {
        assertEquals(emptyMap<String, Any>(), phoneNameField("  "))
    }
}
