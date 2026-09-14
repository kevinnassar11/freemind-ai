package com.freemind.ai.ui.maps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.ui.components.ErrorBanner
import com.freemind.ai.util.AppError
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private val DEFAULT_LOCATION = LatLng(37.7749, -122.4194)
private const val DEFAULT_ZOOM = 11f
private const val RESULT_ZOOM = 14f

@Composable
fun MapsScreen(viewModel: MapsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }

    // Recentre the map whenever the selected place changes (search result tapped,
    // or the first hit auto-selected after a search).
    LaunchedEffect(uiState.selected) {
        val place = uiState.selected ?: return@LaunchedEffect
        val lat = place.latitude ?: return@LaunchedEffect
        val lng = place.longitude ?: return@LaunchedEffect
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), RESULT_ZOOM)
    }

    // A permanently-failed favorite write (not a queued offline one) surfaces
    // once as a Snackbar. The message is resolved here, in composable scope,
    // since stringResource() can't be called from inside the LaunchedEffect below.
    var favoriteError by remember { mutableStateOf<AppError?>(null) }
    LaunchedEffect(Unit) {
        viewModel.favoriteErrorEvents.collect { favoriteError = it }
    }
    val favoriteErrorMessage = favoriteError?.toFavoriteMessage()
    LaunchedEffect(favoriteErrorMessage) {
        val message = favoriteErrorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        favoriteError = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        ) {
            uiState.results.forEach { place ->
                val lat = place.latitude
                val lng = place.longitude
                if (lat != null && lng != null) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = place.title,
                        snippet = place.description,
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SearchModeChips(
                selected = uiState.mode,
                onSelect = viewModel::setMode,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(28.dp)),
                placeholder = { Text(stringResource(uiState.mode.hintRes)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clear) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.maps_clear),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    viewModel.search()
                }),
            )

            uiState.error?.let { error ->
                ErrorBanner(
                    message = error.toMapsMessage(),
                    onRetry = { viewModel.dismissError(); viewModel.search() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (uiState.isSearching) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }

        if (uiState.results.isNotEmpty()) {
            ResultsSheet(
                results = uiState.results,
                selectedId = uiState.selected?.id,
                favoritePlaceIds = uiState.favoritePlaceIds,
                pendingFavoritePlaceIds = uiState.pendingFavoritePlaceIds,
                onSelect = viewModel::selectPlace,
                onToggleFavorite = viewModel::toggleFavorite,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (uiState.hasSearched && !uiState.isSearching && uiState.error == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = stringResource(R.string.maps_no_results, uiState.query),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun SearchModeChips(
    selected: PlaceSearchMode,
    onSelect: (PlaceSearchMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaceSearchMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(mode.labelRes)) },
                leadingIcon = if (mode == selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun ResultsSheet(
    results: List<PlaceResult>,
    selectedId: String?,
    favoritePlaceIds: Set<String>,
    pendingFavoritePlaceIds: Set<String>,
    onSelect: (PlaceResult) -> Unit,
    onToggleFavorite: (PlaceResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column {
            Text(
                text = stringResource(R.string.maps_results_count, results.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(items = results, key = { it.id }) { place ->
                    val isSelected = place.id == selectedId
                    val isFavorite = place.id in favoritePlaceIds
                    val isPending = place.id in pendingFavoritePlaceIds
                    ListItem(
                        headlineContent = { Text(place.title) },
                        supportingContent = { Text(place.description) },
                        trailingContent = {
                            IconButton(onClick = { onToggleFavorite(place) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = stringResource(
                                        if (isFavorite) {
                                            R.string.unfavorite_content_description
                                        } else {
                                            R.string.favorite_content_description
                                        },
                                    ),
                                    // Pending = written locally but not yet confirmed by the
                                    // server (offline, queued for retry) - dimmed so it reads
                                    // as "in progress" rather than fully confirmed.
                                    tint = when {
                                        isPending -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        isFavorite -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        },
                        colors = if (isSelected) {
                            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            ListItemDefaults.colors()
                        },
                        modifier = Modifier.clickable { onSelect(place) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AppError.toMapsMessage(): String = when (this) {
    AppError.Network -> stringResource(R.string.error_network)
    AppError.RateLimited -> stringResource(R.string.error_rate_limited)
    AppError.Unauthorized -> stringResource(R.string.error_maps_denied)
    AppError.NotConfigured -> stringResource(R.string.error_maps_not_configured)
    is AppError.Unknown -> message ?: stringResource(R.string.error_generic)
}

/** Same idea as [toMapsMessage], but for a failed favorite write - never shows a raw
 *  database error string to the user, since [AppError.Unknown] there is typically an
 *  RLS/Postgrest message that means nothing to them. */
@Composable
private fun AppError.toFavoriteMessage(): String = when (this) {
    AppError.Network -> stringResource(R.string.error_network)
    AppError.RateLimited -> stringResource(R.string.error_rate_limited)
    AppError.Unauthorized -> stringResource(R.string.error_maps_denied)
    AppError.NotConfigured -> stringResource(R.string.error_maps_not_configured)
    is AppError.Unknown -> stringResource(R.string.error_favorite_action_failed)
}
