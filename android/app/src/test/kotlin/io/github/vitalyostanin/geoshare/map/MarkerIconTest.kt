package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerIconTest {
    // The same expectations the web page checks. A member should be the same
    // colour whichever client is looking at them, and the two hashes agreeing
    // is what makes that true; this pair of tests is where a change to either
    // one gets caught.
    @Test
    fun `the colour index is taken from the id, and matches the web page`() {
        assertEquals(0, colourIndex(""))
        assertEquals(2, colourIndex("a"))
        assertEquals(4, colourIndex("member"))
        assertEquals(1, colourIndex("u7ExampleOwnerUid00000000000"))
    }

    // The same three cases the web page checks: a member without a picture is
    // drawn as a letter, and both clients have to pick the same one.
    @Test
    fun `the initial is the first letter of the name, in capitals`() {
        assertEquals("A", memberInitial("Ann"))
        assertEquals("O", memberInitial("  oona  "))
    }

    @Test
    fun `a name with nothing in it leaves no initial`() {
        assertEquals("", memberInitial("   "))
    }

    @Test
    fun `an id whose hash has no positive counterpart still lands in the list`() {
        // "polygenelubricants" is the standard example of a string whose hash
        // is the smallest negative integer there is; dropping the sign first
        // would push the index out of the list.
        val index = colourIndex("polygenelubricants")

        assertEquals(true, index in 0..4)
    }

    // The same rule the web page follows, so that a crowd in one place is the
    // same marker wherever it is looked at.
    @Test
    fun `a small group shows every face it has`() {
        assertEquals(1, facesShown(1))
        assertEquals(2, facesShown(2))
        assertEquals(3, facesShown(3))
    }

    @Test
    fun `a group too large to draw gives its last circle to the count`() {
        assertEquals(2, facesShown(4))
        assertEquals(2, facesShown(9))
    }
}
