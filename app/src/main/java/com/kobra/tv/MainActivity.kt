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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
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
    
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var currentPlayingIndex by remember { mutableStateOf<Int?>(null) }
    var currentPlayingList by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(Tab.CHANNELS) }
    
    val prefs = context.getSharedPreferences("kobra_prefs", Context.MODE_PRIVATE)
    var favoriteUrls by remember { 
        mutableStateOf(prefs.getStringSet("favorites", emptySet()) ?: emptySet()) 
    }

    val m3uUrl = "https://raw.githubusercontent.com/piyashltd/all-in-one/refs/heads/main/channels/index.m3u"

    LaunchedEffect(Unit) {
        scope.launch {
            // M3uParser থেকে ডেটা ফেচ
            channels = M3uParser.fetchChannels(m3uUrl)
            isLoading = false
        }
    }

    BackHandler(enabled = currentPlayingIndex != null) {
        currentPlayingIndex = null
    }

    val configuration = LocalConfiguration.current
    val isTv = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

    val toggleFavorite: (String) -> Unit = { url ->
        val newFavs = favoriteUrls.toMutableSet()
        if (newFavs.contains(url)) newFavs.remove(url) else newFavs.add(url)
        favoriteUrls = newFavs
        prefs.edit().putStringSet("favorites", newFavs).apply()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBg)) {
        if (currentPlayingIndex != null && currentPlayingList.isNotEmpty()) {
            ExoPlayerView(
                playlist = currentPlayingList,
                initialIndex = currentPlayingIndex!!,
                onBack = { currentPlayingIndex = null },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            if (isTv) {
                Row(modifier = Modifier.fillMaxSize()) {
                    TvSideNav(currentTab) { currentTab = it }
                    ContentArea(
                        isLoading = isLoading,
                        currentTab = currentTab,
                        channels = channels,
                        favoriteUrls = favoriteUrls,
                        onPlay = { list, index -> 
                            currentPlayingList = list
                            currentPlayingIndex = index 
                        },
                        onToggleFav = toggleFavorite,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Scaffold(
                    containerColor = AppBg,
                    bottomBar = { MobileBottomNav(currentTab) { currentTab = it } }
                ) { padding ->
                    ContentArea(
                        isLoading = isLoading,
                        currentTab = currentTab,
                        channels = channels,
                        favoriteUrls = favoriteUrls,
                        onPlay = { list, index -> 
                            currentPlayingList = list
                            currentPlayingIndex = index 
                        },
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
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayChannels = if (currentTab == Tab.FAVORITES) {
        channels.filter { favoriteUrls.contains(it.url) }
    } else {
        channels
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Search bar dummy placeholder as seen in screenshot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(CardBg, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentTab == Tab.CHANNELS) "Search channels by name..." else "Search your favorites...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        if (currentTab == Tab.FAVORITES && !isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(CardBg, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayChannels.size} saved",
                    color = AccentYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", color = Color(0xFFF87171), fontSize = 14.sp)
                }
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(displayChannels) { index, channel ->
                        ChannelGridCard(
                            channel = channel,
                            isFavorite = favoriteUrls.contains(channel.url),
                            onPlay = { onPlay(displayChannels, index) },
                            onToggleFav = { onToggleFav(channel.url) }
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(displayChannels) { index, channel ->
                        ChannelListCard(
                            channel = channel,
                            isFavorite = favoriteUrls.contains(channel.url),
                            onPlay = { onPlay(displayChannels, index) },
                            onToggleFav = { onToggleFav(channel.url) }
                        )
                    }
                }
            }
        }
    }
}

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
fun ChannelGridCard(
    channel: Channel,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val currentBorderColor = if (isFocused) AccentYellow else Color.Transparent

    Column(
        modifier = Modifier
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(2.dp, currentBorderColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onPlay() }
            .focusable(interactionSource = interactionSource)
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.SignalCellularAlt, 
                contentDescription = null, 
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = channel.name, 
            color = Color.White, 
            fontWeight = FontWeight.Bold, 
            fontSize = 15.sp,
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TagBadge(channel.group)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF2A273F), RoundedCornerShape(8.dp))
                    .clickable { onToggleFav() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Fav",
                    tint = if (isFavorite) AccentYellow else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(AccentYellow, RoundedCornerShape(8.dp))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Text("Watch", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ChannelListCard(channel: Channel, isFavorite: Boolean, onPlay: () -> Unit, onToggleFav: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val currentBorderColor = if (isFocused) AccentYellow else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(2.dp, currentBorderColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onPlay() }
            .focusable(interactionSource = interactionSource)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(Color(0xFF2A273F), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = AccentYellow)
        }
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(text = channel.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            TagBadge(channel.group)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Fav",
                tint = if (isFavorite) AccentYellow else Color.Gray,
                modifier = Modifier.clickable { onToggleFav() }.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(80.dp)
                    .background(AccentYellow, RoundedCornerShape(20.dp))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Text("Watch", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
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
        modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = group.uppercase(), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
