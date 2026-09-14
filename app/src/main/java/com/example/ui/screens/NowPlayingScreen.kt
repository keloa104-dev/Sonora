package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.model.AudioTrack
import com.example.model.SliderPosition
import com.example.ui.components.AudioEqualizerDialog
import com.example.ui.components.PlaylistBottomSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SongInfoDialog
import com.example.ui.components.VerticalVolumeSlider
import com.example.ui.components.VinylRecordCanvas
import com.example.ui.components.MusicVisualizerArtCanvas
import com.example.viewmodel.PlayerViewModel

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onCollapse: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTrack by viewModel.currentTrack.collectAsState()
    val currentQueue by viewModel.currentQueue.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsState()
    val folderTrees by viewModel.folderTrees.collectAsState()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var pendingMoveTracks by remember { mutableStateOf<List<AudioTrack>>(emptyList()) }
    var isUserDraggingSeekBar by remember { mutableStateOf(false) }
    var frozenIsPlaying by remember { mutableStateOf(isPlaying) }

    // Intercept back actions for open dialogs, menus, and overlays in NowPlayingScreen
    BackHandler(
        enabled = showDeleteConfirmDialog ||
                showSongInfoDialog ||
                showEqualizerDialog ||
                showSleepTimerDialog ||
                showOverflowMenu
    ) {
        when {
            showDeleteConfirmDialog -> showDeleteConfirmDialog = false
            showSongInfoDialog -> showSongInfoDialog = false
            showEqualizerDialog -> showEqualizerDialog = false
            showSleepTimerDialog -> showSleepTimerDialog = false
            showOverflowMenu -> showOverflowMenu = false
        }
    }

    // Keep frozenIsPlaying locked to its pre-drag value throughout manual seeking and for a safe
    // stabilization buffer period (500ms) after release so that transient ExoPlayer buffer states
    // never cause the play/pause button icon to flicker or morph into a play arrow.
    androidx.compose.runtime.LaunchedEffect(isUserDraggingSeekBar, isPlaying) {
        if (!isUserDraggingSeekBar) {
            kotlinx.coroutines.delay(500)
            frozenIsPlaying = isPlaying
        } else {
            frozenIsPlaying = isPlaying
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            viewModel.syncVolumeFromSystem()
            kotlinx.coroutines.delay(200)
        }
    }

    val moveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null && pendingMoveTracks.isNotEmpty()) {
            viewModel.moveTracks(context, pendingMoveTracks, uri) {
                pendingMoveTracks = emptyList()
            }
        }
    }

    val isFavorite = currentTrack?.let { favoriteIds.contains(it.id) } ?: false

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = sleepTimerMinutes,
            remainingSeconds = sleepTimerRemainingSec,
            onSetTimer = { viewModel.setSleepTimer(it) },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showSongInfoDialog && currentTrack != null) {
        SongInfoDialog(
            track = currentTrack!!,
            onDismiss = { showSongInfoDialog = false }
        )
    }

    if (showEqualizerDialog) {
        AudioEqualizerDialog(
            onDismiss = { showEqualizerDialog = false }
        )
    }

    if (showDeleteConfirmDialog && currentTrack != null) {
        val trackToDelete = currentTrack!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Song") },
            text = { Text("Are you sure you want to permanently delete '${trackToDelete.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTracks(context, listOf(trackToDelete)) {
                            Toast.makeText(context, "Track deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val topCurvedShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dragOffsetY.coerceAtLeast(0f)
                clip = true
                shape = topCurvedShape
                shadowElevation = 16f
            }
            .then(
                if (!showPlaylistSheet) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffsetY > 200f) {
                                    onCollapse?.invoke()
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
                } else Modifier
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                ),
                shape = topCurvedShape
            )
            .clip(topCurvedShape)
            .testTag("now_playing_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Downward chevron collapse button (∨) on the left
                IconButton(
                    onClick = { onCollapse?.invoke() },
                    modifier = Modifier.testTag("collapse_now_playing_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Centered "Now Playing" title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.titleSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Queue button [≡♯] and Overflow menu [⋮] on the right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showPlaylistSheet = true },
                        modifier = Modifier.testTag("open_playlist_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Current Queue",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("now_playing_overflow_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Overflow Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Track") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    currentTrack?.let { track ->
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "audio/*"
                                                putExtra(Intent.EXTRA_STREAM, track.uri)
                                                putExtra(Intent.EXTRA_TEXT, "Listening to ${track.title} by ${track.artist}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not share track", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Delete Song") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Sleep Timer") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showSleepTimerDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Song Info & Metadata") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showSongInfoDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Audio Equalizer") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF)
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showEqualizerDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Split Upper Region:
            // Left: Vertical volume capsule (Vertical Volume Bar)
            // Right: Vinyl record canvas housing a spinning vinyl graphic with extended tonearm
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (userSettings.sliderPosition == SliderPosition.LEFT) {
                    VerticalVolumeSlider(
                        value = volume,
                        isMuted = isMuted,
                        onValueChange = { viewModel.setVolumeRatio(it) },
                        onSpeakerClick = { viewModel.toggleMute() },
                        lengthFraction = userSettings.volumeSliderLength,
                        thicknessDp = userSettings.volumeBarThicknessDp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .offset(
                                x = userSettings.volumeBarXOffsetDp.dp,
                                y = userSettings.volumeBarYOffsetDp.dp
                            )
                    )
                }

                // Art visualizer canvas (Unified Block 1: Visual Assembly)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .offset(y = userSettings.vinylYOffsetDp.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MusicVisualizerArtCanvas(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        theme = userSettings.musicVisualTheme,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }

                if (userSettings.sliderPosition == SliderPosition.RIGHT) {
                    VerticalVolumeSlider(
                        value = volume,
                        isMuted = isMuted,
                        onValueChange = { viewModel.setVolumeRatio(it) },
                        onSpeakerClick = { viewModel.toggleMute() },
                        lengthFraction = userSettings.volumeSliderLength,
                        thicknessDp = userSettings.volumeBarThicknessDp,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .offset(
                                x = userSettings.volumeBarXOffsetDp.dp,
                                y = userSettings.volumeBarYOffsetDp.dp
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // UNIFIED BLOCK 2: Bottom Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = userSettings.bottomControlsYOffsetDp.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Metadata & Favorite Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: "Select a Track",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val pathSubtitle = if (currentTrack?.relativePath?.isNotBlank() == true) {
                            currentTrack!!.relativePath
                        } else {
                            "${currentTrack?.artist ?: "Unknown Artist"} • ${currentTrack?.album ?: "Unknown Album"}"
                        }

                        Text(
                            text = pathSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            currentTrack?.let { viewModel.toggleFavorite(it.id) }
                        },
                        modifier = Modifier.testTag("toggle_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star Favorite",
                            tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Seekbar Canvas with complete layout isolation
                IsolatedTimelineSlider(
                    currentPosition = currentPosition,
                    duration = duration,
                    progressBarLength = userSettings.progressBarLength,
                    onDragStateChanged = { dragging ->
                        isUserDraggingSeekBar = dragging
                    },
                    onSeek = { targetMs -> viewModel.seekTo(targetMs) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Playback Speed Control Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(userSettings.speedSliderLength.coerceIn(0.3f, 1.0f))
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        val isColored = userSettings.coloredSpeedSlider
                        val speedColor = if (isColored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback Speed",
                                    tint = speedColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Playback Speed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${String.format("%.1f", playbackSpeed)}x",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = speedColor
                            )
                        }

                        Slider(
                            value = playbackSpeed,
                            onValueChange = { viewModel.setPlaybackSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 14,
                            colors = SliderDefaults.colors(
                                thumbColor = speedColor,
                                activeTrackColor = speedColor,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("playback_speed_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transport Control Cluster: Completely isolated from SeekBar drag events
                IsolatedTransportControls(
                    isPlaying = frozenIsPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onTogglePlayPause = {
                        frozenIsPlaying = !frozenIsPlaying
                        viewModel.togglePlayPause()
                    },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeatMode() }
                )
            }
        }

        // Playback Queue Window (Hierarchical bottom sheet overlay)
        if (showPlaylistSheet) {
            PlaylistBottomSheet(
                queueTracks = currentQueue,
                currentTrack = currentTrack,
                folderTrees = folderTrees,
                onTrackSelect = { viewModel.playTrack(it) },
                onPlayTrackList = { list, idx -> viewModel.playTrackList(list, idx) },
                onAddTracksToQueue = { viewModel.addTracksToQueue(it) },
                onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onReorderQueue = { list, from, to -> viewModel.commitReorderedQueue(list, from, to) },
                onRemoveItem = { index -> viewModel.removeFromQueue(index) },
                onClearQueue = { viewModel.clearQueue() },
                onDeleteTracks = { tracksToDelete, onComplete ->
                    viewModel.deleteTracks(context, tracksToDelete) {
                        onComplete()
                    }
                },
                onMoveTracks = { tracksToMove ->
                    pendingMoveTracks = tracksToMove
                    moveFolderLauncher.launch(null)
                },
                visualTheme = userSettings.musicVisualTheme,
                isPlaying = isPlaying,
                onDismiss = { showPlaylistSheet = false }
            )
        }
    }
}

@Composable
private fun IsolatedTimelineSlider(
    currentPosition: Long,
    duration: Long,
    progressBarLength: Float,
    onDragStateChanged: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableStateOf(0f) }

    val currentProgress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val displayRatio = if (isDragging) dragRatio else currentProgress
    val displayPosMs = if (isDragging) (dragRatio * duration).toLong() else currentPosition

    Column(
        modifier = modifier
            .fillMaxWidth(progressBarLength.coerceIn(0.3f, 1.0f))
            .height(68.dp)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Slider(
            value = displayRatio.coerceIn(0f, 1f),
            onValueChange = { ratio ->
                if (!isDragging) {
                    isDragging = true
                    onDragStateChanged(true)
                }
                dragRatio = ratio
            },
            onValueChangeFinished = {
                val targetMs = (dragRatio * duration).toLong()
                onSeek(targetMs)
                isDragging = false
                onDragStateChanged(false)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timeline_progress_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMillis(displayPosMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFeatureSettings = "tnum"
                ),
                modifier = Modifier.widthIn(min = 45.dp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMillis(duration),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFeatureSettings = "tnum"
                ),
                modifier = Modifier.widthIn(min = 45.dp),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IsolatedTransportControls(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button
        Surface(
            onClick = onToggleShuffle,
            modifier = Modifier
                .size(48.dp)
                .testTag("shuffle_button"),
            shape = CircleShape,
            color = if (isShuffle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = if (isShuffle) "Shuffle On" else "Shuffle Off",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Previous Button (◄◄)
        Surface(
            onClick = onPrevious,
            modifier = Modifier
                .size(54.dp)
                .shadow(4.dp, CircleShape)
                .testTag("previous_button"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Play / Pause Button - Rigid dimensions, completely immune to slider dragging & progress updates
        IsolatedPlayPauseButton(
            isPlaying = isPlaying,
            onClick = onTogglePlayPause
        )

        // Next Button (►►)
        Surface(
            onClick = onNext,
            modifier = Modifier
                .size(54.dp)
                .shadow(4.dp, CircleShape)
                .testTag("next_button"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Repeat Button
        val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
        val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
        Surface(
            onClick = onToggleRepeat,
            modifier = Modifier
                .size(48.dp)
                .testTag("repeat_button"),
            shape = CircleShape,
            color = if (isRepeatActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat One"
                        Player.REPEAT_MODE_ALL -> "Repeat All"
                        else -> "Repeat Off"
                    },
                    tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun IsolatedPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(68.dp)
            .shadow(8.dp, CircleShape)
            .testTag("play_pause_button"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private fun formatMillis(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
