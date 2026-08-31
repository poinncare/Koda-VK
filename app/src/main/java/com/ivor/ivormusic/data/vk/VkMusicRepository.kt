package com.ivor.ivormusic.data.vk

import android.content.Context
import android.net.Uri
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.SongSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Native Android port of the small protocol surface used by @toil/vk-audio.
 * API and auth details intentionally live here; UI code only sees Koda models.
 */
class VkMusicRepository(context: Context) {
    private val sessionStore = VkSessionStore(context.applicationContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val isSignedIn: Boolean get() = sessionStore.read() != null

    suspend fun signIn(cookieP: String, remixSid: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(WEB_TOKEN_URL)
            .header("User-Agent", WEB_USER_AGENT)
            .header("Origin", "https://vk.ru")
            .header("Referer", "https://vk.ru/")
            .header("Cookie", "p=$cookieP; remixsid=$remixSid")
            .post(FormBody.Builder().add("version", "1").add("app_id", CLIENT_ID).build())
            .build()
        val json = executeJson(request)
        if (json.optString("type") == "error") {
            throw IOException(json.optString("error_info", "VK rejected this session"))
        }
        val data = json.requireObject("data")
        val token = data.requireString("access_token")
        val expires = data.optLong("expires", 0L)
        sessionStore.write(
            VkSession(
                accessToken = token,
                expiresAtSeconds = expires,
                cookieP = cookieP,
                remixSid = remixSid,
            )
        )
    }

    fun signOut() = sessionStore.clear()

    suspend fun loadCatalog(): VkCatalog = withContext(Dispatchers.IO) {
        val catalog = api("catalog.getAudio")
            .requireObject("response")
            .requireObject("catalog")
        val descriptors = catalog.optJSONArray("sections").objects().mapNotNull { item ->
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            id to item.optString("title", "Music")
        }
        val preferred = catalog.optString("default_section")
        val ordered = descriptors.sortedBy { if (it.first == preferred) 0 else 1 }.take(MAX_HOME_SECTIONS)
        val sections = ordered.mapNotNull { (id, title) ->
            runCatching { loadSection(id, title) }.getOrNull()
        }
        val librarySection = sections.firstOrNull { it.id == preferred } ?: sections.firstOrNull()
        VkCatalog(
            sections = sections.filter { it.songs.isNotEmpty() || it.playlists.isNotEmpty() },
            library = librarySection?.songs.orEmpty(),
            playlists = sections.flatMap { it.playlists }.distinctBy { "${it.ownerId}_${it.id}" },
        )
    }

    suspend fun loadSection(id: String, fallbackTitle: String = "Music", startFrom: String? = null): VkSection =
        withContext(Dispatchers.IO) {
            val response = api(
                "catalog.getSection",
                buildMap {
                    put("section_id", id)
                    startFrom?.let { put("start_from", it) }
                },
            ).requireObject("response")
            val section = response.optJSONObject("section") ?: JSONObject()
            val audios = response.optJSONArray("audios").objects().associateBy { audioKey(it) }
            val playlists = response.optJSONArray("playlists").objects().mapNotNull(::parsePlaylist)
            val orderedAudioIds = mutableListOf<String>()
            section.optJSONArray("blocks").objects().forEach { block ->
                if (block.optString("data_type") == "music_audios") {
                    block.optJSONArray("audios_ids").strings().forEach { key ->
                        if (key !in orderedAudioIds) orderedAudioIds += key
                    }
                }
            }
            val songs = if (orderedAudioIds.isEmpty()) {
                audios.values.mapNotNull(::parseSong)
            } else {
                orderedAudioIds.mapNotNull { audios[it] }.mapNotNull(::parseSong)
            }
            VkSection(
                id = section.optString("id", id),
                title = section.optString("title", fallbackTitle),
                songs = songs,
                playlists = playlists,
                nextFrom = section.optString("next_from").takeIf { it.isNotBlank() },
            )
        }

    suspend fun search(query: String, offset: Int = 0): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        api("audio.search", mapOf("q" to query.trim(), "offset" to offset.toString()))
            .requireObject("response")
            .optJSONArray("items")
            .objects()
            .mapNotNull(::parseSong)
    }

