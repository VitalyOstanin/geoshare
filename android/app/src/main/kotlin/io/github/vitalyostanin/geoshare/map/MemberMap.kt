package io.github.vitalyostanin.geoshare.map

import android.content.Context
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.time.Instant

/** Close enough to tell which street somebody is on, without hunting for them. */
private const val START_ZOOM = 15.0

/** How far around a tap a marker still counts as the one that was meant. */
private const val TOUCH_RADIUS_DP = 18f

private const val MEMBER_SOURCE = "members"
private const val MEMBER_LAYER = "members"

/** The name of the drawing a point wears, and who the point is. */
private const val ICON_PROPERTY = "icon"
private const val MEMBER_PROPERTY = "member"

/**
 * A style with nothing in it but the tiles, the same document the page loads.
 *
 * The library is built for vector tiles and takes a style rather than a tile
 * source, and stating the background once for both clients is what makes a
 * position look the same wherever it is looked at. The link in the credit is
 * quoted with apostrophes to keep the text readable here; the dialogue that
 * shows it reads either form.
 */
private val STYLE_JSON =
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
data class MapFocus(val memberId: String, val request: Int)

/**
 * The members on an OpenStreetMap background.
 *
 * A view rather than a Compose map: the drawing is the library's, and the only
 * thing that crosses the boundary is the list of points.
 */
