package io.github.vitalyostanin.geoshare.project

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.permission.granted
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private const val TAG = "geoshare.scan"

/** How long the green square stays up before the link is handed on. */
private const val FOUND_PAUSE_MS = 500L

/** How long a code that is not a connection link is reported after it was last seen. */
private const val FOREIGN_HOLD_MS = 3_000L

/** The buzz a read invitation gives. */
private const val BUZZ_MS = 60L

/** The square's side as a share of the window's shorter side. */
private const val SQUARE_SHARE = 0.65f

/** Each corner mark's length as a share of the square's side. */
private const val CORNER_SHARE = 0.22f

private val SCRIM = Color(0x99000000)
private val FOUND = Color(0xFF6FD3A0)
private val FOREIGN = Color(0xFFFFB4AB)

/**
 * The invitation scanner, over the whole window: the camera with a square to
 * aim with, until a connection link is read.
 *
 * A link is handed to [onFound] as its text, half a second after the device
 * buzzed and the square turned green, so the person sees what happened
 * before the screen changes. Any other code — a web address, a Wi-Fi network
 * — is said to be one for three seconds and scanning goes on. [onDismiss]
 * closes it with nothing read.
 *
 * The camera is asked for when this opens, not before: a person who came
 * with a link in hand is never asked for it.
 */
@Composable
fun QrScanDialog(onFound: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
        DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val context = LocalContext.current
        var granted by remember { mutableStateOf(granted(context, Manifest.permission.CAMERA)) }
        var refused by remember { mutableStateOf(false) }
        val ask =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                granted = it
                refused = !it
            }

        LaunchedEffect(Unit) {
            if (!granted) {
                ask.launch(Manifest.permission.CAMERA)
            }
        }

        // Back from the application's settings, where the camera may have
        // been allowed by hand.
        LifecycleResumeEffect(Unit) {
            granted = granted(context, Manifest.permission.CAMERA)
            onPauseOrDispose {}
        }

        when {
            granted -> Scanner(onFound = onFound, onDismiss = onDismiss)

            refused -> Refused(onAsk = {
                ask.launch(Manifest.permission.CAMERA)
            }, onBack = onDismiss)

            // The system's own question is up; nothing of ours under it.
            else -> Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }
    }
}

/** The camera, the square and what the scanner has to say. */
@Composable
private fun Scanner(onFound: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var found by remember { mutableStateOf<Scanned.Link?>(null) }
    var foreignSeen by remember { mutableIntStateOf(0) }
    var foreign by remember { mutableStateOf(false) }
    val handOn by rememberUpdatedState(onFound)

    LaunchedEffect(found) {
        val link = found ?: return@LaunchedEffect

        delay(FOUND_PAUSE_MS)
        handOn(link.text)
    }

    // Seen again on every frame while it is in view, so the line stays up as
    // long as the camera is on it and goes three seconds after.
    LaunchedEffect(foreignSeen) {
        if (foreignSeen > 0) {
            delay(FOREIGN_HOLD_MS)
            foreign = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Viewfinder(
            onText = { text ->
                if (found == null) {
                    when (val read = scanned(text)) {
                        is Scanned.Link -> {
                            found = read
                            buzz(context)
                        }

                        Scanned.Foreign -> {
                            foreign = true
                            foreignSeen++
                        }
                    }
                }
            },
        )

        Square(found = found != null)

        ScannerOverlay(found = found, foreign = foreign, onDismiss = onDismiss)
    }
}

/** The title over the square, and under it what is being said and the way out. */
@Composable
private fun ScannerOverlay(found: Scanned.Link?, foreign: Boolean, onDismiss: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.qr_scan_title),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.weight(1f))

        val (line, colour) =
            when {
                found != null -> foundLine(found) to FOUND
                foreign -> stringResource(R.string.qr_scan_foreign) to FOREIGN
                else -> stringResource(R.string.qr_scan_hint) to Color.White
            }

        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = colour,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )

        TextButton(onClick = onDismiss) {
            Text(text = stringResource(R.string.connect_cancel), color = Color.White)
        }
    }
}

