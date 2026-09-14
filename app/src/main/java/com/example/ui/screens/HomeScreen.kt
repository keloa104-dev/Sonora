package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.AudioTrack
import com.example.ui.components.MusicArtworkThumbnail
import com.example.ui.components.StaticTrackThumbnail
import com.example.ui.components.TrackItemRow
import com.example.ui.components.VerticalScrollbar
import com.example.viewmodel.PlayerViewModel

@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSongs: () -> Unit,
    onNavigateToFavoritesScreen: () -> Unit = {},
    onNavigateToAllSongsScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.tracks.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val queueTracks by viewModel.currentQueue.collectAsState()

    val favoriteTracks = remember(tracks, favoriteIds) { tracks.filter { favoriteIds.contains(it.id) } }
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
            .testTag("home_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Collapsible Header Section (Dashboard Header + Stat Cards)
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    // Top Header with Settings Gear Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Welcome to your personal music hub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Quick Overview Cards Row (Capsules)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Songs Stat Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToAllSongsScreen() }
                                .testTag("stat_card_tracks"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${tracks.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Total Songs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Favorites Stat Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToFavoritesScreen() }
                                .testTag("stat_card_favorites"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorites",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${favoriteTracks.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Favorites",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Smooth Lazy List Area for Dashboard Content (Favorites Carousel + Recently Loaded Tracks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Favorites Quick Carousel Section
                    item(key = "home_favorites_section") {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Favorite Tracks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (favoriteTracks.isNotEmpty()) {
                                    Text(
                                        text = "Play All",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.playTrackList(favoriteTracks, 0) }
                                            .padding(4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (favoriteTracks.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "No favorite tracks yet. Tap star icon on any song to add!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(favoriteTracks, key = { it.id }) { track ->
                                        val isTrackPlaying = currentTrack?.id == track.id
                                        Card(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { viewModel.playTrack(track) }
                                                .testTag("favorite_card_${track.id}"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isTrackPlaying) {
                                                    if (isDarkTheme) Color(0xFF223555) else Color(0xFFE2ECFA)
                                                } else {
                                                    if (isDarkTheme) Color(0xFF1C2028) else Color(0xFFF1F5F9)
                                                }
                                            ),
                                            border = BorderStroke(
                                                width = if (isTrackPlaying) 1.5.dp else 1.dp,
                                                color = if (isTrackPlaying) {
                                                    if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB)
                                                } else {
                                                    if (isDarkTheme) Color(0xFF282D39) else Color(0xFFE2E8F0)
                                                }
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(110.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFF232730)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    StaticTrackThumbnail(
                                                        track = track,
                                                        cornerRadius = 10.dp,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    // Star toggle button on dashboard favorite item
                                                    IconButton(
                                                        onClick = { viewModel.toggleFavorite(track.id) },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(28.dp)
                                                            .background(Color(0xFF1E222B), CircleShape)
                                                            .testTag("btn_favorite")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = "Remove Favorite",
                                                            tint = Color(0xFFFFC107),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isTrackPlaying) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isTrackPlaying) {
                                                        if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF0F172A)
                                                    } else {
                                                        if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                                    },
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = track.artist,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isTrackPlaying) {
                                                        if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1E40AF)
                                                    } else {
                                                        if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                                    },
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recent Tracks Section Header
                    item(key = "home_recent_tracks_header") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Loaded Tracks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            if (tracks.isNotEmpty() || queueTracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearRecentlyLoadedTracks() },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("home_clear_tracks_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = "Clear Recently Loaded Tracks",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Recent Tracks Item Rows
                    if (tracks.isEmpty()) {
                        item(key = "home_empty_tracks") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "No audio tracks loaded yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(
                            items = tracks,
                            key = { it.id }
                        ) { track ->
                            val isCurrent = currentTrack?.id == track.id
                            TrackItemRow(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                isFavorite = favoriteIds.contains(track.id),
                                isSelected = false,
                                showCheckbox = false,
                                onTrackClick = { viewModel.playTrack(track) },
                                onFavoriteClick = { viewModel.toggleFavorite(track.id) },
                                onAddToQueueClick = { viewModel.addToQueue(track) },
                                modifier = Modifier.testTag("home_recent_track_${track.id}")
                            )
                        }
                    }

                    // Bottom spacing for floating dock clearance
                    item(key = "home_bottom_spacer") {
                        Spacer(modifier = Modifier.height(120.dp))
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
