package com.freemind.ai.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.freemind.ai.MainActivity
import com.freemind.ai.R
import com.freemind.ai.data.repository.SettingsRepository
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.ItineraryDay
import com.freemind.ai.util.LocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

const val TRIP_CHANNEL_ID = "freemind_trip_reminders"

private const val ALL_REMINDERS_TAG = "trip-reminder"
private const val KEY_PLAN_ID = "planId"
private const val KEY_TITLE = "title"
private const val KEY_BODY = "body"
private const val DAY_REMINDER_HOUR = 8
private const val EVE_REMINDER_HOUR = 18
private const val MAX_STOPS_IN_BODY = 4

/** What to show, and when. Separate from WorkManager so the schedule itself is testable. */
data class TripReminder(val at: LocalDateTime, val title: String, val body: String)

/**
 * Works out the reminders for a plan: one the evening before day 1, and one on the
 * morning of each day listing that day's stops. Times already past are dropped, so
 * editing the start date of a trip under way doesn't fire stale notifications.
 */
fun tripReminders(
    plan: Itinerary,
    now: LocalDateTime,
    eveTitle: (destination: String) -> String,
    dayTitle: (day: Int, destination: String) -> String,
    emptyDayBody: String,
): List<TripReminder> {
    val start = plan.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return emptyList()

    fun bodyFor(day: ItineraryDay?): String {
        val names = day?.stops?.map { it.place.title }.orEmpty()
        return if (names.isEmpty()) emptyDayBody else names.take(MAX_STOPS_IN_BODY).joinToString(" · ")
    }

    val reminders = buildList {
        add(
            TripReminder(
                start.minusDays(1).atTime(EVE_REMINDER_HOUR, 0),
                eveTitle(plan.destinationName),
                bodyFor(plan.days.firstOrNull()),
            ),
        )
        plan.days.forEach { day ->
            add(
                TripReminder(
                    start.plusDays((day.day - 1).toLong()).atTime(DAY_REMINDER_HOUR, 0),
                    dayTitle(day.day, plan.destinationName),
                    bodyFor(day),
                ),
            )
        }
    }
    return reminders.filter { it.at.isAfter(now) }
}

/** Schedules the reminders of saved plans with WorkManager, so they survive a reboot. */
@Singleton
class TripReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val workManager get() = WorkManager.getInstance(context)

    private fun planTag(planId: String) = "trip-$planId"

    suspend fun schedule(planId: String, plan: Itinerary) {
        cancel(planId)
        if (!settingsRepository.tripRemindersEnabled.first()) return

        // Strings come from a context in the user's chosen language, not the process default.
        val localized = LocaleHelper.wrap(context)
        val now = ZonedDateTime.now()
        val reminders = tripReminders(
            plan = plan,
            now = now.toLocalDateTime(),
            eveTitle = { localized.getString(R.string.reminder_eve_title, it) },
            dayTitle = { day, destination -> localized.getString(R.string.reminder_day_title, day, destination) },
            emptyDayBody = localized.getString(R.string.reminder_empty_day),
        )
        reminders.forEach { reminder ->
            val delay = Duration.between(now, reminder.at.atZone(now.zone))
            val request = OneTimeWorkRequestBuilder<TripReminderWorker>()
                .setInitialDelay(delay)
                .addTag(ALL_REMINDERS_TAG)
                .addTag(planTag(planId))
                .setInputData(
                    Data.Builder()
                        .putString(KEY_PLAN_ID, planId)
                        .putString(KEY_TITLE, reminder.title)
                        .putString(KEY_BODY, reminder.body)
                        .build(),
                )
                .build()
            workManager.enqueue(request)
        }
    }

    fun cancel(planId: String) {
        workManager.cancelAllWorkByTag(planTag(planId))
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(ALL_REMINDERS_TAG)
    }
}

class TripReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val body = inputData.getString(KEY_BODY).orEmpty()
        val planId = inputData.getString(KEY_PLAN_ID)

        // Android 13+ gates notifications behind a runtime permission; before that the
        // per-app switch in system settings is the only gate.
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permitted || !NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        val notificationId = (planId + title).hashCode()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            planId?.let { putExtra(EXTRA_PLAN_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, TRIP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        return Result.success()
    }
}
