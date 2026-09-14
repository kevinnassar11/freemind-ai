package com.freemind.ai.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.SettingsRepository
import com.freemind.ai.domain.model.ThemeMode
import com.freemind.ai.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AuthGateState {
    data object Loading : AuthGateState
    data object SignedOut : AuthGateState
    data class SignedIn(val profile: UserProfile) : AuthGateState
}

/**
 * Root-level state shared across MainActivity (splash-screen gating, theme)
 * and FreeMindNavHost (auth-gated start destination): both resolve to the
 * same Activity-scoped instance since neither sits inside a nav back-stack
 * entry when it asks Hilt for this ViewModel.
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<AuthGateState> = authRepository.currentUser
        .map { profile -> if (profile != null) AuthGateState.SignedIn(profile) else AuthGateState.SignedOut }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthGateState.Loading)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
