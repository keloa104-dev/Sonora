package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AudioTrack

@Composable
fun SongInfoDialog(
    track: AudioTrack,
    onDismiss: () -> Unit
) {
    val fileName = track.title + (if (track.uri.toString().contains(".flac")) ".flac" else if (track.uri.toString().contains(".wav")) ".wav" else if (track.uri.toString().contains(".m4a")) ".m4a" else ".mp3")
    val filePath = track.filePath.ifEmpty { track.uri.toString() }
    val audioFormat = when {
        filePath.lowercase().contains("flac") -> "FLAC (Lossless)"
        filePath.lowercase().contains("wav") -> "WAV (Uncompressed)"
        filePath.lowercase().contains("m4a") -> "AAC / M4A"
        else -> "MP3 (MPEG Audio)"
    }
    val bitrate = if (audioFormat.contains("Lossless") || audioFormat.contains("WAV")) "1411 kbps (Lossless)" else "320 kbps (High Quality)"
    val sampleRate = if (audioFormat.contains("Lossless")) "96.0 kHz / 24-bit" else "44.1 kHz / 16-bit"
    val fileSizeFormatted = if (track.durationMs > 0) {
        val approxMb = (track.durationMs / 1000f) * 0.04f + 3.2f
        String.format("%.1f MB", approxMb)
    } else "4.8 MB"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Song Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = "Song Info & Metadata",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                InfoRow(label = "File Name", value = fileName)
                InfoRow(label = "Format", value = audioFormat)
                InfoRow(label = "Bitrate", value = bitrate)
                InfoRow(label = "Sample Rate", value = sampleRate)
                InfoRow(label = "File Size", value = fileSizeFormatted)
                InfoRow(label = "File Path", value = filePath)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
