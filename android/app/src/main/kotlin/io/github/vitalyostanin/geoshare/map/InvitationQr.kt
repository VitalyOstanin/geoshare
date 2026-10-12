package io.github.vitalyostanin.geoshare.map

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.vitalyostanin.geoshare.R

// Dark on white whatever the theme: a camera reads a code by its contrast,
// and the light squares of an inverted one are taken for the dark.
private val QR_DARK = Color(0xFF111111)
private val QR_LIGHT = Color.White

/**
 * The squares of [text] as a QR code, one cell per square, without the
 * margin: whoever draws it puts the white around it.
 *
 * Medium error correction rather than the lowest: the code is read off one
 * screen by the camera of another, through glare and a scratched glass, and
 * an invitation link is short enough to stay a small code with it.
 */
internal fun qrSquares(text: String): BitMatrix = QRCodeWriter().encode(
    text,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
        EncodeHintType.MARGIN to 0,
    ),
)

/** [squares] drawn to the size it is given, on white with a margin of its own. */
@Composable
private fun QrSquares(squares: BitMatrix, modifier: Modifier = Modifier) {
    Canvas(
        modifier =
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(QR_LIGHT)
            .padding(8.dp),
    ) {
        val cell = size.minDimension / squares.width

        for (y in 0 until squares.height) {
            for (x in 0 until squares.width) {
                if (squares[x, y]) {
                    drawRect(
                        color = QR_DARK,
                        topLeft = Offset(x * cell, y * cell),
                        // A hair wider than the cell, so that neighbouring
                        // squares do not show a seam where the size does not
                        // divide evenly.
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}

/**
 * The invitation link as a QR code, shown as soon as the invitation is made:
 * the member to be is usually in the same room, and pointing a camera at it
 * is quicker than any messenger. Pressing it opens it over the whole screen.
 */
@Composable
internal fun InvitationQr(link: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val squares = remember(link) { qrSquares(link) }
    val described = stringResource(R.string.group_invite_qr)

    QrSquares(
        squares = squares,
        modifier =
        modifier
            .size(160.dp)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = described },
    )
}

/**
 * [link]'s code over the whole screen, on white and at full brightness for
 * as long as it is open: a dark theme and a dimmed screen are what keep a
 * camera from reading it.
 */
@Composable
internal fun FullScreenQr(link: String, onClose: () -> Unit) {
    val squares = remember(link) { qrSquares(link) }

    Dialog(
        onDismissRequest = onClose,
        // Drawn edge to edge, so that the white covers the status bar too.
        properties =
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        FullBrightness()

        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .background(QR_LIGHT)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // As large as the window allows either way round, sideways too,
            // and no larger than a camera needs.
            QrSquares(
                squares = squares,
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 480.dp),
            )

            Text(text = stringResource(R.string.group_invite_qr_scan), color = QR_DARK)

            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.group_invite_qr_close))
            }
        }
    }
}

/** Turns the screen of the window it is drawn in up all the way, and back on leaving. */
@Composable
private fun FullBrightness() {
    val view = LocalView.current

    DisposableEffect(view) {
        val window =
            (view.parent as? DialogWindowProvider)?.window ?: (view.context as? Activity)?.window
        val before = window?.attributes?.screenBrightness

        window?.attributes = window?.attributes?.apply { screenBrightness = 1f }

        onDispose {
            if (window != null && before != null) {
                window.attributes = window.attributes.apply { screenBrightness = before }
            }
        }
    }
}
