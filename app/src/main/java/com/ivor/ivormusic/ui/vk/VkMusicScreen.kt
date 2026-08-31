package com.ivor.ivormusic.ui.vk

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ivor.ivormusic.data.PlayerStyle
import com.ivor.ivormusic.data.Song
import com.ivor.ivormusic.data.vk.VkCatalog
import com.ivor.ivormusic.data.vk.VkLoadState
import com.ivor.ivormusic.data.vk.VkPlaylist
import com.ivor.ivormusic.data.vk.VkPlaylistDetails
import com.ivor.ivormusic.ui.player.ExpandablePlayer
import com.ivor.ivormusic.ui.player.PlayerViewModel

private enum class VkTab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home),
    SEARCH("Search", Icons.Rounded.Search, Icons.Outlined.Search),
    LIBRARY("Library", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
}

private enum class SearchKind(val label: String) { TRACKS("Tracks"), ARTISTS("Artists"), ALBUMS("Albums"), PLAYLISTS("Playlists") }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VkMusicScreen(
    playerViewModel: PlayerViewModel,
    ambientBackground: Boolean,
    artworkColors: Boolean,
    playerStyle: PlayerStyle,
    onPlayerStyleChange: (PlayerStyle) -> Unit,
    isDarkMode: Boolean = true,
    nonExpressiveNavigationBar: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: VkMusicViewModel = viewModel { VkMusicViewModel(context.applicationContext) }
    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        VkAuthActivity.sessionFrom(result.data)?.let { session ->
            viewModel.signIn(session.cookieP, session.remixSid)
        }
    }
    val signedIn by viewModel.signedIn.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val search by viewModel.search.collectAsState()
    val query by viewModel.query.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val message by viewModel.message.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isBuffering by playerViewModel.isBuffering.collectAsState()
    val playWhenReady by playerViewModel.playWhenReady.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    var tab by rememberSaveable { mutableStateOf(VkTab.HOME) }
    var playerExpanded by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var createPlaylist by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    BackHandler(enabled = playlist != null || tab != VkTab.HOME) {
        if (playlist != null) viewModel.closePlaylist() else tab = VkTab.HOME
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomOverlay = if (currentSong != null) 188.dp else 100.dp
        val contentModifier = Modifier.fillMaxSize()
        AnimatedContent(
            targetState = playlist to tab,
            label = "VkKodaNavigation",
            transitionSpec = {
                val forward = targetState.second.ordinal >= initialState.second.ordinal
                if (forward) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
        ) { (opened, targetTab) ->
            when (opened) {
                null -> when (targetTab) {
                    VkTab.HOME -> VkHome(
                        state = catalog,
                        signedIn = signedIn,
                        onSignIn = { authLauncher.launch(VkAuthActivity.createIntent(context)) },
                        onRefresh = viewModel::refresh,
                        onPlay = playerViewModel::playSong,
                        onPlayAll = playerViewModel::playQueue,
                        onPlaylist = viewModel::openPlaylist,
                        onLongPress = { selectedSong = it },
                        modifier = contentModifier,
                        contentPadding = PaddingValues(top = statusInset, bottom = bottomOverlay + navInset),
                        onSettings = onNavigateToSettings,
                        onDownloads = onNavigateToDownloads,
                        isDarkMode = isDarkMode,
                    )
                    VkTab.SEARCH -> VkSearch(
                        state = search,
                        query = query,
                        signedIn = signedIn,
                        playlists = (catalog as? VkLoadState.Ready)?.value?.playlists.orEmpty(),
                        onQuery = viewModel::setQuery,
                        onSignIn = { authLauncher.launch(VkAuthActivity.createIntent(context)) },
                        onPlay = playerViewModel::playSong,
                        onPlayAll = playerViewModel::playQueue,
                        onPlaylist = viewModel::openPlaylist,
                        onLongPress = { selectedSong = it },
                        modifier = contentModifier.padding(top = statusInset, bottom = bottomOverlay + navInset),
                    )
                    VkTab.LIBRARY -> VkLibrary(
                        state = catalog,
                        signedIn = signedIn,
                        onSignIn = { authLauncher.launch(VkAuthActivity.createIntent(context)) },
                        onSignOut = viewModel::signOut,
                        onPlay = playerViewModel::playSong,
                        onPlayAll = playerViewModel::playQueue,
                        onPlaylist = viewModel::openPlaylist,
                        onLongPress = { selectedSong = it },
                        modifier = contentModifier,
                        contentPadding = PaddingValues(start = 16.dp, top = statusInset + 12.dp, end = 16.dp, bottom = bottomOverlay + navInset),
                    )
                }
                is VkLoadState.Loading -> LoadingState(contentModifier)
                is VkLoadState.Error -> ErrorState(opened.message, viewModel::closePlaylist, contentModifier)
                is VkLoadState.Ready -> PlaylistScreen(
                    details = opened.value,
                    onBack = viewModel::closePlaylist,
                    onPlay = playerViewModel::playSong,
                    onPlayAll = playerViewModel::playQueue,
                    onLongPress = { selectedSong = it },
                    onDelete = if (opened.value.playlist.canEdit) ({ viewModel.deletePlaylist(opened.value.playlist) }) else null,
                    modifier = contentModifier.padding(top = statusInset, bottom = bottomOverlay + navInset),
                )
            }
        }

        if (tab == VkTab.LIBRARY && playlist == null && signedIn) {
            FloatingActionButton(
                onClick = { createPlaylist = true },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                    .padding(end = 24.dp, bottom = bottomOverlay + 12.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = "Create playlist") }
        }

        if (nonExpressiveNavigationBar) {
            NavigationBar(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                VkTab.entries.forEach { item ->
                    val selected = tab == item
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.closePlaylist(); tab = item },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp),
            ) {
                VkTab.entries.forEach { item ->
                    val selected = tab == item
                    Surface(
                        selected = selected,
                        onClick = { viewModel.closePlaylist(); tab = item },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = if (selected) 20.dp else 12.dp).animateContentSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (selected) item.selectedIcon else item.icon, item.label, Modifier.size(24.dp))
                            AnimatedVisibility(selected) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).padding(top = statusInset))

        ExpandablePlayer(
            isExpanded = playerExpanded,
            onExpandChange = { playerExpanded = it },
            currentSong = currentSong,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            playWhenReady = playWhenReady,
            progress = if (duration > 0) progress.toFloat() / duration else 0f,
            duration = duration,
            onPlayPauseClick = playerViewModel::togglePlayPause,
            onNextClick = playerViewModel::skipToNext,
            viewModel = playerViewModel,
            ambientBackground = ambientBackground,
            artworkColors = artworkColors,
            playerStyle = playerStyle,
            onPlayerStyleChange = onPlayerStyleChange,
            collapsedBottomSpacing = if (nonExpressiveNavigationBar) 96.dp else 100.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    selectedSong?.let { song ->
        SongActionsDialog(
            song = song,
            playlists = (catalog as? VkLoadState.Ready)?.value?.playlists.orEmpty(),
            onDismiss = { selectedSong = null },
            onPlayNext = { playerViewModel.playNext(song) },
            onQueue = { playerViewModel.addToQueue(song) },
            onFavorite = {
                val liked = playerViewModel.toggleLike(song)
                viewModel.reflectFavorite(song, liked)
            },
            onPlaylist = { viewModel.addToPlaylist(it, song) },
        )
    }
    if (createPlaylist) {
        CreatePlaylistDialog(
            onDismiss = { createPlaylist = false },
            onCreate = { title, description -> viewModel.createPlaylist(title, description); createPlaylist = false },
        )
    }
}

@Composable
private fun VkHome(
    state: VkLoadState<VkCatalog>,
    signedIn: Boolean,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onPlaylist: (VkPlaylist) -> Unit,
    onLongPress: (Song) -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onSettings: () -> Unit,
    onDownloads: () -> Unit,
    isDarkMode: Boolean,
) {
    var showMixSettings by rememberSaveable { mutableStateOf(false) }
    var moods by rememberSaveable { mutableStateOf("Any") }
    var familiarity by rememberSaveable { mutableStateOf("Any") }
    var language by rememberSaveable { mutableStateOf("Any") }
    when {
        !signedIn -> SignedOutState(onSignIn, modifier)
        state is VkLoadState.Loading -> LoadingState(modifier)
        state is VkLoadState.Error -> ErrorState(state.message, onRefresh, modifier)
        state is VkLoadState.Ready -> {
            val sourceMix = state.value.sections.firstOrNull { section ->
                section.title.contains("mix", ignoreCase = true) || section.title.contains("микс", ignoreCase = true)
            }?.songs.orEmpty().ifEmpty { state.value.sections.firstOrNull { it.songs.isNotEmpty() }?.songs.orEmpty() }
            val mixSongs = remember(sourceMix, moods, familiarity, language) {
                applyMixSettings(sourceMix, moods, familiarity, language)
            }
            LazyColumn(
                modifier = modifier,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    VkTopBar(
                        onProfile = onSignIn,
                        onDownloads = onDownloads,
                        onSettings = onSettings,
                        onRefresh = onRefresh,
                    )
                }
                item {
                    VkMixHero(
                        songs = mixSongs,
                        onPlay = { onPlayAll(mixSongs) },
                        onSettings = { showMixSettings = true },
                    )
                }
                if (mixSongs.isNotEmpty()) item {
                    com.ivor.ivormusic.ui.home.OrganicSongLayout(
                        songs = mixSongs,
                        onSongClick = onPlay,
                        onSongLongPress = onLongPress,
                    )
                }
                if (state.value.sections.isEmpty()) {
                    item { EmptyState("VK did not return music yet", "Pull again or check the account's music library.") }
                }
                state.value.sections.forEach { section ->
                    if (section.songs.isNotEmpty() && section.songs != sourceMix) item {
                        SongShelf(section.title, section.songs, onPlay, onPlayAll, onLongPress)
                    }
                    if (section.playlists.isNotEmpty()) item {
                        PlaylistShelf(section.title, section.playlists, onPlaylist)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showMixSettings) {
        VkMixSettingsSheet(
            moods = moods,
            familiarity = familiarity,
            language = language,
            onMoodsChange = { moods = it },
            onFamiliarityChange = { familiarity = it },
            onLanguageChange = { language = it },
            onDismiss = { showMixSettings = false },
        )
    }
}

@Composable
private fun VkTopBar(
    onProfile: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(onClick = onProfile, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, "VK profile", Modifier.size(26.dp)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.Refresh, "Refresh") }
            IconButton(onClick = onDownloads, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.Download, "Downloads") }
            IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.Settings, "Settings") }
        }
    }
}