    suspend fun getPlaylist(playlist: VkPlaylist): VkPlaylistDetails = withContext(Dispatchers.IO) {
        val params = buildMap {
            put("owner_id", playlist.ownerId.toString())
            put("playlist_id", playlist.id.toString())
            playlist.accessKey?.let { put("access_key", it) }
            put("count", "1000")
        }
        val response = api("audio.get", params).requireObject("response")
        val songs = response.optJSONArray("items").objects().mapNotNull(::parseSong)
        VkPlaylistDetails(playlist.copy(count = songs.size.takeIf { it > 0 } ?: playlist.count), songs)
    }

    suspend fun createPlaylist(title: String, description: String = ""): VkPlaylist = withContext(Dispatchers.IO) {
        val response = api("audio.createPlaylist", mapOf("title" to title, "description" to description))
            .requireObject("response")
        parsePlaylist(response) ?: throw IOException("VK returned an invalid playlist")
    }

    suspend fun deletePlaylist(playlist: VkPlaylist) = withContext(Dispatchers.IO) {
        api("audio.deletePlaylist", mapOf("owner_id" to playlist.ownerId.toString(), "playlist_id" to playlist.id.toString()))
    }

    suspend fun addToPlaylist(playlist: VkPlaylist, song: Song) = withContext(Dispatchers.IO) {
        val ownerId = song.vkOwnerId ?: throw IOException("Missing VK owner id")
        val audioId = song.vkAudioId ?: throw IOException("Missing VK audio id")
        api(
            "audio.addToPlaylist",
            mapOf(
                "owner_id" to playlist.ownerId.toString(),
                "playlist_id" to playlist.id.toString(),
                "audio_ids" to "${ownerId}_${audioId}",
            ),
        )
    }

    suspend fun setLiked(song: Song, liked: Boolean) = withContext(Dispatchers.IO) {
        val ownerId = song.vkOwnerId ?: throw IOException("Missing VK owner id")
        val audioId = song.vkAudioId ?: throw IOException("Missing VK audio id")
        api(
            if (liked) "audio.add" else "audio.delete",
            mapOf("owner_id" to ownerId.toString(), "audio_id" to audioId.toString()),
        )
    }

    suspend fun resolveStream(songId: String): String? = withContext(Dispatchers.IO) {
        val ids = parseSongId(songId) ?: return@withContext null
        val item = api(
            "audio.getById",
            mapOf("audios" to buildString {
                append(ids.first).append('_').append(ids.second)
                ids.third?.takeIf { it.isNotBlank() }?.let { append('_').append(it) }
            }),
        ).requireArray("response").objects().firstOrNull() ?: return@withContext null
        item.optString("url").takeIf { it.startsWith("http") }
    }

    private suspend fun api(method: String, params: Map<String, String> = emptyMap()): JSONObject {
        var session = sessionStore.read() ?: throw VkAuthRequiredException()
        if (session.expiresAtSeconds > 0 && session.expiresAtSeconds - nowSeconds() < REFRESH_THRESHOLD_SECONDS) {
            signIn(session.cookieP, session.remixSid)
            session = sessionStore.read() ?: throw VkAuthRequiredException()
        }
        val body = FormBody.Builder().apply { params.forEach { (key, value) -> add(key, value) } }.build()
        val url = Uri.parse("${API_BASE}${method}").buildUpon()
            .appendQueryParameter("v", API_VERSION)
            .appendQueryParameter("access_token", session.accessToken)
            .appendQueryParameter("lang", "ru")
            .appendQueryParameter("client_id", CLIENT_ID)
            .build().toString()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEB_USER_AGENT)
            .post(body)
            .build()
        val json = try {
            executeJson(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
        json.optJSONObject("error")?.let { error ->
            val code = error.optInt("error_code")
            if (code == 5) {
                sessionStore.clear()
                throw VkAuthRequiredException(error.optString("error_msg"))
            }
            throw IOException(error.optString("error_msg", "VK API error $code"))
        }
        return json
    }

    private fun executeJson(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("VK returned HTTP ${response.code}")
            return runCatching { JSONObject(body) }
                .getOrElse { throw IOException("VK returned an invalid response", it) }
        }
    }

