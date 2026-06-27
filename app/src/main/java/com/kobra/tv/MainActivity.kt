package com.kobra.tv

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ওয়েবসাইটের থিম কালার
val AppBg = Color(0xFF181623)
val CardBg = Color(0xFF211F30)
val AccentYellow = Color(0xFFFACC15)
val PhoenixColor = Color(0xFF9CA3AF)
val FalconColor = Color(0xFF38BDF8)
val BorderColor = Color(0xFF334155)

enum class Tab { CHANNELS, FAVORITES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScreen()
            }
        }
    }
}

@Composable
fun AppScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // States
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var currentPlayingUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(Tab.CHANNELS) }
    
    // Favorites State (SharedPreferences)
    val prefs = context.getSharedPreferences("kobra_prefs", Context.MODE_PRIVATE)
    var favoriteUrls by remember { 
        mutableStateOf(prefs.getStringSet("favorites", emptySet()) ?: emptySet()) 
    }

    val m3uUrl = "https://raw.githubusercontent.com/piyashltd/all-in-one/refs/heads/main/channels/index.m3u"

    LaunchedEffect(Unit) {
        scope.launch {
            // M3uParser আপনার প্রজেক্টে আগে থেকেই আছে ধরে নেওয়া হলো
            channels = M3uParser.fetchChannels(m3uUrl)
            isLoading = false
        }
    }

    // Hardware Back Button
    BackHandler(enabled = currentPlayingUrl != null) {
        currentPlayingUrl = null
    }

    // TV Detection
    val configuration = LocalConfiguration.current
    val isTv = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

    val toggleFavorite: (String) -> Unit = { url ->
        val newFavs = favoriteUrls.toMutableSet()
        if (newFavs.contains(url)) newFavs.remove(url) else newFavs.add(url)
        favoriteUrls = newFavs
        prefs.edit().putStringSet("favorites", newFavs).apply()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBg)) {
        if (currentPlayingUrl != null) {
            // আপনার VideoPlayer.kt এর ExoPlayerView
            ExoPlayerView(videoUrl = currentPlayingUrl!!, modifier = Modifier.fillMaxSize())
        } else {
            if (isTv) {
                // TV Layout (Side Navigation)
                Row(modifier = Modifier.fillMaxSize()) {
                    TvSideNav(currentTab) { currentTab = it }
                    ContentArea(
                        isLoading = isLoading,
                        currentTab = currentTab,
                        channels = channels,
                        favoriteUrls = favoriteUrls,
                        onPlay = { currentPlayingUrl = it },
                        onToggleFav = toggleFavorite,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Mobile Layout (Bottom Navigation)
                Scaffold(
                    containerColor = AppBg,
                    bottomBar = { MobileBottomNav(currentTab) { currentTab = it } }
                ) { padding ->
                    ContentArea(
                        isLoading = isLoading,
                        currentTab = currentTab,
                        channels = channels,
                        favoriteUrls = favoriteUrls,
                        onPlay = { currentPlayingUrl = it },
                        onToggleFav = toggleFavorite,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun ContentArea(
    isLoading: Boolean,
    currentTab: Tab,
    channels: List<Channel>,
    favoriteUrls: Set<String>,
    onPlay: (String) -> Unit,
    onToggleFav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayChannels = if (currentTab == Tab.FAVORITES) {
        channels.filter { favoriteUrls.contains(it.url) }
    } else {
        channels
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentTab == Tab.CHANNELS) "All Channels" else "My Favorites",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (currentTab == Tab.FAVORITES) {
                Text(
                    text = "${displayChannels.size} saved",
                    color = AccentYellow,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentYellow)
            }
        } else if (displayChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No channels found.", color = Color.Gray)
            }
        } else {
            if (currentTab == Tab.CHANNELS) {
                // Channels (Grid View - ওয়েবসাইটের প্রথম স্ক্রিনশট)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayChannels) { channel ->
                        ChannelGridCard(
                            channel = channel,
                            isFavorite = favoriteUrls.contains(channel.url),
                            onPlay = { onPlay(channel.url) },
                            onToggleFav = { onToggleFav(channel.url) }
                        )
                    }
                }
            } else {
                // Favorites (List View - ওয়েবসাইটের দ্বিতীয় স্ক্রিনশট)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayChannels) { channel ->
                        ChannelListCard(
                            channel = channel,
                            isFavorite = favoriteUrls.contains(channel.url),
                            onPlay = { onPlay(channel.url) },
                            onToggleFav = { onToggleFav(channel.url) }
                        )
                    }
                }
            }
        }
    }
}

// ---------------- UI COMPONENTS ----------------

@Composable
fun MobileBottomNav(currentTab: Tab, onTabSelected: (Tab) -> Unit) {
    NavigationBar(containerColor = Color(0xFF13111C), contentColor = Color.Gray) {
        NavigationBarItem(
            selected = currentTab == Tab.CHANNELS,
            onClick = { onTabSelected(Tab.CHANNELS) },
            icon = { Icon(Icons.Default.Tv, contentDescription = "Channels") },
            label = { Text("Channels") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentYellow,
                selectedTextColor = AccentYellow,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentTab == Tab.FAVORITES,
            onClick = { onTabSelected(Tab.FAVORITES) },
            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites") },
            label = { Text("Favorites") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentYellow,
                selectedTextColor = AccentYellow,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun TvSideNav(currentTab: Tab, onTabSelected: (Tab) -> Unit) {
    NavigationRail(
        containerColor = Color(0xFF13111C),
        modifier = Modifier.fillMaxHeight().width(80.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        NavigationRailItem(
            selected = currentTab == Tab.CHANNELS,
            onClick = { onTabSelected(Tab.CHANNELS) },
            icon = { Icon(Icons.Default.Tv, contentDescription = "Channels") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = AccentYellow,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color(0xFF2A273F)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        NavigationRailItem(
            selected = currentTab == Tab.FAVORITES,
            onClick = { onTabSelected(Tab.FAVORITES) },
            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = AccentYellow,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color(0xFF2A273F)
            )
        )
    }
}

@Composable
fun ChannelGridCard(channel: Channel, isFavorite: Boolean, onPlay: () -> Unit, onToggleFav: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val currentBorderColor = if (isFocused || isFavorite) AccentYellow.copy(alpha = 0.5f) else BorderColor

    Column(
        modifier = Modifier
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, currentBorderColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onPlay() }
            .focusable(interactionSource = interactionSource)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(Color(0xFF2A273F), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Gray)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = channel.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(6.dp))
        TagBadge(channel.group)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2A273F), RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .clickable { onToggleFav() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Fav",
                    tint = if (isFavorite) AccentYellow else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(AccentYellow, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Watch", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ChannelListCard(channel: Channel, isFavorite: Boolean, onPlay: () -> Unit, onToggleFav: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val currentBorderColor = if (isFocused) AccentYellow else BorderColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, currentBorderColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onPlay() }
            .focusable(interactionSource = interactionSource)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(Color(0xFF2A273F), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentYellow)
        }
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text = channel.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            TagBadge(channel.group)
        }
        
        IconButton(onClick = onToggleFav) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Fav",
                tint = if (isFavorite) AccentYellow else Color.Gray
            )
        }
    }
}

@Composable
fun TagBadge(group: String) {
    val (bgColor, textColor) = when (group.uppercase()) {
        "PHOENIX" -> PhoenixColor to Color.White
        "FALCON" -> FalconColor to Color.Black
        else -> AccentYellow to Color.Black
    }
    Box(
        modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = group.uppercase(), color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
