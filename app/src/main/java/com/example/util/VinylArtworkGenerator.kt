package com.example.util

import android.content.Context
import android.graphics.Bitmap
import com.example.model.AudioTrack
import com.example.model.MusicVisualTheme

object VinylArtworkGenerator {

    fun generateVinylByteArray(
        context: Context,
        track: AudioTrack?,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): ByteArray? {
        return MusicVisualArtworkGenerator.generateThemeByteArray(
            context,
            track,
            MusicVisualTheme.VINYL,
            size,
            rotationAngle
        )
    }

    fun generateVinylBitmap(
        context: Context,
        track: AudioTrack?,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): Bitmap? {
        return MusicVisualArtworkGenerator.generateVinylBitmap(context, track, size, rotationAngle)
    }
}


