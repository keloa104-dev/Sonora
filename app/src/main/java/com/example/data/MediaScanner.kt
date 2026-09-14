package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaScanner(private val context: Context) {

    suspend fun scanAudioTracks(
        customFolderUris: List<String> = emptyList(),
        autoScanDevice: Boolean = false
    ): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()

        // 1. Query device local storage via MediaStore ONLY if autoScanDevice is enabled
        if (autoScanDevice) {
            val projectionList = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATA
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projectionList.add(MediaStore.Audio.Media.RELATIVE_PATH)
            }

            val projection = projectionList.toTypedArray()
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.IS_PENDING} == 0"
            } else {
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            }
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            try {
                context.contentResolver.query(
                    collectionUri,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                    val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val relPathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                    } else -1

                    while (cursor.moveToNext()) {
                        val filePath = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                        val relativePath = if (relPathColumn >= 0) cursor.getString(relPathColumn) ?: "" else ""

                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown Track"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val album = cursor.getString(albumColumn) ?: "Unknown Album"
                        val duration = cursor.getLong(durationColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/mpeg"

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        // Fast lazy artwork URI without blocking disk I/O
                        val artworkUri = if (albumId > 0) {
                            ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            )
                        } else null

                        tracks.add(
                            AudioTrack(
                                id = id,
                                title = title,
                                artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                                album = if (album == "<unknown>") "Local Music" else album,
                                durationMs = duration,
                                uri = contentUri,
                                albumArtUri = artworkUri,
                                mimeType = mimeType,
                                filePath = filePath,
                                relativePath = relativePath
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Scan custom folder URIs via DocumentFile to find any tracks not indexed by MediaStore
        customFolderUris.forEach { uriStr ->
            try {
                val folderUri = Uri.parse(uriStr)
                val rootDoc = DocumentFile.fromTreeUri(context, folderUri)
                if (rootDoc != null && rootDoc.isDirectory) {
                    scanDocumentDir(rootDoc, tracks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext tracks
    }

    private fun scanDocumentDir(dir: DocumentFile, tracks: MutableList<AudioTrack>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanDocumentDir(file, tracks)
            } else if (file.isFile) {
                val name = file.name ?: ""
                val mime = file.type ?: ""
                if (isAudioFile(name, mime)) {
                    val uri = file.uri
                    val track = AudioTrack(
                        id = uri.toString().hashCode().toLong(),
                        title = name.substringBeforeLast('.'),
                        artist = "Custom Folder Track",
                        album = dir.name ?: "Folder",
                        durationMs = 180000L,
                        uri = uri,
                        albumArtUri = null,
                        mimeType = if (mime.startsWith("audio/")) mime else "audio/mpeg"
                    )
                    tracks.add(track)
                }
            }
        }
    }

    private fun isAudioFile(name: String, mime: String): Boolean {
        val lower = name.lowercase()
        return mime.startsWith("audio/") ||
                lower.endsWith(".mp3") ||
                lower.endsWith(".wav") ||
                lower.endsWith(".flac") ||
                lower.endsWith(".aac") ||
                lower.endsWith(".ogg") ||
                lower.endsWith(".m4a")
    }
}
