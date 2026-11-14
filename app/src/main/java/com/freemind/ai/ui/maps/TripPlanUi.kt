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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freemind.ai.R
import com.freemind.ai.domain.model.DaySlot
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.ItineraryStop
import com.freemind.ai.domain.model.PlaceResult
import com.google.android.gms.maps.model.BitmapDescriptorFactory

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
}

/** Tallest the scrolling list inside a bottom sheet may be: [portraitMax], but never more
 *  than a third of the screen, so the map stays usable in landscape. */
@Composable
internal fun sheetListMaxHeight(portraitMax: Dp): Dp =
    minOf(portraitMax, (LocalConfiguration.current.screenHeightDp * 0.33f).dp)

/**
 * The day-by-day plan: a header with save/share, a chip per day (picking one zooms the
 * map to that day's stops), and each stop as a tappable row. Stops are coloured by day
 * to match their map markers.
 */
@Composable
internal fun ItinerarySheet(
    plan: Itinerary,
    selectedDay: Int?,
    isSaved: Boolean,
    onSelectDay: (Int?) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onStopClick: (PlaceResult) -> Unit,
    onStopDetails: (PlaceResult) -> Unit,
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
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(if (isSaved) R.string.plan_unsave else R.string.plan_save),
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.plan_share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    onClick = { onSelectDay(null) },
                    label = { Text(stringResource(R.string.plan_all_days)) },
                )
                plan.days.forEach { day ->
                    FilterChip(
                        selected = selectedDay == day.day,
                        onClick = { onSelectDay(day.day) },
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
                        Row(
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dayColor(day.day)),
                            )
                            Text(
                                text = stringResource(R.string.plan_day_header, day.day),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    items(items = day.stops, key = { "stop-${day.day}-${it.place.id}" }) { stop ->
                        StopRow(
                            stop = stop,
                            onClick = { onStopClick(stop.place) },
                            onDetails = { onStopDetails(stop.place) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StopRow(stop: ItineraryStop, onClick: () -> Unit, onDetails: () -> Unit) {
    Column {
        ListItem(
            overlineContent = { Text(stringResource(stop.kind.slot.labelRes())) },
            headlineContent = { Text("${stop.verb} ${stop.place.title}") },
            supportingContent = {
                Column {
                    if (stop.place.description.isNotBlank()) Text(stop.place.description)
                    PlaceMetaLine(stop.place)
                }
            },
            trailingContent = {
                TextButton(onClick = onDetails) { Text(stringResource(R.string.stop_details)) }
            },
            modifier = Modifier.clickable(onClick = onClick),
        )
        HorizontalDivider()
    }
}
