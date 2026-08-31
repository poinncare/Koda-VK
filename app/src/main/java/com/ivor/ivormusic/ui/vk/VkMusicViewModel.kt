package com.ivor.ivormusic.ui.vk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.vk.VkCatalog
import com.ivor.ivormusic.data.vk.VkLoadState
import com.ivor.ivormusic.data.vk.VkMusicRepository
import com.ivor.ivormusic.data.vk.VkPlaylist
import com.ivor.ivormusic.data.vk.VkPlaylistDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VkMusicViewModel(context: Context) : ViewModel() {
    private val repository = VkMusicRepository(context.applicationContext)

    private val _signedIn = MutableStateFlow(repository.isSignedIn)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private val _catalog = MutableStateFlow<VkLoadState<VkCatalog>>(VkLoadState.Loading)
    val catalog: StateFlow<VkLoadState<VkCatalog>> = _catalog.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _search = MutableStateFlow<VkLoadState<List<Song>>>(VkLoadState.Ready(emptyList()))
    val search: StateFlow<VkLoadState<List<Song>>> = _search.asStateFlow()

    private val _playlist = MutableStateFlow<VkLoadState<VkPlaylistDetails>?>(null)
    val playlist: StateFlow<VkLoadState<VkPlaylistDetails>?> = _playlist.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var searchJob: Job? = null

    init {
        if (_signedIn.value) refresh() else _catalog.value = VkLoadState.Ready(VkCatalog())
    }

    fun signIn(cookieP: String, remixSid: String) {
        viewModelScope.launch {
            _catalog.value = VkLoadState.Loading
            runAction {
                repository.signIn(cookieP, remixSid)
                _signedIn.value = true
                loadCatalog()
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _signedIn.value = false
        _catalog.value = VkLoadState.Ready(VkCatalog())
        _search.value = VkLoadState.Ready(emptyList())
        _playlist.value = null
    }

    fun refresh() {
        if (!_signedIn.value) return
        viewModelScope.launch {
            _catalog.value = VkLoadState.Loading
            runAction { loadCatalog() }
        }
    }

    private suspend fun loadCatalog() {
        try {
            _catalog.value = VkLoadState.Ready(repository.loadCatalog())
        } catch (auth: VkMusicRepository.VkAuthRequiredException) {
            _signedIn.value = false
            _catalog.value = VkLoadState.Ready(VkCatalog())
            _message.value = auth.message
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _catalog.value = VkLoadState.Error(error.userMessage())
        }
    }

    fun setQuery(value: String) {
        _query.value = value
        searchJob?.cancel()
        if (value.isBlank()) {
            _search.value = VkLoadState.Ready(emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _search.value = VkLoadState.Loading
            try {
                _search.value = VkLoadState.Ready(repository.search(value))
            } catch (auth: VkMusicRepository.VkAuthRequiredException) {
                _signedIn.value = false
                _search.value = VkLoadState.Error(auth.message ?: "Sign in again")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _search.value = VkLoadState.Error(error.userMessage())
            }
        }
    }

    fun openPlaylist(value: VkPlaylist) {
        _playlist.value = VkLoadState.Loading
        viewModelScope.launch {
            try {
                _playlist.value = VkLoadState.Ready(repository.getPlaylist(value))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _playlist.value = VkLoadState.Error(error.userMessage())
            }
        }
    }

    fun closePlaylist() {
        _playlist.value = null
    }

    fun createPlaylist(title: String, description: String) {
        viewModelScope.launch {
            runAction {
                repository.createPlaylist(title.trim(), description.trim())
                _message.value = "Playlist created"
                loadCatalog()
            }
        }
    }

    fun deletePlaylist(value: VkPlaylist) {
        viewModelScope.launch {
            runAction {
                repository.deletePlaylist(value)
                _playlist.value = null
                _message.value = "Playlist deleted"
                loadCatalog()
            }
        }
    }

    fun addToPlaylist(value: VkPlaylist, song: Song) {
        viewModelScope.launch {
            runAction {
                repository.addToPlaylist(value, song)
                _message.value = "Added to ${value.title}"
            }
        }
    }

    fun toggleFavorite(song: Song) {
        val liked = !song.vkLiked
        replaceSong(song.copy(vkLiked = liked))
        viewModelScope.launch {
            try {
                repository.setLiked(song, liked)
                _message.value = if (liked) "Added to My music" else "Removed from My music"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                replaceSong(song)
                _message.value = error.userMessage()
            }
        }
    }

    fun reflectFavorite(song: Song, liked: Boolean) {
        replaceSong(song.copy(vkLiked = liked))
    }

    private fun replaceSong(updated: Song) {
        _catalog.value = (_catalog.value as? VkLoadState.Ready)?.let { ready ->
            VkLoadState.Ready(
                ready.value.copy(
                    library = ready.value.library.map { if (it.id == updated.id) updated else it },
                    sections = ready.value.sections.map { section ->
                        section.copy(songs = section.songs.map { if (it.id == updated.id) updated else it })
                    },
                )
            )
        } ?: _catalog.value
        _search.value = (_search.value as? VkLoadState.Ready)?.let { ready ->
            VkLoadState.Ready(ready.value.map { if (it.id == updated.id) updated else it })
        } ?: _search.value
        _playlist.value = (_playlist.value as? VkLoadState.Ready)?.let { ready ->
            VkLoadState.Ready(ready.value.copy(songs = ready.value.songs.map { if (it.id == updated.id) updated else it }))
        } ?: _playlist.value
    }

    fun consumeMessage() {
        _message.value = null
    }

    private suspend fun runAction(block: suspend () -> Unit) {
        try {
            block()
        } catch (auth: VkMusicRepository.VkAuthRequiredException) {
            _signedIn.value = false
            _message.value = auth.message
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _message.value = error.userMessage()
        }
    }

    private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "VK Music request failed"
}
