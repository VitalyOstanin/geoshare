package io.github.vitalyostanin.geoshare.auth

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/** What the screen is told of the session as Firebase answers it. */
class SignInControllerTest {
    private val heard = slot<FirebaseAuth.AuthStateListener>()
    private val auth =
        mockk<FirebaseAuth>(relaxed = true) {
            every { addAuthStateListener(capture(heard)) } returns Unit
        }

    @Test
    fun `nobody signed in is a screen asking to sign in`() {
        val controller = started()

        answer(user = null)

        assertEquals(AuthState.SignedOut(), controller.state.value)
    }

    @Test
    fun `a session is told with who it belongs to`() {
        val mark = Mark(noted = true)
        val controller = started(mark)
        val picture = mockk<Uri> {
            every { this@mockk.toString() } returns
                "https://example.org/a.png"
        }

        answer(
            mockk {
                every { uid } returns "member-1"
                every { displayName } returns "Anna"
                every { photoUrl } returns picture
                every { email } returns "anna@example.org"
            },
        )

        assertEquals(
            AuthState.SignedIn("member-1", "Anna", "https://example.org/a.png", "anna@example.org"),
            controller.state.value,
        )
        assertEquals(false, mark.noted)
    }

    @Test
    fun `an earlier screen's sign-in that never ended is told as left, once`() {
        val mark = Mark(noted = true)
        val controller = started(mark)

        answer(user = null)

        assertEquals(AuthState.SignedOut(SignInFailure.Cancelled), controller.state.value)
        assertEquals(false, mark.noted)

        answer(user = null)

        assertEquals(AuthState.SignedOut(), controller.state.value)
    }

    @Test
    fun `a return to the screen does not wipe out a sign-in still under way`() = runBlocking {
        val controller = started()
        val unanswered = mockk<Task<AuthResult>>(relaxed = true) {
            every { isComplete } returns
                false
        }
        every { auth.signInWithEmailAndPassword(any(), any()) } returns unanswered

        val signingIn =
            launch(start = CoroutineStart.UNDISPATCHED) {
                controller.signInAsStandIn("anna@example.org")
            }
        answer(user = null)

        assertEquals(AuthState.SigningIn, controller.state.value)
        signingIn.cancel()
    }

    @Test
    fun `a failed stand-in sign-in says why`() = runBlocking {
        val controller = started()
        val failed =
            mockk<Task<AuthResult>> {
                every { isComplete } returns true
                every { isCanceled } returns false
                every { exception } returns IOException("no network")
            }
        every { auth.signInWithEmailAndPassword(any(), any()) } returns failed

        controller.signInAsStandIn("anna@example.org")

        assertEquals(AuthState.SignedOut(SignInFailure.Offline), controller.state.value)
    }

    @Test
    fun `stopping lets go of the listener it started with`() {
        val controller = started()

        controller.stop()

        verify { auth.removeAuthStateListener(heard.captured) }
    }

    private fun started(mark: SignInMark = NoSignInMark): SignInController =
        SignInController(auth, mark).also { it.start() }

    private fun answer(user: FirebaseUser?) {
        every { auth.currentUser } returns user
        heard.captured.onAuthStateChanged(auth)
    }

    /** A mark kept in memory, so a test can start with one and see it forgotten. */
    private class Mark(var noted: Boolean) : SignInMark {
        override fun has() = noted

        override fun note() {
            noted = true
        }

        override fun forget() {
            noted = false
        }
    }
}
