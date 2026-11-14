package io.github.vitalyostanin.geoshare.managed

/**
 * One application the device policy controller (a device owner or MDM app) lets steer geoshare:
 * its package and the SHA-256 of the certificate it is signed with.
 */
data class TrustedController(val packageName: String, val certSha256: String)

/**
 * [hex] as 64 lower-case hex digits, or null when it is not a SHA-256 digest. Colons and spaces
 * are stripped, since certificate fingerprints are usually written as `AB:CD:...`.
 */
fun normaliseCertSha256(hex: String): String? {
    val digits = hex.filterNot { it == ':' || it.isWhitespace() }.lowercase()
    return digits.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
}

/**
 * Whether [list] holds [packageName] with exactly the certificate [certSha256Hex]. A package name
 * alone proves nothing, since anyone can install an application under any name still free, so the
 * digest has to match as well. A malformed digest never matches.
 */
fun trusts(list: List<TrustedController>, packageName: String, certSha256Hex: String): Boolean {
    val digest = normaliseCertSha256(certSha256Hex) ?: return false
    return list.any {
        it.packageName == packageName && normaliseCertSha256(it.certSha256) == digest
    }
}
