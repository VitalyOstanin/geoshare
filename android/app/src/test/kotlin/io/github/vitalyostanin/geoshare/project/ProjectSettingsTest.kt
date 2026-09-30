package io.github.vitalyostanin.geoshare.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectSettingsTest {
    private val document = """
        {
            "version": 1,
            "projectId": "geoshare-example",
            "applicationId": "1:123456789012:android:0123456789abcdef",
            "apiKey": "AIzaExampleKeyNotReal",
            "gcmSenderId": "123456789012",
            "site": "geoshare-example-abc123"
        }
    """.trimIndent()

    @Test
    fun `a well-formed document is read into the six fields`() {
        assertEquals(
            ProjectSettings(
                version = 1,
                projectId = "geoshare-example",
                applicationId = "1:123456789012:android:0123456789abcdef",
                apiKey = "AIzaExampleKeyNotReal",
                gcmSenderId = "123456789012",
                site = "geoshare-example-abc123",
            ),
            ProjectSettings.parse(document),
        )
    }

    @Test
    fun `a later version is not one this build understands`() {
        assertNull(ProjectSettings.parse(document.replace("\"version\": 1", "\"version\": 2")))
    }

    @Test
    fun `a document missing a field is refused rather than started with a gap`() {
        assertNull(
            ProjectSettings.parse(
                """{"version": 1, "projectId": "geoshare-example"}""",
            ),
        )
    }

    @Test
    fun `a blank field counts the same as a missing one`() {
        val blankApiKey = document.replace("\"AIzaExampleKeyNotReal\"", "\"\"")

        assertNull(ProjectSettings.parse(blankApiKey))
    }

    @Test
    fun `a document missing the site is refused like any other missing field`() {
        assertNull(
            ProjectSettings.parse(
                """
                {
                    "version": 1,
                    "projectId": "geoshare-example",
                    "applicationId": "1:123456789012:android:0123456789abcdef",
                    "apiKey": "AIzaExampleKeyNotReal",
                    "gcmSenderId": "123456789012"
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `a site outside the lowercase-digits-hyphens rule is refused`() {
        val upperCaseSite = document.replace(
            "\"geoshare-example-abc123\"",
            "\"Geoshare-Example\"",
        )

        assertNull(ProjectSettings.parse(upperCaseSite))
    }

    @Test
    fun `text that is not json at all is refused rather than thrown from`() {
        assertNull(ProjectSettings.parse("not json"))
    }
}
