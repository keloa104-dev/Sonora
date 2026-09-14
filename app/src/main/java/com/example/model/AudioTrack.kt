package com.example.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val albumArtDrawableRes: Int? = null,
    val mimeType: String = "audio/mpeg",
    val isFavorite: Boolean = false,
    val filePath: String = "",
    val relativePath: String = ""
)
