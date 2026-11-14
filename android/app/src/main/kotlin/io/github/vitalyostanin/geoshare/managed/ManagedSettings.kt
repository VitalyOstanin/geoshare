package io.github.vitalyostanin.geoshare.managed

import android.os.Bundle

/**
 * What the device policy controller (a device owner or MDM app) put into the managed
 * configuration. Every field says "not set" in its own way, so that an unmanaged device and a
 * managed one that leaves a setting alone look the same and the member keeps their own choice.
 *
 * The trusted applications and their digests come only from here; nothing is built into the
 * program, so a build is not tied to any one controlling app.
 */
data class ManagedSettings(
    val trustedControllers: List<TrustedController> = emptyList(),
    val connectLink: String? = null,
    val expectedAccount: String? = null,
    /** Null when the controller says nothing, so the member's own switch stays in charge. */
    val sharingEnabled: Boolean? = null,
    val lockSharing: Boolean = false,
    val lockGroup: Boolean = false,
    val lockAccount: Boolean = false,
    /** How often, in minutes, the device collects and writes its position; null when not set. */
    val positionIntervalMinutes: Int? = null,
) {
    companion object {
        /** Nothing is managed, so the application behaves as if no controller were there. */
        val NONE = ManagedSettings()

        /** What the controller may ask for; a value outside it reads as not set. */
        private val INTERVAL_MINUTES = 1..20

        private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

        /**
         * The settings in [values], keyed as in `res/xml/app_restrictions.xml`. Plain maps and
         * lists stand for bundles and arrays of them, so a JVM test needs no Bundle. Blank text
         * is treated as not set, an entry with an unusable package name or digest is dropped,
         * and the digests come out normalised.
         */
        fun parse(values: Map<String, Any?>): ManagedSettings = ManagedSettings(
            trustedControllers = (values["trusted_controllers"] as? List<*>).orEmpty()
                .mapNotNull(::controller)
                .distinct(),
            connectLink = text(values["connect_link"]),
            expectedAccount = text(values["expected_account"]),
            sharingEnabled = values["sharing_enabled"] as? Boolean,
            lockSharing = values["lock_sharing"] as? Boolean ?: false,
            lockGroup = values["lock_group"] as? Boolean ?: false,
            lockAccount = values["lock_account"] as? Boolean ?: false,
            positionIntervalMinutes = (values["position_interval_minutes"] as? Int)
                ?.takeIf { it in INTERVAL_MINUTES },
        )

        /** [parse] for what RestrictionsManager hands over. */
        fun fromBundle(bundle: Bundle): ManagedSettings = parse(flatten(bundle))

        private fun text(value: Any?): String? =
            (value as? String)?.trim()?.takeIf { it.isNotEmpty() }

        private fun controller(entry: Any?): TrustedController? {
            val map = entry as? Map<*, *> ?: return null
            val name = text(map["package_name"])?.takeIf { PACKAGE_NAME.matches(it) } ?: return null
            val digest = text(map["cert_sha256"])?.let(::normaliseCertSha256) ?: return null
            return TrustedController(name, digest)
        }

        /** A bundle as plain values; nested bundles and arrays of them become maps and lists. */
        @Suppress("DEPRECATION")
        private fun flatten(bundle: Bundle): Map<String, Any?> =
            bundle.keySet().associateWith { key -> plain(bundle.get(key)) }

        private fun plain(value: Any?): Any? = when (value) {
            is Bundle -> flatten(value)
            is Array<*> -> value.map(::plain)
            is List<*> -> value.map(::plain)
            else -> value
        }
    }
}
