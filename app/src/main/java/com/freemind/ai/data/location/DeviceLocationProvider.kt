package com.freemind.ai.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.freemind.ai.domain.model.DeviceLocation
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device's own position for biasing place searches.
 *
 * Everything here degrades to `null` rather than throwing: location is a genuine
 * optional for this app - without it searches still work, they're just biased by
 * whatever Google infers from the request IP, which is what the app did before.
 */
@Singleton
class DeviceLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Last known position if one is cached, otherwise a fresh low-power fix.
     * `lastLocation` is instant but can be null on a device that hasn't resolved a
     * position yet (a fresh emulator, a phone straight out of airplane mode), so the
     * current-location request is the fallback rather than the default.
     */
    suspend fun current(): DeviceLocation? {
        if (!hasPermission()) return null
        return runCatching {
            client.lastLocation.await()
                ?: client.getCurrentLocation(
                    CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setGranularity(Granularity.GRANULARITY_COARSE)
                        .setMaxUpdateAgeMillis(MAX_FIX_AGE_MS)
                        .build(),
                    null,
                ).await()
        }.getOrNull()?.let { DeviceLocation(it.latitude, it.longitude) }
    }

    private companion object {
        /** A fix from the last two minutes is plenty for "what's near me". */
        const val MAX_FIX_AGE_MS = 2 * 60 * 1000L
    }
}
