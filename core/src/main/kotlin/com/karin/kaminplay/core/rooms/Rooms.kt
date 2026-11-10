package com.karin.kaminplay.core.rooms

import kotlin.random.Random

/** Room codes: six characters from an alphabet without look-alikes (no 0/O, 1/I). */
object RoomCode {
    const val LENGTH = 6
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = Random.Default): String =
        buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** Upper-cases and drops spaces and dashes; returns null unless the result is a valid code. */
    fun normalize(input: String): String? {
        val cleaned = input.uppercase().filter { !it.isWhitespace() && it != '-' }
        return cleaned.takeIf { it.length == LENGTH && it.all { c -> c in ALPHABET } }
    }
}

data class RoomMeta(val filmId: String, val createdBy: String)

data class RoomMember(val uid: String, val name: String, val online: Boolean)

data class ChatMessage(val id: String, val uid: String, val name: String, val text: String, val atMs: Long) {
    companion object {
        const val MAX_LENGTH = 500
    }
}

/** "12:34" or "1:36:05". */
fun formatClock(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
