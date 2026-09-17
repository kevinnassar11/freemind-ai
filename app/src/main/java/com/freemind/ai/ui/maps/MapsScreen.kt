package com.freemind.ai.ui.maps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.domain.model.DeviceLocation
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.domain.model.TrendingSearch
import com.freemind.ai.domain.model.distanceMetresTo
import com.freemind.ai.ui.components.ErrorBanner
import com.freemind.ai.util.AppError
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.roundToInt

private val DEFAULT_LOCATION = LatLng(37.7749, -122.4194)
private const val DEFAULT_ZOOM = 11f
private const val RESULT_ZOOM = 14f

@Composable
fun MapsScreen(
    viewModel: MapsViewModel = hiltViewModel(),
    focusPlaceId: String? = null,
    focusTitle: String? = null,
    focusLatitude: Double? = null,
    focusLongitude: Double? = null,
    /** Set when arriving from a History entry - re-runs that search in its
     *  original mode, the way tapping the entry should always have worked. */
    runQuery: String? = null,
    runMode: String? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Coarse location is all this needs: it biases the Places query and places the
    // blue dot. Asked for once on first open - declining is fine and permanent as far
    // as this screen is concerned, since search still works without it.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshDeviceLocation() }

    LaunchedEffect(Unit) {
        if (!viewModel.uiState.value.hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }

    // Arrived via a push-notification deep link (see FreeMindFcmService) - select
    // it the same way tapping a search result would, so the existing camera-move
    // effect below recenters the map without any separate code path.
    LaunchedEffect(focusPlaceId) {
        if (focusPlaceId != null && focusTitle != null && focusLatitude != null && focusLongitude != null) {
            viewModel.focusPlace(
                PlaceResult(
                    id = focusPlaceId,
                    title = focusTitle,
                    description = "",
                    latitude = focusLatitude,
                    longitude = focusLongitude,
                ),
            )
        }
    }

    // Arrived from tapping a History entry - re-run that exact search in the mode it
    // was originally made in, rather than leaving history entries un-openable.
    LaunchedEffect(runQuery, runMode) {
        if (runQuery == null) return@LaunchedEffect
        val mode = PlaceSearchMode.entries.find { it.name == runMode } ?: PlaceSearchMode.PROMPT
        viewModel.setMode(mode)
        viewModel.onQueryChange(runQuery)
        viewModel.search()
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

    // The map is the largest surface on this screen, so leaving it in daylight style
    // under a dark UI lit the whole screen up at night. Derived from the resolved
    // scheme rather than isSystemInDarkTheme() so it follows the in-app Light/Dark
    // setting too, not just the system one.
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val mapProperties = remember(isDarkTheme, uiState.hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = uiState.hasLocationPermission,
            mapStyleOptions = if (isDarkTheme) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_night)
            } else {
                null
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(myLocationButtonEnabled = uiState.hasLocationPermission),
        ) {
            uiState.results.forEach { place ->
                val lat = place.latitude
                val lng = place.longitude
                if (lat != null && lng != null) {
                    // Keyed on the place id so a genuinely new marker (not just the
                    // same slot re-drawn with different data) gets its own fresh
                    // fade-in rather than replaying on every recomposition.
                    key(place.id) {
                        val markerAlpha = remember { Animatable(0f) }
                        LaunchedEffect(Unit) { markerAlpha.animateTo(1f, tween(450)) }
                        Marker(
                            state = MarkerState(position = LatLng(lat, lng)),
                            title = place.title,
                            snippet = place.description,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                            alpha = markerAlpha.value,
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Trending only makes sense before the user has committed to their own
            // query - once they've typed or searched something, their own intent
            // takes over and the chip row would just be clutter above the results.
            AnimatedVisibility(
                visible = uiState.query.isBlank() && !uiState.hasSearched && uiState.trendingSearches.isNotEmpty(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
            ) {
                TrendingSearchChips(
                    trending = uiState.trendingSearches,
                    onSelect = viewModel::onTrendingSearchSelected,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }

            SearchModeChips(
                selected = uiState.mode,
                onSelect = viewModel::setMode,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )

            // The placeholder is a sibling node in the accessibility tree, so on its
            // own it leaves the field announcing as an unlabelled edit box. This names
            // the field itself, which is what a screen reader reads on focus.
            val searchFieldLabel = stringResource(R.string.maps_search_field_label)
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(28.dp))
                    .semantics { contentDescription = searchFieldLabel },
                placeholder = { Text(stringResource(uiState.mode.hintRes)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = uiState.query.isNotEmpty(),
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150)),
                    ) {
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

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn(tween(280)) + expandVertically(tween(280)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
            ) {
                val error = uiState.error
                if (error != null) {
                    ErrorBanner(
                        message = error.toMapsMessage(),
                        onRetry = { viewModel.dismissError(); viewModel.search() },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isShowingCachedResults,
                enter = fadeIn(tween(280)) + expandVertically(tween(280)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.showing_cached_results),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isSearching,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }

        val itinerary = uiState.itinerary
        val bottomSheetEnter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 4 }
        val bottomSheetExit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 4 }

        AnimatedVisibility(
            visible = itinerary != null,
            enter = bottomSheetEnter,
            exit = bottomSheetExit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            if (itinerary != null) {
                ItineraryCard(
                    text = itinerary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = itinerary == null && uiState.results.isNotEmpty(),
            enter = bottomSheetEnter,
            exit = bottomSheetExit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ResultsSheet(
                results = uiState.results,
                selectedId = uiState.selected?.id,
                favoritePlaceIds = uiState.favoritePlaceIds,
                pendingFavoritePlaceIds = uiState.pendingFavoritePlaceIds,
                deviceLocation = uiState.deviceLocation,
                onSelect = viewModel::selectPlace,
                onToggleFavorite = viewModel::toggleFavorite,
                onDirections = { context.startDirections(it) },
            )
        }

        AnimatedVisibility(
            visible = itinerary == null && uiState.results.isEmpty() && uiState.hasSearched && !uiState.isSearching && uiState.error == null,
            enter = bottomSheetEnter,
            exit = bottomSheetExit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                modifier = Modifier
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
            val isSelected = mode == selected
            val chipScale by animateFloatAsState(
                targetValue = if (isSelected) 1.06f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "modeChipScale",
            )
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(mode.labelRes)) },
                leadingIcon = if (isSelected) {
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
                modifier = Modifier.graphicsLayer { scaleX = chipScale; scaleY = chipScale },
            )
        }
    }
}

@Composable
private fun TrendingSearchChips(
    trending: List<TrendingSearch>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        trending.forEach { item ->
            ElevatedAssistChip(
                onClick = { onSelect(item.query) },
                label = { Text(item.query) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                colors = AssistChipDefaults.elevatedAssistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = AssistChipDefaults.elevatedAssistChipElevation(),
            )
        }
    }
}

@Composable
private fun ItineraryCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.itinerary_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
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
    deviceLocation: DeviceLocation?,
    onSelect: (PlaceResult) -> Unit,
    onToggleFavorite: (PlaceResult) -> Unit,
    onDirections: (PlaceResult) -> Unit,
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
                text = pluralStringResource(R.plurals.maps_results_count, results.size, results.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(items = results, key = { it.id }) { place ->
                    val isSelected = place.id == selectedId
                    val isFavorite = place.id in favoritePlaceIds
                    val isPending = place.id in pendingFavoritePlaceIds
                    val distance = deviceLocation?.distanceMetresTo(place.latitude, place.longitude)

                    // A quick overshoot-and-settle pop whenever this row's favourite
                    // state turns on, the familiar "heart burst" rather than the
                    // icon just silently swapping to filled.
                    val heartScale = remember(place.id) { Animatable(1f) }
                    LaunchedEffect(isFavorite) {
                        if (isFavorite) {
                            heartScale.animateTo(1.5f, tween(120))
                            heartScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        }
                    }
                    val directionsInteractionSource = remember { MutableInteractionSource() }
                    val isDirectionsPressed by directionsInteractionSource.collectIsPressedAsState()
                    val directionsScale by animateFloatAsState(
                        targetValue = if (isDirectionsPressed) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "directionsScale",
                    )

                    ListItem(
                        headlineContent = { Text(place.title) },
                        supportingContent = {
                            Column {
                                Text(place.description)
                                if (distance != null) {
                                    Text(
                                        text = distance.toDistanceLabel(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Directions used to be reachable only by tapping the
                                // pin on the map, which no one finds. The list is where
                                // people actually read the results, so it belongs here.
                                IconButton(onClick = { onDirections(place) }, interactionSource = directionsInteractionSource) {
                                    Icon(
                                        imageVector = Icons.Filled.Directions,
                                        contentDescription = stringResource(R.string.maps_directions),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.graphicsLayer { scaleX = directionsScale; scaleY = directionsScale },
                                    )
                                }
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
                                        modifier = Modifier.graphicsLayer { scaleX = heartScale.value; scaleY = heartScale.value },
                                    )
                                }
                            }
                        },
                        colors = if (isSelected) {
                            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            ListItemDefaults.colors()
                        },
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(280),
                                fadeOutSpec = tween(180),
                                placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            )
                            .clickable { onSelect(place) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** "450 m away" under a kilometre, "1.8 km away" above it - one decimal is as much
 *  precision as a straight-line distance deserves. */
@Composable
private fun Double.toDistanceLabel(): String = if (this < 1_000) {
    stringResource(R.string.maps_distance_m, roundToInt())
} else {
    stringResource(R.string.maps_distance_km, "%.1f".format(this / 1_000))
}

/**
 * Hands the place off to a maps app for turn-by-turn directions. Uses the `google.navigation`
 * intent when Google Maps is installed and falls back to a plain `geo:` URI otherwise, so
 * this still works on a device without it.
 */
private fun Context.startDirections(place: PlaceResult) {
    val lat = place.latitude
    val lng = place.longitude
    val uri = if (lat != null && lng != null) {
        "google.navigation:q=$lat,$lng".toUri()
    } else {
        "geo:0,0?q=${Uri.encode(place.title)}".toUri()
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        return
    }
    val fallback = Intent(
        Intent.ACTION_VIEW,
        if (lat != null && lng != null) {
            "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(place.title)})".toUri()
        } else {
            "geo:0,0?q=${Uri.encode(place.title)}".toUri()
        },
    )
    runCatching { startActivity(fallback) }
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
