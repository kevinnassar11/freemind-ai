package com.freemind.ai.ui.maps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.NearMe
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.domain.model.DeviceLocation
import com.freemind.ai.domain.model.PlaceFilters
import com.freemind.ai.domain.model.PlaceResult
import com.freemind.ai.domain.model.PlaceSearchMode
import com.freemind.ai.domain.model.TrendingSearch
import com.freemind.ai.domain.model.dateOf
import com.freemind.ai.domain.model.distanceMetresTo
import com.freemind.ai.ui.components.ErrorBanner
import com.freemind.ai.util.AppError
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt

private val DEFAULT_LOCATION = LatLng(37.7749, -122.4194)
private const val DEFAULT_ZOOM = 11f
private const val RESULT_ZOOM = 14f

/** Space the mode chips and search bar take from the top of the map while a plan is open. */
private val PLAN_TOP_CHROME_HEIGHT = 200.dp

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
    /** Set when arriving from Favorites > Plans - opens that saved trip plan offline. */
    runPlanId: String? = null,
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

    // "Open near me" needs a position. If the permission was declined at start-up, the first
    // tap asks again; once it is granted the tapped search runs straight away.
    val nearMeNames = NearMeCategory.entries.associateWith { stringResource(it.queryLabelRes()) }
    val nearMeLabels = NearMeCategory.entries.associateWith { stringResource(R.string.nearme_query, nearMeNames.getValue(it)) }
    var pendingNearMe by remember { mutableStateOf<NearMeCategory?>(null) }
    val nearMePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshDeviceLocation()
        val category = pendingNearMe
        pendingNearMe = null
        if (granted && category != null) viewModel.searchNearMe(category, nearMeLabels.getValue(category))
    }

    // Trip reminders are notifications, which need a runtime permission on Android 13+.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val askForNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Plan dialogs and sheets that are open right now.
    var showDatePicker by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var noteTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }
    // Height of the trip-plan sheet as laid out, so the map can keep its framing clear of it.
    var planSheetHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

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

    // Arrived from a saved plan in Favorites - reopen it from the device, no search.
    LaunchedEffect(runPlanId) {
        if (runPlanId != null) viewModel.openSavedPlan(runPlanId)
    }

    // Recentre the map whenever the selected place changes (search result tapped,
    // or the first hit auto-selected after a search).
    LaunchedEffect(uiState.selected) {
        val place = uiState.selected ?: return@LaunchedEffect
        val lat = place.latitude ?: return@LaunchedEffect
        val lng = place.longitude ?: return@LaunchedEffect
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), RESULT_ZOOM)
    }

    // Frame the whole trip - or just the chosen day - whenever the plan, the day
    // picker or the sheet's height changes. The map fills the screen under the search
    // bar and the plan sheet, so it is given content padding for both (see GoogleMap
    // below) and the bounds are fitted into what is actually visible. Declared after
    // the effect above so, on first show, this wins over the plain recentre.
    val framingKey = uiState.itinerary?.days?.map { day -> day.stops.map { it.place.id } }
    LaunchedEffect(framingKey, uiState.selectedDay, planSheetHeightPx) {
        val plan = uiState.itinerary ?: return@LaunchedEffect
        // Wait for the sheet to be measured, or the first fit would ignore it.
        if (planSheetHeightPx == 0) return@LaunchedEffect
        val points = plan.days
            .filter { uiState.selectedDay == null || it.day == uiState.selectedDay }
            .flatMap { it.stops }
            .mapNotNull { stop ->
                val lat = stop.place.latitude
                val lng = stop.place.longitude
                if (lat != null && lng != null) LatLng(lat, lng) else null
            }
        if (points.isEmpty()) return@LaunchedEffect
        try {
            if (points.size == 1) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(points.first(), RESULT_ZOOM), 600)
            } else {
                val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100), 600)
            }
        } catch (_: IllegalStateException) {
            // Map not laid out yet - the destination recentre above already applies.
        }
    }

    // One-off things from the ViewModel: snackbars (some with Undo) and permission prompts.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            fun say(message: String, action: String? = null, onAction: () -> Unit = {}) {
                launch {
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = action,
                        duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) onAction()
                }
            }
            when (event) {
                is MapsEvent.StopRemoved -> say(
                    context.getString(R.string.stop_removed, event.title),
                    context.getString(R.string.undo),
                ) { viewModel.undoEdit() }
                MapsEvent.NothingToSwap -> say(context.getString(R.string.stop_nothing_to_swap))
                MapsEvent.StopAlreadyInDay -> say(context.getString(R.string.stop_already_in_day))
                MapsEvent.WeatherArranged -> say(
                    context.getString(R.string.plan_weather_arranged),
                    context.getString(R.string.undo),
                ) { viewModel.undoEdit() }
                MapsEvent.WeatherAlreadyBest -> say(context.getString(R.string.plan_weather_already_best))
                is MapsEvent.PlanSaved -> {
                    say(context.getString(R.string.plan_saved_toast))
                    if (event.wantsReminders) askForNotifications()
                }
                is MapsEvent.OfflineReady -> say(
                    context.getString(if (event.complete) R.string.plan_offline_ready else R.string.plan_offline_partial),
                )
                MapsEvent.AskNotificationPermission -> askForNotifications()
                is MapsEvent.NeedLocationPermission -> {
                    pendingNearMe = event.category
                    nearMePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }
        }
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
            // Only while a plan is open: keep the framing (and Google's logo / zoom
            // buttons) clear of the mode chips + search bar and of the plan sheet.
            contentPadding = if (uiState.itinerary != null && planSheetHeightPx > 0) {
                PaddingValues(
                    top = PLAN_TOP_CHROME_HEIGHT,
                    bottom = with(density) { planSheetHeightPx.toDp() },
                )
            } else {
                PaddingValues()
            },
        ) {
            val plan = uiState.itinerary
            if (plan != null) {
                // Trip plan: each stop is a pin coloured by its day, so the colour in the
                // list matches the colour on the map.
                plan.days
                    .filter { uiState.selectedDay == null || it.day == uiState.selectedDay }
                    .forEach { day ->
                        day.stops.forEach { stop ->
                            val lat = stop.place.latitude
                            val lng = stop.place.longitude
                            if (lat != null && lng != null) {
                                key("plan-${day.day}-${stop.place.id}") {
                                    val markerAlpha = remember { Animatable(0f) }
                                    LaunchedEffect(Unit) { markerAlpha.animateTo(1f, tween(450)) }
                                    Marker(
                                        state = MarkerState(position = LatLng(lat, lng)),
                                        title = stop.place.title,
                                        snippet = stringResource(R.string.plan_day_header, day.day),
                                        icon = BitmapDescriptorFactory.defaultMarker(dayHue(day.day)),
                                        alpha = markerAlpha.value,
                                    )
                                }
                            }
                        }
                    }
            } else uiState.results.forEach { place ->
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
                // Follow the typed text's own direction, so an English prompt in an Arabic
                // layout (or the reverse) isn't scrambled by the surrounding RTL/LTR.
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
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
                visible = uiState.query.isBlank() && !uiState.hasSearched && uiState.itinerary == null,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
            ) {
                NearMeRow(
                    labels = nearMeNames,
                    onSelect = { category -> viewModel.searchNearMe(category, nearMeLabels.getValue(category)) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                )
            }

            AnimatedVisibility(
                visible = uiState.itinerary == null && uiState.unfilteredResults.isNotEmpty(),
                enter = fadeIn(tween(280)) + expandVertically(tween(280)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
            ) {
                FilterRow(
                    filters = uiState.filters,
                    onOpenNow = viewModel::toggleOpenNow,
                    onRating = { viewModel.toggleMinRating(4.0) },
                    onPrice = { viewModel.toggleMaxPrice(2) },
                    onClear = viewModel::clearFilters,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                )
            }

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
        val scope = rememberCoroutineScope()
        val planShareChooser = stringResource(R.string.plan_share_chooser)
        val bottomSheetEnter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 4 }
        val bottomSheetExit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 4 }

        AnimatedVisibility(
            visible = itinerary != null,
            enter = bottomSheetEnter,
            exit = bottomSheetExit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            if (itinerary != null) {
                val actions = remember(viewModel, itinerary) {
                    PlanActions(
                        onSelectDay = viewModel::selectDay,
                        onSave = viewModel::toggleSavePlan,
                        onShareText = {
                            context.shareText(itinerary.toShareText(context.planShareLabels(itinerary)), planShareChooser)
                        },
                        onSharePdf = {
                            scope.launch {
                                val labels = context.planShareLabels(itinerary)
                                val uri = withContext(Dispatchers.IO) { context.exportPlanPdf(itinerary, labels) }
                                context.sharePdf(uri, planShareChooser)
                            }
                        },
                        onStopClick = viewModel::selectPlace,
                        onStopDetails = viewModel::openDetails,
                        onRemoveStop = viewModel::removeStop,
                        onMoveStop = viewModel::moveStop,
                        onMoveStopToDay = viewModel::moveStopToDay,
                        onSwapStop = viewModel::swapStop,
                        onEditNote = { day, index -> noteTarget = day to index },
                        onAddStop = viewModel::openAddStop,
                        onPickDate = { showDatePicker = true },
                        onMatchWeather = viewModel::matchWeather,
                        onOpenNotes = { showNotes = true },
                    )
                }
                ItinerarySheet(
                    plan = itinerary,
                    selectedDay = uiState.selectedDay,
                    isSaved = uiState.savedItineraryId != null,
                    forecasts = uiState.forecasts,
                    actions = actions,
                    modifier = Modifier
                        .onSizeChanged { planSheetHeightPx = it.height }
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        if (itinerary != null) {
            if (showDatePicker) {
                StartDateDialog(
                    current = itinerary.dateOf(1),
                    onPick = { date -> showDatePicker = false; viewModel.setStartDate(date) },
                    onClear = { showDatePicker = false; viewModel.setStartDate(null) },
                    onDismiss = { showDatePicker = false },
                )
            }
            if (showNotes) {
                TripNotesSheet(
                    notes = itinerary.notes,
                    checklist = itinerary.checklist,
                    onNotesChange = viewModel::setNotes,
                    onAddItem = viewModel::addChecklistItem,
                    onToggleItem = viewModel::toggleChecklistItem,
                    onRemoveItem = viewModel::removeChecklistItem,
                    onDismiss = { showNotes = false },
                )
            }
            noteTarget?.let { (day, index) ->
                val stop = itinerary.days.firstOrNull { it.day == day }?.stops?.getOrNull(index)
                if (stop == null) {
                    noteTarget = null
                } else {
                    StopNoteDialog(
                        placeTitle = stop.place.title,
                        initial = stop.note,
                        onSave = { note -> viewModel.setStopNote(day, index, note); noteTarget = null },
                        onDismiss = { noteTarget = null },
                    )
                }
            }
        }
        uiState.addStop?.let { addStop ->
            AddStopDialog(
                state = addStop,
                onQueryChange = viewModel::onAddStopQueryChange,
                onSearch = viewModel::searchAddStop,
                onPick = viewModel::confirmAddStop,
                onDismiss = viewModel::closeAddStop,
            )
        }

        AnimatedVisibility(
            visible = itinerary == null && uiState.results.isNotEmpty(),
            enter = bottomSheetEnter,
            exit = bottomSheetExit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ResultsSheet(
                results = uiState.results,
                totalCount = uiState.unfilteredResults.size,
                selectedId = uiState.selected?.id,
                favoritePlaceIds = uiState.favoritePlaceIds,
                pendingFavoritePlaceIds = uiState.pendingFavoritePlaceIds,
                deviceLocation = uiState.deviceLocation,
                onSelect = viewModel::selectPlace,
                onDetails = viewModel::openDetails,
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
                if (uiState.unfilteredResults.isNotEmpty()) {
                    // The search found places, but the active filters hid them all.
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.maps_no_filter_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::clearFilters) { Text(stringResource(R.string.filter_clear)) }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.maps_no_results, uiState.query),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        uiState.detailsPlace?.let { place ->
            PlaceDetailsSheet(
                place = place,
                details = uiState.details,
                photo = uiState.detailsPhoto,
                isLoading = uiState.isLoadingDetails,
                isFavorite = place.id in uiState.favoritePlaceIds,
                onDismiss = viewModel::closeDetails,
                onToggleFavorite = { viewModel.toggleFavorite(place) },
                onDirections = { context.startDirections(place) },
                onShare = { context.sharePlace(place) },
                onCall = { context.dial(it) },
                onWebsite = { context.openWebsite(it) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

private fun NearMeCategory.queryLabelRes(): Int = when (this) {
    NearMeCategory.RESTAURANTS -> R.string.nearme_restaurants
    NearMeCategory.CAFES -> R.string.nearme_cafes
    NearMeCategory.BARS -> R.string.nearme_bars
    NearMeCategory.PHARMACIES -> R.string.nearme_pharmacies
    NearMeCategory.SUPERMARKETS -> R.string.nearme_supermarkets
}

/** One-tap "what's open right now, near me" shortcuts, shown before the first search. */
@Composable
private fun NearMeRow(
    labels: Map<NearMeCategory, String>,
    onSelect: (NearMeCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NearMeCategory.entries.forEach { category ->
            ElevatedAssistChip(
                onClick = { onSelect(category) },
                label = { Text(labels.getValue(category)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                colors = AssistChipDefaults.elevatedAssistChipColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = AssistChipDefaults.elevatedAssistChipElevation(),
            )
        }
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
private fun FilterRow(
    filters: PlaceFilters,
    onOpenNow: () -> Unit,
    onRating: () -> Unit,
    onPrice: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        FilterChip(
            selected = filters.openNow,
            onClick = onOpenNow,
            label = { Text(stringResource(R.string.filter_open_now)) },
            colors = chipColors,
        )
        FilterChip(
            selected = filters.minRating != null,
            onClick = onRating,
            label = { Text(stringResource(R.string.filter_rating_4)) },
            colors = chipColors,
        )
        FilterChip(
            selected = filters.maxPriceLevel != null,
            onClick = onPrice,
            label = { Text(stringResource(R.string.filter_price_2)) },
            colors = chipColors,
        )
        if (filters.isActive) {
            TextButton(onClick = onClear) { Text(stringResource(R.string.filter_clear)) }
        }
    }
}

@Composable
private fun ResultsSheet(
    results: List<PlaceResult>,
    totalCount: Int,
    selectedId: String?,
    favoritePlaceIds: Set<String>,
    pendingFavoritePlaceIds: Set<String>,
    deviceLocation: DeviceLocation?,
    onSelect: (PlaceResult) -> Unit,
    onDetails: (PlaceResult) -> Unit,
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
                text = pluralStringResource(R.plurals.maps_results_count, results.size, results.size) +
                    if (totalCount > results.size) " · " + stringResource(R.string.maps_filtered_count, results.size, totalCount) else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            LazyColumn(modifier = Modifier.heightIn(max = sheetListMaxHeight(240.dp))) {
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
                                PlaceMetaLine(place)
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
                            // First tap selects and recentres; tapping the selected row again
                            // opens its full details.
                            .clickable { if (isSelected) onDetails(place) else onSelect(place) },
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
    AppError.NoLocation -> stringResource(R.string.error_no_location)
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
    AppError.NoLocation -> stringResource(R.string.error_no_location)
    is AppError.Unknown -> stringResource(R.string.error_favorite_action_failed)
}
