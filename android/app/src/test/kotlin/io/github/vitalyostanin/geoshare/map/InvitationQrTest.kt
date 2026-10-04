package io.github.vitalyostanin.geoshare.map

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class InvitationQrTest {
    // Read back the way a camera would: each square a few pixels wide, with
    // the white margin the screen draws around it.
    private fun readBack(text: String): String {
        val squares = qrSquares(text)
        val cell = 4
        val margin = 4 * cell
        val side = squares.width * cell + 2 * margin
        val pixels = IntArray(side * side) { WHITE }

        for (y in 0 until squares.height * cell) {
            for (x in 0 until squares.width * cell) {
                if (squares[x / cell, y / cell]) {
                    pixels[(y + margin) * side + x + margin] = BLACK
                }
            }
        }

        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))

        return QRCodeReader().decode(bitmap).text
    }

    @Test
    fun `an invitation link reads back as itself`() {
        val link = "https://geoshare-example.web.app/join#g=group-1&c=ABCDEFGH&n=Pohod&i=Ivan"

        assertEquals(link, readBack(link))
    }

    @Test
    fun `a group name outside Latin reads back unchanged`() {
        val link = "https://geoshare-example.web.app/join#g=group-1&c=ABCDEFGH&n=Семья"

        assertEquals(link, readBack(link))
    }

    private companion object {
        const val WHITE = -0x1
        const val BLACK = -0x1000000
    }
}
