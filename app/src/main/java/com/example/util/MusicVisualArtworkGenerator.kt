package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.collection.LruCache
import com.example.model.AudioTrack
import com.example.model.MusicVisualTheme
import java.io.ByteArrayOutputStream
import kotlin.math.cos
import kotlin.math.sin

object MusicVisualArtworkGenerator {

    private val byteArrayCache = LruCache<String, ByteArray>(12 * 1024 * 1024)

    fun generateThemeByteArray(
        context: Context,
        track: AudioTrack?,
        theme: MusicVisualTheme = MusicVisualTheme.VINYL,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): ByteArray? {
        val cacheKey = "${theme.id}_${track?.id}_${size}_${rotationAngle.toInt()}"
        byteArrayCache.get(cacheKey)?.let { return it }

        val bitmap = generateThemeBitmap(context, track, theme, size, rotationAngle) ?: return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        bitmap.recycle()

        if (byteArray.isNotEmpty()) {
            byteArrayCache.put(cacheKey, byteArray)
        }
        return byteArray
    }

    fun generateThemeBitmap(
        context: Context,
        track: AudioTrack?,
        theme: MusicVisualTheme = MusicVisualTheme.VINYL,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): Bitmap? {
        return when (theme) {
            MusicVisualTheme.VINYL -> generateVinylBitmap(context, track, size, rotationAngle)
            MusicVisualTheme.CYBER_PULSE -> generateCyberPulseBitmap(context, track, size, rotationAngle)
            MusicVisualTheme.SONIC_RADAR -> generateSonicRadarBitmap(context, track, size, rotationAngle)
        }
    }

    fun generateVinylByteArray(
        context: Context,
        track: AudioTrack?,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): ByteArray? = generateThemeByteArray(context, track, MusicVisualTheme.VINYL, size, rotationAngle)

    fun generateVinylBitmap(
        context: Context,
        track: AudioTrack?,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): Bitmap? {
        try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val center = size / 2f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Outer Record Disc Radius
            val maxRadius = (size / 2f) * 0.96f

            // Base dark vinyl disc body
            paint.color = Color.parseColor("#121212")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, maxRadius, paint)

            // Optional rotation
            if (rotationAngle != 0f) {
                canvas.save()
                canvas.rotate(rotationAngle, center, center)
            }

            // 2. Outer Rim Ring (#E63946)
            paint.color = Color.parseColor("#E63946")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.02f
            canvas.drawCircle(center, center, maxRadius * 0.97f, paint)

            // 3. Concentric Red Grooved Rings (#E63946 @ 85% alpha)
            paint.color = Color.argb((255 * 0.85f).toInt(), 0xE6, 0x39, 0x46)
            paint.strokeWidth = maxRadius * 0.012f
            val grooveRatios = listOf(0.92f, 0.86f, 0.80f, 0.74f, 0.68f, 0.62f, 0.56f, 0.50f)
            grooveRatios.forEach { ratio ->
                canvas.drawCircle(center, center, maxRadius * ratio, paint)
            }

            // 4. Stroboscopic Dots Outer Ring (36 White Dots)
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            val outerDotRadius = maxRadius * 0.88f
            val dotSize = maxRadius * 0.018f
            for (i in 0 until 36) {
                val angleRad = Math.toRadians((i * 10).toDouble())
                val dotX = (center + outerDotRadius * cos(angleRad)).toFloat()
                val dotY = (center + outerDotRadius * sin(angleRad)).toFloat()
                canvas.drawCircle(dotX, dotY, dotSize, paint)
            }

            // 5. Stroboscopic Dashes Middle Ring (18 Yellow Arcs #FFD166)
            paint.color = Color.parseColor("#FFD166")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.022f
            val midRadius = maxRadius * 0.68f
            val dashRect = RectF(
                center - midRadius,
                center - midRadius,
                center + midRadius,
                center + midRadius
            )
            for (i in 0 until 18) {
                canvas.drawArc(dashRect, i * 20f, 10f, false, paint)
            }

            // 6. Inner Stroboscopic Dots Ring (12 Red Dots #E63946)
            paint.color = Color.parseColor("#E63946")
            paint.style = Paint.Style.FILL
            val innerDotRadius = maxRadius * 0.52f
            val innerDotSize = maxRadius * 0.022f
            for (i in 0 until 12) {
                val angleRad = Math.toRadians((i * 30).toDouble())
                val dotX = (center + innerDotRadius * cos(angleRad)).toFloat()
                val dotY = (center + innerDotRadius * sin(angleRad)).toFloat()
                canvas.drawCircle(dotX, dotY, innerDotSize, paint)
            }

