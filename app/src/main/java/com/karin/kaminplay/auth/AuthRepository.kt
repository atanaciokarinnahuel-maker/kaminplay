package com.karin.kaminplay.auth

import android.content.Context
import com.karin.kaminplay.core.SessionState
import kotlinx.coroutines.flow.StateFlow

sealed interface SignInResult {
    data object Success : SignInResult
    /** The user closed the Google sheet; not an error. */
    data object Cancelled : SignInResult
    data class Failure(val message: String) : SignInResult
}

interface AuthRepository {
    val session: StateFlow<SessionState>

    /** False when the build has no Firebase configuration. */
    val isConfigured: Boolean

    /** True only in debug builds without Firebase: lets the UI be opened without an account. */
    val canPreview: Boolean

    /** [activityContext] must be an Activity: the Google account sheet is shown on top of it. */
    suspend fun signInWithGoogle(activityContext: Context): SignInResult

    suspend fun signOut(context: Context)

    fun enterPreview()
}
