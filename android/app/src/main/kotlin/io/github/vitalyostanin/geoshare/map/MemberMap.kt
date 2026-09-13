package io.github.vitalyostanin.geoshare.map

import android.content.Context
import android.graphics.Bitmap
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
import org.maplibre.android.style.layers.RasterLayer
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

/**
 * How close two pins have to come before they are drawn as one.
 *
 * A little less than the width of a pin: at that distance the heads already
 * overlap, and the labels above them have been unreadable for a while.
 */
private const val CLUSTER_RADIUS_DP = 44f

private const val MEMBER_SOURCE = "members"
private const val MEMBER_LAYER = "members"

/** The layer the tiles are drawn in, named in the style below. */
private const val TILE_LAYER = "osm"

/** Half a turn of the colour wheel, which puts back what inverting took. */
private const val NIGHT_HUE_TURN = 180f

/** Colours at night: a little of both taken out, so the map does not glare. */
private const val NIGHT_SATURATION = -0.35f
private const val NIGHT_CONTRAST = -0.15f

/** The name of the drawing a point wears, and which positions are standing at it. */
private const val ICON_PROPERTY = "icon"
private const val MEMBERS_PROPERTY = "members"

/** What the ids of a group are joined with; a position id has no comma in it. */
private const val MEMBERS_SEPARATOR = ","

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
    /** Whether the screen around the map is dark, which the tiles follow. */
    dark: Boolean,
    /** What is written above a pin that stands for several members at once. */
    groupLabel: (Int) -> String,
    /** The member the panel asked to look at, if one has been asked for. */
    focus: MapFocus?,
    /** Called with the position whose marker was tapped. */
    onSelect: (String) -> Unit,
    /** Called with every position under a marker that stands for more than one. */
    onSelectGroup: (List<String>) -> Unit,
    /** Called when a tap lands on the map itself rather than on a marker. */
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val holder = remember { MapHolder(createMapView(context)) }

    // Taken on every composition rather than captured once: the map keeps the
    // listener it was handed, and the first one would go on calling whatever
    // the screen wanted at that moment.
    holder.onSelect = onSelect
    holder.onSelectGroup = onSelectGroup
    holder.onDismiss = onDismiss
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

    AndroidView(factory = { holder.view }, modifier = modifier) { _ ->
        holder.show(positions, names, faces, ownMemberId, now, dark)
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
        // The drawing hangs by its bottom edge, and the bottom edge is the
        // point of the pin: the member is where the point is, and everything
        // else in the drawing stands above them.
        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
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
    private var faces: Map<String, Bitmap> = emptyMap()
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

    /** Whether the tiles are being drawn for a dark screen. */
    private var dark = false

    var onSelect: (String) -> Unit = {}

    var onSelectGroup: (List<String>) -> Unit = {}

    var onDismiss: () -> Unit = {}

    var groupLabel: (Int) -> String = { count -> count.toString() }

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

            // Which members cover each other is a question about the screen,
            // so the answer changes with every zoom: a group that was one pin
            // has to come apart once the map is close enough to tell them
            // apart. Redrawn when the map stops rather than while it moves,
            // because regrouping under a moving finger reads as flicker.
            ready.addOnCameraIdleListener { draw() }

            ready.setStyle(Style.Builder().fromJson(STYLE_JSON)) { loaded ->
                loaded.addSource(GeoJsonSource(MEMBER_SOURCE))
                loaded.addLayer(memberLayer())

                style = loaded
                icons.clear()
                paintTiles()

                // Positions can arrive before the style does, and the screen
                // does not ask again until the next one.
                draw()
            }
        }
    }

    fun show(
        positions: List<MemberPosition>,
        names: Map<String, String>,
        faces: Map<String, Bitmap>,
        ownMemberId: String,
        now: Instant,
        dark: Boolean,
    ) {
        members = positions
        this.names = names
        this.faces = faces
        this.ownMemberId = ownMemberId
        this.now = now

        if (dark != this.dark) {
            this.dark = dark
            paintTiles()
        }

        draw()
    }

    /**
     * The tiles as day or as night.
     *
     * The tiles themselves are drawn light on somebody else's server, and the
     * page solves this by turning its canvas over. Here the same is asked of
     * the drawing itself: the brightness range is given back to front, which
     * turns light streets into dark ones, and the hue is put back where it was
     * so that water stays blue rather than becoming orange. Only the tile
     * layer is touched — the markers are a layer of their own, and a member's
     * face is not to be turned inside out.
     */
    private fun paintTiles() {
        val loaded = style ?: return

        loaded.getLayerAs<RasterLayer>(TILE_LAYER)?.setProperties(
            PropertyFactory.rasterBrightnessMin(if (dark) 1f else 0f),
            PropertyFactory.rasterBrightnessMax(if (dark) 0f else 1f),
            PropertyFactory.rasterHueRotate(if (dark) NIGHT_HUE_TURN else 0f),
            PropertyFactory.rasterSaturation(if (dark) NIGHT_SATURATION else 0f),
            PropertyFactory.rasterContrast(if (dark) NIGHT_CONTRAST else 0f),
        )
    }

    fun lookAt(focus: MapFocus?) {
        if (focus == null || focus.request == focused) {
            return
        }

        val ready = map ?: return
        val member = members.firstOrNull { it.positionId == focus.positionId } ?: return

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
        val ready = map ?: return

        val density = view.context.resources.displayMetrics.density
        val placed =
            members.map { position ->
                val screen =
                    ready.projection.toScreenLocation(
                        LatLng(position.latitude, position.longitude),
                    )

                PlacedMember(position.positionId, screen.x.toDouble(), screen.y.toDouble())
            }

        val positions = members.associateBy { it.positionId }
        val radius = (CLUSTER_RADIUS_DP * density).toDouble()

        val features =
            clusterMembers(placed, radius).mapNotNull { cluster ->
                val group = cluster.ids.mapNotNull { positions[it] }

                if (group.isEmpty()) {
                    return@mapNotNull null
                }

                Feature
                    .fromGeometry(
                        Point.fromLngLat(centreLongitude(group), centreLatitude(group)),
                    )
                    .apply {
                        addStringProperty(ICON_PROPERTY, icon(loaded, group))
                        addStringProperty(
                            MEMBERS_PROPERTY,
                            group.joinToString(MEMBERS_SEPARATOR) { it.positionId },
                        )
                    }
            }

        loaded
            .getSourceAs<GeoJsonSource>(MEMBER_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(features))

        // A drawing is made for a particular set of members standing together,
        // with their label and their ages in its name, so the set of names
        // grows with every zoom and every minute. The ones this pass did not
        // ask for are what the style would otherwise hold on to for the rest
        // of the session, a bitmap each.
        val used = features.mapNotNull { it.getStringProperty(ICON_PROPERTY) }.toSet()

        icons.minus(used).forEach { name ->
            loaded.removeImage(name)
            icons.remove(name)
        }

        centre()
    }

    /**
     * The drawing one marker wears, handed to the style the first time it is
     * asked for.
     *
     * Kept by name rather than redrawn every tick: the ages on the labels move
     * slowly, and most ticks want exactly the drawings the style already has.
     */
    private fun icon(loaded: Style, group: List<MemberPosition>): String {
        val name = iconName(group)

        if (icons.add(name)) {
            loaded.addImage(name, drawing(group))
        }

        return name
    }

    private fun drawing(group: List<MemberPosition>): Bitmap {
        val single = group.singleOrNull()

        if (single != null) {
            return markerIcon(
                context = view.context,
                name = label(single),
                colour = memberColour(single.memberId),
                stale = isStale(single.knownAt, now),
                face = faces[single.memberId],
            )
        }

        val shown = group.take(facesShown(group.size))
        val rest = group.size - shown.size

        return groupIcon(
            context = view.context,
            label = groupLabel(group.size),
            faces =
            shown.map { position ->
                PinFace(
                    picture = faces[position.memberId],
                    initial = memberInitial(label(position)),
                    colour = memberColour(position.memberId),
                    stale = isStale(position.knownAt, now),
                )
            },
            // A plus and a number, which is the same in every language the
            // labels above the pins could be written in.
            counter = if (rest > 0) "+$rest" else null,
        )
    }

    /**
     * The name a drawing is kept under.
     *
     * Everything the drawing depends on is in the name, so a group that gains
     * a member, or a member whose label, colour or picture changes, gets a new
     * drawing rather than the old one under a name that no longer describes
     * it. A picture appears as whether there is one: a member has at most one
     * at a time, and it arrives once.
     */
    private fun iconName(group: List<MemberPosition>): String = group.joinToString(
        separator = MEMBERS_SEPARATOR,
        prefix = if (group.size == 1) "member:" else "group:",
    ) { position ->
        val face = faces[position.memberId] != null

        "${position.positionId}:${label(position)}:${isStale(position.knownAt, now)}:$face"
    }

    private fun label(position: MemberPosition): String =
        names[position.positionId] ?: shortName(position.memberId)

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

        val group =
            touched
                .firstNotNullOfOrNull { it.getStringProperty(MEMBERS_PROPERTY) }
                ?.split(MEMBERS_SEPARATOR)
                ?.filter { it.isNotEmpty() }
                .orEmpty()

        if (group.isEmpty()) {
            // Tapping the map away from everybody is how a map is told that
            // whatever was open is no longer being looked at. Dragging the map
            // is not a tap and does not reach this, so a card survives being
            // moved out from under.
            onDismiss()

            return false
        }

        val single = group.singleOrNull()

        if (single != null) {
            onSelect(single)
        } else {
            // A marker standing for several members cannot open a card: which
            // of them was meant is a question only the viewer can answer.
            onSelectGroup(group)
        }

        return true
    }
}

/**
 * Where a marker for several members is put: the middle of the people under it.
 *
 * The middle rather than one of them, because the marker stands for all of
 * them; putting it on the first id would move the whole group whenever that
 * member walked off.
 */
private fun centreLatitude(group: List<MemberPosition>): Double =
    group.sumOf { it.latitude } / group.size

/**
 * The same middle, along the other axis.
 *
 * Longitudes are averaged as offsets from the first member rather than
 * outright: on a map of the whole world two members either side of the date
 * line are a few pixels apart, and their longitudes are half a globe apart as
 * numbers.
 */
private fun centreLongitude(group: List<MemberPosition>): Double {
    val first = group.first().longitude
    val offset = group.sumOf { wrapDegrees(it.longitude - first) } / group.size

    return wrapDegrees(first + offset)
}
