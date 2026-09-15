package com.freemind.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.freemind.ai.domain.model.ThemeMode
import com.freemind.ai.notifications.EXTRA_PLACE_ID
import com.freemind.ai.notifications.EXTRA_PLACE_LATITUDE
import com.freemind.ai.notifications.EXTRA_PLACE_LONGITUDE
import com.freemind.ai.notifications.EXTRA_PLACE_TITLE
import com.freemind.ai.ui.navigation.AuthGateState
import com.freemind.ai.ui.navigation.AuthGateViewModel
import com.freemind.ai.ui.navigation.Destination
import com.freemind.ai.ui.navigation.FreeMindNavHost
import com.freemind.ai.ui.theme.FreeMindAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AuthGateViewModel by viewModels()
    private var pendingMapsFocus by mutableStateOf<Destination.Maps?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { viewModel.state.value is AuthGateState.Loading }
        pendingMapsFocus = intent.toMapsFocusOrNull()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FreeMindAITheme(darkTheme = darkTheme) {
                FreeMindNavHost(
                    authGateViewModel = viewModel,
                    pendingMapsFocus = pendingMapsFocus,
                    onMapsFocusConsumed = { pendingMapsFocus = null },
                )
            }
        }
    }

    /** A notification tapped while the app is already alive arrives here instead of a fresh onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMapsFocus = intent.toMapsFocusOrNull()
    }

    private fun Intent.toMapsFocusOrNull(): Destination.Maps? {
        val placeId = getStringExtra(EXTRA_PLACE_ID) ?: return null
        return Destination.Maps(
            focusPlaceId = placeId,
            focusTitle = getStringExtra(EXTRA_PLACE_TITLE),
            focusLatitude = getDoubleExtraOrNull(EXTRA_PLACE_LATITUDE),
            focusLongitude = getDoubleExtraOrNull(EXTRA_PLACE_LONGITUDE),
        )
    }

    private fun Intent.getDoubleExtraOrNull(key: String): Double? =
        if (hasExtra(key)) getDoubleExtra(key, 0.0) else null
}
