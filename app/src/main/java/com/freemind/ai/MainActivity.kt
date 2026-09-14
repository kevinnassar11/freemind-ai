package com.freemind.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.freemind.ai.domain.model.ThemeMode
import com.freemind.ai.ui.navigation.AuthGateState
import com.freemind.ai.ui.navigation.AuthGateViewModel
import com.freemind.ai.ui.navigation.FreeMindNavHost
import com.freemind.ai.ui.theme.FreeMindAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AuthGateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { viewModel.state.value is AuthGateState.Loading }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FreeMindAITheme(darkTheme = darkTheme) {
                FreeMindNavHost(authGateViewModel = viewModel)
            }
        }
    }
}
