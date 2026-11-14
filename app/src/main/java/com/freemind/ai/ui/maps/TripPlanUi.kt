package com.freemind.ai.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freemind.ai.R
import com.freemind.ai.domain.model.DayForecast
import com.freemind.ai.domain.model.DaySlot
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.ItineraryDay
import com.freemind.ai.domain.model.ItineraryStop
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.WeatherKind
import com.freemind.ai.domain.model.canSwap
import com.freemind.ai.domain.model.dateOf
import com.freemind.ai.domain.model.isOutdoor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One marker/dot colour per day, cycling for trips longer than the palette. */
private val DAY_HUES = floatArrayOf(
    BitmapDescriptorFactory.HUE_RED,
    BitmapDescriptorFactory.HUE_ORANGE,
    BitmapDescriptorFactory.HUE_YELLOW,
    BitmapDescriptorFactory.HUE_GREEN,
    BitmapDescriptorFactory.HUE_CYAN,
    BitmapDescriptorFactory.HUE_AZURE,
    BitmapDescriptorFactory.HUE_BLUE,
    BitmapDescriptorFactory.HUE_VIOLET,
    BitmapDescriptorFactory.HUE_MAGENTA,
    BitmapDescriptorFactory.HUE_ROSE,
)

internal fun dayHue(day: Int): Float = DAY_HUES[(day - 1).coerceAtLeast(0) % DAY_HUES.size]

private fun dayColor(day: Int): Color = Color.hsv(dayHue(day), 0.78f, 0.92f)

private fun DaySlot.labelRes(): Int = when (this) {
    DaySlot.MORNING -> R.string.slot_morning
    DaySlot.MIDDAY -> R.string.slot_midday
    DaySlot.AFTERNOON -> R.string.slot_afternoon
    DaySlot.EVENING -> R.string.slot_evening
    DaySlot.ANYTIME -> R.string.slot_added
}

internal fun WeatherKind.labelRes(): Int = when (this) {
    WeatherKind.CLEAR -> R.string.weather_clear
    WeatherKind.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
    WeatherKind.CLOUDY -> R.string.weather_cloudy
    WeatherKind.FOG -> R.string.weather_fog
    WeatherKind.RAIN -> R.string.weather_rain
    WeatherKind.STORM -> R.string.weather_storm
    WeatherKind.SNOW -> R.string.weather_snow
}

/** "Sat 15 Nov" in the app's language. */
internal fun LocalDate.shortLabel(): String =
    format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

/** Tallest the scrolling list inside a bottom sheet may be: [portraitMax], but never more
 *  than a third of the screen, so the map stays usable in landscape. */
@Composable
internal fun sheetListMaxHeight(portraitMax: Dp): Dp =
    minOf(portraitMax, (LocalConfiguration.current.screenHeightDp * 0.33f).dp)

/** Everything the plan sheet can ask the screen to do. */
internal class PlanActions(
    val onSelectDay: (Int?) -> Unit,
    val onSave: () -> Unit,
    val onShareText: () -> Unit,
    val onSharePdf: () -> Unit,
    val onStopClick: (PlaceResult) -> Unit,
    val onStopDetails: (PlaceResult) -> Unit,
    val onRemoveStop: (day: Int, index: Int) -> Unit,
    val onMoveStop: (day: Int, index: Int, delta: Int) -> Unit,
    val onMoveStopToDay: (day: Int, index: Int, toDay: Int) -> Unit,
    val onSwapStop: (day: Int, index: Int) -> Unit,
    val onEditNote: (day: Int, index: Int) -> Unit,
    val onAddStop: (day: Int) -> Unit,
    val onPickDate: () -> Unit,
    val onMatchWeather: () -> Unit,
    val onOpenNotes: () -> Unit,
)

/**
 * The day-by-day plan: a header with save/share, quick actions (dates, weather, notes),
 * a chip per day (picking one zooms the map to that day's stops), and each stop as a
 * tappable row with a menu to reorder, swap, annotate or remove it. Stops are coloured
 * by day to match their map markers.
 */
