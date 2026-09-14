package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.AudioTrack
import com.example.model.MusicVisualTheme

/**
 * Unified Art Visualizer Canvas
 * Renders the chosen music theme:
 * 1. VINYL: Classic Vinyl Record & Tonearm
 * 2. CYBER_PULSE: Futuristic Holographic Cyber Orb
 * 3. SONIC_RADAR: Precision Audiophile Oscilloscope & Sonar Radar
 */
@Composable
fun MusicVisualizerArtCanvas(
    track: AudioTrack?,
    isPlaying: Boolean,
    theme: MusicVisualTheme = MusicVisualTheme.VINYL,
    modifier: Modifier = Modifier
) {
    when (theme) {
        MusicVisualTheme.VINYL -> {
            VinylRecordCanvas(
                track = track,
                isPlaying = isPlaying,
                modifier = modifier
            )
        }
        MusicVisualTheme.CYBER_PULSE -> {
            CyberPulseCanvas(
                track = track,
                isPlaying = isPlaying,
                modifier = modifier
            )
        }
        MusicVisualTheme.SONIC_RADAR -> {
            SonicRadarCanvas(
                track = track,
                isPlaying = isPlaying,
                modifier = modifier
            )
        }
    }
}

/**
 * Unified Music Artwork Thumbnail
 * Used for music item rows, dashboard cards, library tracks, and mini player capsule.
 * Displays embedded/custom album art if present, or renders the active visual theme canvas!
 */
@Composable
fun MusicArtworkThumbnail(
    track: AudioTrack?,
    theme: MusicVisualTheme = MusicVisualTheme.VINYL,
    isPlaying: Boolean = false,
    cornerRadius: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("music_artwork_thumbnail"),
        contentAlignment = Alignment.Center
    ) {
        if (track?.albumArtDrawableRes != null) {
            Image(
                painter = painterResource(id = track.albumArtDrawableRes),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (track?.albumArtUri != null) {
            AsyncImage(
                model = track.albumArtUri,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Render active visual theme mini-canvas
            MusicVisualizerArtCanvas(
                track = track,
                isPlaying = isPlaying,
                theme = theme,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
