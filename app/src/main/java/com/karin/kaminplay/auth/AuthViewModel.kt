package com.karin.kaminplay.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karin.kaminplay.core.SessionState
import com.karin.kaminplay.ui.login.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    val session: StateFlow<SessionState> = repository.session

    private val _login = MutableStateFlow(LoginUiState(isConfigured = repository.isConfigured, canPreview = repository.canPreview))
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    /** [activityContext] is only used for this call; it is not kept. */
    fun signIn(activityContext: Context) {
        if (_login.value.isLoading) return
        _login.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repository.signInWithGoogle(activityContext)
            _login.update { it.copy(isLoading = false, error = (result as? SignInResult.Failure)?.message) }
        }
    }

    fun signOut(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch { repository.signOut(appContext) }
    }

    fun enterPreview() = repository.enterPreview()

    companion object {
        fun factory(repository: AuthRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
        }
    }
}
