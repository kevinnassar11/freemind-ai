package com.freemind.ai.ui.favorites

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.data.repository.SavedItinerarySummary
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.ui.components.ErrorBanner
import com.freemind.ai.util.toRelativeTimeString
import kotlin.time.Instant

@Composable
private fun PlansList(
    plans: List<SavedItinerarySummary>,
    onOpen: (SavedItinerarySummary) -> Unit,
    onDelete: (SavedItinerarySummary) -> Unit,
) {
    if (plans.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.plans_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(plans, key = { it.id }) { plan ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(
                        fadeInSpec = tween(280),
                        fadeOutSpec = tween(180),
                        placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    ),
                onClick = { onOpen(plan) },
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = plan.destination, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(
                                R.string.plans_row_subtitle,
                                plan.dayCount,
                                Instant.fromEpochMilliseconds(plan.savedAt).toRelativeTimeString(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    IconButton(onClick = { onDelete(plan) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.plan_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private enum class FavoritesContentKey { LOADING, ERROR, EMPTY, LIST }

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onOpenFavorite: (FavoritePlace) -> Unit = {},
    onOpenPlan: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPlans by rememberSaveable { mutableStateOf(false) }

    // Same reasoning as HistoryScreen: the ViewModel survives tab switches,
    // so refreshing here (not just in init{}) is what picks up a favorite
    // added from the Places tab in this same session.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )

        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !showPlans,
                onClick = { showPlans = false },
                label = { Text(stringResource(R.string.favorites_tab_places)) },
            )
            FilterChip(
                selected = showPlans,
                onClick = { showPlans = true },
                label = { Text(stringResource(R.string.favorites_tab_plans) + if (uiState.plans.isNotEmpty()) " (${uiState.plans.size})" else "") },
            )
        }

        if (showPlans) {
            PlansList(
                plans = uiState.plans,
                onOpen = { onOpenPlan(it.id) },
                onDelete = viewModel::deletePlan,
            )
            return@Column
        }

        // Before the empty state: a failed load leaves `favorites` empty too, and
        // showing "places you save will show up here" to someone who has saved
        // plenty reads as though their data is gone.
        val contentKey = when {
            uiState.isLoading -> FavoritesContentKey.LOADING
            uiState.isError -> FavoritesContentKey.ERROR
            uiState.favorites.isEmpty() -> FavoritesContentKey.EMPTY
            else -> FavoritesContentKey.LIST
        }
        Crossfade(targetState = contentKey, label = "favoritesContent") { key ->
            when (key) {
                FavoritesContentKey.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                FavoritesContentKey.ERROR -> ErrorBanner(
                    message = stringResource(R.string.favorites_load_error),
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FavoritesContentKey.EMPTY -> FavoritesEmptyState()
                FavoritesContentKey.LIST -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.favorites, key = { it.id }) { favorite ->
                        FavoriteCard(
                            favorite = favorite,
                            onOpen = { onOpenFavorite(favorite) },
                            onRemove = { viewModel.removeFavorite(favorite) },
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

@Composable
private fun FavoriteCard(
    favorite: FavoritePlace,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
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
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = favorite.title, style = MaterialTheme.typography.bodyLarge)
                if (favorite.description.isNotBlank()) {
                    Text(
                        text = favorite.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = favorite.favoritedAt.toRelativeTimeString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // A quick outsized pop on tap before the row animates away, echoing the
            // familiar "unlike" heart-burst rather than just vanishing.
            val removeInteractionSource = remember { MutableInteractionSource() }
            val isRemovePressed by removeInteractionSource.collectIsPressedAsState()
            val removeScale by animateFloatAsState(
                targetValue = if (isRemovePressed) 1.3f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "removeHeartScale",
            )
            IconButton(onClick = onRemove, interactionSource = removeInteractionSource) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.unfavorite_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { scaleX = removeScale; scaleY = removeScale },
                )
            }
        }
    }
}

@Composable
private fun FavoritesEmptyState() {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            )
            Text(
                text = stringResource(R.string.favorites_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
            )
        }
    }
}
