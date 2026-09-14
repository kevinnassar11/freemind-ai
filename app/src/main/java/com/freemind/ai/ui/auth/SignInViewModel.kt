package com.freemind.ai.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freemind.ai.data.remote.firebase.RoleStampClient
import com.freemind.ai.data.repository.AuthRepository
import com.freemind.ai.data.repository.SupabaseRepository
import com.freemind.ai.di.ApplicationScope
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FreeMindSignIn"

sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object Loading : SignInUiState
    data object Error : SignInUiState
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val supabaseRepository: SupabaseRepository,
    private val roleStampClient: RoleStampClient,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onSignInStarted() {
        _uiState.value = SignInUiState.Loading
    }

    fun onGoogleIdTokenReceived(googleIdToken: String) {
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithGoogleIdToken(googleIdToken)
                .onSuccess { profile ->
                    _uiState.value = SignInUiState.Idle
                    // Firebase auth succeeding here flips AuthGateViewModel's
                    // state on this same tick, which immediately navigates away
                    // from SignInScreen (see FreeMindNavHost) and, because this
                    // ViewModel is nav-entry-scoped, clears it - cancelling
                    // viewModelScope. That raced against everything below and
                    // reliably won: stampRole's network round-trip never got to
                    // finish before cancellation landed, so ensureProfile never
                    // ran at all (silently - it only logs on failure, and it
                    // never even started). Provisioning has to run somewhere
                    // that survives the navigation it itself triggers.
                    applicationScope.launch {
                        // stampRole() awaits the Edge Function actually finishing
                        // before the refresh, so this is deterministic rather
                        // than a guessed delay - the original version raced a
                        // fixed 500ms sleep against the network call.
                        val stamped = roleStampClient.stampRole()
                        if (!stamped) {
                            Log.w(TAG, "stampRole did not confirm success for uid=${profile.userId} - " +
                                "continuing anyway, but Supabase writes will likely be rejected as anon")
                        }
                        authRepository.idToken(forceRefresh = true)
                        // Deliberately not failing sign-in on this: Firebase auth
                        // has already succeeded, and locking the user out over a
                        // Supabase sync error would be worse. But it must never
                        // be silent - without this row, history/favorites/quota
                        // all break.
                        supabaseRepository.ensureProfile(profile)
                            .onFailure {
                                Log.e(
                                    TAG,
                                    "Signed in as ${profile.userId} but ensureProfile failed - " +
                                        "the app will run with no Supabase profile row",
                                    it,
                                )
                            }
                        runCatching { Firebase.messaging.token.await() }.getOrNull()?.let { token ->
                            supabaseRepository.updateFcmToken(profile.userId, token)
                        }
                    }
                }
                .onFailure {
                    _uiState.value = SignInUiState.Error
                }
        }
    }

    fun onSignInCancelledOrFailed() {
        if (_uiState.value is SignInUiState.Loading) {
            _uiState.value = SignInUiState.Error
        }
    }

    fun dismissError() {
        _uiState.value = SignInUiState.Idle
    }
}
