package io.github.vitalyostanin.geoshare.update

import java.security.MessageDigest

/** What a finished download came to when held against the [ApkAsset] GitHub announced. */
enum class ApkVerdict {
    Intact,
    WrongSize,
    WrongDigest,
}

/**
 * Checks bytes against an [ApkAsset] as they arrive, so that the file is
 * never read a second time to hash it: [update] is fed every chunk the
 * download reads, and [verdict] is asked once the stream has ended. Pure,
 * so the rule is tested without a network or a file.
 */
class ApkVerifier(private val expected: ApkAsset) {
    private val digest = MessageDigest.getInstance("SHA-256")

    /** How many bytes have been fed so far. */
    var received: Long = 0
        private set

    /** Whether more has arrived than was announced, which no later byte can put right. */
    val isOverSize: Boolean get() = received > expected.size

    /** Takes the first [count] bytes of [buffer]. */
    fun update(buffer: ByteArray, count: Int) {
        digest.update(buffer, 0, count)
        received += count
    }

    /** The size is judged before the digest: a body of the wrong length cannot match anyway. */
    fun verdict(): ApkVerdict {
        if (received != expected.size) return ApkVerdict.WrongSize

        val actual = digest.digest().joinToString("") { "%02x".format(it) }

        return if (actual.equals(expected.sha256, ignoreCase = true)) {
            ApkVerdict.Intact
        } else {
            ApkVerdict.WrongDigest
        }
    }
}

/**
 * The name the downloaded APK is kept under. The version comes from an
 * answer that is not trusted, so anything that is not a letter, a digit, a dot or a hyphen
 * is replaced, and a tag cannot name a path outside the directory.
 */
fun updateFileName(version: String): String =
    "geoshare-" + version.replace(Regex("[^0-9A-Za-z.\\-]"), "_") + ".apk"
