package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.LEFT_OUT_BY_SOURCE
import io.github.vitalyostanin.geoshare.collection.SkySays
import io.github.vitalyostanin.geoshare.collection.Trail
import io.github.vitalyostanin.geoshare.log.AppLog
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * What becomes of a fix between the provider and the group, driven without
 * the service: the paths that used to be reachable only by running it.
 */
class FixSieveTest {
    private var loggedBefore = true

    private val writer = mockk<EveryGroup>()
    private val radio = mockk<RadioWatch>()
    private val opinion = mockk<NetworkOpinion>()
    private val jumps = mockk<JumpFilterSetting>()
    private val sources = mockk<SourcesSetting>()
    private val silence = Silence()
    private val fenceCentre = MutableStateFlow<Fix?>(null)
    private val kept = mutableListOf<Trail>()

    private val sieve =
        FixSieve(
            writer = writer,
            radio = radio,
            opinion = opinion,
            carried = BeingCarried(sensorsTell = false),
            jumps = jumps,
            sources = sources,
            silence = silence,
            fenceCentre = fenceCentre,
            noteFix = { _, _, _, _ -> },
            satellites = { null },
            charging = { false },
            batteryPercent = { BATTERY },
            keepTrail = { kept += it },
            awake = { Duration.ofHours(1) },
        )

    @Before
    fun setUp() {
        loggedBefore = AppLog.toSystemLog
        AppLog.toSystemLog = false

        // Jumps are let through, so that what is decided below is the source
        // and the write rather than whichever rule a lone fix might trip.
        every { jumps.enabled } returns false
        every { jumps.takeForgetRequest() } returns false
        every { sources.uses(any()) } returns true
        every { opinion.remember(any()) } just Runs
        every { opinion.said } returns null
        every { radio.restingOn(any()) } returns emptyMap()
        every { radio.skySays() } returns SkySays()
        coEvery { writer.write(any(), any(), any(), any(), any(), any()) } returns listOf(GROUP)
        coEvery { writer.noteLeftOut(any(), any(), any()) } returns listOf(GROUP)
    }

    @After
    fun restoreTheLog() {
        AppLog.toSystemLog = loggedBefore
    }

    // DEV-03: the first position written is the one the fence is drawn
    // around, and the one the device then counts as having been.
    @Test
    fun `a written fix becomes the anchor, the fence centre and part of the trail`() = runBlocking {
        val fix = atHome()

        val wrote = sieve.writeIfWorth(fix, CollectionMode.Standby, always = true)

        assertTrue(wrote)
        assertSame(fix, sieve.lastFix)
        assertSame(fix, fenceCentre.value)
        assertEquals(listOf(fix), kept.last().fixes)
        assertTrue(silence.wroteAt != null)
        coVerify(exactly = 1) {
            writer.write(fix, CollectionMode.Standby, BATTERY, false, any(), false)
        }
    }

    // DEV-04: a source switched off in the settings moves no marker, and the
    // group is told why answers stop arriving.
    @Test
    fun `a fix from a source switched off is told as left out and never written`() = runBlocking {
        every { sources.uses(any()) } returns false

        val wrote = sieve.writeIfWorth(atHome(), CollectionMode.Standby, always = true)

        assertFalse(wrote)
        assertNull(sieve.lastFix)
        assertNull(fenceCentre.value)
        coVerify(exactly = 0) { writer.write(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) {
            writer.noteLeftOut(1, match { it.startsWith("$LEFT_OUT_BY_SOURCE:") }, any())
        }
    }

    // DEV-03: an idle device says its last position again, as it was, so
    // that it does not go grey; with nothing known it says nothing.
    @Test
    fun `the last position is said again as it was, and nothing before there is one`() =
        runBlocking {
            assertFalse(sieve.sayAgain(CollectionMode.Standby))
            coVerify(exactly = 0) { writer.write(any(), any(), any(), any(), any(), any()) }

            val fix = atHome()
            sieve.writeIfWorth(fix, CollectionMode.Standby, always = true)

            assertTrue(sieve.sayAgain(CollectionMode.Standby))
            coVerify(exactly = 2) {
                writer.write(fix, CollectionMode.Standby, BATTERY, false, any(), false)
            }
        }

    private fun atHome(): Fix = Fix(
        latitude = 61.03268,
        longitude = 4.40300,
        takenAt = Instant.now(),
        accuracyMetres = 8.0,
    )

    private companion object {
        const val GROUP = "first"
        const val BATTERY = 80
    }
}
