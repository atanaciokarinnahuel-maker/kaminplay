package com.karin.kaminplay.core.sync

import com.karin.kaminplay.core.sync.PlayerCommand.SeekTo
import com.karin.kaminplay.core.sync.PlayerCommand.SetPlayWhenReady
import com.karin.kaminplay.core.sync.PlayerCommand.SetSpeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncEngineTest {
    private val me = "me"
    private val her = "her"
    private val hour = 3_600_000L

    private fun engine() = SyncEngine(me)
    private fun playing(position: Long, at: Long, by: String = her) = PlaybackSnapshot(true, position, at, by)
    private fun paused(position: Long, at: Long, by: String = her, reason: ChangeReason = ChangeReason.USER) =
        PlaybackSnapshot(false, position, at, by, reason)
    private fun local(position: Long, play: Boolean = true, buffering: Boolean = false, speed: Float = 1f) =
        LocalPlayback(position, play, buffering, hour, speed)

    @Test
    fun snapshotExtrapolatesOnlyWhilePlaying() {
        assertEquals(15_000, playing(10_000, 1_000).positionAt(6_000))
        assertEquals(10_000, paused(10_000, 1_000).positionAt(6_000))
        assertEquals(10_000, playing(10_000, 1_000).positionAt(500), "a clock behind the stamp never rewinds")
    }

    @Test
    fun doesNothingWithoutRoomState() {
        assertEquals(TickResult(), engine().tick(local(0), 0))
    }

    @Test
    fun inSyncNeedsNoCommands() {
        val e = engine().apply { onRemote(playing(10_000, 1_000)) }
        val result = e.tick(local(15_050), 6_000)
        assertTrue(result.commands.isEmpty())
        assertEquals(50, result.driftMs)
    }

    @Test
    fun smallDriftIsClosedWithASpeedNudge() {
        val e = engine().apply { onRemote(playing(10_000, 1_000)) }
        assertEquals(listOf<PlayerCommand>(SetSpeed(0.95f)), e.tick(local(15_400), 6_000).commands, "ahead: slow down")
        assertEquals(listOf<PlayerCommand>(SetSpeed(1.05f)), e.tick(local(14_600), 6_000).commands, "behind: speed up")
    }

    @Test
    fun nudgeKeepsGoingUntilSettledThenReturnsToNormalSpeed() {
        val e = engine().apply { onRemote(playing(10_000, 1_000)) }
        assertTrue(e.tick(local(15_100, speed = 0.95f), 6_000).commands.isEmpty(), "still 100 ms ahead: keep nudging")
        assertEquals(listOf<PlayerCommand>(SetSpeed(1f)), e.tick(local(15_030, speed = 0.95f), 6_000).commands)
        assertEquals(listOf<PlayerCommand>(SetSpeed(1.05f)), e.tick(local(14_800, speed = 0.95f), 6_000).commands, "overshot: flip direction")
    }

    @Test
    fun largeDriftIsFixedWithASeekSlightlyAhead() {
        val e = engine().apply { onRemote(playing(10_000, 1_000)) }
        assertEquals(listOf<PlayerCommand>(SeekTo(15_300)), e.tick(local(2_000), 6_000).commands)
        assertEquals(listOf(SeekTo(15_300), SetSpeed(1f)), e.tick(local(60_000, speed = 1.05f), 6_000).commands)
    }

    @Test
    fun pauseStopsTheLocalPlayerAtTheExactPosition() {
        val e = engine().apply { onRemote(paused(42_000, 1_000)) }
        assertEquals(listOf(SetPlayWhenReady(false), SeekTo(42_000)), e.tick(local(42_900), 9_000).commands)
        assertTrue(e.tick(local(42_040, play = false), 9_000).commands.isEmpty())
    }

    @Test
    fun playStartsAPausedLocalPlayer() {
        val e = engine().apply { onRemote(playing(10_000, 1_000)) }
        assertEquals(listOf<PlayerCommand>(SetPlayWhenReady(true)), e.tick(local(15_000, play = false), 6_000).commands)
    }

    @Test
    fun targetIsClampedToTheDuration() {
        val e = engine().apply { onRemote(playing(hour - 1_000, 0)) }
        val result = e.tick(local(hour), 60_000)
        assertTrue(result.commands.isEmpty())
        assertEquals(0, result.driftMs)
    }

    @Test
    fun userActionsBecomeRoomUpdates() {
        val e = engine().apply { onRemote(playing(10_000, 1_000)) }
        assertEquals(PlaybackUpdate(false, 15_000), e.userPause(local(15_000)))
        assertEquals(PlaybackUpdate(true, 15_000), e.userPlay(local(15_000, play = false)))
        assertEquals(PlaybackUpdate(true, 90_000), e.userSeek(90_000, local(15_000)), "seeking keeps the room playing")
        assertEquals(PlaybackUpdate(true, hour), e.userSeek(hour * 2, local(15_000)), "seek is clamped to the duration")
        val pausedRoom = engine().apply { onRemote(paused(10_000, 1_000)) }
        assertEquals(PlaybackUpdate(false, 0), pausedRoom.userSeek(-5_000, local(10_000, play = false)))
    }

    @Test
    fun stallPausesTheRoomAndRecoveryResumesIt() {
        val e = engine().apply { onRemote(playing(0, 0)) }
        assertTrue(e.tick(local(10_000), 10_000).commands.isEmpty())                 // steady
        assertNull(e.tick(local(10_500, buffering = true), 10_500).publish)           // stall starts
        assertNull(e.tick(local(10_500, buffering = true), 12_000).publish)           // 1.5 s: wait
        val stalled = e.tick(local(10_500, buffering = true), 12_600)                 // 2.1 s: pause the room
        assertEquals(PlaybackUpdate(false, 10_500, ChangeReason.STALL), stalled.publish)

        e.onRemote(paused(10_500, 12_650, by = me, reason = ChangeReason.STALL))      // our own write comes back
        assertNull(e.tick(local(10_500, buffering = true), 13_000).publish, "still buffering: stay paused")
        val recovered = e.tick(local(10_500, buffering = false), 14_000)
        assertEquals(PlaybackUpdate(true, 10_500), recovered.publish)
        assertTrue(recovered.commands.isEmpty())
    }

    @Test
    fun bufferingWhileJoiningDoesNotPauseTheRoom() {
        val e = engine().apply { onRemote(playing(0, 0)) }
        // Never steady: this device has just joined and is loading the video.
        for (now in listOf(1_000L, 2_000L, 4_000L, 8_000L)) {
            val result = e.tick(local(0, buffering = true), now)
            assertNull(result.publish)
            assertTrue(result.commands.none { it is SeekTo }, "no seeks while buffering")
        }
        // Once loaded it jumps to the room position.
        assertEquals(listOf<PlayerCommand>(SeekTo(9_300)), e.tick(local(0), 9_000).commands)
    }

    @Test
    fun aShortBufferDoesNotPauseTheRoom() {
        val e = engine().apply { onRemote(playing(0, 0)) }
        e.tick(local(10_000), 10_000)
        assertNull(e.tick(local(10_200, buffering = true), 10_200).publish)
        assertNull(e.tick(local(10_900), 11_000).publish)                             // recovered after 0.8 s
        assertNull(e.tick(local(11_900, buffering = true), 12_000).publish, "the stall timer restarted")
    }

    @Test
    fun someoneElseTakingOverCancelsTheAutomaticResume() {
        val e = engine().apply { onRemote(playing(0, 0)) }
        e.tick(local(10_000), 10_000)
        e.tick(local(10_500, buffering = true), 10_500)
        assertEquals(ChangeReason.STALL, e.tick(local(10_500, buffering = true), 12_600).publish?.reason)
        e.onRemote(paused(10_500, 12_650, by = me, reason = ChangeReason.STALL))
        e.onRemote(paused(30_000, 13_000, by = her))                                  // she seeks while paused
        val result = e.tick(local(10_500, play = false), 14_000)
        assertNull(result.publish, "her pause must not be undone")
        assertEquals(listOf<PlayerCommand>(SeekTo(30_000)), result.commands)
    }

    @Test
    fun ownUserPauseDuringAStallIsNotUndone() {
        val e = engine().apply { onRemote(playing(0, 0)) }
        e.tick(local(10_000), 10_000)
        e.tick(local(10_500, buffering = true), 10_500)
        e.tick(local(10_500, buffering = true), 12_600)                               // stall pause published
        e.userPause(local(10_500, buffering = true))                                  // then I pause on purpose
        e.onRemote(paused(10_500, 12_700, by = me))
        assertNull(e.tick(local(10_500, play = false), 14_000).publish)
    }

    @Test
    fun seekLeadAdaptsToHowLongSeeksTakeOnThisConnection() {
        val e = engine().apply { onRemote(playing(0, 0)) }
        // First correction uses the default lead.
        assertEquals(listOf<PlayerCommand>(SeekTo(10_300)), e.tick(local(0), 10_000).commands)
        // The seek needs 2 s of buffering, so the device is behind again when it finishes...
        assertTrue(e.tick(local(10_300, buffering = true), 11_000).commands.isEmpty())
        // ...and the next correction aims 2 s ahead instead of repeating the same miss.
        assertEquals(listOf<PlayerCommand>(SeekTo(14_000)), e.tick(local(10_300), 12_000).commands)
        // That one lands close enough for a speed nudge to finish the job.
        assertTrue(e.tick(local(14_000, buffering = true), 13_000).commands.isEmpty())
        assertEquals(listOf<PlayerCommand>(SetSpeed(1.05f)), e.tick(local(14_000), 14_200).commands)
    }

    @Test
    fun seekLeadIsCapped() {
        val e = engine().apply { onRemote(playing(0, 0)) }
        e.tick(local(0), 10_000)                                   // corrective seek
        e.tick(local(10_300, buffering = true), 20_000)
        e.tick(local(10_300), 40_000)                              // it took 30 s; the lead is capped at 6 s
        assertEquals(listOf<PlayerCommand>(SeekTo(56_000)), e.tick(local(10_300), 50_000).commands)
    }
}
