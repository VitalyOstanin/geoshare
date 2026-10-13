package io.github.vitalyostanin.geoshare

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The application starts and draws its first screen. A debug build comes
 * connected to the stand-in project, so a fresh install opens on signing in,
 * and nothing on the way there asks for a permission.
 */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshInstallOpensOnSigningIn() {
        val signIn = composeRule.activity.getString(R.string.sign_in_action)

        composeRule.onNodeWithText(signIn).assertExists()
    }
}
