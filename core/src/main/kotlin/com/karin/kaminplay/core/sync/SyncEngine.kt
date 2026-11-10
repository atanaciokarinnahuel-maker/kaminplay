package com.karin.kaminplay.core.sync

import kotlin.math.abs
import kotlin.math.min

enum class ChangeReason { USER, STALL }

/**
 * The room's shared playback state. Nobody streams position updates: the state is written
 * only when someone plays, pauses or seeks, stamped with the server clock, and every device
 * derives where playback should be right now from that timestamp.
 */
data class PlaybackSnapshot(
    val isPlaying: Boolean,
    val positionMs: Long,
    /** Server time at which [positionMs] was true. */
    val updatedAtMs: Long,
    /** Uid of the member who made the change. */
    val by: String,
    val reason: ChangeReason = ChangeReason.USER,
) {
    fun positionAt(serverNowMs: Long): Long =
        if (isPlaying) positionMs + (serverNowMs - updatedAtMs).coerceAtLeast(0) else positionMs
}

/** What the local player reports on each tick. */
data class LocalPlayback(
    val positionMs: Long,
    val playWhenReady: Boolean,
    val isBuffering: Boolean,
    /** Null until the player knows the duration. */
    val durationMs: Long?,
    val speed: Float = 1f,
)

sealed interface PlayerCommand {
    data class SetPlayWhenReady(val play: Boolean) : PlayerCommand
    data class SeekTo(val positionMs: Long) : PlayerCommand
    data class SetSpeed(val speed: Float) : PlayerCommand
}

/** A change this device wants to publish to the room; the server adds the timestamp. */
data class PlaybackUpdate(val isPlaying: Boolean, val positionMs: Long, val reason: ChangeReason = ChangeReason.USER)

data class TickResult(
    val commands: List<PlayerCommand> = emptyList(),
    val publish: PlaybackUpdate? = null,
    /** Local position minus where the room says it should be; positive means ahead. */
    val driftMs: Long? = null,
)

data class SyncConfig(
    /** Drift below this is left alone. */
    val toleranceMs: Long = 120,
    /** While nudging the speed, stop once drift is back under this. */
    val settleMs: Long = 40,
    /** Drift above this is fixed with a seek instead of a speed nudge. */
    val hardSeekMs: Long = 1_500,
    /** Speed change used to close small drifts (5 % is not noticeable). */
    val nudge: Float = 0.05f,
    /** Allowed difference while paused. */
    val pausedToleranceMs: Long = 100,
    /** Continuous buffering after which this device pauses the room for everyone. */
    val stallMs: Long = 2_000,
    /** A corrective seek aims slightly ahead to absorb the time the seek itself takes. */
    val seekLeadMs: Long = 300,
    /** Upper bound for that lead once the engine has measured how long seeks take here. */
    val maxSeekLeadMs: Long = 6_000,
)

/**
 * Decides, on every tick, what the local player must do to match the room, and when this
 * device should change the room on its own (pausing it if playback stalls here, resuming it
 * afterwards). Pure logic with no clock or Android dependency, so it is fully unit-tested.
 */
class SyncEngine(private val selfId: String, private val config: SyncConfig = SyncConfig()) {

    var remote: PlaybackSnapshot? = null
        private set

    /** True once this device has been playing in sync; a stall only counts after that. */
    private var steady = false
    private var bufferingSinceMs: Long? = null
    private var pausedRoomForStall = false

    /**
     * How far ahead a corrective seek aims. It starts at [SyncConfig.seekLeadMs] and then follows
     * the time the last corrective seek took to rebuffer, so on a slow connection the next seek
     * lands in sync instead of falling behind again and seeking forever.
     */
    private var seekLeadMs = config.seekLeadMs
    private var correctiveSeekAtMs: Long? = null

    fun onRemote(snapshot: PlaybackSnapshot) {
        if (snapshot == remote) return
        remote = snapshot
        steady = false
        val isMyStallPause = !snapshot.isPlaying && snapshot.by == selfId && snapshot.reason == ChangeReason.STALL
        if (!isMyStallPause) pausedRoomForStall = false
    }

