package com.ignatrozhko.governedworkflow.ui.servers

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.ui.common.EmptyState
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import java.util.concurrent.Executors

/** Full-screen camera that reports each distinct QR payload until [onScanned] accepts one. */
@Composable
fun QrScanner(onScanned: (String) -> Boolean, onClose: () -> Unit, error: String?) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        if (hasPermission) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp).clip(MaterialTheme.shapes.extraLarge)) { CameraPreview(onScanned) }
        } else {
            Spacer(Modifier.weight(1f))
        }
        EmptyState(
            title = if (hasPermission) "Scanning for a QR code" else "Camera permission needed",
            message = error ?: if (hasPermission) "Point the camera at the panel's connection QR code." else "Allow the camera to scan the panel's QR code, or enter the address yourself.",
            icon = painterResource(R.drawable.ic_qr),
            actions = { PrimaryButton("Enter manually", onClose) },
        )
    }
}

@Composable
private fun CameraPreview(onScanned: (String) -> Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val previewView = remember { PreviewView(context) }
    val analyzer = remember { QrAnalyzer { text -> ContextCompat.getMainExecutor(context).execute { currentOnScanned(text) } } }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(lifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analyzer.executor, analyzer) }
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        cameraProvider = provider
    }
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            analyzer.close()
        }
    }
}

private class QrAnalyzer(private val onDistinctPayload: (String) -> Unit) : ImageAnalysis.Analyzer {
    val executor = Executors.newSingleThreadExecutor()
    private val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    private var lastPayload: String? = null

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                val payload = barcodes.firstNotNullOfOrNull { it.rawValue }
                if (payload != null && payload != lastPayload) {
                    lastPayload = payload
                    onDistinctPayload(payload)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        scanner.close()
        executor.shutdown()
    }
}
