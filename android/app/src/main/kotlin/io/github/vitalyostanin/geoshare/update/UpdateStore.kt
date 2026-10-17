package io.github.vitalyostanin.geoshare.update

import android.content.Context
import io.github.vitalyostanin.geoshare.service.readBoolean
import io.github.vitalyostanin.geoshare.service.readLong
import io.github.vitalyostanin.geoshare.service.readString
import java.time.Instant

private const val FILE = "update"
private const val KEY_LAST_CHECK = "last_check"
private const val KEY_AUTO = "auto_check"
private const val KEY_NOTIFIED = "notified_version"
private const val KEY_VERSION = "found_version"
private const val KEY_PAGE = "found_page"
private const val KEY_PUBLISHED = "found_published"
private const val KEY_APK_URL = "found_apk_url"
private const val KEY_APK_SIZE = "found_apk_size"
private const val KEY_APK_SHA256 = "found_apk_sha256"
private const val KEY_NOTES = "found_notes"

/** The value [KEY_PUBLISHED] holds when GitHub gave no readable date. */
private const val NO_DATE = Long.MIN_VALUE

/**
 * What the update check remembers between runs. An interface so that the
 * check's rules are tested without a device; [UpdateStore] is the one that
 * keeps it.
 */
interface UpdateRecord {
    /** When a check last got an answer from GitHub; null if none has. */
    var lastCheck: Instant?

    /**
     * Whether the app asks GitHub on its own (at the start of the screen and
     * daily from the service). On until the member turns it off; the button
     * in the settings checks whatever this says.
     */
    var autoCheck: Boolean

    /** The version the last notification named; null if none was shown. */
    var notifiedVersion: String?

    /**
     * The newer release the last check found, for the settings screen to
     * offer; null when the last check found nothing newer than the app.
     */
    var found: LatestRelease?
}

/**
 * [UpdateRecord] on SharedPreferences, kept beside the application like the
 * theme: what has been checked and announced belongs to the device.
 */
class UpdateStore(context: Context) : UpdateRecord {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var lastCheck: Instant?
        get() = store.readLong(KEY_LAST_CHECK, NO_DATE).takeIf { it != NO_DATE }
            ?.let(Instant::ofEpochMilli)
        set(value) {
            store.edit().apply {
                if (value == null) {
                    remove(KEY_LAST_CHECK)
                } else {
                    putLong(KEY_LAST_CHECK, value.toEpochMilli())
                }
            }.apply()
        }

    override var autoCheck: Boolean
        get() = store.readBoolean(KEY_AUTO, true)
        set(value) = store.edit().putBoolean(KEY_AUTO, value).apply()

    override var notifiedVersion: String?
        get() = store.readString(KEY_NOTIFIED, null)
        set(value) = store.edit().putString(KEY_NOTIFIED, value).apply()

    override var found: LatestRelease?
        get() = readRelease()
        set(value) = writeRelease(value)

    private fun readRelease(): LatestRelease? {
        val version = store.readString(KEY_VERSION, null) ?: return null
        val apkUrl = store.readString(KEY_APK_URL, null) ?: return null
        val sha256 = store.readString(KEY_APK_SHA256, null) ?: return null
        val published = store.readLong(KEY_PUBLISHED, NO_DATE)

        return LatestRelease(
            version = version,
            pageUrl = store.readString(KEY_PAGE, null).orEmpty(),
            publishedAt = published.takeIf { it != NO_DATE }?.let(Instant::ofEpochMilli),
            apk = ApkAsset(apkUrl, store.readLong(KEY_APK_SIZE, 0), sha256),
            notes = store.readString(KEY_NOTES, null).orEmpty(),
        )
    }

    private fun writeRelease(release: LatestRelease?) {
        store.edit().apply {
            if (release == null) {
                remove(KEY_VERSION)
                remove(KEY_PAGE)
                remove(KEY_PUBLISHED)
                remove(KEY_APK_URL)
                remove(KEY_APK_SIZE)
                remove(KEY_APK_SHA256)
                remove(KEY_NOTES)
            } else {
                putString(KEY_VERSION, release.version)
                putString(KEY_PAGE, release.pageUrl)
                putLong(KEY_PUBLISHED, release.publishedAt?.toEpochMilli() ?: NO_DATE)
                putString(KEY_APK_URL, release.apk.url)
                putLong(KEY_APK_SIZE, release.apk.size)
                putString(KEY_APK_SHA256, release.apk.sha256)
                putString(KEY_NOTES, release.notes)
            }
        }.apply()
    }
}
