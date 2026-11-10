package com.karin.kaminplay.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.core.rooms.RoomCode
import com.karin.kaminplay.ui.home.RoomsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Creating and joining rooms from the film screen and the Salas tab. */
class RoomsViewModel(private val repository: RoomRepository) : ViewModel() {

    private val _state = MutableStateFlow(RoomsUiState(isAvailable = repository.isAvailable))
    val state: StateFlow<RoomsUiState> = _state.asStateFlow()

    private val _opened = Channel<String>(Channel.BUFFERED)

    /** Codes of rooms that are ready to be opened. */
    val opened: Flow<String> = _opened.receiveAsFlow()

    fun create(filmId: String, user: UserProfile) = run { repository.createRoom(filmId, user) }

    fun join(input: String, user: UserProfile) {
        val code = RoomCode.normalize(input)
        if (code == null) {
            _state.update { it.copy(error = "El código tiene ${RoomCode.LENGTH} letras o números.") }
            return
        }
        run {
            repository.joinRoom(code, user)
            code
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun run(action: suspend () -> String) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            try {
                _opened.send(action())
                _state.update { it.copy(isBusy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: RoomException) {
                _state.update { it.copy(isBusy = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(isBusy = false, error = "No se pudo abrir la sala. Inténtalo de nuevo.") }
            }
        }
    }

    companion object {
        fun factory(repository: RoomRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RoomsViewModel(repository) as T
        }
    }
}