@Composable
private fun VkMixHero(songs: List<Song>, onPlay: () -> Unit, onSettings: () -> Unit) {
    val animation = rememberInfiniteTransition(label = "VkMixArtwork")
    val rotation by animation.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(6000), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "VkMixRotation",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VK", style = MaterialTheme.typography.displayLarge)
                    IconButton(onClick = onSettings, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.Rounded.Tune, "VK Mix settings")
                    }
                }
                Text("Mix", style = MaterialTheme.typography.displayLarge)
                Text(
                    songs.take(2).joinToString { it.artist }.ifBlank { "Personal recommendations" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(220.dp),
                )
            }
            FilledIconButton(
                onClick = onPlay,
                enabled = songs.isNotEmpty(),
                modifier = Modifier.padding(top = 40.dp).size(88.dp),
            ) { Icon(Icons.Rounded.PlayArrow, "Play VK Mix", Modifier.size(42.dp)) }
        }
        if (songs.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(330.dp).padding(horizontal = 20.dp, vertical = 12.dp)) {
                Artwork(
                    songs.first().thumbnailUrl,
                    Modifier.align(Alignment.Center).size(258.dp).graphicsLayer { rotationZ = rotation }
                        .clip(RoundedCornerShape(72.dp)),
                )
                songs.getOrNull(1)?.let {
                    Artwork(it.thumbnailUrl, Modifier.align(Alignment.TopStart).size(92.dp).clip(CircleShape))
                }
                songs.getOrNull(2)?.let {
                    Artwork(it.thumbnailUrl, Modifier.align(Alignment.BottomEnd).size(82.dp).clip(CircleShape))
                }
            }
        }
    }
}

