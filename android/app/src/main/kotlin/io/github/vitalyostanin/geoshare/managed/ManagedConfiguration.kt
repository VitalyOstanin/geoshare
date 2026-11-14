package io.github.vitalyostanin.geoshare.managed

import android.content.Context
import android.content.RestrictionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The managed configuration as it stands now, for whoever needs to react to it. Starts out as
 * [ManagedSettings.NONE] and is replaced whole on every [refresh], because the controller always
 * hands over the complete configuration. Who calls [refresh], and when, is decided elsewhere.
 */
object ManagedConfiguration {
    private val settings = MutableStateFlow(ManagedSettings.NONE)

    val state: StateFlow<ManagedSettings> = settings

    val current: ManagedSettings get() = settings.value

    /**
     * Reads the application restrictions and publishes them. A device with no controller yields
     * an empty bundle, hence [ManagedSettings.NONE]; a read that fails keeps what was there
     * rather than dropping the controller's locks for a moment.
     */
    fun refresh(context: Context) {
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as? RestrictionsManager
            ?: return
        val read = try {
            ManagedSettings.fromBundle(manager.applicationRestrictions)
        } catch (e: RuntimeException) {
            return
        }
        settings.value = read
    }
}
