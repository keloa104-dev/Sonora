package com.example.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class FolderNode(
    val id: String,
    val name: String,
    val uri: Uri? = null,
    val isDirectory: Boolean = true,
    val tracks: List<AudioTrack> = emptyList(),
    val subfolders: List<FolderNode> = emptyList()
) {
    /**
     * Recursively retrieves all audio tracks inside this folder and all nested subfolders.
     */
    fun getAllTracksRecursively(): List<AudioTrack> {
        val all = mutableListOf<AudioTrack>()
        all.addAll(tracks)
        for (subfolder in subfolders) {
            all.addAll(subfolder.getAllTracksRecursively())
        }
        return all
    }

    /**
     * Total number of tracks contained in this folder and all subfolders.
     */
    fun totalTrackCount(): Int {
        return tracks.size + subfolders.sumOf { it.totalTrackCount() }
    }
}
