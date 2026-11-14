package com.freemind.ai.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.domain.model.ThemeMode
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.freemind.ai.util.AppLanguage
import com.freemind.ai.util.LocaleHelper

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.historyCleared) {
        if (uiState.historyCleared) {
            Toast.makeText(context, context.getString(R.string.clear_search_history), Toast.LENGTH_SHORT).show()
            viewModel.acknowledgeHistoryCleared()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.exportEvents.collect { request ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, request.uri)
                // FLAG_GRANT_READ_URI_PERMISSION alone doesn't reach the chooser's own
                // preview process - ClipData is what actually propagates the grant to
                // both the resolver UI and whichever target app the user picks.
                clipData = ClipData.newUri(context.contentResolver, request.fileName, request.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, request.fileName))
        }
    }

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(tween(360)) + slideInHorizontally(tween(360)) { it / 6 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SectionHeader(stringResource(R.string.settings_appearance))
            ThemeModeSelector(selected = uiState.themeMode, onSelect = viewModel::setThemeMode)

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_language))
            LanguageSelector(context = context)

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_notifications))
            TripRemindersRow(
                checked = uiState.tripRemindersEnabled,
                onCheckedChange = viewModel::setTripRemindersEnabled,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_data))
            HistoryToggleRow(checked = uiState.historyEnabled, onCheckedChange = viewModel::setHistoryEnabled)
            SettingsRow(
                label = stringResource(R.string.clear_search_history),
                icon = Icons.Filled.DeleteSweep,
                trailing = {
                    AnimatedVisibility(visible = uiState.isClearingHistory, enter = fadeIn(), exit = fadeOut()) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    }
                },
                onClick = { showClearHistoryDialog = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_export))
            SettingsRow(
                label = stringResource(R.string.export_favorites),
                icon = Icons.Filled.FileDownload,
                onClick = viewModel::exportFavorites,
            )
            SettingsRow(
                label = stringResource(R.string.export_history),
                icon = Icons.Filled.FileDownload,
                onClick = viewModel::exportSearchHistory,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_account))
            val identity = uiState.userDisplayName ?: uiState.userEmail
            if (identity != null) {
                Text(
                    text = stringResource(R.string.signed_in_as, identity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            SettingsRow(
                label = stringResource(R.string.sign_out),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = { showSignOutDialog = true },
            )

            // Deleting the account cannot be undone, so it sits in its own, clearly separate
            // zone - not as the next row under Sign out, where a stray tap could reach it.
            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            Text(
                text = stringResource(R.string.settings_danger_zone),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.settings_danger_zone_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SettingsRow(
                label = stringResource(R.string.delete_account),
                icon = Icons.Filled.DeleteForever,
                tint = MaterialTheme.colorScheme.error,
                trailing = {
                    AnimatedVisibility(visible = uiState.isDeletingAccount, enter = fadeIn(), exit = fadeOut()) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    }
                },
                onClick = { showDeleteAccountDialog = true },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showClearHistoryDialog) {
        ConfirmDialog(
            title = stringResource(R.string.clear_search_history_confirm_title),
            body = stringResource(R.string.clear_search_history_confirm_body),
            onConfirm = { showClearHistoryDialog = false; viewModel.clearHistory() },
            onDismiss = { showClearHistoryDialog = false },
        )
    }
    if (showSignOutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.sign_out_confirm_title),
            body = null,
            onConfirm = { showSignOutDialog = false; viewModel.signOut() },
            onDismiss = { showSignOutDialog = false },
        )
    }
    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onConfirm = { showDeleteAccountDialog = false; viewModel.deleteAccount {} },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
    )
}

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
    )
    Column(Modifier.selectableGroup()) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            // The chosen option nudges in and picks up a faint tint, rather than
            // just the radio dot silently filling - makes the pick itself the thing
            // that visibly moves, not a detail you'd only notice by looking for it.
            val indent by animateFloatAsState(
                targetValue = if (isSelected) 6f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "themeRowIndent",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, onClick = { onSelect(mode) })
                    .graphicsLayer { translationX = indent },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = { onSelect(mode) })
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

/** The app's language: follow the system, or pick one. Applied immediately. */
@Composable
private fun LanguageSelector(context: Context) {
    var selected by remember { mutableStateOf(LocaleHelper.current(context)) }
    val options = listOf(
        AppLanguage.SYSTEM to stringResource(R.string.language_system),
        // Each language is named in itself, so someone who can't read the current one can still find theirs.
        AppLanguage.ENGLISH to "English",
        AppLanguage.ARABIC to "العربية",
        AppLanguage.FRENCH to "Français",
    )
    Column(Modifier.selectableGroup()) {
        options.forEach { (language, label) ->
            val isSelected = selected == language
            val choose = {
                if (!isSelected) {
                    selected = language
                    LocaleHelper.set(context, language)
                    // Android 13+ restarts the activity itself when the app locale changes;
                    // earlier versions need to be told to re-read the resources.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) context.findActivity()?.recreate()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, onClick = choose),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = choose)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Trip reminders on/off. Turning it on also asks for the notification permission (Android 13+). */
@Composable
private fun TripRemindersRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.trip_reminders_label), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.trip_reminders_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { enabled ->
                onCheckedChange(enabled)
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}

@Composable
private fun HistoryToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.history_enabled_label), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.history_enabled_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A tappable settings row: icon, label and a chevron, with a ripple and a full-width touch target. */
@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "settingsRowPress",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        trailing()
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Account deletion is permanent, so a tap on "Confirm" is not enough: the traveller has to
 * type a word first. The button stays disabled until it matches.
 */
@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val keyword = stringResource(R.string.delete_account_keyword)
    var typed by remember { mutableStateOf("") }
    val matches = typed.trim().equals(keyword, ignoreCase = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(stringResource(R.string.delete_account_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.delete_account_confirm_body))
                Text(
                    text = stringResource(R.string.delete_account_type_to_confirm, keyword),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    isError = typed.isNotEmpty() && !matches,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = matches) {
                Text(
                    text = stringResource(R.string.delete_account),
                    color = if (matches) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String?,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = body?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.confirm),
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
