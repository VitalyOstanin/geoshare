package io.github.vitalyostanin.geoshare.map

import android.os.SystemClock
import io.github.vitalyostanin.geoshare.collection.IDLE_AFTER
import io.github.vitalyostanin.geoshare.collection.lookedAtFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

/**
 * When somebody last touched this application's screen.
 *
 * Told by the activity on every touch, so that a map left showing on a screen
 * that never goes dark stops claiming that somebody is looking (see
 * [IDLE_AFTER]). On the monotonic clock: a change of the wall clock must
 * neither end a look nor stretch one.
 */
object Attention {
    private val touched = MutableStateFlow(SystemClock.elapsedRealtime())

    fun noticed() {
        touched.value = SystemClock.elapsedRealtime()
    }

    /**
     * True while the screen was touched within [IDLE_AFTER], false once that
     * long has passed without a touch, and true again at the next one.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun looking(): Flow<Boolean> = touched
        .transformLatest { at ->
            val left = lookedAtFor(at, SystemClock.elapsedRealtime())

            if (!left.isZero) {
                emit(true)
                delay(left.toMillis())
            }
            emit(false)
        }
        .distinctUntilChanged()
}
