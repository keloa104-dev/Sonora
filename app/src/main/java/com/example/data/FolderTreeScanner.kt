package com.example.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.model.AudioTrack
import com.example.model.FolderNode

object FolderTreeScanner {

    fun buildFolderNodeFromUriAndTracks(
        context: Context,
        treeUri: Uri,
        allTracks: List<AudioTrack>
    ): FolderNode {
        val uriStr = treeUri.toString()
        val decoded = Uri.decode(uriStr)
        val folderName = decoded.substringAfterLast("%3A")
            .substringAfterLast(":")
            .substringAfterLast("/")
            .ifBlank { "Folder" }

        val targetPathSegment = extractPathSegment(decoded).lowercase()

        val matchingTracks = if (targetPathSegment.isBlank()) {
            allTracks
        } else {
            allTracks.filter { track ->
                val rel = track.relativePath.lowercase()
                val path = track.filePath.lowercase()
                val uriPath = track.uri.toString().lowercase()
                rel.contains(targetPathSegment) || path.contains(targetPathSegment) || uriPath.contains(targetPathSegment)
            }
        }

        if (matchingTracks.isNotEmpty()) {
            val trackPairs = matchingTracks.map { track ->
                val rel = if (track.relativePath.isNotBlank()) track.relativePath else track.filePath
                val cleanRel = rel.replace('\\', '/').trim('/')
                val lowerClean = cleanRel.lowercase()
                val relativeSegment = if (targetPathSegment.isNotBlank() && lowerClean.contains(targetPathSegment)) {
                    val idx = lowerClean.indexOf(targetPathSegment)
                    cleanRel.substring(idx + targetPathSegment.length).trim('/')
                } else {
                    cleanRel
                }

                val parts = relativeSegment.split('/').filter { it.isNotBlank() }
                val dirParts = if (parts.size > 1 && isFileName(parts.last(), track)) {
                    parts.dropLast(1)
                } else if (parts.size == 1 && isFileName(parts.last(), track)) {
                    emptyList()
                } else {
                    parts
                }
                Pair(track, dirParts)
            }

            return buildHierarchicalSubtree(
                currentId = uriStr,
                currentName = folderName,
                currentUri = treeUri,
                trackPairs = trackPairs
            )
        }

        // Fallback to DocumentFile scanning if MediaStore returned no matches
        val docNode = scanDocumentTree(context, treeUri)
        return docNode ?: FolderNode(
            id = uriStr,
            name = folderName,
            uri = treeUri,
            isDirectory = true,
            tracks = emptyList(),
            subfolders = emptyList()
        )
    }

    private fun isFileName(part: String, track: AudioTrack): Boolean {
        val lower = part.lowercase()
        if (lower.contains('.') && (lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".aac") || lower.endsWith(".ogg") || lower.endsWith(".opus") || lower.endsWith(".wma"))) {
            return true
        }
        val fileName = track.filePath.substringAfterLast('/').lowercase()
        if (fileName.isNotBlank() && lower == fileName) return true
        val titleLower = track.title.lowercase()
        if (titleLower.isNotBlank() && lower.contains(titleLower)) return true
        return false
    }

    private fun buildHierarchicalSubtree(
        currentId: String,
        currentName: String,
        currentUri: Uri?,
        trackPairs: List<Pair<AudioTrack, List<String>>>
    ): FolderNode {
        val directTracks = trackPairs.filter { it.second.isEmpty() }.map { it.first }
        val subfolderTrackPairs = trackPairs.filter { it.second.isNotEmpty() }

        val subfolderMap = subfolderTrackPairs.groupBy { it.second[0] }

        val subfolderNodes = subfolderMap.map { (subName, subPairs) ->
            val nextId = "${currentId}_${subName}"
            val formattedSubName = subName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val nextPairs = subPairs.map { Pair(it.first, it.second.drop(1)) }
            buildHierarchicalSubtree(nextId, formattedSubName, null, nextPairs)
        }

        return FolderNode(
            id = currentId,
            name = currentName,
            uri = currentUri,
            isDirectory = true,
            tracks = directTracks,
            subfolders = subfolderNodes
        )
    }

    fun extractPathSegment(decodedUri: String): String {
        return when {
            decodedUri.contains("primary:") -> decodedUri.substringAfter("primary:").trim('/')
            decodedUri.contains("raw:") -> decodedUri.substringAfter("raw:").trim('/')
            decodedUri.contains("tree/") -> decodedUri.substringAfter("tree/").substringAfter(':').trim('/')
            else -> decodedUri.substringAfterLast('/')
        }
    }

    fun scanDocumentTree(context: Context, treeUri: Uri): FolderNode? {
        val rootDoc = try {
            DocumentFile.fromTreeUri(context, treeUri)
        } catch (e: Exception) {
            null
        } ?: return null

        val rootName = rootDoc.name ?: treeUri.lastPathSegment?.substringAfterLast("%3A")?.substringAfterLast("/") ?: "Root Folder"
        return scanDocumentDirectory(dirDoc = rootDoc, dirName = rootName, idPrefix = treeUri.toString())
    }

    private fun scanDocumentDirectory(
        dirDoc: DocumentFile,
        dirName: String,
        idPrefix: String
    ): FolderNode {
        val tracksList = mutableListOf<AudioTrack>()
        val subfoldersList = mutableListOf<FolderNode>()

        val files = dirDoc.listFiles()
        for (file in files) {
            val fileUri = file.uri
            val fileName = file.name ?: "Unknown"

            if (file.isDirectory) {
                val childNode = scanDocumentDirectory(
                    dirDoc = file,
                    dirName = fileName,
                    idPrefix = "${idPrefix}/${fileName}"
                )
                if (childNode.totalTrackCount() > 0) {
                    subfoldersList.add(childNode)
                }
            } else if (file.isFile && isAudioFile(file, fileName)) {
                val track = AudioTrack(
                    id = fileUri.hashCode().toLong(),
                    title = fileName.substringBeforeLast("."),
                    artist = dirName,
                    album = dirName,
                    durationMs = 0L,
                    uri = fileUri
                )
                tracksList.add(track)
            }
        }

        return FolderNode(
            id = idPrefix,
            name = dirName,
            uri = dirDoc.uri,
            isDirectory = true,
            tracks = tracksList,
            subfolders = subfoldersList
        )
    }

    private fun isAudioFile(file: DocumentFile, name: String): Boolean {
        val mime = file.type ?: ""
        if (mime.startsWith("audio/")) return true
        val lowerName = name.lowercase()
        return lowerName.endsWith(".mp3") ||
                lowerName.endsWith(".m4a") ||
                lowerName.endsWith(".flac") ||
                lowerName.endsWith(".wav") ||
                lowerName.endsWith(".aac") ||
                lowerName.endsWith(".ogg") ||
                lowerName.endsWith(".opus")
    }

    /**
     * Builds a folder tree from a flat list of AudioTracks using album/artist or Uri path segments.
     */
    fun buildTreeFromTracks(allTracks: List<AudioTrack>): List<FolderNode> {
        if (allTracks.isEmpty()) return emptyList()

        val grouped = allTracks.groupBy { track ->
            val path = track.uri.path ?: ""
            val segments = path.split("/").filter { it.isNotBlank() }
            if (segments.size >= 2) {
                segments[segments.size - 2]
            } else {
                track.album.ifBlank { "Music Library" }
            }
        }

        return grouped.map { (folderName, tracksInFolder) ->
            FolderNode(
                id = "library_folder_$folderName",
                name = folderName,
                uri = null,
                isDirectory = true,
                tracks = tracksInFolder,
                subfolders = emptyList()
            )
        }
    }
}
