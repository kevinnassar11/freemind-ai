package com.freemind.ai.ui.maps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.freemind.ai.R
import com.freemind.ai.domain.model.ChecklistItem
import com.freemind.ai.domain.model.DaySlot
import com.freemind.ai.domain.model.PlaceResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Pick the day the trip starts. Today and later only - a forecast needs a future date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartDateDialog(
    current: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val state = rememberDatePickerState(
        // The picker works in UTC midnights.
        initialSelectedDateMillis = (current ?: today).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = onClear) { Text(stringResource(R.string.plan_clear_dates)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** A short free-text note on one stop - a booking time, a reservation number. */
@Composable
internal fun StopNoteDialog(
    placeTitle: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(placeTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_STOP_NOTE_CHARS) },
                label = { Text(stringResource(R.string.stop_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val MAX_STOP_NOTE_CHARS = 200

/** Search for any place and add it to a day of the plan. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddStopDialog(
    state: AddStopState,
    onQueryChange: (String) -> Unit,
    onSlotChange: (DaySlot) -> Unit,
    onSearch: () -> Unit,
    onPick: (PlaceResult) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_stop_title, state.day)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.add_stop_when),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Wraps instead of scrolling sideways, so all four times of day are always visible.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    SCHEDULED_SLOTS.forEach { slot ->
                        FilterChip(
                            selected = state.slot == slot,
                            onClick = { onSlotChange(slot) },
                            label = { Text(stringResource(slot.labelRes())) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.add_stop_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.maps_search_field_label))
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
                when {
                    state.isSearching -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                    state.failed -> Text(
                        text = stringResource(R.string.add_stop_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.hasSearched && state.results.isEmpty() -> Text(
                        text = stringResource(R.string.maps_no_results, state.query),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(state.results, key = { it.id }) { place ->
                            ListItem(
                                headlineContent = { Text(place.title) },
                                supportingContent = {
                                    Column {
                                        Text(place.description)
                                        PlaceMetaLine(place)
                                    }
                                },
                                modifier = Modifier.clickable { onPick(place) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Trip-wide notes plus a packing / to-do checklist. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TripNotesSheet(
    notes: String,
    checklist: List<ChecklistItem>,
    onNotesChange: (String) -> Unit,
    onAddItem: (String) -> Unit,
    onToggleItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var newItem by remember { mutableStateOf("") }
    // Typing is edited locally and pushed up as it goes; the plan is re-saved on a delay.
    var notesText by remember { mutableStateOf(notes) }
    val starter = starterChecklist()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.notes_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it; onNotesChange(it) },
                label = { Text(stringResource(R.string.notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
            )

            Text(stringResource(R.string.checklist_title), style = MaterialTheme.typography.titleMedium)
            checklist.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.done, onCheckedChange = { onToggleItem(index) })
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemoveItem(index) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.checklist_remove),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                label = { Text(stringResource(R.string.checklist_add_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onAddItem(newItem)
                    newItem = ""
                }),
            )
            if (checklist.isEmpty()) {
                Text(
                    text = stringResource(R.string.checklist_starter_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Wraps over several lines rather than scrolling sideways, so every one is visible.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    starter.forEach { item ->
                        AssistChip(onClick = { onAddItem(item) }, label = { Text(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun starterChecklist(): List<String> = listOf(
    stringResource(R.string.checklist_passport),
    stringResource(R.string.checklist_tickets),
    stringResource(R.string.checklist_charger),
    stringResource(R.string.checklist_cash),
    stringResource(R.string.checklist_sunscreen),
    stringResource(R.string.checklist_medication),
)
