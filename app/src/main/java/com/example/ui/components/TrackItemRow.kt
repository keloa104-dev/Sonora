package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AudioTrack
import com.example.model.MusicVisualTheme

/**
 * Universally standardized Song List Item Container.
 * Strictly compliant with global design rules:
 * 1. 100% solid, opaque background with zero transparency/glassmorphism.
 * 2. Mandatory theme independence (static artwork, decoupled from Now Playing themes).
 * 3. Enforced high-contrast text readability.
 * 4. Selection checkbox positioned strictly outside the container on the far left.
 * 5. Single-line ellipsis truncation with no wrapping or overlapping.
 */
@Composable
fun TrackItemRow(
    track: AudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    isSelected: Boolean = false,
    showCheckbox: Boolean = true,
    onToggleSelect: (() -> Unit)? = null,
    onTrackClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    onAddToQueueClick: (() -> Unit)? = null,
    visualTheme: MusicVisualTheme = MusicVisualTheme.VINYL,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // 100% solid, opaque colors (Zero transparency, completely eliminating alpha bleed-through)
    val activeTrackBgColor = if (isDarkTheme) Color(0xFF223555) else Color(0xFFE2ECFA)
    val activeTrackBorderColor = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val selectedTrackBgColor = if (isDarkTheme) Color(0xFF2A2D3A) else Color(0xFFE8EDF5)
    val selectedTrackBorderColor = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
    val normalTrackBgColor = if (isDarkTheme) Color(0xFF1C2028) else Color(0xFFF1F5F9)
    val normalTrackBorderColor = if (isDarkTheme) Color(0xFF282D39) else Color(0xFFE2E8F0)

    val containerBgColor = when {
        isSelected -> selectedTrackBgColor
        isCurrent -> activeTrackBgColor
        else -> normalTrackBgColor
    }
    val containerBorder = when {
        isCurrent -> BorderStroke(1.5.dp, activeTrackBorderColor)
        isSelected -> BorderStroke(1.dp, selectedTrackBorderColor)
        else -> BorderStroke(1.dp, normalTrackBorderColor)
    }

    // High contrast typography colors
    val titleColor = when {
        isCurrent -> if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF0F172A)
        isSelected -> if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF0F172A)
        else -> if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }
    val subtitleColor = when {
        isCurrent -> if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1E40AF)
        isSelected -> if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155)
        else -> if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
    }
    val badgeBgColor = if (isDarkTheme) Color(0xFF2563EB) else Color(0xFF1D4ED8)
    val actionIconColor = when {
        isCurrent -> if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)
        else -> if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Selection Checkbox: Strictly positioned OUTSIDE the container bounds on the far left
        if (showCheckbox && onToggleSelect != null) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB),
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("track_select_checkbox_${track.id}")
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // 2. Track Container: Strictly encompasses ONLY track info (artwork, title, artist) and action buttons
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onTrackClick() }
                .testTag("track_row_${track.id}"),
            shape = RoundedCornerShape(8.dp),
            color = containerBgColor,
            border = containerBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Theme-Independent Static Artwork Thumbnail
                    StaticTrackThumbnail(
                        track = track,
                        cornerRadius = 6.dp,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .testTag(if (isCurrent) "now_playing_artwork" else "track_artwork_${track.id}")
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            color = titleColor,
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
                                    color = badgeBgColor,
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
                            val subtitle = if (track.artist.isNotBlank() || track.album.isNotBlank()) {
                                "${track.artist} • ${track.album}"
                            } else {
                                "Audio Track"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = subtitleColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    if (onFavoriteClick != null) {
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_favorite")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFFC107) else actionIconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onAddToQueueClick != null) {
                        IconButton(
                            onClick = onAddToQueueClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Queue",
                                tint = actionIconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Universal, theme-independent static track thumbnail.
 * Strictly decoupled from the global theme engine and Now Playing themes
 * (Vinyl, Cyber Pulse, Sonic Radar, Cassette, CD, etc.).
 */
@Composable
fun StaticTrackThumbnail(
    track: AudioTrack?,
    cornerRadius: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF232730)),
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
            // Independent, static visual identity: classic audio note on sleek gradient tile
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2C3E50), Color(0xFF3498DB))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

