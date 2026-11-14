package com.freemind.ai.ui.favorites

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freemind.ai.R
import com.freemind.ai.data.repository.SavedItinerarySummary
import com.freemind.ai.domain.model.FavoriteList
import com.freemind.ai.domain.model.FavoritePlace
import com.freemind.ai.ui.components.ErrorBanner
import com.freemind.ai.util.toRelativeTimeString
import kotlin.time.Instant

@Composable
private fun PlansList(
    plans: List<SavedItinerarySummary>,
    onOpen: (SavedItinerarySummary) -> Unit,
    onDelete: (SavedItinerarySummary) -> Unit,
) {
    if (plans.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.plans_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(plans, key = { it.id }) { plan ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(
                        fadeInSpec = tween(280),
                        fadeOutSpec = tween(180),
                        placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    ),
                onClick = { onOpen(plan) },
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = plan.destination, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(
                                R.string.plans_row_subtitle,
                                plan.dayCount,
                                Instant.fromEpochMilliseconds(plan.savedAt).toRelativeTimeString(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    IconButton(onClick = { onDelete(plan) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.plan_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private enum class FavoritesContentKey { LOADING, ERROR, EMPTY, LIST }

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onOpenFavorite: (FavoritePlace) -> Unit = {},
    onOpenPlan: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPlans by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Which dialog is open: creating a list, renaming the selected one, or filing a favorite.
    var creatingList by remember { mutableStateOf(false) }
    var renamingList by remember { mutableStateOf<FavoriteList?>(null) }
    var deletingList by remember { mutableStateOf<FavoriteList?>(null) }
    var movingFavorite by remember { mutableStateOf<FavoritePlace?>(null) }
    // A favorite waiting for a brand-new list to file it into.
    var fileIntoNewList by remember { mutableStateOf<FavoritePlace?>(null) }

    // Same reasoning as HistoryScreen: the ViewModel survives tab switches,
    // so refreshing here (not just in init{}) is what picks up a favorite
    // added from the Places tab in this same session.
    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(Unit) {
        viewModel.listErrors.collect { error ->
            snackbarHostState.showSnackbar(
                context.getString(
                    when (error) {
                        ListError.CREATE_FAILED -> R.string.lists_error_create
                        ListError.RENAME_FAILED -> R.string.lists_error_rename
                        ListError.DELETE_FAILED -> R.string.lists_error_delete
                        ListError.MOVE_FAILED -> R.string.lists_error_move
                    },
                ),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.favorites_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )

            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !showPlans,
                    onClick = { showPlans = false },
                    label = { Text(stringResource(R.string.favorites_tab_places)) },
                )
                FilterChip(
                    selected = showPlans,
                    onClick = { showPlans = true },
                    label = { Text(stringResource(R.string.favorites_tab_plans) + if (uiState.plans.isNotEmpty()) " (${uiState.plans.size})" else "") },
                )
            }

            if (showPlans) {
                PlansList(
                    plans = uiState.plans,
                    onOpen = { onOpenPlan(it.id) },
                    onDelete = viewModel::deletePlan,
                )
                return@Column
            }

            ListChipsRow(
                lists = uiState.lists,
                selectedListId = uiState.selectedListId,
                onSelect = viewModel::selectList,
                onNewList = { creatingList = true },
            )
            val selectedList = uiState.lists.firstOrNull { it.id == uiState.selectedListId }
            if (selectedList != null) {
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = { renamingList = selectedList }) { Text(stringResource(R.string.lists_rename)) }
                    TextButton(onClick = { deletingList = selectedList }) {
                        Text(stringResource(R.string.lists_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Before the empty state: a failed load leaves `favorites` empty too, and
            // showing "places you save will show up here" to someone who has saved
            // plenty reads as though their data is gone.
            val visible = uiState.visibleFavorites
            val contentKey = when {
                uiState.isLoading -> FavoritesContentKey.LOADING
                uiState.isError -> FavoritesContentKey.ERROR
                visible.isEmpty() -> FavoritesContentKey.EMPTY
                else -> FavoritesContentKey.LIST
            }
            Crossfade(targetState = contentKey, label = "favoritesContent") { key ->
                when (key) {
                    FavoritesContentKey.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    FavoritesContentKey.ERROR -> ErrorBanner(
                        message = stringResource(R.string.favorites_load_error),
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    FavoritesContentKey.EMPTY -> FavoritesEmptyState(inList = uiState.selectedListId != null)
                    FavoritesContentKey.LIST -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visible, key = { it.id }) { favorite ->
                            FavoriteCard(
                                favorite = favorite,
                                listName = uiState.lists.firstOrNull { it.id == favorite.listId }?.name,
                                onOpen = { onOpenFavorite(favorite) },
                                onRemove = { viewModel.removeFavorite(favorite) },
                                onMove = { movingFavorite = favorite },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(280),
                                    fadeOutSpec = tween(180),
                                    placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                ),
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }

    if (creatingList) {
        ListNameDialog(
            title = stringResource(R.string.lists_new),
            initial = "",
            onConfirm = { name -> creatingList = false; viewModel.createList(name, thenFile = fileIntoNewList); fileIntoNewList = null },
            onDismiss = { creatingList = false; fileIntoNewList = null },
        )
    }
    renamingList?.let { list ->
        ListNameDialog(
            title = stringResource(R.string.lists_rename),
            initial = list.name,
            onConfirm = { name -> renamingList = null; viewModel.renameList(list, name) },
            onDismiss = { renamingList = null },
        )
    }
    deletingList?.let { list ->
        AlertDialog(
            onDismissRequest = { deletingList = null },
            title = { Text(stringResource(R.string.lists_delete_title, list.name)) },
            text = { Text(stringResource(R.string.lists_delete_body)) },
            confirmButton = {
                TextButton(onClick = { deletingList = null; viewModel.deleteList(list) }) {
                    Text(stringResource(R.string.lists_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { OutlinedButton(onClick = { deletingList = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    movingFavorite?.let { favorite ->
        MoveToListDialog(
            title = favorite.title,
            lists = uiState.lists,
            currentListId = favorite.listId,
            onPick = { listId -> movingFavorite = null; viewModel.moveToList(favorite, listId) },
            onNewList = { movingFavorite = null; fileIntoNewList = favorite; creatingList = true },
            onDismiss = { movingFavorite = null },
        )
    }
}

/** "All" plus one chip per list, and a chip to start a new list. */
@Composable
private fun ListChipsRow(
    lists: List<FavoriteList>,
    selectedListId: String?,
    onSelect: (String?) -> Unit,
    onNewList: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedListId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.lists_all)) },
        )
        lists.forEach { list ->
            FilterChip(
                selected = selectedListId == list.id,
                onClick = { onSelect(list.id) },
                leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                label = { Text(list.name) },
            )
        }
        AssistChip(
            onClick = onNewList,
            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
            label = { Text(stringResource(R.string.lists_new)) },
        )
    }
}

@Composable
private fun ListNameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_LIST_NAME_CHARS) },
                label = { Text(stringResource(R.string.lists_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val MAX_LIST_NAME_CHARS = 60

/** Choose which list a favorite belongs to - or none. */
@Composable
private fun MoveToListDialog(
    title: String,
    lists: List<FavoriteList>,
    currentListId: String?,
    onPick: (String?) -> Unit,
    onNewList: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lists_move_title, title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                (listOf<FavoriteList?>(null) + lists).forEach { list ->
                    val selected = list?.id == currentListId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, onClick = { onPick(list?.id) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = { onPick(list?.id) })
                        Text(
                            text = list?.name ?: stringResource(R.string.lists_none),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                TextButton(onClick = onNewList) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.lists_new), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun FavoriteCard(
    favorite: FavoritePlace,
    listName: String?,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = favorite.title, style = MaterialTheme.typography.bodyLarge)
                if (favorite.description.isNotBlank()) {
                    Text(
                        text = favorite.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = favorite.favoritedAt.toRelativeTimeString() + (listName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = onMove) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = stringResource(R.string.lists_move_content_description),
                    tint = if (favorite.listId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // A quick outsized pop on tap before the row animates away, echoing the
            // familiar "unlike" heart-burst rather than just vanishing.
            val removeInteractionSource = remember { MutableInteractionSource() }
            val isRemovePressed by removeInteractionSource.collectIsPressedAsState()
            val removeScale by animateFloatAsState(
                targetValue = if (isRemovePressed) 1.3f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "removeHeartScale",
            )
            IconButton(onClick = onRemove, interactionSource = removeInteractionSource) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.unfavorite_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { scaleX = removeScale; scaleY = removeScale },
                )
            }
        }
    }
}

@Composable
private fun FavoritesEmptyState(inList: Boolean) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            )
            Text(
                text = stringResource(if (inList) R.string.lists_empty else R.string.favorites_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
            )
        }
    }
}
