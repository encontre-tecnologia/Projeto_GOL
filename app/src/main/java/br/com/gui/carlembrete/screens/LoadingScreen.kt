package br.com.gui.carlembrete

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.DpSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun LoadingScreen(
    progress: Float,
    onVideoFinished: () -> Unit,
    videoResId: Int = R.raw.loading
) {
    // Estado para garantir que onVideoFinished so seja chamado uma vez
    var reportedFinished by remember { mutableStateOf(false) }

    val videoAspectRatio = remember(videoResId) {
        val retriever = MediaMetadataRetriever()
        try {
            val packageName = "br.com.gui.carlembrete"
            retriever.setDataSource("android.resource://$packageName/$videoResId")
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 0f
            if (width > 0f && height > 0f) width / height else 16f / 9f
        } catch (e: Exception) {
            16f / 9f
        } finally {
            retriever.release()
        }
    }

    // Cores do Tema
    val backgroundColor = Color(0xFF070C18)
    val iconColor = Color.White.copy(alpha = 0.12f)

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- Video central com tamanho fixo (100% visivel, sem corte) ---
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerRatio = maxWidth / maxHeight
                val baseSize = if (videoAspectRatio >= containerRatio) {
                    DpSize(maxWidth, maxWidth / videoAspectRatio)
                } else {
                    DpSize(maxHeight * videoAspectRatio, maxHeight)
                }
                val scaleUp = 1.15f
                val videoSize = DpSize(
                    width = (baseSize.width * scaleUp).coerceAtMost(maxWidth),
                    height = (baseSize.height * scaleUp).coerceAtMost(maxHeight)
                )

                // Box container do Video + Máscara
                Box(
                    modifier = Modifier
                        .size(videoSize)
                        .align(Alignment.Center)
                ) {
                    // 1. O Video Player
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val uri = Uri.parse("android.resource://${ctx.packageName}/$videoResId")
                                setVideoURI(uri)
                                setOnPreparedListener { player ->
                                    player.isLooping = false
                                    start()
                                }
                                setOnCompletionListener {
                                    if (!reportedFinished) {
                                        reportedFinished = true
                                        onVideoFinished()
                                    }
                                }
                                setOnErrorListener { _, _, _ ->
                                    if (!reportedFinished) {
                                        reportedFinished = true
                                        onVideoFinished()
                                    }
                                    true
                                }
                            }
                        },
                        update = { view ->
                            if (!view.isPlaying && !reportedFinished) {
                                view.start()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. A Máscara Gradiente (Fica por cima do vídeo)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                // Sintaxe corrigida: passamos os pares (posição to cor) diretamente
                                brush = Brush.verticalGradient(
                                    0.0f to backgroundColor,    // Topo totalmente coberto
                                    0.2f to backgroundColor,    // Mantém coberto um pouco mais
                                    0.4f to Color.Transparent,  // Começa a revelar o vídeo
                                    0.6f to Color.Transparent,  // Vídeo visível no meio
                                    0.8f to backgroundColor,    // Começa a cobrir embaixo
                                    1.0f to backgroundColor     // Base totalmente coberta
                                )
                            )
                    )
                }
            }

            // --- Camada de Ícones de Fundo ---
            Box(modifier = Modifier.fillMaxSize()) {
                BackgroundIcon(Icons.Rounded.Settings, 90.dp, iconColor, Alignment.TopStart, offsetX = (-18).dp, offsetY = 36.dp)
                BackgroundIcon(Icons.Rounded.Build, 70.dp, iconColor, Alignment.TopEnd, offsetX = 24.dp, offsetY = 120.dp)
                BackgroundIcon(Icons.Rounded.DirectionsCar, 80.dp, iconColor, Alignment.CenterStart, offsetX = (-28).dp, offsetY = (-8).dp)
                BackgroundIcon(Icons.Rounded.SettingsSuggest, 72.dp, iconColor, Alignment.BottomEnd, offsetX = 24.dp, offsetY = (-90).dp)
                BackgroundIcon(Icons.Rounded.Settings, 96.dp, iconColor, Alignment.BottomStart, offsetX = (-30).dp, offsetY = 36.dp)
                BackgroundIcon(Icons.Rounded.Build, 64.dp, iconColor, Alignment.TopCenter, offsetX = 0.dp, offsetY = (-10).dp)
                BackgroundIcon(Icons.Rounded.DirectionsCar, 72.dp, iconColor, Alignment.CenterEnd, offsetX = 22.dp, offsetY = 6.dp)
                BackgroundIcon(Icons.Rounded.SettingsSuggest, 60.dp, iconColor, Alignment.Center, offsetX = 0.dp, offsetY = 64.dp)
                BackgroundIcon(Icons.Rounded.Settings, 68.dp, iconColor, Alignment.CenterEnd, offsetX = 8.dp, offsetY = 120.dp)
                BackgroundIcon(Icons.Rounded.Build, 58.dp, iconColor, Alignment.BottomCenter, offsetX = (-10).dp, offsetY = 10.dp)
            }

        }
    }
}

// Componente auxiliar para os ícones
@Composable
fun BoxScope.BackgroundIcon(
    icon: ImageVector,
    size: Dp,
    tint: Color,
    alignment: Alignment,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(size)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .rotate(if(size > 100.dp) -15f else 30f)
    )
}
