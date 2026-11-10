package com.karin.kaminplay.core.archive

import java.net.URLEncoder
import kotlin.math.abs

/** One entry of an Internet Archive item's file list (https://archive.org/metadata/{id}). */
data class ArchiveFile(val name: String, val format: String, val sizeBytes: Long, val lengthSeconds: Double?)

object ArchiveSource {
    private const val MAX_BYTES = 1_200L * 1024 * 1024

    fun metadataUrl(itemId: String): String = "https://archive.org/metadata/" + encodeSegment(itemId)

    /** Direct file URL. File names can contain spaces, brackets and sub-folders. */
    fun downloadUrl(itemId: String, fileName: String): String =
        "https://archive.org/download/" + encodeSegment(itemId) + "/" + fileName.split('/').joinToString("/") { encodeSegment(it) }

    /** The API reports durations either as seconds ("5760.03") or as a clock ("1:36:00"). */
    fun parseLength(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.trim().split(':')
        var seconds = 0.0
        for (part in parts) seconds = seconds * 60 + (part.toDoubleOrNull() ?: return null)
        return seconds
    }

    /**
     * Chooses the file to stream: an MP4 whose length is close to the film's runtime (so trailers
     * and extras are skipped), preferring the Archive's web-ready H.264 derivative and avoiding
     * files too large for a phone connection.
     */
    fun pick(files: List<ArchiveFile>, expectedMinutes: Int): ArchiveFile? {
        val expected = expectedMinutes * 60.0
        val mp4 = files.filter { it.name.endsWith(".mp4", ignoreCase = true) }
        val plausible = mp4.filter { it.lengthSeconds != null && it.lengthSeconds / expected in 0.5..1.5 }
        val pool = plausible.ifEmpty { mp4.filter { it.lengthSeconds == null } }
        if (pool.isEmpty()) return null
        val affordable = pool.filter { it.sizeBytes <= MAX_BYTES }
        if (affordable.isEmpty()) return pool.minByOrNull { it.sizeBytes }
        return affordable.sortedWith(
            compareByDescending<ArchiveFile> { formatRank(it.format) }
                .thenBy { abs((it.lengthSeconds ?: expected) - expected) }
                .thenByDescending { it.sizeBytes },
        ).first()
    }

    private fun formatRank(format: String): Int = when (format.lowercase()) {
        "h.264", "h.264 ia" -> 3
        "h.264 mpeg4", "mpeg4" -> 2
        "512kb mpeg4" -> 1
        else -> 0
    }

    private fun encodeSegment(segment: String): String = URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
}
