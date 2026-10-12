package io.github.vitalyostanin.geoshare.map

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.CancellationException

private const val TAG = "geoshare.faces"

/**
 * The account pictures, as drawings rather than as addresses.
 *
 * The markers are raster images handed to the map, so a picture is of no use
 * here until it is a bitmap; this is where the addresses in the profiles turn
 * into one. A member whose picture has not arrived yet is simply drawn with
 * their initial until it does.
 *
 * Kept by address rather than by member: two members can share a picture, a
 * member can change theirs, and the address is what says which of the two has
 * happened.
 */
@Composable
fun rememberMemberFaces(profiles: Map<String, MemberProfile>): Map<String, Bitmap> {
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(emptyMap<String, Bitmap>()) }

    val wanted = profiles.values.mapNotNull { it.pictureUrl }.toSet()

    LaunchedEffect(wanted) {
        // Dropped first, so that a member who removes their picture stops
        // being drawn with it, and the map does not hold pictures nobody in
        // the group uses any more.
        val kept = loaded.filterKeys { it in wanted }

        if (kept.size != loaded.size) {
            loaded = kept
        }

        for (url in wanted - loaded.keys) {
            val face = loadFace(context, url) ?: continue

            // Published one at a time: a face that has arrived should reach
            // the map without waiting for the slowest of the others.
            loaded = loaded + (url to face)
        }
    }

    return profiles.mapNotNull { (memberId, profile) ->
        val face = profile.pictureUrl?.let { loaded[it] } ?: return@mapNotNull null

        memberId to face
    }.toMap()
}

/**
 * One picture, at the size the pin draws it.
 *
 * The loader is the shared one, so a picture already fetched for the account
 * button costs nothing here, and a failed fetch is a line in the log rather
 * than anything on screen: the pin falls back to the initial, which is what a
 * member without a picture gets anyway.
 */
private suspend fun loadFace(context: Context, url: String): Bitmap? {
    // A profile is written by the member it describes, so the address in it is
    // whatever they put there; the profile source already holds it to the
    // account host, and so does this, so that no other way in reaches the
    // network. Anything else leaves the pin with its initial.
    if (accountPicture(url) == null) {
        AppLog.w(TAG, "the picture address is not an account picture")

        return null
    }

    val side = faceSize(context)

    val request =
        ImageRequest.Builder(context)
            .data(url)
            .size(side, side)
            // Decoded into memory the processor can read: the pin is drawn by
            // hand onto a bitmap, and a picture left in the graphics card
            // cannot be drawn that way. Without this the first face to arrive
            // takes the screen down with it.
            .allowHardware(false)
            .build()

    return try {
        val result = SingletonImageLoader.get(context).execute(request)

        (result as? SuccessResult)?.image?.toBitmap(side, side)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not load the picture at $url", error)

        null
    }
}
