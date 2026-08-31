package com.ivor.ivormusic.data.vk

import com.ivor.ivormusic.data.Song

data class VkSession(
    val accessToken: String,
    val expiresAtSeconds: Long,
    val cookieP: String,
    val remixSid: String,
)

data class VkSection(
    val id: String,
    val title: String,
    val songs: List<Song> = emptyList(),
    val playlists: List<VkPlaylist> = emptyList(),
    val nextFrom: String? = null,
)

data class VkPlaylist(
    val id: Long,
    val ownerId: Long,
    val title: String,
    val description: String = "",
    val count: Int = 0,
    val artworkUrl: String? = null,
    val accessKey: String? = null,
    val canEdit: Boolean = false,
)

data class VkCatalog(
    val sections: List<VkSection> = emptyList(),
    val library: List<Song> = emptyList(),
    val playlists: List<VkPlaylist> = emptyList(),
)

data class VkPlaylistDetails(
    val playlist: VkPlaylist,
    val songs: List<Song>,
)

sealed interface VkLoadState<out T> {
    data object Loading : VkLoadState<Nothing>
    data class Ready<T>(val value: T) : VkLoadState<T>
    data class Error(val message: String) : VkLoadState<Nothing>
}
