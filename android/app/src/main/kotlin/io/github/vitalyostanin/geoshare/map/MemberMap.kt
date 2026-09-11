package io.github.vitalyostanin.geoshare.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.Instant

/** Close enough to tell which street somebody is on, without hunting for them. */
private const val START_ZOOM = 15.0

/**
 * A request to look at one member, rather than a statement of what is on
 * screen.
 *
 * The counter is what makes a second tap on the same member a change: after
 * the first one the map may have been dragged away, and the member expects the
 * button to bring it back.
 */
data class MapFocus(val memberId: String, val request: Int)

/**
 * The members on an OpenStreetMap background.
 *
 * A view rather than a Compose map: the drawing is osmdroid's, and the only
 * thing that crosses the boundary is the list of markers.
 */
@Composable
fun MemberMap(
    positions: List<MemberPosition>,
    ownMemberId: String,
    ownName: String,
    now: Instant,
    /** The member the panel asked to look at, if one has been asked for. */
    focus: MapFocus?,
    /** Called with the member whose marker was tapped. */
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val map = remember { createMap(context) }
    // The first position to arrive decides where the map opens. After that the
    // member is in charge of what they are looking at, and a list that updates
    // every few seconds must not drag the view back.
    val centred = remember { booleanArrayOf(false) }
    // The last request acted on. Held outside the composition state on purpose:
    // moving the map is not something to recompose over.
    val focused = remember { intArrayOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    // The tile threads and the cache run only while the map is
                    // on screen; without this pair they keep going behind it.
                    Lifecycle.Event.ON_RESUME -> map.onResume()

                    Lifecycle.Event.ON_PAUSE -> map.onPause()

                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            map.onDetach()
        }
    }

    AndroidView(factory = { map }, modifier = modifier) { view ->
        showMarkers(view, positions, ownMemberId, ownName, now, onSelect)

        val start = positions.firstOrNull { it.memberId == ownMemberId } ?: positions.firstOrNull()

        if (!centred[0] && start != null) {
            centred[0] = true
            view.controller.setZoom(START_ZOOM)
            view.controller.setCenter(GeoPoint(start.latitude, start.longitude))
        }

        if (focus != null && focus.request != focused[0]) {
            focused[0] = focus.request

            positions
                .firstOrNull { it.memberId == focus.memberId }
                ?.let { member ->
                    // Animated rather than set, because the member asked to be
                    // taken somewhere and the way there says how far it was.
                    view.controller.animateTo(GeoPoint(member.latitude, member.longitude))
                }
        }
    }
}

private fun createMap(context: Context): MapView {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        // The tile server turns away anything calling itself osmdroid, which
        // is the default, and its terms ask for a name that identifies the
        // application. The cache goes into the application's own directory,
        // so that none of this needs storage permissions.
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.filesDir, "osmdroid")
        osmdroidTileCache = File(context.filesDir, "osmdroid/tiles")
    }

    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        // The map is as wide as the screen; the built-in zoom buttons sit on
        // top of the list and are not needed next to pinch zoom.
        zoomController.setVisibility(
            org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER,
        )
        controller.setZoom(START_ZOOM)
    }
}

/**
 * Redraws the markers from scratch on every change.
 *
 * With a handful of members that costs nothing measurable, and it keeps the
 * marker list and the position list from drifting apart, which is the failure
 * that would show a member somewhere they no longer are.
 */
private fun showMarkers(
    map: MapView,
    positions: List<MemberPosition>,
    ownMemberId: String,
    ownName: String,
    now: Instant,
    onSelect: (String) -> Unit,
) {
    map.overlays.clear()

    positions.forEach { position ->
        val name = if (position.memberId == ownMemberId) ownName else shortName(position.memberId)

        val drawn =
            markerIcon(
                context = map.context,
                name = name,
                colour = memberColour(position.memberId),
                stale = isStale(position.knownAt, now),
            )

        val marker =
            Marker(map).apply {
                this.position = GeoPoint(position.latitude, position.longitude)
                setAnchor(Marker.ANCHOR_CENTER, drawn.anchorV)
                icon = drawn.drawable
                // The bubble is a card of our own below the map rather than
                // the library's window: it is the one place the details are
                // laid out, and the panel opens the same card.
                setInfoWindow(null)
                setOnMarkerClickListener { _, _ ->
                    onSelect(position.memberId)

                    true
                }
            }

        map.overlays.add(marker)
    }

    map.invalidate()
}
