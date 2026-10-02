package io.github.vitalyostanin.geoshare.map

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.vitalyostanin.geoshare.R
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import java.time.Instant

/** Close enough to tell which street somebody is on, without hunting for them. */
internal const val START_ZOOM = 15.0

/**
 * How close in [FollowMode.ALL] ever zooms, reusing [START_ZOOM]'s distance.
 *
 * `CameraUpdateFactory.newLatLngBounds` has no overload that takes a maximum
 * zoom, and fitting a single member's rectangle -- south-west and north-east
 * the same point -- would otherwise zoom the camera in as far as the map
 * allows. The fit is worked out first with
 * [MapLibreMap.getCameraForLatLngBounds] and clamped to this before the
 * camera is animated there, rather than flown in close and pulled back out.
 */
internal const val MAX_ALL_MODE_ZOOM = START_ZOOM

/** How much room is left around the group's rectangle when [FollowMode.ALL] fits it, at the least. */
internal const val ALL_MODE_PADDING_DP = 48f

/** The gap left between a fitted marker's outermost edge and the screen's. */
internal const val ALL_MODE_MARKER_GAP_DP = 12f

/** How far around a tap a marker still counts as the one that was meant. */
internal const val TOUCH_RADIUS_DP = 18f

/**
 * How close two pins have to come before they are drawn as one.
 *
 * A little less than the width of a pin: at that distance the heads already
 * overlap, and the labels above them have been unreadable for a while.
 */
internal const val CLUSTER_RADIUS_DP = 44f

internal const val MEMBER_SOURCE = "members"
internal const val MEMBER_LAYER = "members"

/** The ring around the marker being looked at: the fill, and the line round it. */
internal const val WIDTH_SOURCE = "width"
internal const val WIDTH_FILL_LAYER = "width-fill"
internal const val WIDTH_LINE_LAYER = "width-line"

/**
 * How much of the marker's colour the fill keeps.
 *
 * Twelve percent is enough to see where the circle ends and light enough to
 * read the streets through it, which is the whole point of drawing it on the
 * map rather than beside it.
 */
private const val WIDTH_FILL_OPACITY = 0.12f

/** The line round the ring, in screen pixels rather than in metres. */
private const val WIDTH_LINE_WIDTH = 1.5f

/** The layer the tiles are drawn in, named in the style below. */
internal const val TILE_LAYER = "osm"

/** Half a turn of the colour wheel, which puts back what inverting took. */
internal const val NIGHT_HUE_TURN = 180f

/** Colours at night: a little of both taken out, so the map does not glare. */
internal const val NIGHT_SATURATION = -0.35f
internal const val NIGHT_CONTRAST = -0.15f

/** The name of the drawing a point wears, and which positions are standing at it. */
internal const val ICON_PROPERTY = "icon"
internal const val MEMBERS_PROPERTY = "members"

/** 1 for the pin of the member whose card is open, 0 for the rest: drawn last, on top. */
internal const val ON_TOP_PROPERTY = "onTop"

/** What the ids of a group are joined with; a position id has no comma in it. */
internal const val MEMBERS_SEPARATOR = ","

/**
 * A style with nothing in it but the tiles, the same document the page loads.
 *
 * The library is built for vector tiles and takes a style rather than a tile
 * source, and stating the background once for both clients is what makes a
 * position look the same wherever it is looked at. The link in the credit is
 * quoted with apostrophes to keep the text readable here; the dialogue that
 * shows it reads either form.
 */
internal val STYLE_JSON =
    """
    {
      "version": 8,
      "sources": {
        "osm": {
          "type": "raster",
          "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
          "tileSize": 256,
          "maxzoom": 19,
          "attribution": "© <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors"
        }
      },
      "layers": [{ "id": "osm", "type": "raster", "source": "osm" }]
    }
    """.trimIndent()

/**
 * A request to look at one member, rather than a statement of what is on
 * screen.
 *
 * The counter is what makes a second tap on the same member a change: after
 * the first one the map may have been dragged away, and the member expects the
 * button to bring it back.
 */
data class MapFocus(val positionId: String, val request: Int)

/**
 * The members on an OpenStreetMap background.
 *
 * A view rather than a Compose map: the drawing is the library's, and the only
 * thing that crosses the boundary is the list of points.
 */
