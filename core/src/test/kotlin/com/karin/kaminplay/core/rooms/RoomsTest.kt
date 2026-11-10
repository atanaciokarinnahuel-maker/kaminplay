package com.karin.kaminplay.core.rooms

import com.karin.kaminplay.core.SeedCatalog
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomsTest {
    @Test
    fun generatedCodesAreValid() {
        val random = Random(7)
        repeat(200) {
            val code = RoomCode.generate(random)
            assertEquals(code, RoomCode.normalize(code))
        }
    }

    @Test
    fun normalizeAcceptsHowPeopleTypeCodes() {
        assertEquals("ABC234", RoomCode.normalize(" abc-234 "))
        assertEquals("ABC234", RoomCode.normalize("abc 234"))
        assertNull(RoomCode.normalize("ABC23"), "too short")
        assertNull(RoomCode.normalize("ABC2345"), "too long")
        assertNull(RoomCode.normalize("ABC10O"), "0, 1 and O are not in the alphabet")
    }

    @Test
    fun clockFormatting() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:00", formatClock(-500))
        assertEquals("12:34", formatClock(754_900))
        assertEquals("1:36:05", formatClock(5_765_000))
    }

    @Test
    fun playableFilmsHaveAnArchiveItem() {
        val playable = SeedCatalog.films.filter { it.isPlayable }
        assertEquals(20, playable.size)
        assertTrue(playable.all { assertNotNull(it.archiveId).isNotBlank() })
        assertNull(SeedCatalog.byId("metropolis-1927")!!.archiveId)
    }
}