@Composable
private fun foundLine(found: Scanned.Link?): String = found?.groupName?.let {
    stringResource(R.string.qr_scan_found_group, it)
} ?: stringResource(R.string.qr_scan_found)

/**
 * The camera's picture, its frames read for a QR code on a thread of their
 * own; [onText] hears each code read, on the main thread.
 *
 * Only the newest frame is kept while one is being read, and every frame is
 * closed once read, or the camera stops sending them.
 */
@Composable
private fun Viewfinder(onText: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val heard by rememberUpdatedState(onText)
    val view =
        remember {
            PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        }

    AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())

    LaunchedEffect(view, owner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val frames = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val preview = Preview.Builder().build()
        val analysis =
            ImageAnalysis
                .Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
        var luminance = ByteArray(0)

        preview.setSurfaceProvider(view.surfaceProvider)
        analysis.setAnalyzer(frames) { image ->
            val text =
                image.use {
                    val plane = it.planes[0]
                    val size = plane.rowStride * it.height

                    if (luminance.size != size) {
                        luminance = ByteArray(size)
                    }
                    plane.buffer.get(luminance, 0, minOf(size, plane.buffer.remaining()))
                    qrTextIn(luminance, it.width, it.height, plane.rowStride)
                }

            if (text != null) {
                main.execute { heard(text) }
            }
        }

        try {
            provider.bindToLifecycle(owner, cameraOf(provider), preview, analysis)
            awaitCancellation()
        } catch (unusable: IllegalArgumentException) {
            AppLog.w(TAG, "no camera to scan with", unusable)
        } finally {
            provider.unbind(preview, analysis)
            frames.shutdown()
        }
    }
}

/** The back camera, or the front one on a device that has no other. */
private fun cameraOf(provider: ProcessCameraProvider): CameraSelector =
    if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
        CameraSelector.DEFAULT_BACK_CAMERA
    } else {
        CameraSelector.DEFAULT_FRONT_CAMERA
    }

/** [ImageProxy] closed after [block], whatever it ends with. */
private inline fun <T> ImageProxy.use(block: (ImageProxy) -> T): T = try {
    block(this)
} finally {
    close()
}

/** The square to aim with, the window darkened around it; green once a link is read. */
@Composable
private fun Square(found: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val side = minOf(size.width, size.height) * SQUARE_SHARE
        val square =
            Rect(Offset((size.width - side) / 2, (size.height - side) / 2), Size(side, side))
        val radius = 16.dp.toPx()
        val around =
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                addRoundRect(RoundRect(square, CornerRadius(radius)))
            }

        drawPath(around, SCRIM)

        val colour = if (found) FOUND else Color.White
        val stroke = 3.dp.toPx()
        val arm = side * CORNER_SHARE
        val corners =
            listOf(
                square.topLeft to Offset(1f, 1f),
                square.topRight to Offset(-1f, 1f),
                square.bottomLeft to Offset(1f, -1f),
                square.bottomRight to Offset(-1f, -1f),
            )

        for ((corner, inward) in corners) {
            drawLine(colour, corner, corner + Offset(inward.x * arm, 0f), stroke, StrokeCap.Round)
            drawLine(colour, corner, corner + Offset(0f, inward.y * arm), stroke, StrokeCap.Round)
        }
    }
}

/**
 * The camera refused. [onAsk] asks again while the system still shows its
 * question; once it no longer does, the application's settings are opened
 * instead, which is the only place left to allow it.
 */
@Composable
private fun Refused(onAsk: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.qr_camera_refused_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.qr_camera_refused_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = {
                    val asksAgain =
                        activity?.shouldShowRequestPermissionRationale(
                            Manifest.permission.CAMERA,
                        ) ==
                            true

                    if (asksAgain) {
                        onAsk()
                    } else {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    }
                },
            ) {
                Text(text = stringResource(R.string.qr_camera_allow))
            }
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.qr_camera_back))
            }
        }
    }
}

/** A short buzz, as the camera reads an invitation. */
private fun buzz(context: Context) {
    val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }

    vibrator?.vibrate(VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE))
}