    fun userPlay(local: LocalPlayback): PlaybackUpdate = userChange(true, local.positionMs)

    fun userPause(local: LocalPlayback): PlaybackUpdate = userChange(false, local.positionMs)

    /** Seeking keeps the room's current play/pause state. */
    fun userSeek(positionMs: Long, local: LocalPlayback): PlaybackUpdate =
        userChange(remote?.isPlaying ?: local.playWhenReady, clamp(positionMs, local.durationMs))

    private fun userChange(isPlaying: Boolean, positionMs: Long): PlaybackUpdate {
        pausedRoomForStall = false
        steady = false
        return PlaybackUpdate(isPlaying, positionMs.coerceAtLeast(0), ChangeReason.USER)
    }

    fun tick(local: LocalPlayback, serverNowMs: Long): TickResult {
        val room = remote ?: return TickResult()
        val target = clamp(room.positionAt(serverNowMs), local.durationMs)
        val drift = local.positionMs - target
        val commands = mutableListOf<PlayerCommand>()

        if (!room.isPlaying) {
            bufferingSinceMs = null
            steady = false
            correctiveSeekAtMs = null
            if (pausedRoomForStall && !local.isBuffering) {
                // This device paused the room because it stalled; it has recovered, so resume.
                pausedRoomForStall = false
                if (local.speed != 1f) commands += PlayerCommand.SetSpeed(1f)
                return TickResult(commands, PlaybackUpdate(true, local.positionMs), drift)
            }
            if (local.playWhenReady) commands += PlayerCommand.SetPlayWhenReady(false)
            if (abs(drift) > config.pausedToleranceMs) commands += PlayerCommand.SeekTo(target)
            if (local.speed != 1f) commands += PlayerCommand.SetSpeed(1f)
            return TickResult(commands, null, drift)
        }

        if (!local.playWhenReady) commands += PlayerCommand.SetPlayWhenReady(true)

        if (local.isBuffering) {
            // No corrections while buffering: a seek would only restart the buffer.
            val since = bufferingSinceMs ?: serverNowMs.also { bufferingSinceMs = it }
            if (steady && serverNowMs - since >= config.stallMs) {
                steady = false
                pausedRoomForStall = true
                return TickResult(commands, PlaybackUpdate(false, local.positionMs, ChangeReason.STALL), drift)
            }
            return TickResult(commands, null, drift)
        }
        bufferingSinceMs = null
        correctiveSeekAtMs?.let { issuedAt ->
            seekLeadMs = (serverNowMs - issuedAt).coerceIn(config.seekLeadMs, config.maxSeekLeadMs)
            correctiveSeekAtMs = null
        }

        val reachedEnd = local.durationMs != null && target >= local.durationMs
        val nudging = local.speed != 1f
        when {
            reachedEnd -> if (nudging) commands += PlayerCommand.SetSpeed(1f)
            abs(drift) > config.hardSeekMs -> {
                steady = false
                correctiveSeekAtMs = serverNowMs
                commands += PlayerCommand.SeekTo(clamp(target + seekLeadMs, local.durationMs))
                if (nudging) commands += PlayerCommand.SetSpeed(1f)
            }
            nudging -> {
                if (abs(drift) <= config.settleMs) {
                    commands += PlayerCommand.SetSpeed(1f)
                } else {
                    val wanted = nudgeSpeed(drift)
                    if (local.speed != wanted) commands += PlayerCommand.SetSpeed(wanted)
                }
            }
            abs(drift) > config.toleranceMs -> commands += PlayerCommand.SetSpeed(nudgeSpeed(drift))
            else -> if (local.playWhenReady) steady = true
        }
        return TickResult(commands, null, drift)
    }

    /** Ahead of the room: slow down. Behind: speed up. */
    private fun nudgeSpeed(driftMs: Long): Float = if (driftMs > 0) 1f - config.nudge else 1f + config.nudge

    private fun clamp(positionMs: Long, durationMs: Long?): Long {
        val nonNegative = positionMs.coerceAtLeast(0)
        return if (durationMs != null && durationMs > 0) min(nonNegative, durationMs) else nonNegative
    }
}
