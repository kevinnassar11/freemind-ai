package com.freemind.ai.ui.maps

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.freemind.ai.R
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.priceLabel
import com.freemind.ai.domain.model.ratingLabel

// Dark green reads well on a light surface; on a dark one it nearly vanishes, so it flips to a light green.
private val OpenGreenOnLight = Color(0xFF2E7D32)
private val OpenGreenOnDark = Color(0xFF81C784)

/**
 * "4.6★ (1,234) · $$ · Open now" - whichever of rating, price and open/closed the
 * place actually reports. Draws nothing when it reports none of them.
 */
@Composable
internal fun PlaceMetaLine(place: PlaceResult, modifier: Modifier = Modifier) {
    val rating = place.ratingLabel()
    val price = place.priceLabel()
    val openNow = place.openNow
    if (rating == null && price == null && openNow == null) return

    val openText = when (openNow) {
        true -> stringResource(R.string.place_open_now)
        false -> stringResource(R.string.place_closed_now)
        null -> null
    }
    val closedColor = MaterialTheme.colorScheme.error
    val openColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) OpenGreenOnDark else OpenGreenOnLight
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = buildAnnotatedString {
            val parts = listOfNotNull(rating, price)
            append(parts.joinToString(" · "))
            if (openText != null) {
                if (parts.isNotEmpty()) append(" · ")
                withStyle(SpanStyle(color = if (openNow == true) openColor else closedColor)) { append(openText) }
            }
        },
        style = MaterialTheme.typography.labelMedium,
        color = neutral,
        modifier = modifier,
    )
}