@Composable
private fun VkMixSettingsSheet(
    moods: String,
    familiarity: String,
    language: String,
    onMoodsChange: (String) -> Unit,
    onFamiliarityChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("VK Mix settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Combine several characteristics to tune your recommendations.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            MixChipGroup("Mood", listOf("Active", "Calm", "Joyful", "Sad", "Love", "Instrumental"), moods.split(',').filter { it.isNotBlank() }.toSet()) { value ->
                val selected = moods.split(',').filter { it.isNotBlank() && it != "Any" }.toMutableSet()
                if (!selected.add(value)) selected.remove(value)
                onMoodsChange(selected.joinToString(",").ifBlank { "Any" })
            }
            MixSingleChoice("Familiarity", listOf("Any", "Favorites", "New"), familiarity, onFamiliarityChange)
            MixSingleChoice("Language", listOf("Any", "Russian", "Other"), language, onLanguageChange)
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Apply") }
        }
    }
}

@Composable
private fun MixChipGroup(title: String, values: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values) { value -> FilterChip(selected = value in selected, onClick = { onToggle(value) }, label = { Text(value) }) }
    }
}

@Composable
private fun MixSingleChoice(title: String, values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values) { value -> FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(value) }) }
    }
}

private fun applyMixSettings(source: List<Song>, moods: String, familiarity: String, language: String): List<Song> {
    if (source.isEmpty()) return source
    val filtered = source.filter { song ->
        val familiar = when (familiarity) {
            "Favorites" -> song.vkLiked
            "New" -> !song.vkLiked
            else -> true
        }
        val hasCyrillic = (song.title + song.artist).any { it in '\u0400'..'\u04FF' }
        val matchingLanguage = when (language) {
            "Russian" -> hasCyrillic
            "Other" -> !hasCyrillic
            else -> true
        }
        familiar && matchingLanguage
    }.ifEmpty { source }
    val offset = (moods.hashCode() and Int.MAX_VALUE) % filtered.size
    return filtered.drop(offset) + filtered.take(offset)
}

