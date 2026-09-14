package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.MusicArtworkThumbnail
import com.example.ui.components.TrackItemRow
import com.example.viewmodel.PlayerViewModel

@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.tracks.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Favorites", "Albums", "Artists")

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val favoriteTracks = remember(tracks, favoriteIds) {
        tracks.filter { favoriteIds.contains(it.id) }
    }

    val albumGroups = remember(tracks) {
        tracks.groupBy { it.album }
    }

    val artistGroups = remember(tracks) {
        tracks.groupBy { it.artist }
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
            .testTag("library_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Collapsible Header Section (Capsules)
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Library",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Playlists, Albums & Favorites",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("library_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("library_tab_row")
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Tab Contents
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Favorites
                        if (favoriteTracks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No favorite tracks saved yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(favoriteTracks, key = { it.id }) { track ->
                                TrackItemRow(
                                    track = track,
                                    isCurrent = currentTrack?.id == track.id,
                                    isPlaying = currentTrack?.id == track.id && isPlaying,
                                    isFavorite = true,
                                    isSelected = false,
                                    showCheckbox = false,
                                    onTrackClick = { viewModel.playTrack(track) },
                                    onFavoriteClick = { viewModel.toggleFavorite(track.id) },
                                    onAddToQueueClick = { viewModel.addToQueue(track) },
                                    modifier = Modifier.testTag("favorite_item_${track.id}")
                                )
                            }
                        }
                    }
                    1 -> { // Albums
                        items(albumGroups.keys.toList()) { albumName ->
                            val albumTracks = albumGroups[albumName] ?: emptyList()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (albumTracks.isNotEmpty()) {
                                            viewModel.playTrackList(albumTracks, 0)
                                        }
                                    }
                                    .testTag("album_item_$albumName"),
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
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = albumName.ifBlank { "Unknown Album" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${albumTracks.size} tracks",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Album",
                                        tint = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                    2 -> { // Artists
                        items(artistGroups.keys.toList()) { artistName ->
                            val artistTracks = artistGroups[artistName] ?: emptyList()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (artistTracks.isNotEmpty()) {
                                            viewModel.playTrackList(artistTracks, 0)
                                        }
                                    }
                                    .testTag("artist_item_$artistName"),
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
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artistName.ifBlank { "Unknown Artist" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${artistTracks.size} tracks",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Artist Songs",
                                        tint = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
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
