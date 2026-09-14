package com.freemind.ai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.freemind.ai.R
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable data object SignIn : Destination
    @Serializable data object Maps : Destination
    @Serializable data object History : Destination
    @Serializable data object Favorites : Destination
    @Serializable data object Settings : Destination
}

data class TopLevelTab(
    val destination: Destination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val topLevelTabs = listOf(
    TopLevelTab(Destination.Maps, R.string.nav_maps, Icons.Filled.Place, Icons.Outlined.Place),
    TopLevelTab(Destination.History, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    TopLevelTab(Destination.Favorites, R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.Favorite),
    TopLevelTab(Destination.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)
