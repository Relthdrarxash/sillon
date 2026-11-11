package fr.pilmil.sillon.ui.doctor

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import fr.pilmil.sillon.vision.FrameQuality
import fr.pilmil.sillon.vision.FrameQualityAnalyzer
import fr.pilmil.sillon.vision.FrameQualityStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

/**
 * Viseur caméra interactif plein-écran avec assistant de cadrage intelligent :
 * - Cadre dynamique coloré (Vert optimal / Jaune moyen / Rouge inexploitable)
 * - Déclenchement automatique stabilisé après 500 ms de cadrage optimal
 * - Bouton de déclenchement manuel permanent pour forcer la prise en conditions dégradées.
 */
@Composable
fun SmartCameraViewfinder(
    outputFile: File,
    onPhotoCaptured: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    var quality by remember {
        mutableStateOf(FrameQuality(FrameQualityStatus.WARNING, 0f, 0f, 0f, "Cadrez la feuille au centre"))
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Suivi du temps en condition optimale pour l'auto-déclenchement
    var optimalSince by remember { mutableLongStateOf(0L) }

    val frameColor by animateColorAsState(
        targetValue = when (quality.status) {
            FrameQualityStatus.OPTIMAL -> Color(0xFF2E7D32) // Vert vif
            FrameQualityStatus.WARNING -> Color(0xFFF9A825) // Jaune ambré
            FrameQualityStatus.UNUSABLE -> Color(0xFFD32F2F) // Rouge alerte
        },
        animationSpec = tween(300),
        label = "frameColor",
    )

    fun takePhoto() {
        if (isCapturing) return
        val capture = imageCapture ?: return
        isCapturing = true

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                }
            },
        )
    }

    // Déclenchement automatique si optimal pendant plus de 500 ms consécutives
    LaunchedEffect(quality.status) {
        if (quality.status == FrameQualityStatus.OPTIMAL) {
            val start = System.currentTimeMillis()
            optimalSince = start
            delay(500)
            if (optimalSince == start && !isCapturing) {
                takePhoto()
            }
        } else {
            optimalSince = 0L
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Flux caméra
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(
                        executor,
                        FrameQualityAnalyzer { evaluated ->
                            ContextCompat.getMainExecutor(ctx).execute {
                                quality = evaluated
                            }
                        },
                    )

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture,
                            analysis,
                        )
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Cadre de visée central dynamique
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .border(width = 4.dp, color = frameColor, shape = RoundedCornerShape(20.dp)),
        )

        // Barre supérieure : bouton fermer + statut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fermer le viseur",
                    tint = Color.White,
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, frameColor.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(frameColor, CircleShape),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = when (quality.status) {
                            FrameQualityStatus.OPTIMAL -> "Optimal"
                            FrameQualityStatus.WARNING -> "Acceptable"
                            FrameQualityStatus.UNUSABLE -> "Inexploitable"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Zone inférieure : conseil textuel + bouton de déclenchement manuel permanent
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Conseil contextuel
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                Text(
                    text = quality.tip,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Bouton de capture manuel
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .border(width = 4.dp, color = Color.White, shape = CircleShape)
                    .padding(6.dp),
            ) {
                IconButton(
                    onClick = { takePhoto() },
                    enabled = !isCapturing,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (quality.status == FrameQualityStatus.OPTIMAL) frameColor else Color.White,
                            CircleShape,
                        ),
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Prendre la photo maintenant",
                            tint = if (quality.status == FrameQualityStatus.OPTIMAL) Color.White else Color.Black,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Déclenchement automatique si vert, ou appuyez pour forcer",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
        }
    }
}
