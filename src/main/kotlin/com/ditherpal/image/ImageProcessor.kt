package com.ditherpal.image

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import com.ditherpal.dithering.DitheringEngine
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Image processing pipeline for upscaling, grayscale conversion, and dithering
 */
class ImageProcessor(private val ditheringEngine: DitheringEngine) {

    /**
     * Process an image: upscale, convert to grayscale, apply dithering
     */
    fun processImage(
        bitmap: Bitmap,
        upscaleFactor: Int = 1,
        ditheringMethod: DitheringEngine.DitheringMethod = DitheringEngine.DitheringMethod.FLOYD_STEINBERG,
        levels: Int = 2,
        downscaleAfter: Boolean = false,
        progressCallback: ((Int) -> Unit)? = null
    ): Bitmap {
        progressCallback?.invoke(10)

        // Step 1: Upscale if needed
        var processed = bitmap
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        if (upscaleFactor > 1) {
            processed = upscaleBitmap(bitmap, upscaleFactor, progressCallback)
            progressCallback?.invoke(30)
        }

        // Step 2: Convert to grayscale
        val grayscale = bitmapToGrayscaleArray(processed)
        progressCallback?.invoke(40)

        // Step 3: Apply dithering
        val dithered = ditheringEngine.applyDithering(
            grayscale,
            processed.width,
            processed.height,
            ditheringMethod,
            levels
        )
        progressCallback?.invoke(70)

        // Step 4: Convert back to bitmap
        var result = grayscaleArrayToBitmap(dithered, processed.width, processed.height)
        progressCallback?.invoke(80)

        // Step 5: Downscale if requested
        if (downscaleAfter && upscaleFactor > 1) {
            result = downscaleBitmap(result, originalWidth, originalHeight)
            progressCallback?.invoke(90)
        }

        return result
    }

    /**
     * Convert bitmap to grayscale array (0-255 values)
     */
    private fun bitmapToGrayscaleArray(bitmap: Bitmap): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val grayscale = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // Standard grayscale conversion: 0.299*R + 0.587*G + 0.114*B
            val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            grayscale[i] = gray.coerceIn(0, 255)
        }

        return grayscale
    }

    /**
     * Convert grayscale array back to bitmap
     */
    private fun grayscaleArrayToBitmap(grayscale: IntArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in grayscale.indices) {
            val gray = grayscale[i] and 0xFF
            pixels[i] = Color.argb(255, gray, gray, gray)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Upscale bitmap using high-quality interpolation
     */
    private fun upscaleBitmap(
        bitmap: Bitmap,
        factor: Int,
        progressCallback: ((Int) -> Unit)? = null
    ): Bitmap {
        val newWidth = bitmap.width * factor
        val newHeight = bitmap.height * factor

        // For very large upscales, use stepped approach to avoid memory issues
        var current = bitmap
        var currentFactor = 1

        while (currentFactor < factor) {
            val stepFactor = maxOf(
                2,
                minOf(10, factor / currentFactor)
            )

            val nextWidth = current.width * stepFactor
            val nextHeight = current.height * stepFactor

            current = Bitmap.createScaledBitmap(current, nextWidth, nextHeight, true)
            currentFactor *= stepFactor

            progressCallback?.invoke(10 + (currentFactor * 15 / factor))
        }

        // Fine-tune to exact size if needed
        if (current.width != newWidth || current.height != newHeight) {
            current = Bitmap.createScaledBitmap(current, newWidth, newHeight, true)
        }

        return current
    }

    /**
     * Downscale bitmap to original size using nearest neighbor
     */
    private fun downscaleBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
    }

    /**
     * Create a thumbnail preview of the image
     */
    fun createPreview(bitmap: Bitmap, maxWidth: Int = 300, maxHeight: Int = 250): Bitmap {
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val previewWidth: Int
        val previewHeight: Int

        if (ratio > maxWidth.toFloat() / maxHeight.toFloat()) {
            previewWidth = maxWidth
            previewHeight = (maxWidth / ratio).toInt()
        } else {
            previewHeight = maxHeight
            previewWidth = (maxHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, true)
    }

    /**
     * Get image dimensions without loading entire bitmap
     */
    fun getImageDimensions(bitmap: Bitmap): Pair<Int, Int> {
        return Pair(bitmap.width, bitmap.height)
    }
}
