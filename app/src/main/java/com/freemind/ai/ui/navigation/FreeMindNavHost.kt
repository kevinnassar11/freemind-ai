package com.freemind.ai.ui.navigation

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
import com.freemind.ai.ui.auth.SignInScreen
import com.freemind.ai.ui.favorites.FavoritesScreen
import com.freemind.ai.ui.history.HistoryScreen
import com.freemind.ai.ui.maps.MapsScreen
import com.freemind.ai.ui.settings.SettingsScreen

/** Matches Material's standard compact/expanded breakpoint - phone vs tablet. */
private const val EXPANDED_WIDTH_BREAKPOINT_DP = 600

/** Keeps content from stretching edge-to-edge on large tablets. */
private const val MAX_CONTENT_WIDTH_DP = 840

@Composable
fun FreeMindNavHost(
    authGateViewModel: AuthGateViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val authState by authGateViewModel.state.collectAsState()
    val startDestination = if (authState is AuthGateState.SignedIn) Destination.Maps else Destination.SignIn
    val isExpandedWidth = LocalConfiguration.current.screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT_DP

    LaunchedEffect(authState) {
        val current = navController.currentBackStackEntry?.destination
        when (authState) {
            is AuthGateState.SignedIn -> {
                if (current?.hasRoute(Destination.SignIn::class) == true) {
                    navController.navigate(Destination.Maps) {
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
            ) {
                composable<Destination.SignIn> { SignInScreen() }
                composable<Destination.Maps> { MapsScreen() }
                composable<Destination.History> { HistoryScreen() }
                composable<Destination.Favorites> { FavoritesScreen() }
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
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelRes),
                            )
                        },
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
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
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
