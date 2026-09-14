package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import android.net.Uri
import com.example.model.AudioTrack

@Entity(
    tableName = "tracks",
    indices = [
        Index("filePath"),
        Index("parentFolderPath"),
        Index("dateModified"),
        Index("durationMs")
    ]
)
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumArtUriString: String? = null,
    val albumArtDrawableRes: Int? = null,
    val mimeType: String = "audio/mpeg",
    val isFavorite: Boolean = false,
    val filePath: String = "",
    val relativePath: String = "",
    val parentFolderPath: String = "",
    val dateModified: Long = 0L
) {
    fun toAudioTrack(): AudioTrack {
        return AudioTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = Uri.parse(uriString),
            albumArtUri = albumArtUriString?.let { Uri.parse(it) },
            albumArtDrawableRes = albumArtDrawableRes,
            mimeType = mimeType,
            isFavorite = isFavorite,
            filePath = filePath,
            relativePath = relativePath
        )
    }

    companion object {
        fun fromAudioTrack(track: AudioTrack, parentPath: String = "", dateMod: Long = 0L): TrackEntity {
            return TrackEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                uriString = track.uri.toString(),
                albumArtUriString = track.albumArtUri?.toString(),
                albumArtDrawableRes = track.albumArtDrawableRes,
                mimeType = track.mimeType,
                isFavorite = track.isFavorite,
                filePath = track.filePath,
                relativePath = track.relativePath,
                parentFolderPath = parentPath,
                dateModified = dateMod
            )
        }
    }
}
