package com.example.presentation.login

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, crash-resilient background player for the Login screen.
 * Combines an ultra-smooth, 100% crash-proof Jetpack Compose Canvas ambient animation
 * with a fail-safe TextureView + MediaPlayer loop.
 */
@Composable
fun BackgroundVideoPlayer(
    modifier: Modifier = Modifier,
    videoResId: Int = R.raw.login_bg_video,
    overlayColor: Color = Color(0x22FFFFFF)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reference-held player to avoid triggering recompositions
    val mediaPlayerRef = remember { arrayOfNulls<MediaPlayer>(1) }
    var hasVideoPlaybackError by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        mediaPlayerRef[0]?.start()
                    } catch (_: Throwable) {}
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mediaPlayerRef[0]?.pause()
                    } catch (_: Throwable) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayerRef[0]?.stop()
                mediaPlayerRef[0]?.release()
                mediaPlayerRef[0] = null
            } catch (_: Throwable) {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Guaranteed crash-proof Pure Compose Animated Canvas Background
        AnimatedCanvasBackground(modifier = Modifier.fillMaxSize())

        // Layer 2: Optional TextureView video loop with complete exception isolation
        if (!hasVideoPlaybackError) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    try {
                        TextureView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            val currentTextureView = this
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                    try {
                                        val surface = Surface(surfaceTexture)
                                        val mp = MediaPlayer().apply {
                                            setOnErrorListener { _, what, extra ->
                                                Log.w("BackgroundVideoPlayer", "MediaPlayer warning ($what, $extra), graceful fallback active.")
                                                hasVideoPlaybackError = true
                                                true
                                            }
                                            setSurface(surface)
                                            val videoUri = Uri.parse("android.resource://${ctx.packageName}/$videoResId")
                                            setDataSource(ctx, videoUri)
                                            isLooping = true
                                            setVolume(0f, 0f)
                                            setOnPreparedListener { player ->
                                                try {
                                                    player.start()
                                                    adjustAspectRatio(currentTextureView, player.videoWidth, player.videoHeight, width, height)
                                                } catch (_: Throwable) {
                                                    hasVideoPlaybackError = true
                                                }
                                            }
                                            setOnVideoSizeChangedListener { player, vWidth, vHeight ->
                                                try {
                                                    adjustAspectRatio(currentTextureView, vWidth, vHeight, width, height)
                                                } catch (_: Throwable) {}
                                            }
                                            prepareAsync()
                                        }
                                        mediaPlayerRef[0] = mp
                                    } catch (e: Throwable) {
                                        Log.w("BackgroundVideoPlayer", "Video texture unavailable: ${e.message}")
                                        hasVideoPlaybackError = true
                                    }
                                }

                                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                                    try {
                                        mediaPlayerRef[0]?.let { mp ->
                                            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                                                adjustAspectRatio(currentTextureView, mp.videoWidth, mp.videoHeight, width, height)
                                            }
                                        }
                                    } catch (_: Throwable) {}
                                }

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                    try {
                                        mediaPlayerRef[0]?.stop()
                                        mediaPlayerRef[0]?.release()
                                        mediaPlayerRef[0] = null
                                    } catch (_: Throwable) {}
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w("BackgroundVideoPlayer", "TextureView creation notice: ${t.message}")
                        hasVideoPlaybackError = true
                        TextureView(ctx)
                    }
                },
                update = {}
            )
        }

        // Layer 3: Soft ambient frost overlay to maximize UI contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x0AF0F9FF),
                            Color(0x20FFFFFF),
                            Color(0x55FFFFFF),
                            Color(0x80FFFFFF)
                        )
                    )
                )
        )
    }
}

/**
 * Pure Jetpack Compose animated canvas delivering 60fps glowing financial energy orbs and ambient gradients.
 */
@Composable
private fun AnimatedCanvasBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_canvas")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE0F2FE),
                    Color(0xFFF0F9FF),
                    Color(0xFFF8FAFC)
                )
            )
        )

        // Floating ambient orbs
        val orb1X = w * (0.3f + 0.15f * sin(phase))
        val orb1Y = h * (0.25f + 0.1f * cos(phase))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x4438BDF8), Color.Transparent),
                center = Offset(orb1X, orb1Y),
                radius = w * 0.55f
            ),
            center = Offset(orb1X, orb1Y),
            radius = w * 0.55f
        )

        val orb2X = w * (0.7f + 0.12f * cos(phase * 0.8f))
        val orb2Y = h * (0.7f + 0.15f * sin(phase * 0.8f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x350284C7), Color.Transparent),
                center = Offset(orb2X, orb2Y),
                radius = w * 0.6f
            ),
            center = Offset(orb2X, orb2Y),
            radius = w * 0.6f
        )
    }
}

/**
 * CenterCrop matrix calculation so the video fills the screen proportionally on all form factors.
 */
private fun adjustAspectRatio(textureView: TextureView, videoWidth: Int, videoHeight: Int, viewWidth: Int, viewHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

    val viewRatio = viewWidth.toFloat() / viewHeight
    val videoRatio = videoWidth.toFloat() / videoHeight

    val scaleX: Float
    val scaleY: Float

    if (videoRatio > viewRatio) {
        scaleX = videoRatio / viewRatio
        scaleY = 1.0f
    } else {
        scaleX = 1.0f
        scaleY = viewRatio / videoRatio
    }

    val matrix = Matrix()
    val px = viewWidth / 2f
    val py = viewHeight / 2f
    matrix.setScale(scaleX, scaleY, px, py)
    textureView.setTransform(matrix)
}
