package com.freemind.ai.ui.history

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.domain.model.PlaceSearchHistoryEntry
import com.freemind.ai.ui.components.ErrorBanner
import com.freemind.ai.util.toRelativeTimeString

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenEntry: (PlaceSearchHistoryEntry) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // The nav graph keeps this screen's ViewModel alive across tab switches
    // (saveState/restoreState), so a fetch tied to the ViewModel's init{}
    // only ever runs once. Re-running it whenever this screen re-enters
    // composition - e.g. after clearing history from Settings, or after a
    // Places-tab search adds a new entry - is what actually keeps it current.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )

        // Checked before the empty state on purpose: a failed load also leaves
        // `entries` empty, and falling through to "no past searches" told users
        // their history was gone when the fetch had simply failed.
        val contentKey = when {
            uiState.isLoading -> HistoryContentKey.LOADING
            uiState.isError -> HistoryContentKey.ERROR
            uiState.entries.isEmpty() -> HistoryContentKey.EMPTY
            else -> HistoryContentKey.LIST
        }
        Crossfade(targetState = contentKey, label = "historyContent") { key ->
            when (key) {
                HistoryContentKey.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                HistoryContentKey.ERROR -> ErrorBanner(
                    message = stringResource(R.string.history_load_error),
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                HistoryContentKey.EMPTY -> HistoryEmptyState()
                HistoryContentKey.LIST -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.entries, key = { it.id }) { entry ->
                        SearchHistoryCard(
                            entry = entry,
                            onOpen = { onOpenEntry(entry) },
                            onDelete = { viewModel.deleteEntry(entry.id) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(280),
                                fadeOutSpec = tween(180),
                                placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private enum class HistoryContentKey { LOADING, ERROR, EMPTY, LIST }

@Composable
private fun SearchHistoryCard(
    entry: PlaceSearchHistoryEntry,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.query, style = MaterialTheme.typography.titleMedium)
                val subtitle = entry.resultTitle?.let {
                    stringResource(R.string.history_result_subtitle, stringResource(entry.mode.labelRes), it)
                } ?: stringResource(entry.mode.labelRes)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.searchedAt.toRelativeTimeString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val deleteInteractionSource = remember { MutableInteractionSource() }
            val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
            val deleteScale by animateFloatAsState(
                targetValue = if (isDeletePressed) 1.25f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "deleteIconScale",
            )
            IconButton(onClick = onDelete, interactionSource = deleteInteractionSource) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_search_history_entry),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { scaleX = deleteScale; scaleY = deleteScale },
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            )
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
            )
        }
    }
}
