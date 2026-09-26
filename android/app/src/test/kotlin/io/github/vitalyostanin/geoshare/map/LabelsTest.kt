package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

/** The names a member gives the phones and the people, and the order the names are taken in. */
class LabelsTest {
    private val position =
        MemberPosition(
            positionId = "her.phone",
            memberId = "her",
            deviceName = "MAR-LX1",
            latitude = 60.9,
            longitude = 4.3,
            takenAt = Instant.ofEpochSecond(1_000),
            writtenAt = Instant.ofEpochSecond(1_000),
            mode = null,
            batteryPercent = null,
        )

    private fun grantsNaming(name: String?) = combinedGrants(
        named = emptyMap(),
        phones = mapOf("her" to mapOf("phone" to PhoneStatus(name = name))),
    )

    @Test
    fun `the viewer's own name for a phone comes first`() {
        val labels = Labels(phones = mapOf("her.phone" to "new phone"))

        val grants = grantsNaming("mum's phone")
        val shown = labelledPositions(listOf(position), labels, grants).single()

        assertEquals("new phone", shown.deviceName)
    }

    @Test
    fun `the owner's name for a phone comes next`() {
        val grants = grantsNaming("mum's phone")
        val shown = labelledPositions(listOf(position), Labels(), grants).single()

        assertEquals("mum's phone", shown.deviceName)
    }

    @Test
    fun `the model is left where nobody named the phone`() {
        val shown = labelledPositions(listOf(position), Labels(), grantsNaming(null)).single()

        assertSame(position, shown)
    }

    @Test
    fun `the viewer's name for a person replaces the one on the account`() {
        val profiles =
            mapOf(
                "her" to MemberProfile("her", "Maria", null),
                "him" to MemberProfile("him", "Ivan", null),
            )

        val shown = labelledProfiles(profiles, Labels(members = mapOf("her" to "grandma")))

        assertEquals("grandma", shown.getValue("her").displayName)
        assertEquals("Ivan", shown.getValue("him").displayName)
    }

    @Test
    fun `a document keeps what is a name and drops the rest`() {
        val labels =
            labelsFrom(
                phones = mapOf("her.phone" to "  new phone  ", "his.phone" to "", "odd" to 5L),
                members = "not a map",
            )

        assertEquals(mapOf("her.phone" to "new phone"), labels.phones)
        assertEquals(emptyMap<String, String>(), labels.members)
    }

    @Test
    fun `an empty name resets and a long one is cut`() {
        assertNull(labelText("   "))
        assertEquals(LABEL_LIMIT, labelText("x".repeat(LABEL_LIMIT + 10))?.length)
    }
}
