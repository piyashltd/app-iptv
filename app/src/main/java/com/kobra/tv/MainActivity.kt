package com.kobra.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val appBg = Color(0xFF181623)
    private val cardBg = Color(0xFF211F30)
    private val accentYellow = Color(0xFFFACC15)
    private val titanColor = Color(0xFFFACC15)
    private val phoenixColor = Color(0xFF9CA3AF)
    private val falconColor = Color(0xFF38BDF8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScreen()
            }
        }
    }

    @Composable
    fun AppScreen() {
        val scope = rememberCoroutineScope()
        var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
        var currentPlayingUrl by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        val m3uUrl = "https://raw.githubusercontent.com/piyashltd/all-in-one/refs/heads/main/channels/index.m3u"

        LaunchedEffect(Unit) {
            scope.launch {
                channels = M3uParser.fetchChannels(m3uUrl)
                isLoading = false
            }
        }

        // হার্ডওয়্যার ব্যাক বাটন হ্যান্ডেল করা
        BackHandler(enabled = currentPlayingUrl != null) {
            currentPlayingUrl = null
        }

        Box(modifier = Modifier.fillMaxSize().background(appBg)) {
            if (currentPlayingUrl != null) {
                ExoPlayerView(
                    videoUrl = currentPlayingUrl!!,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "KOBRA CHANNELS",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentYellow)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(channels) { channel ->
                                ChannelCard(channel) { selectedUrl ->
                                    currentPlayingUrl = selectedUrl
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ChannelCard(channel: Channel, onClick: (String) -> Unit) {
        val tagColor = when (channel.group) {
            "PHOENIX" -> phoenixColor
            "FALCON" -> falconColor
            else -> titanColor
        }

        val tagTextColor = if (channel.group == "PHOENIX") Color.White else Color.Black

        Column(
            modifier = Modifier
                .background(cardBg, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .clickable { onClick(channel.url) }
                .focusable() 
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2A273F), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("TV", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = channel.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Box(
                modifier = Modifier
                    .background(tagColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = channel.group,
                    color = tagTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