    private fun parseSong(json: JSONObject): Song? {
        val id = json.optLong("id", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: return null
        val ownerId = json.optLong("owner_id", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: return null
        val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null
        val artist = json.optString("artist", "Unknown artist")
        val accessKey = json.optString("access_key").takeIf { it.isNotBlank() }
        val album = json.optJSONObject("album")
        val thumb = json.optJSONObject("thumb") ?: album?.optJSONObject("thumb")
        return Song(
            id = songId(ownerId, id, accessKey),
            title = title,
            artist = artist,
            album = album?.optString("title").orEmpty(),
            duration = json.optLong("duration").coerceAtLeast(0L) * 1000L,
            thumbnailUrl = bestPhoto(thumb),
            source = SongSource.VK,
            dateAdded = json.optLong("date").takeIf { it > 0 }?.times(1000L),
            vkOwnerId = ownerId,
            vkAudioId = id,
            vkAccessKey = accessKey,
            vkLiked = json.optBoolean("like", false),
            vkStreamUrl = json.optString("url").takeIf { it.startsWith("http") },
        )
    }

    private fun parsePlaylist(json: JSONObject): VkPlaylist? {
        val id = json.optLong("id", json.optLong("playlist_id", Long.MIN_VALUE))
            .takeIf { it != Long.MIN_VALUE } ?: return null
        val ownerId = json.optLong("owner_id", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: return null
        val photo = json.optJSONObject("photo") ?: json.optJSONArray("thumbs").objects().firstOrNull()
        return VkPlaylist(
            id = id,
            ownerId = ownerId,
            title = json.optString("title", "Playlist"),
            description = json.optString("description"),
            count = json.optInt("count"),
            artworkUrl = bestPhoto(photo),
            accessKey = json.optString("access_key").takeIf { it.isNotBlank() },
            canEdit = json.optJSONObject("permissions")?.optBoolean("edit", false) == true,
        )
    }

    private fun audioKey(json: JSONObject) = "${json.optLong("owner_id")}_${json.optLong("id")}"

    private fun bestPhoto(json: JSONObject?): String? {
        if (json == null) return null
        val preferred = listOf("photo_1200", "photo_600", "photo_300", "photo_270", "photo_135", "photo_68")
        return preferred.firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.startsWith("http") } }
            ?: json.keys().asSequence().map { json.optString(it) }.firstOrNull { it.startsWith("http") }
    }

    private fun JSONArray?.objects(): List<JSONObject> = buildList {
        val source = this@objects ?: return@buildList
        for (index in 0 until source.length()) source.optJSONObject(index)?.let(::add)
    }

    private fun JSONArray?.strings(): List<String> = buildList {
        val source = this@strings ?: return@buildList
        for (index in 0 until source.length()) source.optString(index).takeIf { it.isNotBlank() }?.let(::add)
    }

    private fun JSONObject.requireObject(key: String): JSONObject =
        optJSONObject(key) ?: throw IOException("VK response has no $key object")

    private fun JSONObject.requireArray(key: String): JSONArray =
        optJSONArray(key) ?: throw IOException("VK response has no $key array")

    private fun JSONObject.requireString(key: String): String =
        optString(key).takeIf { it.isNotBlank() } ?: throw IOException("VK response has no $key")

    private fun nowSeconds() = System.currentTimeMillis() / 1000L

    class VkAuthRequiredException(message: String = "Sign in to VK Music") : IOException(message)

    companion object {
        private const val CLIENT_ID = "6287487"
        private const val API_VERSION = "5.282"
        private const val API_BASE = "https://api.vk.ru/method/"
        private const val WEB_TOKEN_URL = "https://login.vk.ru/?act=web_token"
        private const val WEB_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/127.0 Mobile Safari/537.36"
        private const val REFRESH_THRESHOLD_SECONDS = 600L
        private const val MAX_HOME_SECTIONS = 8

        fun songId(ownerId: Long, audioId: Long, accessKey: String?): String =
            buildString {
                append("vk:").append(ownerId).append(':').append(audioId)
                accessKey?.takeIf { it.isNotBlank() }?.let { append(':').append(it) }
            }

        fun parseSongId(value: String): Triple<Long, Long, String?>? {
            if (!value.startsWith("vk:")) return null
            val parts = value.split(':', limit = 4)
            if (parts.size < 3) return null
            return Triple(parts[1].toLongOrNull() ?: return null, parts[2].toLongOrNull() ?: return null, parts.getOrNull(3))
        }
    }
}
