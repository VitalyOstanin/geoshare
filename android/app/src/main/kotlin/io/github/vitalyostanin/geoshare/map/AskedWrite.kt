package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

/** How often a sounding countdown redraws. */
private const val SIREN_TICK_MS = 1_000L

/**
 * A write a dialog asks for: what it is about, how the last attempt came out,
 * and whether one is on its way.
 *
 * Held apart from each other so that a refusal on one phone does not linger
 * once the dialog is opened on another, and a dialog stays open on anything
 * but a write that went through.
 */
@Stable
class AskedWrite<T>(private val scope: CoroutineScope) {
    /** What the dialog is about; null while it is closed. */
    var asked: T? by mutableStateOf(null)
        private set

    /** How the last attempt came out; null before any. */
    var trouble: WriteOutcome? by mutableStateOf(null)
        private set

    var busy: Boolean by mutableStateOf(false)
        private set

    /** Opens the dialog on [what], clear of an earlier refusal. */
    fun ask(what: T) {
        asked = what
        trouble = null
    }

    /** Changes what the open dialog is about without forgetting the last attempt. */
    fun change(what: T) {
        asked = what
    }

    fun cancel() {
        asked = null
    }

    /** Makes the write; the dialog closes only once it went through. */
    fun write(attempt: suspend (T) -> WriteOutcome) {
        val what = asked ?: return

        busy = true
        scope.launch {
            val outcome = attempt(what)

            busy = false

            if (outcome == WriteOutcome.DONE) {
                asked = null
                trouble = null
            } else {
                trouble = outcome
            }
        }
    }
}

@Composable
fun <T> rememberAskedWrite(): AskedWrite<T> {
    val scope = rememberCoroutineScope()

    return remember { AskedWrite(scope) }
}

/**
 * The clock a sounding countdown reads: it ticks every second while any of
 * [sirens] is live, where the screen's own clock ticks far slower.
 */
@Composable
fun sirenClock(now: Instant, sirens: Map<String, Siren>): State<Instant> =
    produceState(now, sirens) {
        value = Instant.now()

        while (anyLive(sirens.values, value)) {
            delay(SIREN_TICK_MS)
            value = Instant.now()
        }
    }
