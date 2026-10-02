package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.remember
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.time.Instant

/**
 * What the composition and the map's own callbacks both have to reach.
 *
 * The map and its style arrive after the view does, and a tap arrives at any
 * time, so what the screen last asked for is kept here rather than captured
 * inside one of them.
 */
internal class MapHolder(val view: MapView) {
    private var map: MapLibreMap? = null

    /** Held from the callback that loaded it: the style refuses every call before that. */
    private var style: Style? = null

    /** The drawings already handed over, so that a tick does not copy them in again. */
    private val icons = mutableSetOf<String>()

    /** The tallest and the widest drawing handed over, in pixels, for [followAll]'s padding. */
    private var tallestIcon = 0
    private var widestIcon = 0

    private var members: List<MemberPosition> = emptyList()
    private var names: Map<String, String> = emptyMap()
    private var faces: Map<String, Bitmap> = emptyMap()
    private var ownMemberId: String = ""
    private var now: Instant = Instant.EPOCH

    /** The phones the group is looking for right now, by their `{uid}.{deviceId}`. */
    private var soughtPhones: Set<String> = emptySet()
    private var linkLost = false

    /** The word a sought member's pin carries, read once from the resources. */
    var soughtMark: String = ""

    /** Whose width is drawn as a circle, if anybody's card is open. */
    private var selected: String? = null

    /** What the camera does on its own; see [FollowMode]. */
    private var followMode: FollowMode = FollowMode.NONE

    /** The position [followMode] centres on in [FollowMode.ONE]. */
    private var followTarget: String? = null

    /**
     * When a hand last dragged or pinched the map, or null before that has
     * ever happened.
     *
     * Set from [MapLibreMap.addOnCameraMoveStartedListener] rather than
     * carried in from the composable: a gesture is a fact about the map's own
     * camera, which only this class touches, and turning it into a parameter
     * that travels out to [MemberMap] and back would buy nothing a listener
     * here does not already have. Read against the wall clock rather than
     * against [now] -- [now] only ticks with a fresh batch of positions or the
     * age clock's own half minute, and a pause timed against it would run
     * short or long by however stale that tick happens to be.
     */
    private var pausedAt: Instant? = null

    /** Told whether a hand is holding the following still, for the button to show. */
    var onPausedChange: (Boolean) -> Unit = {}

    /** Moves the camera once a hand's pause has worn off. */
    private val pauseEnds =
        Runnable {
            onPausedChange(false)
            map?.takeIf { !destroyed }?.let { followCamera(it) }
        }

    /** The rectangle [FollowMode.ALL] last fit the camera to, for [boundsAreClose]. */
    private var lastAppliedBounds: MapBounds? = null

    /** The point [FollowMode.ONE] last centred the camera on. */
    private var followedOnePoint: LatLng? = null

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

