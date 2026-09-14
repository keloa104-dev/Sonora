package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AudioTrack
import com.example.ui.components.TrackItemRow
import com.example.ui.components.VerticalScrollbar
import com.example.viewmodel.PlayerViewModel

@Composable
fun DedicatedSongsScreen(
    title: String,
    subtitle: String,
    tracks: List<AudioTrack>,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTrackIds by remember { mutableStateOf(setOf<Long>()) }

    BackHandler {
        when {
            selectedTrackIds.isNotEmpty() -> selectedTrackIds = emptySet()
            searchQuery.isNotEmpty() -> searchQuery = ""
            else -> onBack()
        }
    }

    val filteredTracks = remember(tracks, searchQuery) {
        if (searchQuery.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val moveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val selectedList = filteredTracks.filter { selectedTrackIds.contains(it.id) }
            viewModel.moveTracks(context, selectedList, uri) {
                selectedTrackIds = emptySet()
            }
        }
    }

    val userSettings by viewModel.userSettings.collectAsState()
    val hideHeaderOnScroll = userSettings.hideHeaderOnScroll
    val isHeaderVisible by viewModel.isScrollHeaderDockVisible.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState.canScrollBackward) {
        if (!listState.canScrollBackward) {
            viewModel.setScrollHeaderDockVisible(true)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 10) {
            viewModel.setScrollHeaderDockVisible(true)
        }
    }

    val nestedScrollConnection = com.example.ui.components.rememberCapsuleScrollConnection(
        enabled = hideHeaderOnScroll,
        onVisibilityChanged = { visible ->
            viewModel.setScrollHeaderDockVisible(visible)
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .testTag("${testTagPrefix}_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Collapsible Header Section
            AnimatedVisibility(
                visible = !hideHeaderOnScroll || isHeaderVisible,
                enter = expandVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(durationMillis = 180)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                )
            ) {
                Column {
                    // Header with Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("${testTagPrefix}_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (filteredTracks.size == tracks.size) "$subtitle (${tracks.size})" else "${filteredTracks.size} of ${tracks.size} Tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("${testTagPrefix}_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search $title...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTagPrefix}_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MULTI-SELECTION & BATCH ACTIONS BAR (STRICTLY ICON-ONLY BUTTONS)
            if (selectedTrackIds.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selection_action_bar"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isAllSelected = filteredTracks.isNotEmpty() && selectedTrackIds.size == filteredTracks.size

                        Text(
                            text = "${selectedTrackIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // ICON-ONLY Action Buttons Row (NO text labels next to or below icons)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 1. Select All
                            IconButton(
                                onClick = {
                                    selectedTrackIds = if (isAllSelected) emptySet() else filteredTracks.map { it.id }.toSet()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("select_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 2. Play Selected
                            IconButton(
                                onClick = {
                                    val selectedList = filteredTracks.filter { selectedTrackIds.contains(it.id) }
                                    if (selectedList.isNotEmpty()) {
                                        viewModel.playTrackList(selectedList, 0)
                                        selectedTrackIds = emptySet()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("play_selected_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Selected",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 3. Add to Queue / Playlist
                            IconButton(
                                onClick = {
                                    val selectedList = filteredTracks.filter { selectedTrackIds.contains(it.id) }
                                    selectedList.forEach { viewModel.addToQueue(it) }
                                    selectedTrackIds = emptySet()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("add_to_playlist_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Add Selected to Queue",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 4. Move to Folder
                            IconButton(
                                onClick = { moveFolderLauncher.launch(null) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("move_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Move Selected",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 5. Cancel Selection
                            IconButton(
                                onClick = { selectedTrackIds = emptySet() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("cancel_selection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Selection",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Track List
            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "No Tracks",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No $title found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTracks, key = { it.id }, contentType = { "song_row" }) { track ->
                            val isCurrent = currentTrack?.id == track.id
                            val isFav = favoriteIds.contains(track.id)
                            val isSelected = selectedTrackIds.contains(track.id)

                            TrackItemRow(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                isFavorite = isFav,
                                isSelected = isSelected,
                                visualTheme = userSettings.musicVisualTheme,
                                onToggleSelect = {
                                    selectedTrackIds = if (isSelected) {
                                        selectedTrackIds - track.id
                                    } else {
                                        selectedTrackIds + track.id
                                    }
                                },
                                onTrackClick = { viewModel.playTrack(track) },
                                onFavoriteClick = { viewModel.toggleFavorite(track.id) },
                                onAddToQueueClick = { viewModel.addToQueue(track) }
                            )
                        }
                    }

                    VerticalScrollbar(
                        state = listState,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
