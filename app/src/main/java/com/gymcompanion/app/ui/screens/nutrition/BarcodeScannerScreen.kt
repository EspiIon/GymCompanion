package com.gymcompanion.app.ui.screens.nutrition

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.gymcompanion.app.ui.theme.*
import java.util.concurrent.Executors

/**
 * Full-screen barcode scanner overlay.
 * Requests CAMERA permission at runtime using standard ActivityResultContracts.
 */
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) permissionDenied = true
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            permissionGranted -> {
                CameraPreview(
                    onBarcodeDetected = onBarcodeDetected,
                    modifier = Modifier.fillMaxSize()
                )
                ScannerOverlay()
            }
            permissionDenied -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("Accès caméra requis", style = MaterialTheme.typography.titleMedium,
                            color = Color.White)
                        Text("Autorise l'accès dans Paramètres > Applications > GymCompanion.",
                            style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        }

        // Close
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Fermer", tint = Color.White)
        }

        // Hint
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = "Pointe la caméra vers le code-barres",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp, 140.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Primary, RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanned by remember { mutableStateOf(false) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { ia ->
                        ia.setAnalyzer(executor) { proxy ->
                            if (!scanned) {
                                processImage(proxy, barcodeScanner) { code ->
                                    scanned = true
                                    onBarcodeDetected(code)
                                }
                            } else proxy.close()
                        }
                    }
                try {
                    // Bind direct ; en cas de conflit (ancien bind résiduel), on ne
                    // délie QUE si nécessaire — pas de unbindAll() systématique.
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                    )
                } catch (_: Exception) {
                    runCatching { provider.unbindAll() }
                    runCatching {
                        provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                    }
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImage(
    proxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onDetected: (String) -> Unit
) {
    val media = proxy.image ?: run { proxy.close(); return }
    val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(img)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull { it.valueType == Barcode.TYPE_PRODUCT }
                ?.rawValue?.let(onDetected)
        }
        .addOnCompleteListener { proxy.close() }
}
