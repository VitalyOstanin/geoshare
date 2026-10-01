package io.github.vitalyostanin.geoshare.service

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Whether Play Services are on this phone and working.
 *
 * Asked once per service: a phone does not gain or lose them while it runs,
 * and an update that changes the answer restarts the application anyway.
 */
fun playServicesPresent(context: Context): Boolean =
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
        ConnectionResult.SUCCESS
