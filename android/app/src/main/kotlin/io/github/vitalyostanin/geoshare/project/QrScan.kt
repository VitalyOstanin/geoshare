package io.github.vitalyostanin.geoshare.project

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** Told to zxing on every frame: only QR codes are looked for, and they are UTF-8. */
private val HINTS =
    mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
    )

/**
 * The text of a QR code in a camera frame, or null for a frame with none
 * that zxing could read.
 *
 * [luminance] is the frame's first plane as the camera hands it over: one
 * byte of brightness per pixel, [rowStride] bytes to a row, of which the
 * first [width] are the picture. The colour planes are not needed — a QR
 * code is black on white.
 *
 * The whole frame is searched, not only what the scanner's square marks out:
 * the square shows where to aim, and a code held a little off it is still
 * read.
 */
fun qrTextIn(luminance: ByteArray, width: Int, height: Int, rowStride: Int): String? {
    val source =
        PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)

    return try {
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), HINTS).text
    } catch (_: ReaderException) {
        null
    }
}

/** What the scanner does with a code it has read. */
sealed interface Scanned {
    /** A connection link: [text] goes on to where a pasted one would. */
    data class Link(val text: String, val groupName: String?) : Scanned

    /** Some other code — a web address, a Wi-Fi network: scanning goes on. */
    data object Foreign : Scanned
}

/** [Scanned] for [text], a QR code's whole content. */
fun scanned(text: String): Scanned {
    val link = ConnectLink.parse(text) ?: return Scanned.Foreign

    return Scanned.Link(text, link.invitation?.groupName)
}
