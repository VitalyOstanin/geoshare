package io.github.vitalyostanin.geoshare.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
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

    @After
    fun forgetTheStatics() {
        unmockkStatic(OAuthProvider::class, LocalBroadcastManager::class)
        unmockkConstructor(Intent::class)
    }

    // The browser can outlive the activity, and Firebase keeps the flow's
    // result for the one that comes back: a new flow would lose it.
    @Test
    fun `a result Firebase kept waiting is taken instead of a new page`() = runBlocking {
        val mark = Mark(noted = false)
        val controller = started(mark)
        browserFlow()
        every { auth.pendingAuthResult } returns answered()

        controller.signIn(mockk<Activity>())

        verify(exactly = 0) { auth.startActivityForSignInWithProvider(any(), any()) }
        assertEquals(AuthState.SigningIn, controller.state.value)
        assertEquals(true, mark.noted)
    }

    @Test
    fun `a page that fails says why and forgets the sign-in was started`() = runBlocking {
        val mark = Mark(noted = false)
        val controller = started(mark)
        browserFlow()
        every { auth.pendingAuthResult } returns null
        every { auth.startActivityForSignInWithProvider(any(), any()) } returns
            failed(IOException("no network"))

        controller.signIn(mockk<Activity>())

        assertEquals(AuthState.SignedOut(SignInFailure.Offline), controller.state.value)
        assertEquals(false, mark.noted)
    }

    // Coming back through the launcher leaves Firebase holding a flow open,
    // and every sign-in after it would fail as already in progress.
    @Test
    fun `a flow still held open is given up and the page opened again`() = runBlocking {
        val controller = started()
        browserFlow()
        val broadcasts = mockk<LocalBroadcastManager>(relaxed = true)
        mockkStatic(LocalBroadcastManager::class)
        every { LocalBroadcastManager.getInstance(any()) } returns broadcasts
        val left = mockk<Intent>()
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Boolean>()) } returns left
        val held =
            mockk<FirebaseAuthException> {
                every { errorCode } returns "ERROR_WEB_CONTEXT_ALREADY_PRESENTED"
            }
        every { auth.pendingAuthResult } returns null
        every { auth.startActivityForSignInWithProvider(any(), any()) } returnsMany
            listOf(failed(held), answered())

        controller.signIn(mockk<Activity>())

        verify {
            anyConstructed<Intent>().putExtra(
                "com.google.firebase.auth.internal.EXTRA_CANCELED",
                true,
            )
        }
        verify(exactly = 1) { broadcasts.sendBroadcastSync(left) }
        verify(exactly = 2) { auth.startActivityForSignInWithProvider(any(), any()) }
        assertEquals(AuthState.SigningIn, controller.state.value)
    }

    @Test
    fun `stopping lets go of the listener it started with`() {
        val controller = started()

        controller.stop()

        verify { auth.removeAuthStateListener(heard.captured) }
    }

    private fun started(mark: SignInMark = NoSignInMark): SignInController =
        SignInController(auth, mark).also { it.start() }

    /** A browser flow built on this group's own instance, asking which account to use. */
    private fun browserFlow() {
        val builder = mockk<OAuthProvider.Builder>()
        every { builder.addCustomParameter(any(), any()) } returns builder
        every { builder.build() } returns mockk()
        mockkStatic(OAuthProvider::class)
        every { OAuthProvider.newBuilder("google.com", auth) } returns builder
    }

    private fun answered(): Task<AuthResult> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { result } returns mockk()
    }

    private fun failed(error: Exception): Task<AuthResult> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns error
    }

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
