package com.karin.kaminplay.watch

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.karin.kaminplay.core.SeedCatalog
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.core.sync.LocalPlayback
import com.karin.kaminplay.core.sync.PlaybackSnapshot
import com.karin.kaminplay.core.sync.PlaybackUpdate
import com.karin.kaminplay.core.sync.PlayerCommand
import com.karin.kaminplay.core.sync.SyncEngine
import com.karin.kaminplay.player.ArchiveResolver
import com.karin.kaminplay.player.SourceException
import com.karin.kaminplay.rooms.RoomException
import com.karin.kaminplay.rooms.RoomRepository
import com.karin.kaminplay.ui.watch.PlaybackUi
import com.karin.kaminplay.ui.watch.RoomUi
import com.karin.kaminplay.ui.watch.SourceUi
import com.karin.kaminplay.ui.watch.WatchUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** What to open: a film on its own, or a room (which says which film it plays). */
sealed interface WatchRequest {
    data class Solo(val filmId: String) : WatchRequest
    data class Party(val code: String) : WatchRequest
}

/**
 * Owns the player for one viewing. In a room it runs the [SyncEngine] four times a second:
 * the engine compares the local player with the room state and returns the commands to apply
 * and, when this device changes the room, the update to publish.
 */
class WatchViewModel(
    app: Application,
    private val request: WatchRequest,
    private val user: UserProfile,
    private val resolver: ArchiveResolver,
    private val rooms: RoomRepository,
) : AndroidViewModel(app) {

    val player: ExoPlayer = ExoPlayer.Builder(app).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            /* handleAudioFocus = */ true,
        )
        setHandleAudioBecomingNoisy(true)
    }

    private val roomCode: String? = (request as? WatchRequest.Party)?.code
    private val engine: SyncEngine? = roomCode?.let { SyncEngine(user.uid) }

    private val _state = MutableStateFlow(
        WatchUiState(room = roomCode?.let { RoomUi(code = it, selfUid = user.uid) }),
    )
    val state: StateFlow<WatchUiState> = _state.asStateFlow()

    private var serverOffsetMs = 0L
    private var inForeground = true
    private var roomStarted = false
    private var loadJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "Playback failed", error)
            _state.update { it.copy(source = SourceUi.Failed("No se pudo reproducir el vídeo. Revisa tu conexión e inténtalo de nuevo.")) }
        }
    }

    init {
        player.addListener(listener)
        load()
        viewModelScope.launch {
            while (isActive) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    /** Also used by the retry button. */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(source = SourceUi.Loading) }
            try {
                val filmId = when (request) {
                    is WatchRequest.Solo -> request.filmId
                    is WatchRequest.Party -> rooms.joinRoom(request.code, user).filmId.also { startRoom(request.code) }
                }
                val film = SeedCatalog.byId(filmId)
                    ?: throw SourceException("Esta sala usa una película que esta versión de la app no conoce.")
                _state.update { it.copy(film = film) }
                val url = resolver.resolve(film)
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                // On its own the film starts right away; in a room the engine decides.
                if (request is WatchRequest.Solo) player.playWhenReady = true
                _state.update { it.copy(source = SourceUi.Ready) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SourceException) {
                _state.update { it.copy(source = SourceUi.Failed(e.message ?: GENERIC_ERROR)) }
            } catch (e: RoomException) {
                _state.update { it.copy(source = SourceUi.Failed(e.message ?: GENERIC_ERROR)) }
            } catch (e: Exception) {
                Log.w(TAG, "Could not open the film", e)
                _state.update { it.copy(source = SourceUi.Failed(GENERIC_ERROR)) }
            }
        }
    }

    private fun startRoom(code: String) {
        if (roomStarted) return
        roomStarted = true
        collectRoom(rooms.serverTimeOffset()) { serverOffsetMs = it }
        collectRoom(rooms.playback(code)) { snapshot -> if (snapshot != null) engine?.onRemote(snapshot) }
        collectRoom(rooms.members(code)) { members -> updateRoom { it.copy(members = members) } }
        collectRoom(rooms.messages(code)) { messages -> updateRoom { it.copy(messages = messages) } }
        collectRoom(rooms.connection(code, user)) { connected -> updateRoom { it.copy(isConnected = connected) } }
    }

    private fun <T> collectRoom(flow: Flow<T>, onEach: (T) -> Unit) {
        viewModelScope.launch {
            flow.catch { error ->
                Log.w(TAG, "Room stream failed", error)
                updateRoom { it.copy(isConnected = false, problem = (error as? RoomException)?.message ?: "Se perdió la conexión con la sala.") }
            }.collect { onEach(it) }
        }
    }

    private fun updateRoom(change: (RoomUi) -> RoomUi) = _state.update { it.copy(room = it.room?.let(change)) }

    private fun tick() {
        val ready = _state.value.source == SourceUi.Ready
        var driftMs: Long? = null
        if (engine != null && roomCode != null && ready && inForeground) {
            val result = engine.tick(localPlayback(), serverNow())
            for (command in result.commands) apply(command)
            result.publish?.let { publish(it) }
            driftMs = result.driftMs
        }
        val roomPlaying = engine?.remote?.isPlaying
        _state.update { current ->
            current.copy(
                playback = PlaybackUi(
                    // In a room the button shows the room's state, which is what a tap would change.
                    isPlaying = roomPlaying ?: player.playWhenReady,
                    isBuffering = ready && player.playbackState == Player.STATE_BUFFERING,
                    positionMs = player.currentPosition.coerceAtLeast(0),
                    durationMs = durationOrNull() ?: 0L,
                ),
                room = current.room?.copy(driftMs = driftMs),
            )
        }
    }

    private fun apply(command: PlayerCommand) {
        when (command) {
            is PlayerCommand.SetPlayWhenReady -> player.playWhenReady = command.play
            is PlayerCommand.SeekTo -> player.seekTo(command.positionMs)
            is PlayerCommand.SetSpeed -> player.setPlaybackSpeed(command.speed)
        }
    }

    fun togglePlay() {
        if (_state.value.source != SourceUi.Ready) return
        val play = !(engine?.remote?.isPlaying ?: player.playWhenReady)
        if (play && player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        val local = localPlayback()
        player.playWhenReady = play
        if (engine != null) publish(if (play) engine.userPlay(local) else engine.userPause(local))
    }

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun seekTo(positionMs: Long) {
        if (_state.value.source != SourceUi.Ready) return
        val duration = durationOrNull()
        val target = if (duration != null) positionMs.coerceIn(0, duration) else positionMs.coerceAtLeast(0)
        val update = engine?.userSeek(target, localPlayback())
        player.seekTo(target)
        update?.let { publish(it) }
    }

    fun sendMessage(text: String) {
        val code = roomCode ?: return
        rooms.sendMessage(code, user, text)
    }

    /**
     * Leaving the screen (home button, screen off) pauses this device only; the room keeps
     * going and the engine catches up on return.
     */
    fun setInForeground(visible: Boolean) {
        inForeground = visible
        if (!visible) player.playWhenReady = false
    }

    private fun publish(update: PlaybackUpdate) {
        val code = roomCode ?: return
        // Apply our own change at once so the next tick does not undo it while the write travels.
        engine?.onRemote(PlaybackSnapshot(update.isPlaying, update.positionMs, serverNow(), user.uid, update.reason))
        rooms.publish(code, user.uid, update)
    }

    private fun localPlayback() = LocalPlayback(
        positionMs = player.currentPosition.coerceAtLeast(0),
        playWhenReady = player.playWhenReady,
        isBuffering = player.playbackState == Player.STATE_BUFFERING,
        durationMs = durationOrNull(),
        speed = player.playbackParameters.speed,
    )

    private fun durationOrNull(): Long? = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }

    private fun serverNow(): Long = System.currentTimeMillis() + serverOffsetMs

    override fun onCleared() {
        player.removeListener(listener)
        player.release()
    }

    class Factory(
        private val app: Application,
        private val request: WatchRequest,
        private val user: UserProfile,
        private val resolver: ArchiveResolver,
        private val rooms: RoomRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WatchViewModel(app, request, user, resolver, rooms) as T
    }

    private companion object {
        const val TAG = "KaminWatch"
        const val TICK_MS = 250L
        const val GENERIC_ERROR = "No se pudo abrir la película. Inténtalo de nuevo."
    }
}
