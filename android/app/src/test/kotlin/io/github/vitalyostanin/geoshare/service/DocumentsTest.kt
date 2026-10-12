package io.github.vitalyostanin.geoshare.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.FoundBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Date

class DocumentsTest {
    private val takenAt = Instant.parse("2026-10-12T19:30:00Z")
    private val fix =
        Fix(latitude = 59.93, longitude = 30.31, takenAt = takenAt, accuracyMetres = 12.0)

    private fun position(fix: Fix = this.fix, settling: Boolean = false) = positionDocument(
        memberId = "member",
        fix = fix,
        mode = CollectionMode.Viewing,
        batteryPercent = 15,
        charging = true,
        foundBy = FoundBy.Satellites,
        settling = settling,
    )

    @Test
    fun `a position carries what the readers draw it by`() {
        val document = position()

        assertEquals("member", document[PositionField.MEMBER_ID])
        assertEquals(59.93, document[PositionField.LATITUDE])
        assertEquals(30.31, document[PositionField.LONGITUDE])
        assertEquals(Timestamp(Date.from(takenAt)), document[PositionField.TAKEN_AT])
        assertTrue(document[PositionField.WRITTEN_AT] is FieldValue)
        assertEquals("Viewing", document[PositionField.MODE])
        assertEquals(15, document[PositionField.BATTERY_PERCENT])
        assertEquals(true, document[PositionField.CHARGING])
        assertEquals("Satellites", document[PositionField.FOUND_BY])
        assertEquals(12.0, document[PositionField.ACCURACY_METRES])
    }

    @Test
    fun `a width the provider did not give is left out, not written as null`() {
        val document = position(fix.copy(accuracyMetres = null))

        assertTrue(PositionField.ACCURACY_METRES !in document)
    }

    @Test
    fun `settling is written only while the receiver has not settled`() {
        assertTrue(PositionField.SETTLING !in position(settling = false))
        assertEquals(true, position(settling = true)[PositionField.SETTLING])
    }

    @Test
    fun `a device's entry says how it is, and nothing about where`() {
        val entry =
            phoneEntry(
                model = "BVL-N49",
                mode = CollectionMode.Night,
                batteryPercent = 80,
                charging = false,
            )

        assertEquals(
            setOf(
                PhoneField.MODEL,
                PhoneField.ONLINE_AT,
                PhoneField.MODE,
                PhoneField.BATTERY_PERCENT,
                PhoneField.CHARGING,
            ),
            entry.keys,
        )
        assertEquals("BVL-N49", entry[PhoneField.MODEL])
        assertTrue(entry[PhoneField.ONLINE_AT] is FieldValue)
        assertEquals("Night", entry[PhoneField.MODE])
        assertEquals(80, entry[PhoneField.BATTERY_PERCENT])
        assertEquals(false, entry[PhoneField.CHARGING])
    }

    @Test
    fun `a move in gain is told in signed decibels, or as nothing heard`() {
        assertEquals("+2.5 dB", moved(Epochs(recent = 30.0, earlier = 27.5)))
        assertEquals("-3.0 dB", moved(Epochs(recent = 25.0, earlier = 28.0)))
        assertEquals("not heard", moved(Epochs(recent = null, earlier = 28.0)))
    }
}
