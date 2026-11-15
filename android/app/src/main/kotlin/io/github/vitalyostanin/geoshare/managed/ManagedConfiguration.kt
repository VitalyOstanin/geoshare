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
     * Reads the application restrictions, publishes them and returns them. A device with no
     * controller yields an empty bundle, hence [ManagedSettings.NONE]; a read that fails returns
     * null and keeps what was published rather than dropping the controller's locks for a moment.
     */
    fun refresh(context: Context): ManagedSettings? = refreshWith {
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as? RestrictionsManager
        manager?.let { ManagedSettings.fromBundle(it.applicationRestrictions) }
    }

    /** [refresh] with the reading handed in, so that a JVM test needs no system service. */
    internal fun refreshWith(read: () -> ManagedSettings?): ManagedSettings? {
        val read = try {
            read()
        } catch (e: RuntimeException) {
            null
        } ?: return null
        settings.value = read
        return read
    }
}