@Composable
fun MemberMap(
    positions: List<MemberPosition>,
    /** What to write on each marker, by member id; the screen works the fallbacks out. */
    names: Map<String, String>,
    ownMemberId: String,
    now: Instant,
    /** The member the panel asked to look at, if one has been asked for. */
    focus: MapFocus?,
    /** Called with the member whose marker was tapped. */
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val holder = remember { MapHolder(createMapView(context)) }

    // Taken on every composition rather than captured once: the map keeps the
    // listener it was handed, and the first one would go on calling whatever
    // the screen wanted at that moment.
    holder.onSelect = onSelect

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

    AndroidView(factory = { holder.view }, modifier = modifier) { _ ->
        holder.show(positions, names, ownMemberId, now)
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

private fun memberLayer(): SymbolLayer = SymbolLayer(MEMBER_LAYER, MEMBER_SOURCE)
    .withProperties(
        // One layer for everybody, and each point names the drawing it wears.
        PropertyFactory.iconImage(Expression.get(ICON_PROPERTY)),
        // Two members standing together is the case this screen exists for,
        // and the default would drop one of the two labels to keep the map
        // tidy — which is to say it would hide a person.
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
        // The drawing hangs by its bottom edge and is then pushed down by the
        // distance from that edge up to the dot, which leaves the dot on the
        // position and the label above it.
        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
        PropertyFactory.iconOffset(arrayOf(0f, MARKER_ANCHOR_DP)),
    )

/**
 * What the composition and the map's own callbacks both have to reach.
 *
 * The map and its style arrive after the view does, and a tap arrives at any
 * time, so what the screen last asked for is kept here rather than captured
 * inside one of them.
 */
private class MapHolder(val view: MapView) {
    private var map: MapLibreMap? = null

    /** Held from the callback that loaded it: the style refuses every call before that. */
    private var style: Style? = null

    /** The drawings already handed over, so that a tick does not copy them in again. */
    private val icons = mutableSetOf<String>()

    private var members: List<MemberPosition> = emptyList()
    private var names: Map<String, String> = emptyMap()
    private var ownMemberId: String = ""
    private var now: Instant = Instant.EPOCH

    /** The last request acted on, so that an unchanged one does not move the map again. */
    private var focused = 0

    /**
     * Whether the map has been put somewhere once.
     *
     * The first position to arrive decides where it opens. After that the
     * member is in charge of what they are looking at, and a list that updates
     * every few seconds must not drag the view back.
     */
    private var centred = false

    private var destroyed = false

    var onSelect: (String) -> Unit = {}

    init {
        view.getMapAsync { ready ->
            map = ready

            ready.uiSettings.apply {
                // Nothing here is drawn in three dimensions, and a map that
                // turns under a finger is a map the viewer has to straighten.
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                // The credit stays; the library's own badge is a second thing
                // in a corner the account button already reaches into.
                isLogoEnabled = false
            }

            ready.addOnMapClickListener { point -> select(ready, point) }

            ready.setStyle(Style.Builder().fromJson(STYLE_JSON)) { loaded ->
                loaded.addSource(GeoJsonSource(MEMBER_SOURCE))
                loaded.addLayer(memberLayer())

                style = loaded
                icons.clear()

                // Positions can arrive before the style does, and the screen
                // does not ask again until the next one.
                draw()
            }
        }
    }

    fun show(
        positions: List<MemberPosition>,
        names: Map<String, String>,
        ownMemberId: String,
        now: Instant,
    ) {
        members = positions
        this.names = names
        this.ownMemberId = ownMemberId
        this.now = now

        draw()
    }

    fun lookAt(focus: MapFocus?) {
        if (focus == null || focus.request == focused) {
            return
        }

        val ready = map ?: return
        val member = members.firstOrNull { it.memberId == focus.memberId } ?: return

        focused = focus.request

        // Animated rather than set, because the member asked to be taken
        // somewhere and the way there says how far it was.
        ready.animateCamera(
            CameraUpdateFactory.newLatLng(LatLng(member.latitude, member.longitude)),
        )
    }

    fun destroy() {
        if (destroyed) {
            return
        }

        destroyed = true

        view.onStop()
        view.onDestroy()
    }

    private fun draw() {
        val loaded = style ?: return

        val features =
            members.map { position ->
                val name = names[position.memberId] ?: shortName(position.memberId)
                val stale = isStale(position.knownAt, now)
                val icon = iconName(position.memberId, name, stale)

                if (icons.add(icon)) {
                    loaded.addImage(
                        icon,
                        markerIcon(
                            context = view.context,
                            name = name,
                            colour = memberColour(position.memberId),
                            stale = stale,
                        ),
                    )
                }

                Feature
                    .fromGeometry(Point.fromLngLat(position.longitude, position.latitude))
                    .apply {
                        addStringProperty(ICON_PROPERTY, icon)
                        addStringProperty(MEMBER_PROPERTY, position.memberId)
                    }
            }

        loaded
            .getSourceAs<GeoJsonSource>(MEMBER_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(features))

        centre()
    }

    private fun centre() {
        val ready = map ?: return

        if (centred) {
            return
        }

        val start =
            members.firstOrNull { it.memberId == ownMemberId } ?: members.firstOrNull() ?: return

        centred = true

        ready.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(start.latitude, start.longitude),
                START_ZOOM,
            ),
        )
    }

    /**
     * Answers a tap by asking the map what is drawn under the finger.
     *
     * One question to the map rather than a listener on every marker: the
     * markers are a layer of the style here and not objects of their own, and
     * a finger covers more than the single pixel a tap reports.
     */
    private fun select(ready: MapLibreMap, point: LatLng): Boolean {
        val screen = ready.projection.toScreenLocation(point)
        val radius = TOUCH_RADIUS_DP * view.context.resources.displayMetrics.density

        val touched =
            ready.queryRenderedFeatures(
                RectF(
                    screen.x - radius,
                    screen.y - radius,
                    screen.x + radius,
                    screen.y + radius,
                ),
                MEMBER_LAYER,
            )

        val member =
            touched
                .firstNotNullOfOrNull { it.getStringProperty(MEMBER_PROPERTY) }
                ?: return false

        onSelect(member)

        return true
    }
}

/**
 * The name a drawing is kept under.
 *
 * Everything the drawing depends on is in the name, so a member whose label or
 * colour changes gets a new drawing rather than the old one under a name that
 * no longer describes it.
 */
private fun iconName(memberId: String, name: String, stale: Boolean): String =
    "member:$memberId:$name:$stale"
