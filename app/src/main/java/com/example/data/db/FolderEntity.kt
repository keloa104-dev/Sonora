package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import android.net.Uri
import com.example.model.FolderNode

@Entity(
    tableName = "folders",
    indices = [
        Index("parentFolderPath"),
        Index("dateModified")
    ]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val uriString: String? = null,
    val isDirectory: Boolean = true,
    val parentFolderPath: String = "",
    val dateModified: Long = 0L
) {
    fun toFolderNode(tracks: List<com.example.model.AudioTrack> = emptyList(), subfolders: List<FolderNode> = emptyList()): FolderNode {
        return FolderNode(
            id = id,
            name = name,
            uri = uriString?.let { Uri.parse(it) },
            isDirectory = isDirectory,
            tracks = tracks,
            subfolders = subfolders
        )
    }

    companion object {
        fun fromFolderNode(folder: FolderNode, parentPath: String = "", dateMod: Long = 0L): FolderEntity {
            return FolderEntity(
                id = folder.id,
                name = folder.name,
                uriString = folder.uri?.toString(),
                isDirectory = folder.isDirectory,
                parentFolderPath = parentPath,
                dateModified = dateMod
            )
        }
    }
}
