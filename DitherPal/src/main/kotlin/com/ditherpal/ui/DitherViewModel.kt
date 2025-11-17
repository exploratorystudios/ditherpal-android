package com.ditherpal.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ditherpal.dithering.DitheringEngine
import com.ditherpal.image.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DitherState(
    val selectedImageUri: Uri? = null,
    val selectedImagePath: String = "No image selected",
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val resultBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val progress: Int = 0,
    val progressMessage: String = "Ready",
    val ditheringMethod: DitheringEngine.DitheringMethod = DitheringEngine.DitheringMethod.FLOYD_STEINBERG,
    val outputLevels: Int = 2,
    val upscaleFactor: Int = 1,
    val downscaleAfter: Boolean = false,
    val useGrayscalePalette: Boolean = false,
    val lastError: String? = null,
    val lastSuccessMessage: String? = null
)

class DitherViewModel : ViewModel() {
    private val ditheringEngine = DitheringEngine()
    private val imageProcessor = ImageProcessor(ditheringEngine)

    private val _state = MutableStateFlow(DitherState())
    val state: StateFlow<DitherState> = _state

    fun setSelectedImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri, context)
                if (bitmap != null) {
                    val preview = imageProcessor.createPreview(bitmap)
                    val (width, height) = imageProcessor.getImageDimensions(bitmap)

                    _state.value = _state.value.copy(
                        selectedImageUri = uri,
                        selectedImagePath = "Selected: ${uri.lastPathSegment}",
                        imageWidth = width,
                        imageHeight = height,
                        originalBitmap = bitmap,
                        previewBitmap = preview,
                        lastError = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lastError = "Failed to load image: ${e.message}"
                )
            }
        }
    }

    fun setDitheringMethod(method: DitheringEngine.DitheringMethod) {
        _state.value = _state.value.copy(ditheringMethod = method)
    }

    fun setOutputLevels(levels: Int) {
        _state.value = _state.value.copy(outputLevels = levels.coerceIn(2, 16))
    }

    fun setUpscaleFactor(factor: Int) {
        _state.value = _state.value.copy(upscaleFactor = factor.coerceIn(1, 999999))
    }

    fun setDownscaleAfter(downscale: Boolean) {
        _state.value = _state.value.copy(downscaleAfter = downscale)
    }

    fun setUseGrayscalePalette(use: Boolean) {
        _state.value = _state.value.copy(useGrayscalePalette = use)
    }

    fun processImage() {
        val currentState = _state.value
        val bitmap = currentState.originalBitmap ?: run {
            _state.value = _state.value.copy(lastError = "No image selected")
            return
        }

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isProcessing = true,
                    progress = 0,
                    lastError = null,
                    lastSuccessMessage = null
                )

                val result = imageProcessor.processImage(
                    bitmap = bitmap,
                    upscaleFactor = currentState.upscaleFactor,
                    ditheringMethod = currentState.ditheringMethod,
                    levels = currentState.outputLevels,
                    downscaleAfter = currentState.downscaleAfter,
                    progressCallback = { progress ->
                        _state.value = _state.value.copy(
                            progress = progress,
                            progressMessage = when {
                                progress < 20 -> "Loading image..."
                                progress < 40 -> "Upscaling..."
                                progress < 50 -> "Converting to grayscale..."
                                progress < 80 -> "Dithering..."
                                progress < 90 -> "Finalizing..."
                                else -> "Complete!"
                            }
                        )
                    }
                )

                _state.value = _state.value.copy(
                    resultBitmap = result,
                    isProcessing = false,
                    progress = 100,
                    progressMessage = "✓ Complete!",
                    lastSuccessMessage = "Dithering completed successfully"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    lastError = "Processing error: ${e.message}"
                )
            }
        }
    }

    fun saveResult(context: Context) {
        val bitmap = _state.value.resultBitmap ?: run {
            _state.value = _state.value.copy(lastError = "No result to save")
            return
        }

        viewModelScope.launch {
            try {
                val fileName = generateFileName()
                val file = File(context.getExternalFilesDir(null), fileName)

                file.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                _state.value = _state.value.copy(
                    lastSuccessMessage = "Saved to: ${file.absolutePath}"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    lastError = "Failed to save: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(lastError = null)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(lastSuccessMessage = null)
    }

    private fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            android.graphics.BitmapFactory.decodeStream(inputStream)
        }
    }

    private fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "dithered_$timestamp.png"
    }
}
