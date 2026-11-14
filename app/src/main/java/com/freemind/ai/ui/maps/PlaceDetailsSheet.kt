package com.freemind.ai.ui.maps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.freemind.ai.R
import com.freemind.ai.domain.model.PlaceDetails
import com.freemind.ai.domain.model.PlaceResult
import java.time.LocalDate

private const val DAYS_IN_WEEK = 7

/**
 * Everything known about one place: photo, rating/price/open status, address, and
 * one-tap actions (directions, call, website, share, favourite) plus weekly hours.
 * Phone, website and hours arrive after the sheet opens, so they fill in once loaded.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PlaceDetailsSheet(
    place: PlaceResult,
    details: PlaceDetails?,
    photo: Bitmap?,
    isLoading: Boolean,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDirections: () -> Unit,
    onShare: () -> Unit,
    onCall: (String) -> Unit,
    onWebsite: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = stringResource(R.string.details_photo_description, place.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Text(text = place.title, style = MaterialTheme.typography.headlineSmall)
            PlaceMetaLine(place)
            if (place.description.isNotBlank()) {
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Wraps rather than scrolling sideways, so no action is ever clipped at the edge.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onDirections) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.maps_directions), modifier = Modifier.padding(start = 8.dp))
                }
                details?.dialNumber?.let { number ->
                    // The number itself is the label, so it appears once and is the thing you tap.
                    val callDescription = stringResource(R.string.details_call_number, number)
                    OutlinedButton(
                        onClick = { onCall(number) },
                        modifier = Modifier.semantics { contentDescription = callDescription },
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(number, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                details?.website?.let { site ->
                    OutlinedButton(onClick = { onWebsite(site) }) {
                        Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.details_website), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.details_share), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.unfavorite_content_description else R.string.favorite_content_description,
                        ),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            when {
                isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.details_loading),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                details == null -> Text(
                    text = stringResource(R.string.details_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    if (details.weekdayHours.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.details_hours),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        // Google lists the week Monday first, so today's line is at that index.
                        val todayIndex = LocalDate.now().dayOfWeek.value - 1
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            details.weekdayHours.forEachIndexed { index, line ->
                                val isToday = details.weekdayHours.size == DAYS_IN_WEEK && index == todayIndex
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shares a place as text with a Google Maps link that opens it directly. */
internal fun Context.sharePlace(place: PlaceResult) {
    val lat = place.latitude
    val lng = place.longitude
    val link = if (lat != null && lng != null) {
        "https://www.google.com/maps/search/?api=1&query=$lat,$lng&query_place_id=${place.id}"
    } else {
        "https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode(place.title)}"
    }
    val text = buildString {
        append(place.title)
        if (place.description.isNotBlank()) append(" - ${place.description}")
        append("\n").append(link)
    }
    shareText(text, place.title)
}

internal fun Context.shareText(text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Nothing on the device can take text - nothing useful to fall back to.
    }
}

internal fun Context.dial(number: String) {
    try {
        startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // A device without a dialler (tablet, emulator image) - nothing to do.
    }
}

internal fun Context.openWebsite(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser installed.
    }
}
