package io.github.vitalyostanin.geoshare.project

import io.github.vitalyostanin.geoshare.map.qrSquares
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrScanTest {
    /**
     * A camera frame's brightness plane with [text]'s code drawn into it at
     * ([left], [top]), each square [cell] pixels wide, on the white margin a
     * screen shows it with. Rows are [PADDING] bytes longer than the picture,
     * as a camera's often are.
     */
    private fun frame(text: String?, left: Int = 40, top: Int = 60, cell: Int = 4): ByteArray {
        val bytes = ByteArray(STRIDE * HEIGHT) { GREY }

        for (y in 0 until HEIGHT) {
            for (x in WIDTH until STRIDE) {
                // What lies past the picture is not part of it, and must not
                // be read as though it were.
                bytes[y * STRIDE + x] = DARK
            }
        }

        if (text == null) {
            return bytes
        }

        val squares = qrSquares(text)
        val margin = 4 * cell

        for (y in top - margin until top + squares.height * cell + margin) {
            for (x in left - margin until left + squares.width * cell + margin) {
                bytes[y * STRIDE + x] = LIGHT
            }
        }

        for (y in 0 until squares.height * cell) {
            for (x in 0 until squares.width * cell) {
                val dark = squares[x / cell, y / cell]

                bytes[(y + top) * STRIDE + x + left] = if (dark) DARK else LIGHT
            }
        }

        return bytes
    }

    private fun read(bytes: ByteArray) = qrTextIn(bytes, WIDTH, HEIGHT, STRIDE)

    @Test
    fun `an invitation's code is read out of a frame`() {
        assertEquals(LINK, read(frame(LINK)))
    }

    @Test
    fun `a code away from the middle of the frame is read too`() {
        assertEquals(LINK, read(frame(LINK, left = 16, top = 16)))
    }

    @Test
    fun `a group name outside Latin is read unchanged`() {
        val link = "https://geoshare-example.web.app/join#g=group-1&c=ABCDEFGH&n=Семья"

        assertEquals(link, read(frame(link)))
    }

    @Test
    fun `a frame with no code in it reads as nothing`() {
        assertNull(read(frame(null)))
    }

    @Test
    fun `a connection link is taken, with the group it names`() {
        assertEquals(Scanned.Link(LINK, "Pohod"), scanned(LINK))
    }

    @Test
    fun `an owner's link without an invitation is taken, with no group`() {
        val link = "https://geoshare-example.web.app/join"

        assertEquals(Scanned.Link(link, null), scanned(link))
    }

    @Test
    fun `any other code is not a connection link`() {
        assertEquals(Scanned.Foreign, scanned("https://example.com/join#g=group-1&c=ABCDEFGH"))
        assertEquals(Scanned.Foreign, scanned("WIFI:S:home;T:WPA;P:secret;;"))
    }

    private companion object {
        const val LINK = "https://geoshare-example.web.app/join#g=group-1&c=ABCDEFGH&n=Pohod&i=Ivan"
        const val WIDTH = 320
        const val HEIGHT = 240
        const val PADDING = 64
        const val STRIDE = WIDTH + PADDING
        const val GREY: Byte = 0x80.toByte()
        const val DARK: Byte = 0x10
        const val LIGHT: Byte = 0xF0.toByte()
    }
}
