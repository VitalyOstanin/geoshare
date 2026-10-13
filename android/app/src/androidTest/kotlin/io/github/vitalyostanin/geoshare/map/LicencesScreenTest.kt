package io.github.vitalyostanin.geoshare.map

import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vitalyostanin.geoshare.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The licences screen reads the list the build collected and draws it: the
 * list is in the application, and a library opens onto its licence.
 */
@RunWith(AndroidJUnit4::class)
class LicencesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun headerClosesTheScreen() {
        var closed = false
        composeRule.setContent { LicencesScreen(onClose = { closed = true }) }

        composeRule.onNodeWithText(context.getString(R.string.licences_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.settings_close)).performClick()

        assertTrue(closed)
    }

    // MapLibre is under BSD-2-Clause, so its dialog showing that licence
    // means the texts the build packed reached the screen.
    @Test
    fun libraryOpensOntoItsLicenceText() {
        composeRule.setContent { LicencesScreen(onClose = {}) }
        val mapLibre = hasText("MapLibre", substring = true, ignoreCase = true)

        // The list is read off the main thread, and the scrolling container
        // stands before its rows do: waiting for the container alone let the
        // search run over an empty list.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(mapLibre)
            }.isSuccess
        }
        composeRule.onAllNodes(mapLibre)[0].performClick()

        // A phrase of the licence itself: the label under the name says BSD
        // as well, so the name of the licence would be found twice.
        composeRule
            .onNodeWithText("Redistribution and use in source and binary forms", substring = true)
            .assertExists()
    }
}
