package com.freemind.ai.ui.history

import com.freemind.ai.domain.model.PlaceSearchHistoryEntry

data class HistoryUiState(
    val entries: List<PlaceSearchHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)