@Composable
fun MemberMap(
    positions: List<MemberPosition>,
    /** What to write on each marker, by position id; the screen works the fallbacks out. */
    names: Map<String, String>,
    /** The account pictures, by member id; a member without one is drawn with their initial. */
    faces: Map<String, Bitmap>,
    ownMemberId: String,
    now: Instant,
    /** The phones the group is looking for right now, by their `{uid}.{deviceId}`. */
    soughtPhones: Set<String> = emptySet(),
    /** Whether the screen around the map is dark, which the tiles follow. */
    dark: Boolean,
    /** What is written above a pin that stands for several members at once. */
    groupLabel: (Int) -> String,
    /** The member the panel asked to look at, if one has been asked for. */
    focus: MapFocus?,
    /**
     * The position whose card is open, whose width is drawn as a circle.
     *
     * One at a time rather than all of them: four circles of a kilometre and a
     * half cover the map in each other, and the question a circle answers is
     * asked about the member being looked at.
     */
    selected: String?,
    /**
     * What the camera does on its own, between the taps that ask it to look
     * somewhere in particular. See [FollowMode].
     *
     * A parameter of the composable rather than a field set on [MapHolder]
     * directly, for the same reason [selected] is one: it is what makes a
     * change from the follow button reach the view at all, by giving
     * [AndroidView]'s update block something of its own to have changed.
     *
     * A gesture that pauses this is a different matter, and is not a
     * parameter here. It is read off the map's own camera, through
     * [MapLibreMap.addOnCameraMoveStartedListener], which only [MapHolder]
     * is in a position to hear, and the camera is moved again by a timer of
     * the holder's own when the pause wears off. What leaves the map is only
     * the fact that it is paused, through [onPausedChange], for the button.
     */
    followMode: FollowMode,
    /** The position [FollowMode.ONE] centres on; whose card was last opened. */
    followTarget: String?,
    /** Called with the position whose marker was tapped. */
    onSelect: (String) -> Unit,
    /** Called with every position under a marker that stands for more than one. */
    onSelectGroup: (List<String>) -> Unit,
    /** Called when a tap lands on the map itself rather than on a marker. */
    onDismiss: () -> Unit,
    /** Called with true when a hand pauses the following, and false when the pause ends. */
    onPausedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val holder = remember { MapHolder(createMapView(context)) }
    // Read here rather than inside the holder, which draws bitmaps and does
    // not reach the resources: the word is the same for every marker, so one
    // reading of it a composition serves the whole map.
    holder.soughtMark = stringResource(R.string.search_mark)

    // Taken on every composition rather than captured once: the map keeps the
    // listener it was handed, and the first one would go on calling whatever
    // the screen wanted at that moment.
    holder.onSelect = onSelect
    holder.onSelectGroup = onSelectGroup
    holder.onDismiss = onDismiss
    holder.onPausedChange = onPausedChange
    holder.groupLabel = groupLabel

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                // The drawing thread, the tile cache and the connectivity
                // watcher hang off these; without the pairs they keep going
                // behind a screen nobody is looking at.
                when (event) {
                    Lifecycle.Event.ON_START -> holder.view.onStart()
                    Lifecycle.Event.ON_RESUME -> holder.view.onResume()
                    Lifecycle.Event.ON_PAUSE -> holder.view.onPause()
                    Lifecycle.Event.ON_STOP -> holder.view.onStop()
                    Lifecycle.Event.ON_DESTROY -> holder.destroy()
                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // The screen can be left without the activity going anywhere, and
            // the native half of the map is not freed by the collector.
            holder.destroy()
        }
    }

    // What the markers show is what the phone last heard while the database
    // is silent: grey, the way a phone long out of touch is drawn.
    val linkLost = LocalLink.current.lost

    AndroidView(factory = { holder.view }, modifier = modifier) { _ ->
        holder.show(
            positions, names, faces, ownMemberId, now, soughtPhones, dark, selected, followMode,
            followTarget, linkLost,
        )
        holder.lookAt(focus)
    }
}

private fun createMapView(context: Context): MapView {
    // Loads the native half of the library. Every other call into it, the view
    // included, has to come after this one.
    MapLibre.getInstance(context)

    return MapView(context).apply {
        // Nothing is restored: where the map opens comes from the members
        // rather than from what was on screen the time before.
        onCreate(null)
    }
}

internal fun memberLayer(): SymbolLayer = SymbolLayer(MEMBER_LAYER, MEMBER_SOURCE)
    .withProperties(
        // One layer for everybody, and each point names the drawing it wears.
        PropertyFactory.iconImage(Expression.get(ICON_PROPERTY)),
        // Two members standing together is the case this screen exists for,
        // and the default would drop one of the two labels to keep the map
        // tidy — which is to say it would hide a person.
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
        // The drawing hangs by its bottom edge, and the bottom edge is the
        // point of the pin: the member is where the point is, and everything
        // else in the drawing stands above them.
        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
        // The member whose card is open has a pin of their own, and another
        // member in the same spot would otherwise cover the pin the circle is
        // drawn for.
        PropertyFactory.symbolSortKey(Expression.get(ON_TOP_PROPERTY)),
    )

/**
 * The inside of the circle: the member's own colour, thinned to a wash.
 *
 * The member's colour rather than one colour for every circle, because the
 * circle belongs to a marker and has to be readable as that marker's when two
 * members are near each other.
 */
internal fun widthFillLayer(): FillLayer = FillLayer(WIDTH_FILL_LAYER, WIDTH_SOURCE)
    .withProperties(PropertyFactory.fillOpacity(WIDTH_FILL_OPACITY))

/** The edge of the circle, which is what says where the width stops. */
internal fun widthLineLayer(): LineLayer = LineLayer(WIDTH_LINE_LAYER, WIDTH_SOURCE)
    .withProperties(PropertyFactory.lineWidth(WIDTH_LINE_WIDTH))
