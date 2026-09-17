package com.freemind.ai.ui.auth

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.ui.components.ErrorBanner
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

private const val TAG = "SignInScreen"

@Composable
fun SignInScreen(viewModel: SignInViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // A gentle, continuous breathing scale on the logo mark - runs the whole time
    // the screen is up, independent of the one-shot entrance animation below.
    val logoPulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        logoPulse.animateTo(
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
    }

    // Logo, text, and button fade/slide in one after another rather than all at
    // once, so the first thing someone sees settling into place is the mark, not
    // a wall of content appearing simultaneously.
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        logoVisible = true
        kotlinx.coroutines.delay(120)
        textVisible = true
        kotlinx.coroutines.delay(120)
        buttonVisible = true
    }

    fun launchGoogleSignIn() {
        scope.launch {
            // Looked up by name rather than R.string.default_web_client_id: that resource
            // is only generated once the Google provider is enabled in the Firebase console
            // (see SETUP.md) - referencing it directly would fail the whole module's
            // compilation for everyone who hasn't done that step yet.
            val webClientIdRes = context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName,
            )
            if (webClientIdRes == 0) {
                Log.w(TAG, "default_web_client_id missing - enable Google as a Sign-in provider in the Firebase console (see SETUP.md), then re-download google-services.json")
                viewModel.onSignInCancelledOrFailed()
                return@launch
            }
            try {
                val webClientId = context.getString(webClientIdRes)
                // GetSignInWithGoogleOption, not GetGoogleIdOption: the latter
                // filters to accounts already authorized for this app and throws
                // NoCredentialException when none match - which looks identical to
                // "no account on the device" even when one is present. The
                // sign-in-with-Google flow always presents the picker instead.
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.onGoogleIdTokenReceived(googleIdTokenCredential.idToken)
                } else {
                    viewModel.onSignInCancelledOrFailed()
                }
            } catch (e: GetCredentialCancellationException) {
                viewModel.onSignInCancelledOrFailed()
            } catch (e: GetCredentialException) {
                // No authorized-accounts retry needed any more: the picker is
                // always shown, so a failure here is a real failure.
                Log.w(TAG, "Credential Manager request failed", e)
                viewModel.onSignInCancelledOrFailed()
            } catch (e: GoogleIdTokenParsingException) {
                Log.w(TAG, "Failed to parse Google ID token", e)
                viewModel.onSignInCancelledOrFailed()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 32.dp, vertical = 24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 2 },
            ) {
                Surface(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer { scaleX = logoPulse.value; scaleY = logoPulse.value },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = textVisible,
                enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 2 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.sign_in_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    Text(
                        text = stringResource(R.string.sign_in_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            AnimatedVisibility(visible = uiState is SignInUiState.Error) {
                ErrorBanner(
                    message = stringResource(R.string.sign_in_error),
                    onRetry = null,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 2 },
            ) {
                val pressScale by animateFloatAsState(
                    targetValue = if (uiState is SignInUiState.Loading) 0.97f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "signInButtonScale",
                )
                Button(
                    onClick = {
                        viewModel.onSignInStarted()
                        launchGoogleSignIn()
                    },
                    enabled = uiState !is SignInUiState.Loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
                ) {
                    if (uiState is SignInUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(R.string.sign_in_with_google),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is SignInUiState.Error) {
            kotlinx.coroutines.delay(4000)
            viewModel.dismissError()
        }
    }
}
