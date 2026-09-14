package com.freemind.ai.domain.model

data class PlaceResult(
    val id: String,
    val title: String,
    val description: String,
    val latitude: Double?,
    val longitude: Double?,
)
