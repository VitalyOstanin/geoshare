package io.github.vitalyostanin.geoshare.project

import io.github.vitalyostanin.geoshare.BuildConfig

/**
 * Where someone with no invitation sets up a group of their own: the page
 * walks them through creating a Firebase project and ends by sending them
 * back to the app with a connection link.
 */
object SetupPage {
    /**
     * The project owner's own setup page, or the one a fork names in
     * `setupPageUrl` in `android/gradle.properties`.
     */
    val SETUP_PAGE_URL: String = BuildConfig.SETUP_PAGE_URL
}