@Composable
private fun VkSearch(
    state: VkLoadState<List<Song>>,
    query: String,
    signedIn: Boolean,
    playlists: List<VkPlaylist>,
    onQuery: (String) -> Unit,
    onSignIn: () -> Unit,
    onPlay: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onPlaylist: (VkPlaylist) -> Unit,
    onLongPress: (Song) -> Unit,
    modifier: Modifier,
) {
    if (!signedIn) return SignedOutState(onSignIn, modifier)
    var kind by remember { mutableStateOf(SearchKind.TRACKS) }
    Column(modifier.padding(horizontal = 16.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Tracks, artists, albums, playlists") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onQuery(query) }),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            items(SearchKind.entries) { item ->
                AssistChip(onClick = { kind = item }, label = { Text(item.label) }, leadingIcon = if (kind == item) ({ Icon(Icons.Rounded.MusicNote, null, Modifier.size(18.dp)) }) else null)
            }
        }
        when (state) {
            is VkLoadState.Loading -> LoadingState(Modifier.weight(1f).fillMaxWidth())
            is VkLoadState.Error -> ErrorState(state.message, { onQuery(query) }, Modifier.weight(1f).fillMaxWidth())
            is VkLoadState.Ready -> {
                val songs = state.value
                when (kind) {
                    SearchKind.TRACKS -> SongList(songs, onPlay, onLongPress, Modifier.weight(1f), onPlayAll)
                    SearchKind.ARTISTS -> GroupedSearch(songs.groupBy { it.artist }, onPlayAll, Modifier.weight(1f), Icons.Rounded.Person)
                    SearchKind.ALBUMS -> GroupedSearch(songs.filter { it.album.isNotBlank() }.groupBy { it.album }, onPlayAll, Modifier.weight(1f), Icons.Rounded.Album)
                    SearchKind.PLAYLISTS -> {
                        val matched = playlists.filter { it.title.contains(query, ignoreCase = true) }
                        PlaylistList(matched, onPlaylist, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun VkLibrary(
    state: VkLoadState<VkCatalog>,
    signedIn: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onPlay: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onPlaylist: (VkPlaylist) -> Unit,
    onLongPress: (Song) -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    if (!signedIn) return SignedOutState(onSignIn, modifier)
    when (state) {
        is VkLoadState.Loading -> LoadingState(modifier)
        is VkLoadState.Error -> ErrorState(state.message, {}, modifier)
        is VkLoadState.Ready -> LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your music", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSignOut) { Icon(Icons.Rounded.Logout, contentDescription = "Sign out") }
                }
            }
            if (state.value.playlists.isNotEmpty()) {
                item { Text("Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
                items(state.value.playlists, key = { "${it.ownerId}_${it.id}" }) { PlaylistRow(it, onPlaylist) }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tracks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onPlayAll(state.value.library) }, enabled = state.value.library.isNotEmpty()) { Icon(Icons.Rounded.Shuffle, null); Text(" Shuffle") }
                }
            }
            if (state.value.library.isEmpty()) item { EmptyState("Your library is empty", "Add tracks to My music in VK or from a track menu here.") }
            items(state.value.library, key = { it.id }) { SongRow(it, { onPlay(it) }, { onLongPress(it) }) }
        }
    }
}

@Composable
private fun PlaylistScreen(
    details: VkPlaylistDetails,
    onBack: () -> Unit,
    onPlay: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onLongPress: (Song) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                Text(details.playlist.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f))
                if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete playlist") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onPlayAll(details.songs) }, enabled = details.songs.isNotEmpty()) { Icon(Icons.Rounded.PlayArrow, null); Text(" Play") }
                Text("${details.songs.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
        if (details.songs.isEmpty()) item { EmptyState("No tracks", "This playlist has no available tracks.") }
        items(details.songs, key = { it.id }) { SongRow(it, { onPlay(it) }, { onLongPress(it) }) }
    }
}

@Composable
private fun SongShelf(title: String, songs: List<Song>, onPlay: (Song) -> Unit, onPlayAll: (List<Song>) -> Unit, onLongPress: (Song) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { onPlayAll(songs) }) { Icon(Icons.Rounded.PlayArrow, null); Text(" Play") }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(songs.take(20), key = { it.id }) { song -> SongCard(song, { onPlay(song) }, { onLongPress(song) }) }
        }
    }
}