@Composable
internal fun ItinerarySheet(
    plan: Itinerary,
    selectedDay: Int?,
    isSaved: Boolean,
    forecasts: Map<Int, DayForecast>,
    actions: PlanActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.plan_title, plan.days.size, plan.destinationName),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = actions.onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(if (isSaved) R.string.plan_unsave else R.string.plan_save),
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ShareMenuButton(onShareText = actions.onShareText, onSharePdf = actions.onSharePdf)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val startDate = plan.dateOf(1)
                AssistChip(
                    onClick = actions.onPickDate,
                    label = { Text(startDate?.shortLabel() ?: stringResource(R.string.plan_set_dates)) },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(AssistChipDefaults.IconSize)) },
                )
                if (forecasts.size >= 2 && plan.days.size >= 2) {
                    AssistChip(
                        onClick = actions.onMatchWeather,
                        label = { Text(stringResource(R.string.plan_match_weather)) },
                        leadingIcon = { Icon(Icons.Filled.WbSunny, null, Modifier.size(AssistChipDefaults.IconSize)) },
                    )
                }
                AssistChip(
                    onClick = actions.onOpenNotes,
                    label = {
                        val open = plan.checklist.count { !it.done }
                        Text(
                            if (open > 0) stringResource(R.string.plan_notes_with_count, open) else stringResource(R.string.plan_notes),
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.EditNote, null, Modifier.size(AssistChipDefaults.IconSize)) },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedDay == null,
                    onClick = { actions.onSelectDay(null) },
                    label = { Text(stringResource(R.string.plan_all_days)) },
                )
                plan.days.forEach { day ->
                    FilterChip(
                        selected = selectedDay == day.day,
                        onClick = { actions.onSelectDay(day.day) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dayColor(day.day)),
                            )
                        },
                        label = { Text(stringResource(R.string.plan_day_chip, day.day)) },
                    )
                }
            }

            val visibleDays = plan.days.filter { selectedDay == null || it.day == selectedDay }
            LazyColumn(modifier = Modifier.heightIn(max = sheetListMaxHeight(300.dp))) {
                visibleDays.forEach { day ->
                    item(key = "header-${day.day}") {
                        DayHeader(day = day, date = plan.dateOf(day.day), forecast = forecasts[day.day])
                    }
                    if (day.stops.isEmpty()) {
                        item(key = "empty-${day.day}") {
                            Text(
                                text = stringResource(R.string.plan_day_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    itemsIndexed(items = day.stops, key = { _, stop -> "stop-${day.day}-${stop.place.id}" }) { index, stop ->
                        StopRow(
                            stop = stop,
                            day = day.day,
                            index = index,
                            count = day.stops.size,
                            canSwap = plan.canSwap(day.day, index),
                            otherDays = plan.days.map { it.day }.filter { it != day.day },
                            rainWarning = forecasts[day.day]?.isRainy == true && stop.kind.isOutdoor(),
                            actions = actions,
                        )
                    }
                    item(key = "add-${day.day}") {
                        TextButton(
                            onClick = { actions.onAddStop(day.day) },
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.plan_add_stop), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareMenuButton(onShareText: () -> Unit, onSharePdf: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.plan_share),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_share_text)) },
                onClick = { open = false; onShareText() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plan_share_pdf)) },
                onClick = { open = false; onSharePdf() },
            )
        }
    }
}

@Composable
private fun DayHeader(day: ItineraryDay, date: LocalDate?, forecast: DayForecast?) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dayColor(day.day)),
        )
        Text(
            text = stringResource(R.string.plan_day_header, day.day) + (date?.let { " · ${it.shortLabel()}" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
        if (forecast != null) {
            val label = stringResource(forecast.kind.labelRes())
            Text(
                text = "${forecast.kind.emoji} ${forecast.tempMaxC}°/${forecast.tempMinC}° · ${stringResource(R.string.weather_rain_chance, forecast.rainChance)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (forecast.isRainy) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "$label, ${forecast.tempMaxC}°" },
            )
        }
    }
}

@Composable
private fun StopRow(
    stop: ItineraryStop,
    day: Int,
    index: Int,
    count: Int,
    canSwap: Boolean,
    otherDays: List<Int>,
    rainWarning: Boolean,
    actions: PlanActions,
) {
    Column {
        ListItem(
            overlineContent = { Text(stringResource(stop.kind.slot.labelRes())) },
            headlineContent = { Text(stopHeadline(stop)) },
            supportingContent = {
                Column {
                    if (stop.place.description.isNotBlank()) Text(stop.place.description)
                    PlaceMetaLine(stop.place)
                    if (rainWarning) {
                        Text(
                            text = stringResource(R.string.stop_rain_warning),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (stop.note.isNotBlank()) {
                        Text(
                            text = stop.note,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { actions.onStopDetails(stop.place) }) { Text(stringResource(R.string.stop_details)) }
                    StopMenu(stop, day, index, count, canSwap, otherDays, actions)
                }
            },
            modifier = Modifier.clickable { actions.onStopClick(stop.place) },
        )
        HorizontalDivider()
    }
}

/** "Lunch at Name" (in the app's language), or just the name for a stop the traveller added. */
@Composable
private fun stopHeadline(stop: ItineraryStop): String {
    val verb = LocalContext.current.localizedVerb(stop)
    return if (verb.isBlank()) stop.place.title else "$verb ${stop.place.title}"
}

@Composable
private fun StopMenu(
    stop: ItineraryStop,
    day: Int,
    index: Int,
    count: Int,
    canSwap: Boolean,
    otherDays: List<Int>,
    actions: PlanActions,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.stop_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (index > 0) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.stop_move_up)) },
                    leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                    onClick = { open = false; actions.onMoveStop(day, index, -1) },
                )
            }
            if (index < count - 1) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.stop_move_down)) },
                    leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                    onClick = { open = false; actions.onMoveStop(day, index, 1) },
                )
            }
            otherDays.forEach { other ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.stop_move_to_day, other)) },
                    onClick = { open = false; actions.onMoveStopToDay(day, index, other) },
                )
            }
            if (canSwap) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.stop_swap)) },
                    leadingIcon = { Icon(Icons.Filled.SwapHoriz, null) },
                    onClick = { open = false; actions.onSwapStop(day, index) },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(if (stop.note.isBlank()) R.string.stop_add_note else R.string.stop_edit_note)) },
                leadingIcon = { Icon(Icons.Filled.EditNote, null) },
                onClick = { open = false; actions.onEditNote(day, index) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stop_remove), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { open = false; actions.onRemoveStop(day, index) },
            )
        }
    }
}
