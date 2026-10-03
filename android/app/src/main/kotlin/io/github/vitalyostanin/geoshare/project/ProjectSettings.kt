package io.github.vitalyostanin.geoshare.project

import com.google.firebase.FirebaseOptions
import org.json.JSONException
import org.json.JSONObject

/** The only document version this build understands. */
private const val VERSION = 1

private const val FIELD_VERSION = "version"
private const val FIELD_PROJECT_ID = "projectId"
private const val FIELD_APPLICATION_ID = "applicationId"
private const val FIELD_API_KEY = "apiKey"
private const val FIELD_GCM_SENDER_ID = "gcmSenderId"
private const val FIELD_SITE = "site"

/** What `tools/project.sh`'s `hosting_site` names a group's Hosting site. */
private val SITE_PATTERN = Regex("[a-z0-9-]+")

/**
 * What a group's site hands the phone in `geoshare.json`, written by
 * `tools/project.sh`'s `app_settings`: enough to start Firebase with,
 * without a project baked into the build. One build serves every group, so
 * it carries none of its own, and this is the document a connection link
 * downloads in its place.
 *
 * The site it was fetched from rides along too: a connection link built
 * later needs it, and nothing else on the phone learns it any other way.
 */
data class ProjectSettings(
    val version: Int,
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
    val gcmSenderId: String,
    val site: String,
) {
    /** What [FamilyProject] hands Firebase; the storage bucket is never asked for. */
    fun toOptions(): FirebaseOptions = FirebaseOptions.Builder()
        .setProjectId(projectId)
        .setApplicationId(applicationId)
        .setApiKey(apiKey)
        .setGcmSenderId(gcmSenderId)
        .build()

    /** The same document [parse] reads, so a holder can keep it exactly as it came. */
    fun toJson(): String = JSONObject()
        .put(FIELD_VERSION, version)
        .put(FIELD_PROJECT_ID, projectId)
        .put(FIELD_APPLICATION_ID, applicationId)
        .put(FIELD_API_KEY, apiKey)
        .put(FIELD_GCM_SENDER_ID, gcmSenderId)
        .put(FIELD_SITE, site)
        .toString()

    companion object {
        /**
         * Reads a group's settings, or null when the document is not one
         * this build can start Firebase with.
         *
         * Read rather than trusted blindly: a site answers with whatever it
         * is asked to serve, and a document `FirebaseApp.initializeApp` was
         * simply handed would fail somewhere inside the library instead of
         * here, where the reason is still in reach. Only version 1 is
         * accepted — a group's site is written once and rarely touched
         * again, and a phone running an older build has no way of being told
         * the document has moved past what it understands.
         */
        fun parse(json: String): ProjectSettings? {
            val document = try {
                JSONObject(json)
            } catch (malformed: JSONException) {
                return null
            }

            if (document.optInt(FIELD_VERSION, -1) != VERSION) {
                return null
            }

            val projectId = document.optString(FIELD_PROJECT_ID)
            val applicationId = document.optString(FIELD_APPLICATION_ID)
            val apiKey = document.optString(FIELD_API_KEY)
            val gcmSenderId = document.optString(FIELD_GCM_SENDER_ID)
            val site = document.optString(FIELD_SITE)

            if (projectId.isBlank() ||
                applicationId.isBlank() ||
                apiKey.isBlank() ||
                gcmSenderId.isBlank() ||
                !site.matches(SITE_PATTERN)
            ) {
                return null
            }

            return ProjectSettings(VERSION, projectId, applicationId, apiKey, gcmSenderId, site)
        }
    }
}