@Composable
private fun PlaylistShelf(title: String, playlists: List<VkPlaylist>, onPlaylist: (VkPlaylist) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists, key = { "${it.ownerId}_${it.id}" }) { item -> PlaylistCard(item) { onPlaylist(item) } }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongCard(song: Song, onClick: () -> Unit, onLongPress: () -> Unit) {
    Column(Modifier.width(156.dp).combinedClickable(onClick = onClick, onLongClick = onLongPress)) {
        Artwork(song.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)))
        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(song: Song, onClick: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).combinedClickable(onClick = onClick, onLongClick = onLongPress).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.thumbnailUrl, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(song.artist + song.album.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (song.vkLiked) Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Icon(Icons.Rounded.MoreVert, "More")
    }
}

@Composable
private fun SongList(songs: List<Song>, onPlay: (Song) -> Unit, onLongPress: (Song) -> Unit, modifier: Modifier, onPlayAll: (List<Song>) -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (songs.isNotEmpty()) item { TextButton(onClick = { onPlayAll(songs) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play results") } }
        if (songs.isEmpty()) item { EmptyState("Start typing to search", "Results from VK Music will appear here.") }
        items(songs, key = { it.id }) { SongRow(it, { onPlay(it) }, { onLongPress(it) }) }
    }
}

@Composable
private fun GroupedSearch(groups: Map<String, List<Song>>, onPlayAll: (List<Song>) -> Unit, modifier: Modifier, icon: ImageVector) {
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (groups.isEmpty()) item { EmptyState("Nothing found", "Try another search phrase.") }
        items(groups.entries.toList(), key = { it.key }) { (name, songs) ->
            Card(onClick = { onPlayAll(songs) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null) } }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${songs.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.PlayArrow, null)
                }
            }
        }
    }
}

