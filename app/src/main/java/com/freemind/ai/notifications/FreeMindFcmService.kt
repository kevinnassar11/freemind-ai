package com.freemind.ai.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.freemind.ai.MainActivity
import com.freemind.ai.R
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.SupabaseRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FreeMindFcmService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository

    @Inject lateinit var supabaseRepository: SupabaseRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        val userId = authRepository.userId ?: return
        serviceScope.launch { supabaseRepository.updateFcmToken(userId, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(title, body, message.data)
    }

    /**
     * data carries the favorited place directly (place_id/title/lat/lng) rather
     * than just an id, because looking a place back up by id would need a
     * Places Details API call this app doesn't otherwise make - the
     * notify-favorites Edge Function already has this data on hand from
     * favorite_places, so it's cheaper to just pass it through.
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data["place_id"]?.let { putExtra(EXTRA_PLACE_ID, it) }
            data["place_title"]?.let { putExtra(EXTRA_PLACE_TITLE, it) }
            data["place_latitude"]?.toDoubleOrNull()?.let { putExtra(EXTRA_PLACE_LATITUDE, it) }
            data["place_longitude"]?.toDoubleOrNull()?.let { putExtra(EXTRA_PLACE_LONGITUDE, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
