package com.karin.kaminplay.player

import com.karin.kaminplay.core.Film
import com.karin.kaminplay.core.archive.ArchiveFile
import com.karin.kaminplay.core.archive.ArchiveSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A problem finding something to play, with a message that can be shown as is. */
class SourceException(message: String) : Exception(message)

/** Finds the streamable file of a film through Internet Archive's metadata API. */
class ArchiveResolver {

    suspend fun resolve(film: Film): String = withContext(Dispatchers.IO) {
        val itemId = film.archiveId
            ?: throw SourceException("Esta película todavía no tiene una copia verificada para ver en la app.")
        val files = try {
            parseFiles(httpGet(ArchiveSource.metadataUrl(itemId)))
        } catch (e: IOException) {
            throw SourceException("No se pudo contactar con Internet Archive. Revisa tu conexión.")
        } catch (e: JSONException) {
            throw SourceException("Internet Archive devolvió una respuesta que no se pudo leer.")
        }
        val file = ArchiveSource.pick(files, film.minutes)
            ?: throw SourceException("Internet Archive no tiene un archivo de vídeo compatible para esta película.")
        ArchiveSource.downloadUrl(itemId, file.name)
    }

    private fun parseFiles(json: String): List<ArchiveFile> {
        val array = JSONObject(json).optJSONArray("files") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val entry = array.optJSONObject(index) ?: return@mapNotNull null
            ArchiveFile(
                name = entry.optString("name"),
                format = entry.optString("format"),
                sizeBytes = entry.optString("size").toLongOrNull() ?: 0L,
                lengthSeconds = ArchiveSource.parseLength(entry.optString("length")),
            )
        }
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
