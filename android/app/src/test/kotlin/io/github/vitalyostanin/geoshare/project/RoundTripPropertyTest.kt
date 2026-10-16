package io.github.vitalyostanin.geoshare.project

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * What each kept document reads back as, for values far from the handful the
 * other tests use. A fixed seed keeps a failure the same on every run.
 */
class RoundTripPropertyTest {
    @Test
    fun `any settings read back as they were written`() {
        val random = Random(582L)

        repeat(500) { case ->
            val settings = randomSettings(random)

            assertEquals(
                "case $case: $settings",
                settings,
                ProjectSettings.parse(settings.toJson()),
            )
        }
    }

    // A blank group or code is no invitation, and a blank address or inviter is
    // none given, so those read back as null; every other value reads back as is.
    @Test
    fun `any pending invitation reads back as parse would keep it`() {
        val random = Random(583L)

        repeat(500) { case ->
            val invitation = PendingInvitation(
                groupId = randomNonBlank(random),
                code = randomNonBlank(random),
                invitedAddress = if (random.nextInt(4) == 0) null else randomAwkward(random),
                inviter = if (random.nextInt(4) == 0) null else randomAwkward(random),
                refused = random.nextBoolean(),
            )

            assertEquals(
                "case $case: $invitation",
                invitation.copy(
                    invitedAddress = invitation.invitedAddress?.ifBlank { null },
                    inviter = invitation.inviter?.ifBlank { null },
                ),
                PendingInvitation.parse(invitation.toJson()),
            )
        }
    }

    @Test
    fun `a pending invitation with a blank group or code is not read`() {
        val random = Random(584L)

        repeat(200) { case ->
            val blank = " ".repeat(random.nextInt(4))
            val invitation = if (random.nextBoolean()) {
                PendingInvitation(groupId = blank, code = randomNonBlank(random))
            } else {
                PendingInvitation(groupId = randomNonBlank(random), code = blank)
            }

            assertEquals(
                "case $case: $invitation",
                null,
                PendingInvitation.parse(invitation.toJson()),
            )
        }
    }

    // parse settles the selection: one that names no listed group moves to the
    // first group, and with no group there is none.
    @Test
    fun `any list of groups reads back as parse would keep it`() {
        val random = Random(585L)

        repeat(400) { case ->
            val connections = List(random.nextInt(5)) {
                Connected(randomSettings(random), group = randomNonBlank(random))
            }
            val ids = connections.map { it.settings.projectId }
            val selected = when (random.nextInt(4)) {
                0 -> null
                1 -> randomAwkward(random)
                else -> if (ids.isEmpty()) null else ids[random.nextInt(ids.size)]
            }
            val list = ConnectionList(connections, selected)

            assertEquals(
                "case $case: $list",
                ConnectionList(
                    connections,
                    if (selected != null && selected in ids) selected else ids.firstOrNull(),
                ),
                ConnectionList.parse(list.toJson()),
            )
        }
    }

    // ProjectSettings.parse reads what a site serves, so it must answer with a
    // value or null whatever the site served.
    @Test
    fun `settings parse answers any text with settings or null`() {
        val random = Random(586L)
        val valid = randomSettings(random).toJson()

        repeat(600) { case ->
            val text = when (case % 6) {
                0 -> randomAwkward(random)

                1 -> valid.substring(0, random.nextInt(valid.length + 1))

                2 -> mutated(valid, random)

                3 -> wrongTyped(random).toString()

                4 -> nested(random).toString()

                else -> JSONObject(
                    valid,
                ).put(randomAwkward(random), randomAwkward(random)).toString()
            }
            val parsed = try {
                ProjectSettings.parse(text)
            } catch (thrown: Exception) {
                throw AssertionError("case $case threw on: $text", thrown)
            }

            if (parsed != null) {
                assertTrue(
                    "case $case: $text",
                    parsed.version == 1 && parsed.projectId.isNotBlank(),
                )
            }
        }
    }

    private fun mutated(valid: String, random: Random): String {
        val chars = StringBuilder(valid)

        repeat(1 + random.nextInt(4)) {
            val at = random.nextInt(chars.length)

            when (random.nextInt(3)) {
                0 -> chars.deleteCharAt(at)
                1 -> chars.insert(at, "{}[]\",:\\\u0000"[random.nextInt(9)])
                else -> chars.setCharAt(at, "{}[]\",:\\\u0000"[random.nextInt(9)])
            }
        }

        return chars.toString()
    }

    private fun wrongValue(random: Random): Any = when (random.nextInt(9)) {
        0 -> JSONObject.NULL
        1 -> random.nextBoolean()
        2 -> random.nextInt()
        3 -> Long.MAX_VALUE
        4 -> Double.MIN_VALUE
        5 -> JSONArray().put(randomAwkward(random))
        6 -> JSONObject().put("a", randomAwkward(random))
        7 -> "1"
        else -> randomAwkward(random)
    }

    // Every field present, each holding a value of whatever type.
    private fun wrongTyped(random: Random): JSONObject {
        val fields =
            listOf("version", "projectId", "applicationId", "apiKey", "gcmSenderId", "site")
        val document = JSONObject()

        fields.forEach { field ->
            if (random.nextInt(6) != 0) {
                document.put(
                    field,
                    if (field == "version" &&
                        random.nextBoolean()
                    ) {
                        1
                    } else {
                        wrongValue(random)
                    },
                )
            }
        }

        return document
    }

    private fun nested(random: Random): JSONObject {
        var document = wrongTyped(random)

        repeat(1 + random.nextInt(40)) {
            document = if (random.nextBoolean()) {
                JSONObject().put("settings", document)
            } else {
                JSONObject().put("version", 1).put("projectId", JSONArray().put(document))
            }
        }

        return document
    }
}
