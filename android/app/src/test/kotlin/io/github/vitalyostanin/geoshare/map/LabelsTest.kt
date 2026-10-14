package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

/** The names a member gives the devices and the people, and the order the names are taken in. */
class LabelsTest {
    private val position =
        MemberPosition(
            positionId = "her.phone",
            memberId = "her",
            deviceName = null,
            latitude = 60.9,
            longitude = 4.3,
            takenAt = Instant.ofEpochSecond(1_000),
            writtenAt = Instant.ofEpochSecond(1_000),
            mode = null,
            batteryPercent = null,
        )

    private fun grantsNaming(name: String?, model: String? = null) = combinedGrants(
        named = emptyMap(),
        phones = mapOf("her" to mapOf("phone" to PhoneStatus(name = name, model = model))),
    )

    @Test
    fun `the viewer's own name for a device comes first`() {
        val labels = Labels(phones = mapOf("her.phone" to "new phone"))

        val grants = grantsNaming("mum's phone")
        val shown = labelledPositions(listOf(position), labels, grants).single()

        assertEquals("new phone", shown.deviceName)
    }

    @Test
    fun `the owner's name for a device comes next`() {
        val grants = grantsNaming("mum's phone")
        val shown = labelledPositions(listOf(position), Labels(), grants).single()

        assertEquals("mum's phone", shown.deviceName)
    }

    @Test
    fun `the device's own model is shown where nobody named it`() {
        val grants = grantsNaming(null, model = "MAR-LX1")
        val shown = labelledPositions(listOf(position), Labels(), grants).single()

        assertEquals("MAR-LX1", shown.deviceName)
    }

    @Test
    fun `a device that has said nothing about itself is left unnamed`() {
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

    // Two devices of one model that nobody named read the same everywhere; a
    // number after the name tells them apart, in the order of their ids so
    // that every screen numbers them alike.
    @Test
    fun `devices sharing a name are numbered in the order of their ids`() {
        val phones =
            mapOf(
                "zulu" to PhoneStatus(model = "BVL-N49"),
                "alpha" to PhoneStatus(model = "BVL-N49"),
                "other" to PhoneStatus(model = "MAR-LX1"),
            )

        val names = phoneNames("her", phones, Labels())

        assertEquals("BVL-N49 (1)", names["alpha"])
        assertEquals("BVL-N49 (2)", names["zulu"])
        assertEquals("MAR-LX1", names["other"])
    }

    @Test
    fun `twins skip a number another device is already called by`() {
        val phones =
            mapOf(
                "alpha" to PhoneStatus(model = "BVL-N49"),
                "zulu" to PhoneStatus(model = "BVL-N49"),
                "third" to PhoneStatus(model = "MAR-LX1"),
            )
        val labels = Labels(phones = mapOf("her.third" to "BVL-N49 (1)"))

        val names = phoneNames("her", phones, labels)

        assertEquals("BVL-N49 (1)", names["third"])
        assertEquals("BVL-N49 (2)", names["alpha"])
        assertEquals("BVL-N49 (3)", names["zulu"])
    }

    @Test
    fun `a device named apart from its twin leaves the other unnumbered`() {
        val phones =
            mapOf(
                "alpha" to PhoneStatus(model = "BVL-N49"),
                "zulu" to PhoneStatus(model = "BVL-N49"),
            )
        val labels = Labels(phones = mapOf("her.zulu" to "old phone"))

        val names = phoneNames("her", phones, labels)

        assertEquals("BVL-N49", names["alpha"])
        assertEquals("old phone", names["zulu"])
    }

    @Test
    fun `the map numbers twins the way the panel does`() {
        val grants =
            combinedGrants(
                named = emptyMap(),
                phones =
                mapOf(
                    "her" to
                        mapOf(
                            "alpha" to PhoneStatus(model = "BVL-N49"),
                            "zulu" to PhoneStatus(model = "BVL-N49"),
                        ),
                ),
            )
        val twins = listOf("zulu", "alpha").map { deviceId ->
            position.copy(positionId = "her.$deviceId")
        }

        val shown = labelledPositions(twins, Labels(), grants)

        assertEquals(listOf("BVL-N49 (2)", "BVL-N49 (1)"), shown.map { it.deviceName })
    }

    @Test
    fun `devices of two members are numbered apart`() {
        val grants =
            combinedGrants(
                named = emptyMap(),
                phones =
                mapOf(
                    "her" to mapOf("alpha" to PhoneStatus(model = "BVL-N49")),
                    "him" to mapOf("beta" to PhoneStatus(model = "BVL-N49")),
                ),
            )
        val positions =
            listOf(
                position.copy(positionId = "her.alpha"),
                position.copy(positionId = "him.beta", memberId = "him"),
            )

        val shown = labelledPositions(positions, Labels(), grants)

        assertEquals(listOf("BVL-N49", "BVL-N49"), shown.map { it.deviceName })
    }
}
