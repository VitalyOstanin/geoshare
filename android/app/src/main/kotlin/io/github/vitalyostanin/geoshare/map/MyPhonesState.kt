package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * What [MyPhonesScreen] remembers between frames: which dialog is up for which
 * device, and how its write is going.
 *
 * Held apart from the screen so the screen reads as layout; the dialogs and the
 * row buttons both reach the same asks through here.
 */
internal class MyPhonesState(
    private val scope: CoroutineScope,
    val ringing: AskedWrite<PhoneLine>,
    val searching: AskedWrite<PhoneLine>,
    val resetting: AskedWrite<PhoneLine>,
) {
    var searchHours by mutableStateOf(DEFAULT_SEARCH_HOURS)
    var helpOpen by mutableStateOf(false)

    /** The device the member asked to remove, held while the dialog is up. */
    var removeAsk by mutableStateOf<PhoneLine?>(null)
    var removeTrouble by mutableStateOf(false)
    var removeBusy by mutableStateOf(false)

    /** Takes the asked device out of every group; keeps the dialog up when the database refuses. */
    fun writeRemoval(forget: suspend (phoneId: String) -> Boolean) {
        val phone = removeAsk ?: return

        removeBusy = true
        scope.launch {
            val removed = forget(phone.phoneId)

            removeBusy = false

            if (removed) {
                removeAsk = null
                removeTrouble = false
            } else {
                removeTrouble = true
            }
        }
    }

    fun askSearch(phone: PhoneLine) {
        searching.ask(phone)
        searchHours = DEFAULT_SEARCH_HOURS
    }

    fun askRemoval(phone: PhoneLine) {
        removeTrouble = false
        removeAsk = phone
    }

    /** The row buttons, wired to the asks above and to the plain writes that need no dialog. */
    fun actions(
        mayLookFor: Boolean,
        onStopSiren: suspend (phoneId: String) -> Unit,
        onEndSearch: suspend (phoneId: String) -> Unit,
    ) = MyPhoneActions(
        mayLookFor = mayLookFor,
        onReset = { resetting.ask(it) },
        onCall = { ringing.ask(it) },
        onStop = { scope.launch { onStopSiren(it.phoneId) } },
        onSearch = ::askSearch,
        onEndSearch = { scope.launch { onEndSearch(it.phoneId) } },
        onRemove = ::askRemoval,
    )
}

@Composable
internal fun rememberMyPhonesState(): MyPhonesState {
    val scope = rememberCoroutineScope()
    val ringing = rememberAskedWrite<PhoneLine>()
    val searching = rememberAskedWrite<PhoneLine>()
    val resetting = rememberAskedWrite<PhoneLine>()

    return remember { MyPhonesState(scope, ringing, searching, resetting) }
}
