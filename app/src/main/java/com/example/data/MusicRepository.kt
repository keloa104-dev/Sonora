package com.example.data

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.FolderEntity
import com.example.data.db.TrackEntity
import com.example.model.AudioTrack
import com.example.model.FolderNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val trackDao = db.trackDao()
    private val folderDao = db.folderDao()
    private val mediaScanner = MediaScanner(context)

    val allTracksFlow: Flow<List<AudioTrack>> = trackDao.getAllTracksFlow()
        .map { entities ->
            val tracks = entities.map { it.toAudioTrack() }
            if (tracks.isNotEmpty()) {
                SongCache.allSongs = tracks
            }
            tracks
        }
        .flowOn(Dispatchers.IO)

    suspend fun getCachedOrDbTracks(): List<AudioTrack> = withContext(Dispatchers.IO) {
        trackDao.deletePreinstalledTracks()
        if (SongCache.isLoaded) {
            val filtered = SongCache.allSongs.filter { it.id !in 10001L..10005L && !it.uri.toString().contains("exoplayer-test-media") }
            SongCache.allSongs = filtered
            return@withContext filtered
        }
        val fromDb = trackDao.getAllTracks().map { it.toAudioTrack() }
            .filter { it.id !in 10001L..10005L && !it.uri.toString().contains("exoplayer-test-media") }
        if (fromDb.isNotEmpty()) {
            SongCache.allSongs = fromDb
        }
        fromDb
    }

    val allFoldersFlow: Flow<List<FolderNode>> = combine(
        folderDao.getAllFoldersFlow(),
        trackDao.getAllTracksFlow()
    ) { folderEntities, trackEntities ->
        buildFolderTree(folderEntities, trackEntities)
    }.flowOn(Dispatchers.IO)

    suspend fun syncMedia(
        customFolderUris: List<String> = emptyList(),
        autoScanDevice: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            trackDao.deletePreinstalledTracks()
            val scannedTracks = mediaScanner.scanAudioTracks(customFolderUris, autoScanDevice)
            
            // Build folder nodes from scanned tracks and custom URIs
            val folderNodes = mutableListOf<FolderNode>()
            
            if (autoScanDevice) {
                val libraryFolders = FolderTreeScanner.buildTreeFromTracks(scannedTracks)
                folderNodes.addAll(libraryFolders)
            }

            for (uriStr in customFolderUris) {
                try {
                    val uri = android.net.Uri.parse(uriStr)
                    val treeNode = FolderTreeScanner.buildFolderNodeFromUriAndTracks(context, uri, scannedTracks)
                    if (folderNodes.none { it.id == treeNode.id }) {
                        folderNodes.add(treeNode)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Flatten folder hierarchy into FolderEntities and TrackEntities with parentFolderPath relationships
            val flatFolderEntities = mutableListOf<FolderEntity>()
            val flatTrackEntities = mutableListOf<TrackEntity>()

            fun processFolderNode(node: FolderNode, parentPath: String = "") {
                val entity = FolderEntity.fromFolderNode(node, parentPath = parentPath)
                flatFolderEntities.add(entity)

                node.tracks.forEach { track ->
                    val trackEntity = TrackEntity.fromAudioTrack(track, parentPath = node.id)
                    flatTrackEntities.add(trackEntity)
                }

                node.subfolders.forEach { sub ->
                    processFolderNode(sub, parentPath = node.id)
                }
            }

            folderNodes.forEach { node ->
                processFolderNode(node, parentPath = "")
            }

            // Sync with Room DB (Insert/Update & Purge missing only if autoScanDevice is active)
            val existingTrackEntities = trackDao.getAllTracks()
            val existingFolderEntities = folderDao.getAllFolders()

            val scannedTrackIds = flatTrackEntities.map { it.id }.toSet()
            val scannedFolderIds = flatFolderEntities.map { it.id }.toSet()

            if (autoScanDevice) {
                val tracksToDelete = existingTrackEntities.filter { !scannedTrackIds.contains(it.id) }.map { it.id }
                val foldersToDelete = existingFolderEntities.filter { !scannedFolderIds.contains(it.id) }.map { it.id }

                if (tracksToDelete.isNotEmpty()) {
                    trackDao.deleteTracksByIds(tracksToDelete)
                }
                if (foldersToDelete.isNotEmpty()) {
                    folderDao.deleteFoldersByIds(foldersToDelete)
                }
            }

            if (flatFolderEntities.isNotEmpty()) {
                folderDao.insertFolders(flatFolderEntities)
            }
            if (flatTrackEntities.isNotEmpty()) {
                val existingFavIds = existingTrackEntities.filter { it.isFavorite }.map { it.id }.toSet()
                val preservedTrackEntities = flatTrackEntities.map { entity ->
                    if (existingFavIds.contains(entity.id)) {
                        entity.copy(isFavorite = true)
                    } else {
                        entity
                    }
                }
                trackDao.insertTracks(preservedTrackEntities)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        trackDao.updateFavorite(id, isFavorite)
    }

    suspend fun isFavorite(id: Long): Boolean = withContext(Dispatchers.IO) {
        trackDao.getTrackById(id)?.isFavorite == true
    }

    suspend fun toggleFavorite(id: Long): Boolean = withContext(Dispatchers.IO) {
        val current = isFavorite(id)
        val newState = !current
        trackDao.updateFavorite(id, newState)
        newState
    }

    suspend fun deleteTracksByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        trackDao.deleteTracksByIds(ids)
    }

    suspend fun deleteTrackById(id: Long) = withContext(Dispatchers.IO) {
        trackDao.deleteTrackById(id)
    }

    suspend fun deleteFolderById(id: String) = withContext(Dispatchers.IO) {
        folderDao.deleteFolderById(id)
    }

    private fun buildFolderTree(
        folderEntities: List<FolderEntity>,
        trackEntities: List<TrackEntity>
    ): List<FolderNode> {
        if (folderEntities.isEmpty()) return emptyList()

        val tracksGroup = trackEntities.groupBy { it.parentFolderPath }
        val subfolderGroup = folderEntities.groupBy { it.parentFolderPath }

        fun constructNode(entity: FolderEntity): FolderNode {
            val childEntities = subfolderGroup[entity.id] ?: emptyList()
            val childSubfolders = childEntities.map { constructNode(it) }

            val directTrackEntities = tracksGroup[entity.id]
            val directTracks = if (directTrackEntities != null) {
                directTrackEntities.map { it.toAudioTrack() }
            } else {
                trackEntities.filter { entity.id.isNotBlank() && (it.uriString.contains(entity.id) || it.filePath.contains(entity.id)) }
                    .map { it.toAudioTrack() }
            }

            return entity.toFolderNode(
                tracks = directTracks,
                subfolders = childSubfolders
            )
        }

        val rootEntities = folderEntities.filter { it.parentFolderPath.isBlank() }
        if (rootEntities.isEmpty()) {
            return folderEntities.map { it.toFolderNode() }
        }

        return rootEntities.map { constructNode(it) }
    }
}
