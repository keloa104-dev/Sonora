package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.model.AudioTrack
import com.example.model.FolderNode
import com.example.model.MusicVisualTheme
import com.example.ui.components.MusicArtworkThumbnail
import kotlinx.coroutines.launch

enum class SheetSortMode {
    DEFAULT,
    TITLE_ASC,
    ARTIST_ASC,
    DURATION_DESC,
    DURATION_ASC
}

@Composable
fun PlaylistBottomSheet(
    queueTracks: List<AudioTrack>,
    currentTrack: AudioTrack?,
    folderTrees: List<FolderNode>,
    onTrackSelect: (AudioTrack) -> Unit,
    onPlayTrackList: (List<AudioTrack>, Int) -> Unit,
    onAddTracksToQueue: (List<AudioTrack>) -> Unit = {},
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onDeleteTracks: (List<AudioTrack>, () -> Unit) -> Unit,
    onMoveTracks: (List<AudioTrack>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    visualTheme: MusicVisualTheme = MusicVisualTheme.VINYL,
    isPlaying: Boolean = true,
    onReorderQueue: ((List<AudioTrack>, Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Tab state: 0 = "Queue", 1..N = Root Folders
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Multi-selection state
    var selectedTrackIds by remember { mutableStateOf(emptySet<Long>()) }

    // Search and Sort states
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(SheetSortMode.DEFAULT) }

    // Hierarchical Folder Navigation Stack
    val playlistFolderStack = remember { mutableStateListOf<FolderNode>() }

    // Reset selection and folder stack on tab change
    LaunchedEffect(selectedTabIndex) {
        selectedTrackIds = emptySet()
        searchQuery = ""
        isSearchActive = false
        playlistFolderStack.clear()
        listState.scrollToItem(0)
    }

    // Scroll to current track strictly ONLY when Queue tab (index 0) opens, NEVER in folder tabs
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0 && currentTrack != null) {
            val index = queueTracks.indexOfFirst { it.id == currentTrack.id }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    // Resolve current root folder and active subfolder
    val selectedFolderTree = if (selectedTabIndex > 0 && selectedTabIndex - 1 < folderTrees.size) {
        folderTrees[selectedTabIndex - 1]
    } else {
        null
    }

    val activeFolderNode = if (playlistFolderStack.isNotEmpty()) {
        playlistFolderStack.last()
    } else {
        selectedFolderTree
    }

    // Strictly reset the scroll position of the parent folder's list to the top (index 0)
    // whenever returning from a subfolder or changing folder hierarchy levels.
    // The application shall not automatically scroll to the current playing track's position upon returning.
    LaunchedEffect(activeFolderNode?.id, playlistFolderStack.size) {
        if (selectedTabIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    // Tracks for current tab / active folder
    val currentTabTracks: List<AudioTrack> = remember(selectedTabIndex, queueTracks, activeFolderNode) {
        if (selectedTabIndex == 0) {
            queueTracks
        } else {
            activeFolderNode?.tracks ?: emptyList()
        }
    }

    val subfoldersInActiveNode: List<FolderNode> = remember(activeFolderNode) {
        activeFolderNode?.subfolders ?: emptyList()
    }

    val totalDiscoveredCount = remember(activeFolderNode, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            queueTracks.size
        } else {
            activeFolderNode?.totalTrackCount() ?: activeFolderNode?.tracks?.size ?: 0
        }
    }

    // Filter & Sort tracks
    val sortedAndFilteredTracks = remember(currentTabTracks, searchQuery, sortMode) {
        var result = if (searchQuery.isNotBlank()) {
            currentTabTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
            }
        } else {
            currentTabTracks
        }

        when (sortMode) {
            SheetSortMode.TITLE_ASC -> result.sortedBy { it.title.lowercase() }
            SheetSortMode.ARTIST_ASC -> result.sortedBy { it.artist.lowercase() }
            SheetSortMode.DURATION_DESC -> result.sortedByDescending { it.durationMs }
            SheetSortMode.DURATION_ASC -> result.sortedBy { it.durationMs }
            SheetSortMode.DEFAULT -> result
        }
    }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // High-Priority Hierarchical Back Navigation Shield:
    // When inside a subfolder, system back button or back gesture strictly returns user to immediate parent folder!
    // Window is dismissed only when back stack is empty at root level!
    BackHandler(enabled = true) {
        when {
            selectedTrackIds.isNotEmpty() -> {
                selectedTrackIds = emptySet()
            }
            isSearchActive && searchQuery.isNotEmpty() -> {
                searchQuery = ""
            }
            isSearchActive -> {
                isSearchActive = false
            }
            playlistFolderStack.isNotEmpty() -> {
                playlistFolderStack.removeAt(playlistFolderStack.lastIndex)
                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            }
            else -> {
                onDismiss()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlist_bottom_sheet")
    ) {
        // Scrim Background - Tapping outside dismisses
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
        )

        // Bottom Sheet Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = dragOffsetY.coerceAtLeast(0f)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Consume tap events so tapping sheet does not trigger scrim dismiss
                },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Top Grab Handle Area - The ONLY gesture zone permitted to dismiss the Playback Queue Window via drag
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetY > 160f) {
                                        onDismiss()
                                    }
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    dragOffsetY = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                }
                            )
                        }
                        .testTag("queue_sheet_drag_handle_area"),
                    contentAlignment = Alignment.Center
                ) {
                    // Horizontal Pill Indicator
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                            .testTag("queue_sheet_drag_handle")
                    )
                }

                // Header: Multi-selection mode OR Compact Unified Header
                if (selectedTrackIds.isNotEmpty()) {
                    // Header when items are selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedTrackIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Select All Button
                            IconButton(
                                onClick = {
                                    val isAllSelected = selectedTrackIds.size == sortedAndFilteredTracks.size
                                    selectedTrackIds = if (isAllSelected) emptySet() else sortedAndFilteredTracks.map { it.id }.toSet()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sheet_select_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckBox,
                                    contentDescription = "Select All",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete Button (Queue Tab only)
                            if (selectedTabIndex == 0) {
                                IconButton(
                                    onClick = {
                                        val selectedList = currentTabTracks.filter { selectedTrackIds.contains(it.id) }
                                        if (selectedList.isNotEmpty()) {
                                            onDeleteTracks(selectedList) {
                                                selectedTrackIds = emptySet()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("sheet_delete_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Move Button
                            IconButton(
                                onClick = {
                                    val selectedList = currentTabTracks.filter { selectedTrackIds.contains(it.id) }
                                    onMoveTracks(selectedList)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sheet_move_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Move",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Play Selected Button
                            IconButton(
                                onClick = {
                                    val selectedList = currentTabTracks.filter { selectedTrackIds.contains(it.id) }
                                    if (selectedList.isNotEmpty()) {
                                        onPlayTrackList(selectedList, 0)
                                        selectedTrackIds = emptySet()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sheet_play_selected_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Add Selected to Queue Button
                            IconButton(
                                onClick = {
                                    val selectedList = currentTabTracks.filter { selectedTrackIds.contains(it.id) }
                                    if (selectedList.isNotEmpty()) {
                                        onAddTracksToQueue(selectedList)
                                        selectedTrackIds = emptySet()
                                        Toast.makeText(context, "Added ${selectedList.size} tracks to queue", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sheet_add_selected_to_queue_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Add Selected to Queue",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Cancel selection
                            IconButton(
                                onClick = { selectedTrackIds = emptySet() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Selection",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Compact Single-Row Unified Header: Title & Context on Left, Flat Grouped Actions on Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button when navigating subfolders
                        if (playlistFolderStack.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    playlistFolderStack.removeAt(playlistFolderStack.lastIndex)
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("playlist_folder_back")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Left: Title and Song Count cleanly stacked
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Text(
                                text = if (selectedTabIndex == 0) "Play Queue" else (activeFolderNode?.name ?: "Folder"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = if (selectedTabIndex == 0) {
                                if (queueTracks.isEmpty()) {
                                    "Queue is empty"
                                } else {
                                    val currentIndex = if (currentTrack != null) queueTracks.indexOfFirst { it.id == currentTrack.id } else -1
                                    val remaining = if (currentIndex >= 0) queueTracks.size - currentIndex - 1 else queueTracks.size
                                    if (remaining > 0) "${queueTracks.size} songs • $remaining remaining" else "${queueTracks.size} songs"
                                }
                            } else {
                                "$totalDiscoveredCount songs"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right: Grouped Horizontal Actions (Play All, Search, Clear Queue, Sort)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Play All Subtle Refined Pill Button (Folders only)
                            if (selectedTabIndex > 0 && sortedAndFilteredTracks.isNotEmpty()) {
                                Surface(
                                    onClick = { onPlayTrackList(sortedAndFilteredTracks, 0) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("queue_play_all_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play All",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Play All",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Flat Search Toggle Icon
                            IconButton(
                                onClick = {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) searchQuery = ""
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("sheet_search_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Music",
                                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Flat Clear Queue Icon (Queue Tab only)
                            if (selectedTabIndex == 0 && queueTracks.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearQueue,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("sheet_clear_queue_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = "Clear Queue",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Flat Sort Menu Dropdown Icon
                            Box {
                                var showSortMenu by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("sheet_sort_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort Tracks",
                                        tint = if (sortMode != SheetSortMode.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Default Order",
                                                fontWeight = if (sortMode == SheetSortMode.DEFAULT) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortMode = SheetSortMode.DEFAULT
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Title (A to Z)",
                                                fontWeight = if (sortMode == SheetSortMode.TITLE_ASC) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortMode = SheetSortMode.TITLE_ASC
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Artist (A to Z)",
                                                fontWeight = if (sortMode == SheetSortMode.ARTIST_ASC) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortMode = SheetSortMode.ARTIST_ASC
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Duration (Longest)",
                                                fontWeight = if (sortMode == SheetSortMode.DURATION_DESC) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortMode = SheetSortMode.DURATION_DESC
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Duration (Shortest)",
                                                fontWeight = if (sortMode == SheetSortMode.DURATION_ASC) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortMode = SheetSortMode.DURATION_ASC
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter songs or artist...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("sheet_search_text_field")
                    )
                }

                // Scrollable Tabs (Queue + All Available Discovered Folders)
                val totalTabsCount = 1 + folderTrees.size
                if (totalTabsCount > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("queue_folder_tabs_row")
                    ) {
                        // Queue Tab (Index 0)
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = {
                                selectedTabIndex = 0
                                playlistFolderStack.clear()
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                }
                            },
                            text = {
                                Text(
                                    text = "Queue (${queueTracks.size})",
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("queue_tab_0")
                        )

                        // Discovered Folder Tabs (Index 1..N)
                        folderTrees.forEachIndexed { index, folderNode ->
                            val tabIndex = index + 1
                            val isSelected = selectedTabIndex == tabIndex
                            val folderCount = folderNode.totalTrackCount()
                            val label = if (folderNode.name.length > 18) folderNode.name.take(16) + "…" else folderNode.name

                            Tab(
                                selected = isSelected,
                                onClick = {
                                    selectedTabIndex = tabIndex
                                    playlistFolderStack.clear()
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                },
                                text = {
                                    Text(
                                        text = "$label ($folderCount)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("queue_folder_tab_$tabIndex")
                            )
                        }
                    }
                }

                // Main Content List Area (Subdirectories + Tracks) with Scrollbar
                // Content scrolling is decoupled from window dismissal - dragging list only scrolls list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                ) {
                    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

                    // Universal standardization: 100% solid, opaque colors, decoupled from Now Playing theme
                    val activeTrackBgColor = if (isDarkTheme) Color(0xFF223555) else Color(0xFFE2ECFA)
                    val activeTrackBorderColor = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB)
                    val activeTitleColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF0F172A)
                    val activeSubtitleColor = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1E40AF)
                    val activeBadgeBgColor = if (isDarkTheme) Color(0xFF2563EB) else Color(0xFF1D4ED8)
                    val activeActionIconColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                    val selectedTrackBgColor = if (isDarkTheme) Color(0xFF2A2D3A) else Color(0xFFE8EDF5)
                    val selectedTrackBorderColor = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                    val normalTrackBgColor = if (isDarkTheme) Color(0xFF1C2028) else Color(0xFFF1F5F9)
                    val normalTrackBorderColor = if (isDarkTheme) Color(0xFF282D39) else Color(0xFFE2E8F0)
                    val normalTitleColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    val normalSubtitleColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                    val normalActionIconColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

                    if (selectedTabIndex == 0) {
                        // Native 120Hz Hardware-Accelerated Reorderable Queue with ItemTouchHelper
                        ReorderableQueueList(
                            tracks = sortedAndFilteredTracks,
                            currentTrack = currentTrack,
                            selectedTrackIds = selectedTrackIds,
                            isDarkTheme = isDarkTheme,
                            canReorder = searchQuery.isBlank() && sortMode == SheetSortMode.DEFAULT && selectedTrackIds.isEmpty(),
                            searchQuery = searchQuery,
                            onTrackClick = { track -> onTrackSelect(track) },
                            onToggleSelect = { track ->
                                selectedTrackIds = if (selectedTrackIds.contains(track.id)) {
                                    selectedTrackIds - track.id
                                } else {
                                    selectedTrackIds + track.id
                                }
                            },
                            onMoveItem = { from, to -> onMoveItem(from, to) },
                            onReorderCommitted = { list, from, to ->
                                onReorderQueue?.invoke(list, from, to) ?: onMoveItem(from, to)
                            },
                            onRemoveItem = { index -> onRemoveItem(index) },
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                        )
                    } else {
                        // Folder Hierarchy List (Subfolders + Tracks in Folder)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .padding(end = 26.dp)
                                .testTag("playlist_sheet_tracks_list")
                        ) {
                            // Subfolders section (When on folder tabs)
                            if (subfoldersInActiveNode.isNotEmpty() && searchQuery.isBlank()) {
                                item(key = "subfolders_header") {
                                    Text(
                                        text = "SUBDIRECTORIES (${subfoldersInActiveNode.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                                    )
                                }

                                items(
                                    items = subfoldersInActiveNode,
                                    key = { "subfolder_${it.id}" }
                                ) { subfolder ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                playlistFolderStack.add(subfolder)
                                                coroutineScope.launch {
                                                    listState.scrollToItem(0)
                                                }
                                            }
                                            .testTag("bottom_subfolder_${subfolder.name}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = normalTrackBgColor
                                        ),
                                        border = BorderStroke(1.dp, normalTrackBorderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = subfolder.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${subfolder.tracks.size} direct tracks • ${subfolder.totalTrackCount()} total",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Open Folder",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                if (sortedAndFilteredTracks.isNotEmpty()) {
                                    item(key = "tracks_header") {
                                        Text(
                                            text = "FILES IN PATH (${sortedAndFilteredTracks.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Empty State
                            if (sortedAndFilteredTracks.isEmpty() && subfoldersInActiveNode.isEmpty()) {
                                item(key = "empty_state") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "No songs match \"$searchQuery\"" else "No audio files in this folder",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Folder Tracks Items
                            itemsIndexed(
                                items = sortedAndFilteredTracks,
                                key = { _, track -> "track_${track.id}" }
                            ) { _, track ->
                                val isCurrent = currentTrack?.id == track.id
                                val isSelected = selectedTrackIds.contains(track.id)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedTrackIds = if (checked) selectedTrackIds + track.id else selectedTrackIds - track.id
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB),
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("queue_checkbox_${track.id}")
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (selectedTrackIds.isNotEmpty()) {
                                                    selectedTrackIds = if (isSelected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                                } else {
                                                    onTrackSelect(track)
                                                }
                                            }
                                            .testTag("queue_item_${track.id}"),
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isSelected -> selectedTrackBgColor
                                            isCurrent -> activeTrackBgColor
                                            else -> normalTrackBgColor
                                        },
                                        border = when {
                                            isCurrent -> BorderStroke(1.5.dp, activeTrackBorderColor)
                                            isSelected -> BorderStroke(1.dp, selectedTrackBorderColor)
                                            else -> BorderStroke(1.dp, normalTrackBorderColor)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                StaticTrackThumbnail(
                                                    track = track,
                                                    cornerRadius = 6.dp,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .testTag(if (isCurrent) "now_playing_artwork" else "track_artwork_${track.id}")
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = track.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isCurrent) activeTitleColor else normalTitleColor,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        if (isCurrent) {
                                                            Surface(
                                                                shape = RoundedCornerShape(3.dp),
                                                                color = activeBadgeBgColor,
                                                                modifier = Modifier.testTag("now_playing_badge")
                                                            ) {
                                                                Text(
                                                                    text = "NOW PLAYING",
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontSize = 8.sp,
                                                                        letterSpacing = 0.3.sp,
                                                                        lineHeight = 10.sp
                                                                    ),
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(5.dp))
                                                        }
                                                        val subtitle = if (track.relativePath.isNotBlank()) {
                                                            track.relativePath
                                                        } else if (track.artist.isNotBlank() || track.album.isNotBlank()) {
                                                            "${track.artist} • ${track.album}"
                                                        } else {
                                                            "Audio Track"
                                                        }
                                                        Text(
                                                            text = subtitle,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isCurrent) activeSubtitleColor else normalSubtitleColor,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTabIndex != 0) {
                        VerticalScrollbar(
                            state = listState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

