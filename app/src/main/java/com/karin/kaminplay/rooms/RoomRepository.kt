package com.karin.kaminplay.rooms

import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.core.rooms.ChatMessage
import com.karin.kaminplay.core.rooms.RoomMember
import com.karin.kaminplay.core.rooms.RoomMeta
import com.karin.kaminplay.core.sync.PlaybackSnapshot
import com.karin.kaminplay.core.sync.PlaybackUpdate
import kotlinx.coroutines.flow.Flow

/** A room problem, with a message that can be shown as is. */
class RoomException(message: String) : Exception(message)

interface RoomRepository {
    /** False when the build has no Firebase configuration or no Realtime Database. */
    val isAvailable: Boolean

    /** Creates a room for [filmId], paused at the start, and returns its code. */
    suspend fun createRoom(filmId: String, user: UserProfile): String

    /** Adds [user] to the room. Safe to call again for a room already joined. */
    suspend fun joinRoom(code: String, user: UserProfile): RoomMeta

    /**
     * Keeps [user] marked as online for as long as the flow is collected and emits whether
     * the device is connected to the server. The server marks the member offline on its own
     * if the connection drops.
     */
    fun connection(code: String, user: UserProfile): Flow<Boolean>

    /** Milliseconds to add to the local clock to get the server clock. */
    fun serverTimeOffset(): Flow<Long>

    fun playback(code: String): Flow<PlaybackSnapshot?>

    fun members(code: String): Flow<List<RoomMember>>

    fun messages(code: String): Flow<List<ChatMessage>>

    fun publish(code: String, uid: String, update: PlaybackUpdate)

    fun sendMessage(code: String, user: UserProfile, text: String)
}
