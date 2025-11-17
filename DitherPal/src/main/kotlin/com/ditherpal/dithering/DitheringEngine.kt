package com.ditherpal.dithering

import kotlin.math.abs

/**
 * Core dithering engine with multiple dithering methods optimized for Android.
 * Implements Floyd-Steinberg, Sierra Lite, and Jarvis-Judice-Ninke algorithms.
 */
class DitheringEngine {

    enum class DitheringMethod {
        FLOYD_STEINBERG,
        SIERRA_LITE,
        JARVIS_JUDICE_NINKE
    }

    /**
     * Apply dithering to a grayscale image array
     *
     * @param imageData Grayscale pixel data (0-255)
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param method Dithering method to use
     * @param levels Number of output levels (2-256)
     * @return Dithered image data
     */
    fun applyDithering(
        imageData: IntArray,
        width: Int,
        height: Int,
        method: DitheringMethod,
        levels: Int
    ): IntArray {
        // Prepare quantization levels
        val quantizeLevels = FloatArray(levels)
        for (i in 0 until levels) {
            quantizeLevels[i] = (i * 255f) / (levels - 1)
        }

        // Convert input to 2D array for processing
        val image = Array(height) { y ->
            FloatArray(width) { x ->
                (imageData[y * width + x] and 0xFF).toFloat()
            }
        }

        // Apply selected dithering method
        return when (method) {
            DitheringMethod.FLOYD_STEINBERG -> floydSteinberg(image, quantizeLevels)
            DitheringMethod.SIERRA_LITE -> sierraLite(image, quantizeLevels)
            DitheringMethod.JARVIS_JUDICE_NINKE -> jarvisJudiceNinke(image, quantizeLevels)
        }
    }

    /**
     * Floyd-Steinberg dithering - classic and high quality
     */
    private fun floydSteinberg(
        image: Array<FloatArray>,
        quantizeLevels: FloatArray
    ): IntArray {
        val height = image.size
        val width = image[0].size
        val result = IntArray(height * width)

        // Error buffer for current and next row
        var errorBuffer = FloatArray(width)

        for (y in 0 until height) {
            val nextError = FloatArray(width)

            for (x in 0 until width) {
                // Original value + accumulated error
                val originalValue = image[y][x] + errorBuffer[x]

                // Find nearest quantization level
                val nearestLevel = findNearestLevel(originalValue, quantizeLevels)
                result[y * width + x] = nearestLevel.toInt() and 0xFF

                val error = originalValue - nearestLevel

                // Distribute error
                if (x + 1 < width) {
                    errorBuffer[x + 1] += error * 0.4375f // 7/16
                }
                if (x - 1 >= 0) {
                    nextError[x - 1] += error * 0.1875f // 3/16
                }
                nextError[x] += error * 0.3125f // 5/16
                if (x + 1 < width) {
                    nextError[x + 1] += error * 0.0625f // 1/16
                }
            }

            errorBuffer = nextError
        }

        return result
    }

    /**
     * Sierra Lite dithering - faster than Floyd-Steinberg with similar quality
     */
    private fun sierraLite(
        image: Array<FloatArray>,
        quantizeLevels: FloatArray
    ): IntArray {
        val height = image.size
        val width = image[0].size
        val result = IntArray(height * width)

        var errorBuffer = FloatArray(width)

        for (y in 0 until height) {
            val nextError = FloatArray(width)

            for (x in 0 until width) {
                val originalValue = image[y][x] + errorBuffer[x]
                val nearestLevel = findNearestLevel(originalValue, quantizeLevels)
                result[y * width + x] = nearestLevel.toInt() and 0xFF

                val error = originalValue - nearestLevel

                // Sierra Lite error distribution
                if (x + 1 < width) {
                    errorBuffer[x + 1] += error * 0.5f // 2/4
                }
                if (x - 1 >= 0) {
                    nextError[x - 1] += error * 0.25f // 1/4
                }
                nextError[x] += error * 0.25f // 1/4
            }

            errorBuffer = nextError
        }

        return result
    }

    /**
     * Jarvis-Judice-Ninke dithering - highest quality but slower
     */
    private fun jarvisJudiceNinke(
        image: Array<FloatArray>,
        quantizeLevels: FloatArray
    ): IntArray {
        val height = image.size
        val width = image[0].size
        val result = IntArray(height * width)

        var errorBuffer0 = FloatArray(width)
        var errorBuffer1 = FloatArray(width)

        for (y in 0 until height) {
            val nextError0 = FloatArray(width)
            val nextError1 = FloatArray(width)

            for (x in 0 until width) {
                val originalValue = image[y][x] + errorBuffer0[x]
                val nearestLevel = findNearestLevel(originalValue, quantizeLevels)
                result[y * width + x] = nearestLevel.toInt() and 0xFF

                val error = originalValue - nearestLevel

                // Current row error distribution
                if (x + 1 < width) {
                    errorBuffer0[x + 1] += error * (7.0f / 48.0f)
                }
                if (x + 2 < width) {
                    errorBuffer0[x + 2] += error * (5.0f / 48.0f)
                }

                // Next row errors
                if (x - 2 >= 0) {
                    nextError0[x - 2] += error * (3.0f / 48.0f)
                }
                if (x - 1 >= 0) {
                    nextError0[x - 1] += error * (5.0f / 48.0f)
                }
                nextError0[x] += error * (7.0f / 48.0f)
                if (x + 1 < width) {
                    nextError0[x + 1] += error * (5.0f / 48.0f)
                }
                if (x + 2 < width) {
                    nextError0[x + 2] += error * (3.0f / 48.0f)
                }

                // Row+2 errors
                if (x - 2 >= 0) {
                    nextError1[x - 2] += error * (1.0f / 48.0f)
                }
                if (x - 1 >= 0) {
                    nextError1[x - 1] += error * (3.0f / 48.0f)
                }
                nextError1[x] += error * (5.0f / 48.0f)
                if (x + 1 < width) {
                    nextError1[x + 1] += error * (3.0f / 48.0f)
                }
                if (x + 2 < width) {
                    nextError1[x + 2] += error * (1.0f / 48.0f)
                }
            }

            errorBuffer0 = nextError0
            errorBuffer1 = nextError1
        }

        return result
    }

    /**
     * Find the nearest quantization level to a value
     */
    private fun findNearestLevel(value: Float, levels: FloatArray): Float {
        var nearest = levels[0]
        var minDistance = abs(levels[0] - value)

        for (i in 1 until levels.size) {
            val distance = abs(levels[i] - value)
            if (distance < minDistance) {
                minDistance = distance
                nearest = levels[i]
            }
        }

        return nearest
    }
}
