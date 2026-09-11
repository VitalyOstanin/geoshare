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

    @Test
    fun `an id whose hash has no positive counterpart still lands in the list`() {
        // "polygenelubricants" is the standard example of a string whose hash
        // is the smallest negative integer there is; dropping the sign first
        // would push the index out of the list.
        val index = colourIndex("polygenelubricants")

        assertEquals(true, index in 0..4)
    }
}
