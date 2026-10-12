package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SubscriptionsTest {
    @Test
    fun `being told this phone may not ask is a refusal`() {
        assertTrue(refusedOutright(refusal()))
    }

    // A device in a lift and a device taken off a roster both stop hearing
    // answers, and only one of them is left showing something untrue.
    @Test
    fun `a connection that dropped is not one`() {
        assertFalse(
            refusedOutright(
                FirebaseFirestoreException(
                    "the server could not be reached",
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                ),
            ),
        )
    }

    @Test
    fun `neither is anything the database did not say`() {
        assertFalse(refusedOutright(IOException("the socket is gone")))
    }

    // The point of the whole arrangement: a member taken off the roster sees
    // the group empty rather than the list they were on a minute ago. Heard
    // well inside the second this waits, because the subscription itself is
    // not taken out again for five.
    @Test
    fun `what was last heard is dropped once the phone is refused`() {
        assertEquals(
            listOf("the roster", "nothing"),
            heardWithinASecond(forgetting = { "nothing" }),
        )
    }

    // A caller with nothing on a screen says nothing to drop, and then the
    // refusal changes what is known not at all.
    @Test
    fun `a subscription with nothing to forget is left as it was`() {
        assertEquals(listOf("the roster"), heardWithinASecond(forgetting = null))
    }

    /**
     * What a subscription refused after its first answer says within a second.
     *
     * A second because the wait before it is taken out again is five: anything
     * heard inside the one is the refusal being answered rather than the
     * subscription coming back.
     */
    private fun heardWithinASecond(forgetting: (() -> String)?): List<String> = runBlocking {
        val heard = mutableListOf<String>()
        val told =
            flow {
                emit("the roster")

                throw refusal()
            }.comingBack("roster", forgetting = forgetting)

        withTimeoutOrNull(1_000) { told.collect { heard.add(it) } }

        heard
    }

    private fun refusal(): FirebaseFirestoreException = FirebaseFirestoreException(
        "this phone may not ask",
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
    )
}
