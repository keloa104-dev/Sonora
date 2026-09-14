package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderNode
import com.example.viewmodel.PlayerViewModel

@Composable
fun FoldersSubWindow(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val folderStack = remember { mutableStateListOf<FolderNode>() }

    // Local High-Priority BackHandler Shield:
    // Strictly closes Folders container and returns safely to main Settings menu
    BackHandler {
        if (folderStack.isNotEmpty()) {
            folderStack.removeAt(folderStack.lastIndex)
        } else {
            onDismiss()
        }
    }

    val folderTrees by viewModel.folderTrees.collectAsState()
    val isFoldersLoading by viewModel.isFoldersLoading.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        try {
            if (uri != null) {
                viewModel.addCustomFolderUri(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("folders_sub_window"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            // Top Navigation Header Bar with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            if (folderStack.isNotEmpty()) {
                                folderStack.removeAt(folderStack.lastIndex)
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("folders_sub_window_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Folders & Directory",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Directory tree & inclusion controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        try {
                            folderLauncher.launch(null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_root_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Add Directory",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Path",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-scan entire device toggle card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 14.dp)
                    ) {
                        Text(
                            text = "Auto-scan entire device",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (userSettings.autoScanDevice)
                                "The app automatically scans device storage to discover all music files across your phone."
                            else
                                "When this option is turned off, the app will not automatically scan your device's storage. The app will only rely on folders you manually add. No data will be sent, and no files outside the specified folders will be read.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userSettings.autoScanDevice,
                        onCheckedChange = { enabled ->
                            viewModel.setAutoScanDevice(enabled)
                        },
                        modifier = Modifier.testTag("auto_scan_device_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isFoldersLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (folderTrees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty Folders",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No custom folders indexed.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add Path' to include music directory folders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val activeIndex = selectedTabIndex.coerceIn(0, folderTrees.size - 1)
                val currentRootFolder = folderTrees[activeIndex]
                val activeFolderNode = if (folderStack.isNotEmpty()) folderStack.last() else currentRootFolder

                ScrollableTabRow(
                    selectedTabIndex = activeIndex,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (activeIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeIndex]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_tabs_row")
                ) {
                    folderTrees.forEachIndexed { index, rootNode ->
                        val isSelected = activeIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                selectedTabIndex = index
                                folderStack.clear()
                            },
                            text = {
                                Text(
                                    text = rootNode.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = rootNode.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("folder_tab_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val allFolderTracks = remember(activeFolderNode) { activeFolderNode.getAllTracksRecursively() }
                val isSubfolder = folderStack.isNotEmpty()
                val pathNodes = remember(currentRootFolder, folderStack.toList()) { listOf(currentRootFolder) + folderStack }

                // Breadcrumbs bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("folder_breadcrumbs")
                ) {
                    itemsIndexed(pathNodes) { index, node ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = {
                                    if (index == 0) {
                                        folderStack.clear()
                                    } else if (index < pathNodes.lastIndex) {
                                        while (folderStack.size > index) {
                                            folderStack.removeAt(folderStack.lastIndex)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (index == pathNodes.lastIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (index == pathNodes.lastIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (index == pathNodes.lastIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (index < pathNodes.lastIndex) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubfolder) {
                            IconButton(
                                onClick = { folderStack.removeAt(folderStack.lastIndex) },
                                modifier = Modifier.testTag("folder_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeFolderNode.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeFolderNode.tracks.size} direct audio files • ${allFolderTracks.size} total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (allFolderTracks.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    viewModel.playTrackList(allFolderTracks, 0)
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("play_all_folder_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (!isSubfolder) {
                            val uriStr = userSettings.customFolderUris.getOrNull(activeIndex)
                            if (uriStr != null) {
                                IconButton(
                                    onClick = {
                                        viewModel.removeCustomFolderUri(uriStr)
                                        if (selectedTabIndex > 0) selectedTabIndex--
                                    },
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Path",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val detailListState = rememberLazyListState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = detailListState,
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (activeFolderNode.subfolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "SUBDIRECTORIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(activeFolderNode.subfolders, key = { it.id }) { subfolder ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { folderStack.add(subfolder) }
                                        .testTag("subfolder_item_${subfolder.name}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkTheme) Color(0xFF1C2028) else Color(0xFFF1F5F9)
                                    ),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF282D39) else Color(0xFFCBD5E1))
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
                                            tint = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = subfolder.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${subfolder.totalTrackCount()} tracks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "FILES IN PATH (${activeFolderNode.tracks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        if (activeFolderNode.tracks.isEmpty()) {
                            item {
                                Text(
                                    text = "No audio tracks directly in this folder path.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            itemsIndexed(activeFolderNode.tracks, key = { index, track -> "${track.id}_$index" }) { idx, track ->
                                val isCurrent = currentTrack?.id == track.id
                                TrackItemRow(
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    isFavorite = favoriteIds.contains(track.id),
                                    isSelected = false,
                                    showCheckbox = false,
                                    onTrackClick = {
                                        viewModel.playTrack(track)
                                    },
                                    onFavoriteClick = { viewModel.toggleFavorite(track.id) },
                                    onAddToQueueClick = { viewModel.addToQueue(track) },
                                    modifier = Modifier.testTag("folder_track_${track.id}")
                                )
                            }
                        }
                    }

                    VerticalScrollbar(
                        state = detailListState,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
