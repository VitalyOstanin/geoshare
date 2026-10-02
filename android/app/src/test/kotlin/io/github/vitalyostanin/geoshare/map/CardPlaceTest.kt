package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test

class CardPlaceTest {
    @Test
    fun `a card stands centred over its button`() {
        assertEquals(300f, cardLeft(centre = 500f, cardWidth = 400f, room = 1200f, margin = 16f))
    }

    @Test
    fun `a card over the last button is pushed back inside`() {
        assertEquals(784f, cardLeft(centre = 1150f, cardWidth = 400f, room = 1200f, margin = 16f))
    }

    @Test
    fun `a card over the first button is pushed back inside`() {
        assertEquals(16f, cardLeft(centre = 60f, cardWidth = 400f, room = 1200f, margin = 16f))
    }

    // A phone held upright is narrower than the card would like to be.
    @Test
    fun `a card wider than the room keeps to the left margin`() {
        assertEquals(16f, cardLeft(centre = 600f, cardWidth = 1300f, room = 1200f, margin = 16f))
    }
}
