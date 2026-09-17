package com.freemind.ai.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import android.content.ClipData
import android.content.Intent
import android.widget.Toast

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
            Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

            SectionHeader(stringResource(R.string.settings_appearance))
            ThemeModeSelector(selected = uiState.themeMode, onSelect = viewModel::setThemeMode)

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_data))
            HistoryToggleRow(checked = uiState.historyEnabled, onCheckedChange = viewModel::setHistoryEnabled)
            SettingsRow(
                label = stringResource(R.string.clear_search_history),
                trailing = {
                    AnimatedVisibility(visible = uiState.isClearingHistory, enter = fadeIn(), exit = fadeOut()) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    }
                },
                onClick = { showClearHistoryDialog = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_export))
            SettingsRow(label = stringResource(R.string.export_favorites), onClick = viewModel::exportFavorites)
            SettingsRow(label = stringResource(R.string.export_history), onClick = viewModel::exportSearchHistory)

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
            SettingsRow(label = stringResource(R.string.sign_out), onClick = { showSignOutDialog = true })
            SettingsRow(
                label = stringResource(R.string.delete_account),
                labelColor = MaterialTheme.colorScheme.error,
                trailing = {
                    AnimatedVisibility(visible = uiState.isDeletingAccount, enter = fadeIn(), exit = fadeOut()) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    }
                },
                onClick = { showDeleteAccountDialog = true },
            )
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
        ConfirmDialog(
            title = stringResource(R.string.delete_account_confirm_title),
            body = stringResource(R.string.delete_account_confirm_body),
            destructive = true,
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

@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
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
            .padding(vertical = 10.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onClick, interactionSource = interactionSource, modifier = Modifier.weight(1f)) {
            Text(text = label, color = labelColor, modifier = Modifier.fillMaxWidth())
        }
        trailing()
    }
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