            // 7. Center Album Art Label Core
            val labelRadius = maxRadius * 0.42f
            paint.color = Color.parseColor("#E63946")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, labelRadius, paint)

            // Try loading album art if present
            drawEmbeddedAlbumArtOrFallback(context, track, canvas, center, labelRadius, paint)

            // Label Core Cyan Border (#00E5FF)
            paint.color = Color.parseColor("#00E5FF")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.035f
            canvas.drawCircle(center, center, labelRadius, paint)

            // 8. Center Spindle Hole (Black with Gray Border)
            val holeRadius = maxRadius * 0.08f
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, holeRadius, paint)

            paint.color = Color.GRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.012f
            canvas.drawCircle(center, center, holeRadius, paint)

            if (rotationAngle != 0f) {
                canvas.restore()
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Theme 1: Cyber Pulse Orb (هولوجرام نبض النيون)
     * High-tech futuristic holographic sphere with neon cyan/magenta energy rings,
     * radial laser grid, orbital sound nodes, and glowing cybernetic core.
     */
    fun generateCyberPulseBitmap(
        context: Context,
        track: AudioTrack?,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): Bitmap? {
        try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val center = size / 2f
            val maxRadius = (size / 2f) * 0.94f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // 1. Dark Deep Cyber Space Background
            paint.color = Color.parseColor("#080B14")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, maxRadius, paint)

            if (rotationAngle != 0f) {
                canvas.save()
                canvas.rotate(rotationAngle, center, center)
            }

            // 2. Outer Cyber Energy Hexagon & Ring Glow
            paint.color = Color.parseColor("#00F0FF")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.025f
            canvas.drawCircle(center, center, maxRadius * 0.96f, paint)

            // 3. Cyber Matrix Radial Laser Lines (12 lines)
            paint.color = Color.argb(120, 0, 240, 255)
            paint.strokeWidth = maxRadius * 0.01f
            for (i in 0 until 12) {
                val angleRad = Math.toRadians((i * 30).toDouble())
                val startR = maxRadius * 0.46f
                val endR = maxRadius * 0.94f
                val x1 = (center + startR * cos(angleRad)).toFloat()
                val y1 = (center + startR * sin(angleRad)).toFloat()
                val x2 = (center + endR * cos(angleRad)).toFloat()
                val y2 = (center + endR * sin(angleRad)).toFloat()
                canvas.drawLine(x1, y1, x2, y2, paint)
            }

            // 4. Outer Neon Magenta Wave Arcs (#FF007F)
            paint.color = Color.parseColor("#FF007F")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.03f
            val arcR1 = maxRadius * 0.82f
            val rectArc1 = RectF(center - arcR1, center - arcR1, center + arcR1, center + arcR1)
            for (i in 0 until 4) {
                canvas.drawArc(rectArc1, i * 90f + 15f, 60f, false, paint)
            }

            // 5. Middle Glowing Cyan Frequency Ring (#00F0FF)
            paint.color = Color.argb(200, 0, 240, 255)
            paint.strokeWidth = maxRadius * 0.018f
            canvas.drawCircle(center, center, maxRadius * 0.68f, paint)

            // 6. Orbital Sound Nodes (Glowing particle circles around the orb)
            val orbitR = maxRadius * 0.68f
            paint.style = Paint.Style.FILL
            val nodeColors = listOf("#00F0FF", "#FF007F", "#A855F7", "#00F0FF", "#FF007F", "#A855F7")
            for (i in 0 until 6) {
                val angleRad = Math.toRadians((i * 60 + 20).toDouble())
                val nx = (center + orbitR * cos(angleRad)).toFloat()
                val ny = (center + orbitR * sin(angleRad)).toFloat()
                paint.color = Color.parseColor(nodeColors[i])
                canvas.drawCircle(nx, ny, maxRadius * 0.035f, paint)
                paint.color = Color.WHITE
                canvas.drawCircle(nx, ny, maxRadius * 0.015f, paint)
            }

            // 7. Inner Violet Pulse Ring (#8B5CF6)
            paint.color = Color.parseColor("#8B5CF6")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.022f
            val arcR2 = maxRadius * 0.54f
            val rectArc2 = RectF(center - arcR2, center - arcR2, center + arcR2, center + arcR2)
            for (i in 0 until 6) {
                canvas.drawArc(rectArc2, i * 60f + 5f, 40f, false, paint)
            }

            // 8. Holographic Central Plasma Core
            val labelRadius = maxRadius * 0.42f
            paint.color = Color.parseColor("#0D1B2A")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, labelRadius, paint)

            // Embedded album art / Cyber Glyph fallback
            drawEmbeddedAlbumArtOrFallback(context, track, canvas, center, labelRadius, paint, isCyber = true)

            // Glowing Dual Neon Ring Around Core (Cyan & Magenta)
            paint.color = Color.parseColor("#00F0FF")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.03f
            canvas.drawCircle(center, center, labelRadius, paint)

            paint.color = Color.parseColor("#FF007F")
            paint.strokeWidth = maxRadius * 0.015f
            canvas.drawCircle(center, center, labelRadius * 0.94f, paint)

            // Cyber Target Crosshair
            paint.color = Color.parseColor("#00F0FF")
            paint.strokeWidth = maxRadius * 0.02f
            val crossSize = maxRadius * 0.12f
            canvas.drawLine(center - crossSize, center, center - crossSize * 0.3f, center, paint)
            canvas.drawLine(center + crossSize * 0.3f, center, center + crossSize, center, paint)
            canvas.drawLine(center, center - crossSize, center, center - crossSize * 0.3f, paint)
            canvas.drawLine(center, center + crossSize * 0.3f, center, center + crossSize, paint)

            if (rotationAngle != 0f) {
                canvas.restore()
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Theme 2: Sonic Radar & Oscilloscope (رادار الأمواج الصوتية والمذبذب)
     * Audiophile precision oscilloscope screen with concentric range decibel grids,
     * scanning sonar radar sweep line, dynamic soundwave ripples, and emerald frequency meters.
     */
    fun generateSonicRadarBitmap(
        context: Context,
        track: AudioTrack?,
        size: Int = 512,
        rotationAngle: Float = 0f
    ): Bitmap? {
        try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val center = size / 2f
            val maxRadius = (size / 2f) * 0.94f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // 1. Deep Tactical Dark Green Canvas (#08140E)
            paint.color = Color.parseColor("#08140E")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, maxRadius, paint)

            if (rotationAngle != 0f) {
                canvas.save()
                canvas.rotate(rotationAngle, center, center)
            }

            // 2. Outer Oscilloscope Bezel Ring (#00FF88)
            paint.color = Color.parseColor("#00FF88")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.025f
            canvas.drawCircle(center, center, maxRadius * 0.96f, paint)

            // 3. Precision Radar Degree Tick Marks (Every 10 degrees)
            for (i in 0 until 36) {
                val angleRad = Math.toRadians((i * 10).toDouble())
                val isMajor = i % 9 == 0
                val isMedium = i % 3 == 0
                val tickLen = when {
                    isMajor -> maxRadius * 0.08f
                    isMedium -> maxRadius * 0.05f
                    else -> maxRadius * 0.028f
                }
                paint.color = if (isMajor) Color.parseColor("#00FF88") else Color.argb(160, 0, 255, 136)
                paint.strokeWidth = if (isMajor) maxRadius * 0.016f else maxRadius * 0.008f
                val startR = maxRadius * 0.96f - tickLen
                val endR = maxRadius * 0.96f
                val x1 = (center + startR * cos(angleRad)).toFloat()
                val y1 = (center + startR * sin(angleRad)).toFloat()
                val x2 = (center + endR * cos(angleRad)).toFloat()
                val y2 = (center + endR * sin(angleRad)).toFloat()
                canvas.drawLine(x1, y1, x2, y2, paint)
            }

            // 4. Concentric Acoustic Range Grid Circles (#00FF88 @ alpha)
            val gridRatios = listOf(0.85f, 0.70f, 0.55f)
            gridRatios.forEach { ratio ->
                paint.color = Color.argb(80, 0, 255, 136)
                paint.strokeWidth = maxRadius * 0.01f
                canvas.drawCircle(center, center, maxRadius * ratio, paint)
            }

            // 5. Radial Equalizer Frequency Spikes (around the mid circle)
            val spikeR = maxRadius * 0.70f
            for (i in 0 until 24) {
                val angleRad = Math.toRadians((i * 15).toDouble())
                val spikeHeight = (maxRadius * 0.06f) * ((i % 4) + 1) * 0.4f
                val x1 = (center + (spikeR - spikeHeight) * cos(angleRad)).toFloat()
                val y1 = (center + (spikeR - spikeHeight) * sin(angleRad)).toFloat()
                val x2 = (center + (spikeR + spikeHeight) * cos(angleRad)).toFloat()
                val y2 = (center + (spikeR + spikeHeight) * sin(angleRad)).toFloat()
                paint.color = Color.parseColor(if (i % 2 == 0) "#00FF88" else "#00E5FF")
                paint.strokeWidth = maxRadius * 0.012f
                canvas.drawLine(x1, y1, x2, y2, paint)
            }

            // 6. Sweeping Sonar Radar Laser Beam Segment
            paint.color = Color.argb(90, 0, 255, 136)
            paint.style = Paint.Style.FILL
            val sweepRect = RectF(center - maxRadius * 0.92f, center - maxRadius * 0.92f, center + maxRadius * 0.92f, center + maxRadius * 0.92f)
            canvas.drawArc(sweepRect, 270f, 60f, true, paint)

            paint.color = Color.parseColor("#00FF88")
            paint.strokeWidth = maxRadius * 0.02f
            canvas.drawLine(center, center, center, center - maxRadius * 0.92f, paint)

            // 7. Center Oscilloscope Audio Lens
            val labelRadius = maxRadius * 0.42f
            paint.color = Color.parseColor("#051B11")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, labelRadius, paint)

            // Embedded album art / Oscilloscope waveform fallback
            drawEmbeddedAlbumArtOrFallback(context, track, canvas, center, labelRadius, paint, isRadar = true)

            // Glowing Dual Oscilloscope Rim (Emerald & Cyan)
            paint.color = Color.parseColor("#00FF88")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius * 0.035f
            canvas.drawCircle(center, center, labelRadius, paint)

            paint.color = Color.parseColor("#00E5FF")
            paint.strokeWidth = maxRadius * 0.012f
            canvas.drawCircle(center, center, labelRadius * 0.92f, paint)

            // Reticle Target Lines
            paint.color = Color.parseColor("#00FF88")
            paint.strokeWidth = maxRadius * 0.014f
            val reticleR = labelRadius * 0.25f
            canvas.drawCircle(center, center, reticleR, paint)
            canvas.drawLine(center - reticleR * 1.5f, center, center + reticleR * 1.5f, center, paint)
            canvas.drawLine(center, center - reticleR * 1.5f, center, center + reticleR * 1.5f, paint)

            if (rotationAngle != 0f) {
                canvas.restore()
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawEmbeddedAlbumArtOrFallback(
        context: Context,
        track: AudioTrack?,
        canvas: Canvas,
        center: Float,
        labelRadius: Float,
        paint: Paint,
        isCyber: Boolean = false,
        isRadar: Boolean = false
    ) {
        var albumArtBitmap: Bitmap? = null
        try {
            if (track?.albumArtDrawableRes != null) {
                albumArtBitmap = BitmapFactory.decodeResource(context.resources, track.albumArtDrawableRes)
            } else if (track?.albumArtUri != null) {
                context.contentResolver.openInputStream(track.albumArtUri)?.use { stream ->
                    albumArtBitmap = BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            albumArtBitmap = null
        }

        if (albumArtBitmap != null) {
            val saveCount = canvas.save()
            val clipPath = Path().apply {
                addCircle(center, center, labelRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val srcRect = Rect(0, 0, albumArtBitmap.width, albumArtBitmap.height)
            val destRectF = RectF(center - labelRadius, center - labelRadius, center + labelRadius, center + labelRadius)
            canvas.drawBitmap(albumArtBitmap, srcRect, destRectF, paint)
            canvas.restoreToCount(saveCount)
            try { albumArtBitmap.recycle() } catch (_: Exception) {}
        } else {
            // Draw decorative fallback glyph inside the core
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = labelRadius * 0.08f
            if (isCyber) {
                paint.color = Color.parseColor("#00F0FF")
                val polyPath = Path()
                for (i in 0 until 6) {
                    val a = Math.toRadians((i * 60).toDouble())
                    val r = labelRadius * 0.55f
                    val x = (center + r * cos(a)).toFloat()
                    val y = (center + r * sin(a)).toFloat()
                    if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
                }
                polyPath.close()
                canvas.drawPath(polyPath, paint)
            } else if (isRadar) {
                paint.color = Color.parseColor("#00FF88")
                // Draw sine wave line
                val wavePath = Path()
                val waveWidth = labelRadius * 1.2f
                val startX = center - waveWidth / 2f
                wavePath.moveTo(startX, center)
                val steps = 30
                for (s in 0..steps) {
                    val frac = s / steps.toFloat()
                    val x = startX + frac * waveWidth
                    val y = center + (sin(frac * Math.PI * 4) * (labelRadius * 0.35f)).toFloat()
                    wavePath.lineTo(x, y)
                }
                canvas.drawPath(wavePath, paint)
            }
        }
    }
}
