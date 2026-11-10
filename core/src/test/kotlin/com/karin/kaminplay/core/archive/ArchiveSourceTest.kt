package com.karin.kaminplay.core.archive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArchiveSourceTest {
    private fun file(name: String, format: String, mb: Long, minutes: Double?) =
        ArchiveFile(name, format, mb * 1024 * 1024, minutes?.let { it * 60 })

    @Test
    fun prefersTheWebReadyDerivativeOfTheRightLength() {
        val files = listOf(
            file("film.avi", "Cinepack", 700, 96.0),
            file("film_512kb.mp4", "512Kb MPEG4", 388, 96.0),
            file("film.mp4", "h.264", 569, 96.0),
            file("trailer.mp4", "h.264", 20, 2.0),
        )
        assertEquals("film.mp4", ArchiveSource.pick(files, 96)?.name)
    }

    @Test
    fun skipsFilesTooLargeForAPhoneWhenThereIsAnAlternative() {
        val files = listOf(file("film_hd.mp4", "h.264", 2_983, 87.0), file("film_512kb.mp4", "512Kb MPEG4", 300, 87.0))
        assertEquals("film_512kb.mp4", ArchiveSource.pick(files, 86)?.name)
        assertEquals("film_hd.mp4", ArchiveSource.pick(files.take(1), 86)?.name, "the only copy is still better than nothing")
    }

    @Test
    fun acceptsAShorterCutButNotUnrelatedLengths() {
        assertEquals("cut.mp4", ArchiveSource.pick(listOf(file("cut.mp4", "512Kb MPEG4", 211, 51.0)), 76)?.name)
        assertNull(ArchiveSource.pick(listOf(file("short.mp4", "h.264", 30, 9.0)), 76))
        assertNull(ArchiveSource.pick(listOf(file("film.ogv", "Ogg Video", 300, 76.0)), 76))
    }

    @Test
    fun fallsBackToFilesWithoutALengthOnlyWhenNoneHasOne() {
        val unknown = file("a.mp4", "MPEG4", 400, null)
        assertEquals("a.mp4", ArchiveSource.pick(listOf(unknown), 90)?.name)
        assertEquals("b.mp4", ArchiveSource.pick(listOf(unknown, file("b.mp4", "512Kb MPEG4", 300, 90.0)), 90)?.name)
    }

    @Test
    fun parsesBothLengthFormats() {
        assertEquals(5760.03, ArchiveSource.parseLength("5760.03"))
        assertEquals(5760.0, ArchiveSource.parseLength("1:36:00"))
        assertEquals(754.0, ArchiveSource.parseLength("12:34"))
        assertNull(ArchiveSource.parseLength(""))
        assertNull(ArchiveSource.parseLength("unknown"))
    }

    @Test
    fun buildsUrlsWithEncodedNames() {
        assertEquals("https://archive.org/metadata/Little_ShopOf_Horrors.avi", ArchiveSource.metadataUrl("Little_ShopOf_Horrors.avi"))
        assertEquals(
            "https://archive.org/download/silent-safety-last/Safety%20Last%21.mp4",
            ArchiveSource.downloadUrl("silent-safety-last", "Safety Last!.mp4"),
        )
        assertEquals(
            "https://archive.org/download/item/Sub%20Folder/A%20%26%20B.mp4",
            ArchiveSource.downloadUrl("item", "Sub Folder/A & B.mp4"),
        )
    }
}
