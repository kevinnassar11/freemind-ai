package com.freemind.ai.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.freemind.ai.ui.auth.SignInScreen
import com.freemind.ai.ui.favorites.FavoritesScreen
import com.freemind.ai.ui.history.HistoryScreen
import com.freemind.ai.ui.maps.MapsScreen
import com.freemind.ai.ui.settings.SettingsScreen

/** Matches Material's standard compact/expanded breakpoint - phone vs tablet. */
private const val EXPANDED_WIDTH_BREAKPOINT_DP = 600

/** Keeps content from stretching edge-to-edge on large tablets. */
private const val MAX_CONTENT_WIDTH_DP = 840

/** Screen-to-screen transition: a soft cross-fade with a slight scale-up on the
 *  incoming screen. Used for every destination (sign-in, all four tabs, and the
 *  focused-place/re-run-search deep links into Maps) rather than a directional
 *  slide, since bottom-nav tabs have no real left/right order to signal. */
private const val NAV_TRANSITION_MS = 260

@Composable
fun FreeMindNavHost(
    authGateViewModel: AuthGateViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    /** Set when the app was opened via a push-notification deep link (see MainActivity). */
    pendingMapsFocus: Destination.Maps? = null,
    onMapsFocusConsumed: () -> Unit = {},
) {
    val authState by authGateViewModel.state.collectAsState()
    val startDestination = if (authState is AuthGateState.SignedIn) Destination.Maps() else Destination.SignIn
    val isExpandedWidth = LocalConfiguration.current.screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT_DP

    LaunchedEffect(authState) {
        val current = navController.currentBackStackEntry?.destination
        when (authState) {
            is AuthGateState.SignedIn -> {
                if (current?.hasRoute(Destination.SignIn::class) == true) {
                    navController.navigate(Destination.Maps()) {
                        popUpTo(Destination.SignIn) { inclusive = true }
                    }
                }
            }
            AuthGateState.SignedOut -> {
                val onMainShell = topLevelTabs.any { tab -> current?.hasRoute(tab.destination::class) == true }
                if (onMainShell) {
                    navController.navigate(Destination.SignIn) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            AuthGateState.Loading -> Unit
        }
    }

    // Handles a notification tap both on cold start (graph is created already
    // pointed at plain Destination.Maps(), so this promotes it to the focused
    // version) and warm start (MainActivity.onNewIntent updates this same
    // parameter while the graph already exists).
    LaunchedEffect(pendingMapsFocus, authState) {
        val focus = pendingMapsFocus ?: return@LaunchedEffect
        if (authState is AuthGateState.SignedIn) {
            navController.navigate(focus) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
            onMapsFocusConsumed()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentIsTopLevel = topLevelTabs.any { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.destination::class) } == true
    }
    val isTabSelected: (TopLevelTab) -> Boolean = { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.destination::class) } == true
    }
    val onTabClick: (Destination) -> Unit = { destination ->
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH_DP.dp),
                enterTransition = { fadeIn(tween(NAV_TRANSITION_MS)) + scaleIn(initialScale = 0.96f, animationSpec = tween(NAV_TRANSITION_MS)) },
                exitTransition = { fadeOut(tween(NAV_TRANSITION_MS / 2)) },
                popEnterTransition = { fadeIn(tween(NAV_TRANSITION_MS)) + scaleIn(initialScale = 0.96f, animationSpec = tween(NAV_TRANSITION_MS)) },
                popExitTransition = { fadeOut(tween(NAV_TRANSITION_MS / 2)) },
            ) {
                composable<Destination.SignIn> { SignInScreen() }
                composable<Destination.Maps> { backStackEntry ->
                    val args = backStackEntry.toRoute<Destination.Maps>()
                    MapsScreen(
                        focusPlaceId = args.focusPlaceId,
                        focusTitle = args.focusTitle,
                        focusLatitude = args.focusLatitude,
                        focusLongitude = args.focusLongitude,
                        runQuery = args.runQuery,
                        runMode = args.runMode,
                        runPlanId = args.runPlanId,
                    )
                }
                composable<Destination.History> {
                    HistoryScreen(
                        // Deliberately no restoreState here, unlike onTabClick below:
                        // this Maps() carries specific arguments (the search to
                        // re-run), and restoreState would instead reattach whatever
                        // Maps state the tab last had saved, silently discarding
                        // them - the same reason the push-notification deep link
                        // effect above never sets it either.
                        onOpenEntry = { entry ->
                            navController.navigate(
                                Destination.Maps(runQuery = entry.query, runMode = entry.mode.name),
                            ) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable<Destination.Favorites> {
                    FavoritesScreen(
                        // Same reasoning as History's onOpenEntry above.
                        onOpenFavorite = { favorite ->
                            navController.navigate(
                                Destination.Maps(
                                    focusPlaceId = favorite.placeId,
                                    focusTitle = favorite.title,
                                    focusLatitude = favorite.latitude,
                                    focusLongitude = favorite.longitude,
                                ),
                            ) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onOpenPlan = { planId ->
                            navController.navigate(Destination.Maps(runPlanId = planId)) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable<Destination.Settings> { SettingsScreen() }
            }
        }
    }

    if (isExpandedWidth && currentIsTopLevel) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                topLevelTabs.forEach { tab ->
                    val selected = isTabSelected(tab)
                    NavigationRailItem(
                        selected = selected,
                        onClick = { onTabClick(tab.destination) },
                        icon = { BouncyTabIcon(tab = tab, selected = selected, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
            content(Modifier.weight(1f))
        }
    } else {
        Scaffold(
            bottomBar = {
                if (currentIsTopLevel) {
                    NavigationBar {
                        topLevelTabs.forEach { tab ->
                            val selected = isTabSelected(tab)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onTabClick(tab.destination) },
                                icon = { BouncyTabIcon(tab = tab, selected = selected, contentDescription = stringResource(tab.labelRes)) },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}

/** The tab's icon swaps to its filled variant and pops slightly larger when
 *  selected, instead of just snapping between the outlined/filled drawables. */
@Composable
private fun BouncyTabIcon(tab: TopLevelTab, selected: Boolean, contentDescription: String) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabIconScale",
    )
    Icon(
        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
        contentDescription = contentDescription,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}