            ready.addOnCameraMoveStartedListener { reason ->
                // The other two reasons are this class's own animateCamera
                // calls -- lookAt's and followCamera's -- and are not a
                // request to be left alone the way a hand on the screen is.
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    pausedAt = Instant.now()
                    // Nothing else is bound to move the camera when the pause
                    // wears off: the next batch of positions may be half an
                    // hour away for a member standing still, and the age
                    // clock ticks only every half minute. So the end of the
                    // pause is an event of its own.
                    view.removeCallbacks(pauseEnds)
                    view.postDelayed(pauseEnds, PAUSE_AFTER_A_GESTURE.toMillis())
                    onPausedChange(true)
                    // The hand has taken the camera off the place these two
                    // remember putting it. Without forgetting them, a
                    // household that has not moved hands back the same
                    // rectangle and the same point once the pause wears off,
                    // both are found to be where the camera already is, and
                    // the map stays where the hand left it with the mode
                    // still on.
                    forgetWhereTheCameraWas()
                }
            }

            // Which members cover each other is a question about the screen,
            // so the answer changes with every zoom: a group that was one pin
            // has to come apart once the map is close enough to tell them
            // apart. Redrawn when the map stops rather than while it moves,
            // because regrouping under a moving finger reads as flicker.
            ready.addOnCameraIdleListener { draw() }

            ready.setStyle(Style.Builder().fromJson(STYLE_JSON)) { loaded ->
                loaded.addSource(GeoJsonSource(MEMBER_SOURCE))
                loaded.addLayer(memberLayer())

                // Under the markers, so that a circle drawn around a member
                // never covers the member it is about — nor the face of
                // somebody standing inside it.
                loaded.addSource(GeoJsonSource(WIDTH_SOURCE))
                loaded.addLayerBelow(widthFillLayer(), MEMBER_LAYER)
                loaded.addLayerBelow(widthLineLayer(), MEMBER_LAYER)

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
        soughtPhones: Set<String>,
        dark: Boolean,
        selected: String?,
        followMode: FollowMode,
        followTarget: String?,
        linkLost: Boolean,
    ) {
        members = positions
        this.linkLost = linkLost
        this.names = names
        this.faces = faces
        this.ownMemberId = ownMemberId
        this.now = now
        this.soughtPhones = soughtPhones
        this.selected = selected

        // A press on the follow button is somebody asking the camera to move
        // now: it lifts a pause a hand left behind, and forgets where the
        // camera was put last, so that a mode entered afresh fits the view
        // again instead of finding it near enough to leave alone.
        if (followMode != this.followMode || followTarget != this.followTarget) {
            pausedAt = null
            view.removeCallbacks(pauseEnds)
            onPausedChange(false)
            forgetWhereTheCameraWas()
        }

        this.followMode = followMode
        this.followTarget = followTarget

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
        view.removeCallbacks(pauseEnds)

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
            clusterMembers(placed, radius, alone = selected).mapNotNull { cluster ->
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

        drawWidth()
        centre()
        followCamera(ready)
    }

    /** Drops what the follow modes remember about where they put the camera. */
    private fun forgetWhereTheCameraWas() {
        lastAppliedBounds = null
        followedOnePoint = null
    }

    /**
     * Moves the camera the way [followMode] asks, once the rest of a draw is
     * done.
     *
     * Skipped outright in [FollowMode.NONE] -- the camera stays wherever a
     * hand or [lookAt] left it -- and skipped while [pausedAt] still holds,
     * so that a drag is not fought straight back to where it was.
     */
    private fun followCamera(ready: MapLibreMap) {
        if (followMode == FollowMode.NONE || isPaused(pausedAt, Instant.now())) {
            return
        }

        when (followMode) {
            FollowMode.ONE -> followOne(ready)
            FollowMode.ALL -> followAll(ready)
            FollowMode.NONE -> Unit
        }
    }

    /**
     * Keeps the camera centred on [followTarget], the zoom left as it is.
     *
     * Left alone once the target's position has dropped out of [members] --
     * a phone that stopped sending is not a place to snap the camera to --
     * and left alone once the camera is already there, so that a batch of
     * positions that did not move this member does not restart the
     * animation.
     */
    private fun followOne(ready: MapLibreMap) {
        val position = targetPosition(members, followTarget) ?: return
        val point = LatLng(position.latitude, position.longitude)

        if (point == followedOnePoint) {
            return
        }

        followedOnePoint = point
        ready.animateCamera(CameraUpdateFactory.newLatLng(point))
    }

    /**
     * Fits every member in [members] inside the view, zoomed no closer than
     * [MAX_ALL_MODE_ZOOM].
     *
     * Checked against [lastAppliedBounds] with [boundsAreClose] first, or a
     * household standing still would hand the camera a fresh, slightly
     * different rectangle -- and a move for that -- on every batch of
     * positions.
     */
    private fun followAll(ready: MapLibreMap) {
        val bounds = boundsOf(members) ?: return

        if (lastAppliedBounds?.let { boundsAreClose(it, bounds) } == true) {
            return
        }

        lastAppliedBounds = bounds

        val target =
            LatLngBounds.Builder()
                .include(LatLng(bounds.south, bounds.west))
                .include(LatLng(bounds.north, bounds.east))
                .build()
        // The rectangle is fitted by the points of the pins, and a pin hangs
        // above its point with the name over it and reaches half its width to
        // either side. The same padding on all four sides left the member at
        // the top with the head of their pin past the edge of the screen.
        val density = view.context.resources.displayMetrics.density
        val padding = (ALL_MODE_PADDING_DP * density).toInt()
        val gap = (ALL_MODE_MARKER_GAP_DP * density).toInt()
        val top = maxOf(padding, tallestIcon + gap)
        val side = maxOf(padding, widestIcon / 2 + gap)
        // Left, top, right, bottom.
        val fitted =
            ready.getCameraForLatLngBounds(target, intArrayOf(side, top, side, padding))
                ?: return
        val zoom = fitted.zoom.coerceAtMost(MAX_ALL_MODE_ZOOM)
        val clamped = CameraPosition.Builder(fitted).zoom(zoom).build()

        ready.animateCamera(CameraUpdateFactory.newCameraPosition(clamped))
    }

    /**
     * The circle around the member being looked at, or nothing at all.
     *
     * Redrawn with everything else rather than only when the selection
     * changes: the position under the circle moves on its own, and a ring left
     * where the member used to be would be a claim about the wrong place.
     */
    private fun drawWidth() {
        val loaded = style ?: return

        val ring =
            members
                .firstOrNull { it.positionId == selected }
                ?.let { position ->
                    val metres = position.accuracyMetres ?: return@let null

                    accuracyRing(position.latitude, position.longitude, metres) to
                        memberColour(position.memberId)
                }

        loaded.getSourceAs<GeoJsonSource>(WIDTH_SOURCE)?.setGeoJson(
            ring
                ?.let { (points, _) ->
                    Feature.fromGeometry(
                        Polygon.fromLngLats(
                            listOf(points.map { (east, north) -> Point.fromLngLat(east, north) }),
                        ),
                    )
                }
                ?.let { FeatureCollection.fromFeature(it) }
                // An empty collection rather than a layer taken away: the
                // layers are put in once, and emptying the source is what
                // removes the circle when the card is closed.
                ?: FeatureCollection.fromFeatures(emptyList()),
        )

        val colour = ring?.second ?: return

        loaded.getLayerAs<FillLayer>(WIDTH_FILL_LAYER)?.setProperties(
            PropertyFactory.fillColor(colour),
        )
        loaded.getLayerAs<LineLayer>(WIDTH_LINE_LAYER)?.setProperties(
            PropertyFactory.lineColor(colour),
        )
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
            val drawn = drawing(group)

            tallestIcon = maxOf(tallestIcon, drawn.height)
            widestIcon = maxOf(widestIcon, drawn.width)
            loaded.addImage(name, drawn)
        }

        return name
    }

    private fun drawing(group: List<MemberPosition>): Bitmap {
        val single = group.singleOrNull()
        // A marker holding a phone looked for is ringed whoever else stands
        // under it: the ring is what the rest of the group is scanning the
        // map for, not a fact about one phone of a crowded pin.
        val sought =
            group.any { it.positionId in soughtPhones }.let { if (it) soughtMark else null }

        if (single != null) {
            return markerIcon(
                context = view.context,
                name = label(single),
                colour = memberColour(single.memberId),
                stale = greyed(single),
                face = faces[single.memberId],
                soughtMark = sought,
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
                    stale = greyed(position),
                )
            },
            // A plus and a number, which is the same in every language the
            // labels above the pins could be written in.
            counter = if (rest > 0) "+$rest" else null,
            soughtMark = sought,
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
        // Whether any phone under this marker is being looked for changes the
        // whole drawing -- the ring and the word above it -- so it goes in
        // the prefix rather than beside one member's own id.
        prefix =
        (if (group.size == 1) "member:" else "group:") +
            "${group.any { it.positionId in soughtPhones }}:",
    ) { position ->
        val face = faces[position.memberId] != null

        "${position.positionId}:${label(position)}:${greyed(position)}:$face"
    }

    /**
     * Grey for a phone long out of touch or in a false place, and for all of
     * them while the database is silent.
     */
    private fun greyed(position: MemberPosition): Boolean = linkLost || isGrey(position, now)

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
