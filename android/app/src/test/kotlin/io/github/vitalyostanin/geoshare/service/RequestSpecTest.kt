package io.github.vitalyostanin.geoshare.service

import com.google.android.gms.location.Priority
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class RequestSpecTest {
    @Test
    fun `the idle mode asks for nothing`() {
        assertNull(requestSpec(CollectionMode.Idle))
    }

    @Test
    fun `a viewer gets the accurate request without batching`() {
        val spec = requestSpec(CollectionMode.Viewing)!!

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, spec.priority)
        assertEquals(Duration.ofSeconds(30).toMillis(), spec.intervalMillis)
        assertEquals(0L, spec.maxDelayMillis)
    }

    @Test
    fun `the night mode asks for the cheapest fixes and batches them`() {
        val spec = requestSpec(CollectionMode.Night)!!

        assertEquals(Priority.PRIORITY_LOW_POWER, spec.priority)
        assertEquals(CollectionMode.Night.interval.toMillis(), spec.intervalMillis)
        assertEquals(CollectionMode.Night.maxUpdateDelay.toMillis(), spec.maxDelayMillis)
        // The numbers themselves belong to the mode and are checked there,
        // against the half hour after which a position is drawn as old; what
        // this asks is that the request carries them through unchanged.
    }

    @Test
    fun `standby and moving differ in how often, not in how accurately`() {
        val standby = requestSpec(CollectionMode.Standby)!!
        val moving = requestSpec(CollectionMode.Moving)!!

        assertEquals(Priority.PRIORITY_BALANCED_POWER_ACCURACY, standby.priority)
        assertEquals(standby.priority, moving.priority)
        assertEquals(Duration.ofMinutes(15).toMillis(), standby.intervalMillis)
        assertEquals(Duration.ofMinutes(5).toMillis(), moving.intervalMillis)
    }
}
