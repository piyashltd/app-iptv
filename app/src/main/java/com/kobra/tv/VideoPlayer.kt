package com.kobra.tv

import android.view.KeyEvent as AndroidKeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    playlist: List<Channel>, 
    initialIndex: Int, 
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(initialIndex) }
    var isControllerVisible by remember { mutableStateOf(true) } // শুরুতে কন্ট্রোলার ভাসবে
    val focusRequester = remember { FocusRequester() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Auto-Next Channel Logic
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (currentIndex < playlist.size - 1) {
                        currentIndex++
                    } else {
                        currentIndex = 0 // লিস্টের শেষে গেলে প্রথম থেকে শুরু হবে
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { 
            exoPlayer.removeListener(listener)
            exoPlayer.release() 
        }
    }

    // Load new channel when currentIndex changes
    LaunchedEffect(currentIndex) {
        val url = playlist[currentIndex].url
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.play()
        isControllerVisible = true // চ্যানেল চেঞ্জ হলে কন্ট্রোলার ভাসবে
    }

    // 4 Second Controller Auto-Hide Logic
    LaunchedEffect(isControllerVisible, currentIndex) {
        if (isControllerVisible) {
            delay(4000)
            isControllerVisible = false
        }
    }
    
    // Auto-focus for TV Remote D-Pad
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val isDown = event.type == KeyEventType.KeyDown
                val isUp = event.type == KeyEventType.KeyUp
                val keyCode = event.nativeKeyEvent.keyCode

                when (keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (isUp) isControllerVisible = !isControllerVisible
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        if (isDown) {
                            // Previous Channel (Up)
                            currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
                        }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isDown) {
                            // Next Channel (Down)
                            currentIndex = if (currentIndex < playlist.size - 1) currentIndex + 1 else 0
                        }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                        // Long press to Rewind (একমাত্র চেপে ধরলেই কাজ করবে)
                        if (isDown && event.nativeKeyEvent.repeatCount > 0) {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 5000).coerceAtLeast(0))
                            isControllerVisible = true
                            true
                        } else {
                            // শর্ট প্রেস করলে কিছুই হবে না
                            false
                        }
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        // Long press to Forward
                        if (isDown && event.nativeKeyEvent.repeatCount > 0) {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 5000).coerceAtMost(exoPlayer.duration))
                            isControllerVisible = true
                            true
                        } else {
                            false
                        }
                    }
                    AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.KEYCODE_ESCAPE -> {
                        if (isUp) {
                            if (isControllerVisible) {
                                isControllerVisible = false // কন্ট্রোলার হাইড হবে
                            } else {
                                onBack() // প্লেয়ার ক্লোজ হবে
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isControllerVisible = !isControllerVisible }
                )
            }
    ) {
        // Video Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // আমরা কাস্টম কন্ট্রোলার ব্যবহার করছি
                    keepScreenOn = true 
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            }
        )

        // Custom Overlay Controller
        if (isControllerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(32.dp)
                ) {
                    Text(
                        text = "Now Playing",
                        color = AccentYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playlist[currentIndex].name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "▲ Prev / ▼ Next | ◄► Hold to Seek",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
