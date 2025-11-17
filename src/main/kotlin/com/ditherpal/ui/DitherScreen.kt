package com.ditherpal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ditherpal.dithering.DitheringEngine
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun DitherScreen(
    context: Context,
    viewModel: DitherViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSelectedImage(uri, context)
        }
    }

    LaunchedEffect(state.lastError) {
        if (state.lastError != null) {
            // Error will be shown in UI
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DitherPal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel - Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image Selection
                ControlCard(title = "Image") {
                    Text(
                        text = state.selectedImagePath,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (state.imageWidth > 0) {
                        Text(
                            text = "Size: ${state.imageWidth}x${state.imageHeight}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image")
                    }
                }

                // Dithering Method
                ControlCard(title = "Dithering Method") {
                    val methods = listOf(
                        "Floyd-Steinberg" to DitheringEngine.DitheringMethod.FLOYD_STEINBERG,
                        "Sierra Lite" to DitheringEngine.DitheringMethod.SIERRA_LITE,
                        "Jarvis-Judice-Ninke" to DitheringEngine.DitheringMethod.JARVIS_JUDICE_NINKE
                    )

                    methods.forEach { (label, method) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.ditheringMethod == method,
                                onClick = { viewModel.setDitheringMethod(method) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }

                // Output Levels
                ControlCard(title = "Output Levels") {
                    Text(
                        text = if (state.outputLevels == 2) "2 (Black & White)" else "${state.outputLevels} (Grayscale)",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Slider(
                        value = state.outputLevels.toFloat(),
                        onValueChange = { viewModel.setOutputLevels(it.toInt()) },
                        valueRange = 2f..16f,
                        steps = 13
                    )
                }

                // Upscale Factor
                ControlCard(title = "Super-Sampling") {
                    Text(
                        text = if (state.upscaleFactor == 1) "1x (No upsampling)" else "${state.upscaleFactor}x",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = state.upscaleFactor.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { viewModel.setUpscaleFactor(it) }
                        },
                        label = { Text("Upscale Factor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.downscaleAfter,
                            onCheckedChange = { viewModel.setDownscaleAfter(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downscale to original size", fontSize = 12.sp)
                    }
                }

                // Output Format
                ControlCard(title = "Output Format") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.useGrayscalePalette,
                            onCheckedChange = { viewModel.setUseGrayscalePalette(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use grayscale palette", fontSize = 12.sp)
                    }
                }

                // Progress
                if (state.isProcessing) {
                    ControlCard(title = "Progress") {
                        LinearProgressIndicator(
                            progress = state.progress.toFloat() / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.progress}% - ${state.progressMessage}",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                // Process Button
                Button(
                    onClick = { viewModel.processImage() },
                    enabled = state.originalBitmap != null && !state.isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process Image", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Save Button
                Button(
                    onClick = { viewModel.saveResult(context) },
                    enabled = state.resultBitmap != null && !state.isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Result")
                }

                // Status Messages
                if (state.lastError != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = state.lastError ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.lastSuccessMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = state.lastSuccessMessage ?: "",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Right Panel - Previews
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Original Preview
                PreviewCard(
                    title = "📷 Original Image",
                    bitmap = state.previewBitmap
                )

                // Result Preview
                PreviewCard(
                    title = "✨ Dithered Result",
                    bitmap = state.resultBitmap?.let {
                        val preview = state.resultBitmap!!
                        android.graphics.Bitmap.createScaledBitmap(
                            preview,
                            minOf(300, preview.width),
                            minOf(250, preview.height),
                            true
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ControlCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun PreviewCard(
    title: String,
    bitmap: android.graphics.Bitmap?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (bitmap != null) {
                androidx.compose.material3.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Text(
                    text = "${title.substringAfter(" ")} will appear here",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
