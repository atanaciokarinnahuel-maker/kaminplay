package com.karin.kaminplay.rooms

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.karin.kaminplay.core.UserProfile
import com.karin.kaminplay.core.rooms.ChatMessage
import com.karin.kaminplay.core.rooms.RoomCode
import com.karin.kaminplay.core.rooms.RoomMember
import com.karin.kaminplay.core.rooms.RoomMeta
import com.karin.kaminplay.core.sync.ChangeReason
import com.karin.kaminplay.core.sync.PlaybackSnapshot
import com.karin.kaminplay.core.sync.PlaybackUpdate
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Rooms on Firebase Realtime Database. Layout (see firebase/database.rules.json):
 *
 * rooms/{code}/meta      filmId, createdBy, createdAt
 * rooms/{code}/playback  playing, positionMs, updatedAt (server time), by, reason
 * rooms/{code}/members   {uid}: name, online, lastSeen
 * rooms/{code}/messages  {pushId}: uid, name, text, at
 */
class FirebaseRoomRepository(appContext: Context) : RoomRepository {

    private val database: FirebaseDatabase? = try {
        // Throws when the Firebase project has no Realtime Database (no URL in google-services.json).
        if (FirebaseApp.getApps(appContext).isNotEmpty()) FirebaseDatabase.getInstance() else null
    } catch (e: Exception) {
        Log.w(TAG, "Realtime Database is not available", e)
        null
    }

    override val isAvailable: Boolean = database != null

    private fun db(): FirebaseDatabase = database ?: throw RoomException("Las salas no están disponibles en esta compilación.")

    private fun room(code: String): DatabaseReference = db().getReference("rooms").child(code)

    override suspend fun createRoom(filmId: String, user: UserProfile): String {
        repeat(5) {
            val code = RoomCode.generate()
            val ref = room(code)
            if (ref.child("meta").get().awaitServer().exists()) return@repeat // code taken, try another
            ref.child("meta").setValue(
                mapOf("filmId" to filmId, "createdBy" to user.uid, "createdAt" to ServerValue.TIMESTAMP),
            ).awaitServer()
            ref.child("members").child(user.uid).setValue(memberValues(user, online = true)).awaitServer()
            ref.child("playback").setValue(playbackValues(PlaybackUpdate(isPlaying = false, positionMs = 0), user.uid)).awaitServer()
            return code
        }
        throw RoomException("No se pudo crear la sala. Inténtalo de nuevo.")
    }

    override suspend fun joinRoom(code: String, user: UserProfile): RoomMeta {
        val ref = room(code)
        val meta = ref.child("meta").get().awaitServer()
        val filmId = meta.child("filmId").value as? String
        if (!meta.exists() || filmId == null) throw RoomException("No existe ninguna sala con el código $code.")
        ref.child("members").child(user.uid).updateChildren(memberValues(user, online = true)).awaitServer()
        return RoomMeta(filmId = filmId, createdBy = meta.child("createdBy").value as? String ?: "")
    }

    override fun connection(code: String, user: UserProfile): Flow<Boolean> = callbackFlow {
        val member = room(code).child("members").child(user.uid)
        val connectedRef = db().getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.value == true
                if (connected) {
                    // Registered on every (re)connection: the server applies it if this device vanishes.
                    member.onDisconnect().updateChildren(memberValues(user, online = false))
                    member.updateChildren(memberValues(user, online = true))
                }
                trySend(connected)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        connectedRef.addValueEventListener(listener)
        awaitClose {
            connectedRef.removeEventListener(listener)
            member.onDisconnect().cancel()
            member.updateChildren(memberValues(user, online = false))
        }
    }

    override fun serverTimeOffset(): Flow<Long> =
        db().getReference(".info/serverTimeOffset").observe().map { (it.value as? Number)?.toLong() ?: 0L }

    override fun playback(code: String): Flow<PlaybackSnapshot?> = room(code).child("playback").observe().map { it.toPlayback() }

