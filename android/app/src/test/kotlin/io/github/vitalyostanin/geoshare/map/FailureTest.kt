package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/** A database saying no, a database out of reach, and a fault of our own, told apart. */
class FailureTest {
    @Test
    fun `the rules saying no is a refusal`() {
        assertEquals(
            Failure.REFUSED,
            failureOf(FirebaseFirestoreException("denied", Code.PERMISSION_DENIED)),
        )
    }

    @Test
    fun `a server out of reach or a wait that ran out is unreachable`() {
        assertEquals(
            Failure.UNREACHABLE,
            failureOf(FirebaseFirestoreException("down", Code.UNAVAILABLE)),
        )
        assertEquals(Failure.UNREACHABLE, failureOf(UnansweredException()))
        assertEquals(Failure.UNREACHABLE, failureOf(IOException("reset")))
    }

    @Test
    fun `a fault of the application's own is neither`() {
        assertEquals(Failure.UNEXPECTED, failureOf(IllegalStateException("no state")))
        assertEquals(Failure.UNEXPECTED, failureOf(NullPointerException()))
    }
}
