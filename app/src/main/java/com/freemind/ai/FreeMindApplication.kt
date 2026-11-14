package com.freemind.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.PlacesRepository
import com.freemind.ai.di.ApplicationScope
import com.freemind.ai.notifications.TRIP_CHANNEL_ID
import com.freemind.ai.util.ConnectivityObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FreeMindApplication : Application() {

    @Inject lateinit var placesRepository: PlacesRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var connectivityObserver: ConnectivityObserver

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeConnectivityForPendingFavorites()
    }

    /**
     * Runs app-wide, not tied to MapsScreen's lifecycle - a favorite queued
     * while offline (see PendingFavoriteQueue) must still flush even if the
     * user has since navigated away from the Places tab.
     */
    private fun observeConnectivityForPendingFavorites() {
        applicationScope.launch {
            connectivityObserver.isOnline.filter { online -> online }.collect {
                authRepository.userId?.let { userId ->
                    placesRepository.flushPendingFavorites(userId)
                    // Saved plans follow the account: pick up other devices' plans, push ours.
                    placesRepository.syncSavedItineraries(userId)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        manager?.createNotificationChannel(
            NotificationChannel(
                TRIP_CHANNEL_ID,
                getString(R.string.trip_reminders_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}