    override fun members(code: String): Flow<List<RoomMember>> = room(code).child("members").observe().map { snapshot ->
        snapshot.children.mapNotNull { child ->
            val uid = child.key ?: return@mapNotNull null
            RoomMember(uid = uid, name = child.child("name").value as? String ?: "", online = child.child("online").value == true)
        }.sortedBy { it.name.lowercase() }
    }

    override fun messages(code: String): Flow<List<ChatMessage>> =
        room(code).child("messages").orderByKey().limitToLast(MESSAGE_LIMIT).observe().map { snapshot ->
            snapshot.children.mapNotNull { child ->
                val id = child.key ?: return@mapNotNull null
                val text = child.child("text").value as? String ?: return@mapNotNull null
                ChatMessage(
                    id = id,
                    uid = child.child("uid").value as? String ?: "",
                    name = child.child("name").value as? String ?: "",
                    text = text,
                    atMs = (child.child("at").value as? Number)?.toLong() ?: 0L,
                )
            }
        }

    override fun publish(code: String, uid: String, update: PlaybackUpdate) {
        room(code).child("playback").setValue(playbackValues(update, uid))
            .addOnFailureListener { Log.w(TAG, "Could not publish the playback state", it) }
    }

    override fun sendMessage(code: String, user: UserProfile, text: String) {
        val clean = text.trim().take(ChatMessage.MAX_LENGTH)
        if (clean.isEmpty()) return
        room(code).child("messages").push().setValue(
            mapOf("uid" to user.uid, "name" to displayName(user), "text" to clean, "at" to ServerValue.TIMESTAMP),
        ).addOnFailureListener { Log.w(TAG, "Could not send the message", it) }
    }

    private fun memberValues(user: UserProfile, online: Boolean): Map<String, Any> =
        mapOf("name" to displayName(user), "online" to online, "lastSeen" to ServerValue.TIMESTAMP)

    private fun playbackValues(update: PlaybackUpdate, uid: String): Map<String, Any> = mapOf(
        "playing" to update.isPlaying,
        "positionMs" to update.positionMs,
        "updatedAt" to ServerValue.TIMESTAMP,
        "by" to uid,
        "reason" to if (update.reason == ChangeReason.STALL) "stall" else "user",
    )

    private fun displayName(user: UserProfile): String = user.displayName.ifBlank { "Sin nombre" }.take(60)

    private fun DataSnapshot.toPlayback(): PlaybackSnapshot? {
        val playing = child("playing").value as? Boolean ?: return null
        val position = (child("positionMs").value as? Number)?.toLong() ?: return null
        // While a local write is pending the timestamp is an estimate; it is still a number.
        val updatedAt = (child("updatedAt").value as? Number)?.toLong() ?: return null
        return PlaybackSnapshot(
            isPlaying = playing,
            positionMs = position,
            updatedAtMs = updatedAt,
            by = child("by").value as? String ?: "",
            reason = if (child("reason").value == "stall") ChangeReason.STALL else ChangeReason.USER,
        )
    }

    /** Emits the current value and every later change until the collector stops. */
    private fun Query.observe(): Flow<DataSnapshot> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                close(RoomException(describe(error)))
            }
        }
        addValueEventListener(listener)
        awaitClose { removeEventListener(listener) }
    }

    /** Waits for the server to confirm; writes made offline would otherwise wait forever. */
    private suspend fun <T> Task<T>.awaitServer(): T = try {
        withTimeout(SERVER_TIMEOUT_MS) { await() }
    } catch (e: TimeoutCancellationException) {
        throw RoomException("El servidor de salas no responde. Revisa tu conexión.")
    } catch (e: com.google.firebase.database.DatabaseException) {
        throw RoomException("El servidor de salas rechazó la operación. Revisa las reglas de Realtime Database.")
    }

    private fun describe(error: DatabaseError): String = when (error.code) {
        DatabaseError.PERMISSION_DENIED -> "No tienes acceso a esta sala."
        DatabaseError.DISCONNECTED, DatabaseError.NETWORK_ERROR -> "Se perdió la conexión con la sala."
        else -> "La sala dejó de responder."
    }

    private companion object {
        const val TAG = "KaminRooms"
        const val SERVER_TIMEOUT_MS = 10_000L
        const val MESSAGE_LIMIT = 100
    }
}
