package com.freemind.ai.domain.model

data class UserProfile(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
