package io.github.vitalyostanin.geoshare.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

private val BODY = "synthetic apk body".toByteArray()

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

private fun asset(size: Long = BODY.size.toLong(), sha256: String = sha256(BODY)) =
    ApkAsset("https://github.com/x/geoshare.apk", size, sha256)

class ApkVerifierTest {
    @Test
    fun `a body of the announced size and digest is intact`() {
        val verifier = ApkVerifier(asset())

        verifier.update(BODY, BODY.size)

        assertEquals(ApkVerdict.Intact, verifier.verdict())
    }

    @Test
    fun `chunks fed one after another give the same digest as the whole body`() {
        val verifier = ApkVerifier(asset())

        verifier.update(BODY, 5)
        verifier.update(BODY.copyOfRange(5, BODY.size), BODY.size - 5)

        assertEquals(BODY.size.toLong(), verifier.received)
        assertEquals(ApkVerdict.Intact, verifier.verdict())
    }

    @Test
    fun `only the counted part of a buffer is read`() {
        val verifier = ApkVerifier(asset())
        val buffer = BODY + ByteArray(10) { 7 }

        verifier.update(buffer, BODY.size)

        assertEquals(ApkVerdict.Intact, verifier.verdict())
    }

    @Test
    fun `a short body is the wrong size`() {
        val verifier = ApkVerifier(asset())

        verifier.update(BODY, BODY.size - 1)

        assertEquals(ApkVerdict.WrongSize, verifier.verdict())
    }

    @Test
    fun `a body of the right size and another digest is wrong`() {
        val verifier = ApkVerifier(asset())
        val altered = BODY.copyOf().also { it[0] = (it[0] + 1).toByte() }

        verifier.update(altered, altered.size)

        assertEquals(ApkVerdict.WrongDigest, verifier.verdict())
    }

    @Test
    fun `the digest is compared without regard to letter case`() {
        val verifier = ApkVerifier(asset(sha256 = sha256(BODY).uppercase()))

        verifier.update(BODY, BODY.size)

        assertEquals(ApkVerdict.Intact, verifier.verdict())
    }

    @Test
    fun `a body past the announced size is known to be too long at once`() {
        val verifier = ApkVerifier(asset())

        verifier.update(BODY, BODY.size)
        assertFalse(verifier.isOverSize)

        verifier.update(BODY, 1)
        assertTrue(verifier.isOverSize)
        assertEquals(ApkVerdict.WrongSize, verifier.verdict())
    }
}

class UpdateFileNameTest {
    @Test
    fun `the name carries the version`() {
        assertEquals("geoshare-0.1.2.apk", updateFileName("0.1.2"))
    }

    @Test
    fun `nothing but letters digits and dots survives into the name`() {
        assertEquals("geoshare-1.2.3-_.._x.apk", updateFileName("1.2.3-/../x"))
    }
}
