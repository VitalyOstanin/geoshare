package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The two languages the application is written in, held to saying the same
 * things.
 *
 * A string added to the English file and forgotten in the Russian one is not
 * an error anywhere: the phone falls back to English for that one line, and
 * the screen ends up half translated. This test is what turns that into a
 * failure at the time the string is added.
 */
class TranslationTest {
    private val english = named(ENGLISH)
    private val russian = named(RUSSIAN)

    @Test
    fun `both files are where this test thinks they are`() {
        assertTrue("$ENGLISH is not there", File(ENGLISH).isFile)
        assertTrue("$RUSSIAN is not there", File(RUSSIAN).isFile)
    }

    @Test
    fun `everything the application says is said in Russian too`() {
        assertEquals(emptySet<String>(), english - russian)
    }

    @Test
    fun `nothing is said in Russian alone`() {
        assertEquals(emptySet<String>(), russian - english)
    }

    // Russian counts in four forms where English has two, and a form left out
    // is the count read in the wrong case rather than a build that fails.
    @Test
    fun `every counted phrase carries the four forms Russian counts in`() {
        val text = File(RUSSIAN).readText()

        for (name in PLURALS.findAll(text).map { it.groupValues[1] }) {
            val block = Regex(
                """<plurals name="$name">(.*?)</plurals>""",
                RegexOption.DOT_MATCHES_ALL,
            )
                .find(text)
                ?.groupValues
                ?.get(1)
                .orEmpty()

            for (quantity in listOf("one", "few", "many", "other")) {
                assertTrue("$name has no $quantity", block.contains("""quantity="$quantity""""))
            }
        }
    }

    /**
     * What a file says, by name, leaving out what is the same in every
     * language.
     *
     * The mark is the one the build itself reads: a string a translator has
     * no business touching says so where the platform can see it, rather
     * than in a list here that would have to be remembered twice.
     */
    private fun named(path: String): Set<String> = NAMES.findAll(File(path).readText())
        .filterNot { it.groupValues[2].contains("""translatable="false"""") }
        .map { it.groupValues[1] }
        .toSet()

    companion object {
        const val ENGLISH = "src/main/res/values/strings.xml"
        const val RUSSIAN = "src/main/res/values-ru/strings.xml"

        val NAMES = Regex("""<(?:string|plurals) name="([^"]+)"([^>]*)>""")
        val PLURALS = Regex("""<plurals name="([^"]+)"""")
    }
}