@Composable
private fun PlaylistList(playlists: List<VkPlaylist>, onClick: (VkPlaylist) -> Unit, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (playlists.isEmpty()) item { EmptyState("No matching playlists", "Your VK playlists matching this title will appear here.") }
        items(playlists, key = { "${it.ownerId}_${it.id}" }) { PlaylistRow(it, onClick) }
    }
}

@Composable
private fun PlaylistRow(item: VkPlaylist, onClick: (VkPlaylist) -> Unit) {
    Card(onClick = { onClick(item) }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(item.artworkUrl, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.count} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.QueueMusic, null)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PlaylistCard(item: VkPlaylist, onClick: () -> Unit) {
    Column(Modifier.width(156.dp).combinedClickable(onClick = onClick, onLongClick = {})) {
        Artwork(item.artworkUrl, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)))
        Text(item.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text("${item.count} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Artwork(url: String?, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        if (url == null) Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp)) } },
            error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null) } },
        )
    }
}

@Composable
private fun SignedOutState(onSignIn: () -> Unit, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(104.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, Modifier.size(52.dp)) } }
            Text("VK Music in Koda", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Sign in to see recommendations, your library and playlists. Your session is encrypted on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onSignIn) { Text("Sign in with VK") }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) = Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(message: String, retry: () -> Unit, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(28.dp)) {
            Text("Couldn't load VK Music", style = MaterialTheme.typography.titleLarge)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = retry) { Text("Try again") }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.MusicNote, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SongActionsDialog(
    song: Song,
    playlists: List<VkPlaylist>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onQueue: () -> Unit,
    onFavorite: () -> Unit,
    onPlaylist: (VkPlaylist) -> Unit,
) {
    var playlistMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                ActionRow(Icons.Rounded.PlayArrow, "Play next") { onPlayNext(); onDismiss() }
                ActionRow(Icons.Rounded.QueueMusic, "Add to queue") { onQueue(); onDismiss() }
                ActionRow(if (song.vkLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (song.vkLiked) "Remove from My music" else "Save to My music") { onFavorite(); onDismiss() }
                Box {
                    ActionRow(Icons.Rounded.PlaylistAdd, "Add to playlist") { playlistMenu = true }
                    DropdownMenu(expanded = playlistMenu, onDismissRequest = { playlistMenu = false }) {
                        playlists.filter { it.canEdit }.forEach { playlist ->
                            DropdownMenuItem(text = { Text(playlist.title) }, onClick = { onPlaylist(playlist); playlistMenu = false; onDismiss() })
                        }
                        if (playlists.none { it.canEdit }) DropdownMenuItem(text = { Text("No editable playlists") }, onClick = {})
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null)
            Text(label, modifier = Modifier.padding(start = 14.dp))
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New VK playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, maxLines = 3)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { onCreate(title, description) }, enabled = title.isNotBlank()) { Text("Create") } },
    )
}
