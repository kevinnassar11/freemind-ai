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

    /** focus* fields are set when arriving via a push-notification deep link
     *  (see FreeMindFcmService) or by tapping a saved place in Favorites.
     *  runQuery/runMode are set when arriving from a History entry, which re-runs
     *  that search in the mode it was originally made in. All null for a normal
     *  tap on the Places tab. */
    @Serializable
    data class Maps(
        val focusPlaceId: String? = null,
        val focusTitle: String? = null,
        val focusLatitude: Double? = null,
        val focusLongitude: Double? = null,
        val runQuery: String? = null,
        val runMode: String? = null,
    ) : Destination

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
    TopLevelTab(Destination.Maps(), R.string.nav_maps, Icons.Filled.Place, Icons.Outlined.Place),
    TopLevelTab(Destination.History, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    TopLevelTab(Destination.Favorites, R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.Favorite),
    TopLevelTab(Destination.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)
