package com.karin.kaminplay.core

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
) {
    val initial: String get() = displayName.trim().firstOrNull()?.uppercase() ?: "?"
}

sealed interface SessionState {
    /** Firebase has not reported the stored session yet. */
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: UserProfile) : SessionState
}
