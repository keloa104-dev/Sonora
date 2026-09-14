package com.example.data

import com.example.model.AudioTrack

/**
 * Singleton in-memory cache for ultra-fast, 0-latency display of audio tracks.
 * Populated at application startup from Room and updated whenever media scans complete.
 */
object SongCache {
    @Volatile
    var allSongs: List<AudioTrack> = emptyList()

    val isLoaded: Boolean
        get() = allSongs.isNotEmpty()
}
