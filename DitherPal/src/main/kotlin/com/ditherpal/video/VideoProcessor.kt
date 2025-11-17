package com.ditherpal.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.ditherpal.dithering.DitheringEngine
import com.ditherpal.image.ImageProcessor
import java.io.File

/**
 * Video processing handler for extracting frames, dithering, and reassembling
 * Currently supports MP4 and MOV formats on Android 21+
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VideoProcessor(
    private val context: Context,
    private val ditheringEngine: DitheringEngine,
    private val imageProcessor: ImageProcessor
) {

    /**
     * Extract frames from video file
     * Returns list of (frameIndex, bitmap, durationMs)
     */
    fun extractVideoFrames(
        videoUri: Uri,
        progressCallback: ((Int) -> Unit)? = null
    ): List<Triple<Int, android.graphics.Bitmap, Long>> {
        val frames = mutableListOf<Triple<Int, android.graphics.Bitmap, Long>>()

        try {
            val inputStream = context.contentResolver.openInputStream(videoUri) ?: return frames
            val fileDescriptor = context.contentResolver.openAssetFileDescriptor(videoUri, "r")?.fileDescriptor
                ?: return frames

            val extractor = MediaExtractor()
            extractor.setDataSource(fileDescriptor)

            // Find video track
            var videoTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    videoTrackIndex = i
                    break
                }
            }

            if (videoTrackIndex < 0) {
                extractor.release()
                return frames
            }

            extractor.selectTrack(videoTrackIndex)
            val videoFormat = extractor.getTrackFormat(videoTrackIndex)

            // Get video properties
            val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val duration = videoFormat.getLong(MediaFormat.KEY_DURATION) / 1000 // Convert to ms
            val frameRate = videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
            val frameDurationMs = (1000 / frameRate).toLong()

            progressCallback?.invoke(5)

            // Create codec
            val mime = videoFormat.getString(MediaFormat.KEY_MIME) ?: return frames
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(videoFormat, null, null, 0)
            codec.start()

            var frameIndex = 0
            var bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    // Frame ready - would need to extract using Surface/SurfaceTexture
                    // This is a simplified version - full implementation would require:
                    // - Creating a Surface and passing to codec
                    // - Using SurfaceTexture or similar to get actual frame data
                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }

                    frameIndex++
                    val progress = 5 + (frameIndex * 10 / (duration / frameDurationMs).toInt())
                    progressCallback?.invoke(minOf(progress, 14))
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return frames
    }

    /**
     * Check if a URI points to a video file
     */
    fun isVideoFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri) ?: return false
        return mimeType.startsWith("video/")
    }

    /**
     * Get video dimensions without extracting all frames
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getVideoDimensions(videoUri: Uri): Pair<Int, Int>? {
        return try {
            val fileDescriptor = context.contentResolver.openAssetFileDescriptor(videoUri, "r")?.fileDescriptor
                ?: return null

            val extractor = MediaExtractor()
            extractor.setDataSource(fileDescriptor)

            var width = 0
            var height = 0

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    break
                }
            }

            extractor.release()

            if (width > 0 && height > 0) {
                Pair(width, height)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
