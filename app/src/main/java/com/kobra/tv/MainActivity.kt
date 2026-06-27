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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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

// ৫টি ট্যাব অপশন
enum class Tab { KOBRA, CHANNELS, DISCORD, FAVORITES, MORE }

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
    var searchQuery by remember { mutableStateOf("") }
    
    val prefs = context.getSharedPreferences("kobra_prefs", Context.MODE_PRIVATE)
    var favoriteUrls by remember { 
        mutableStateOf(prefs.getStringSet("favorites", emptySet()) ?: emptySet()) 
    }

    val m3uUrl = "https://raw.githubusercontent.com/piyashltd/all-in-one/refs/heads/main/channels/index.m3u"

    LaunchedEffect(Unit) {
        scope.launch {
            channels = M3uParser.fetchChannels(m3uUrl)
            isLoading = false
        }
    }

    LaunchedEffect(currentTab) {
        searchQuery = ""
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

    val clearAllFavorites: () -> Unit = {
        favoriteUrls = emptySet()
        prefs.edit().putStringSet("favorites", emptySet()).apply()
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
                    MainContentArea(
                        isLoading = isLoading,
                        currentTab = currentTab,
                        channels = channels,
                        favoriteUrls = favoriteUrls,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onPlay = { list, index -> 
                            currentPlayingList = list
                            currentPlayingIndex = index 
                        },
                        onToggleFav = toggleFavorite,
                        onClearAllFavs = clearAllFavorites,
                        onRefresh = { 
                            isLoading = true
                            scope.launch { channels = M3uParser.fetchChannels(m3uUrl); isLoading = false }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Scaffold(
                    containerColor = AppBg,
                    bottomBar = { MobileBottomNav(currentTab) { currentTab = it } }
                ) { padding ->
                    MainContentArea(
                        isLoading = isLoading,
                        currentTab = currentTab,
                        channels = channels,
                        favoriteUrls = favoriteUrls,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onPlay = { list, index -> 
                            currentPlayingList = list
                            currentPlayingIndex = index 
                        },
                        onToggleFav = toggleFavorite,
                        onClearAllFavs = clearAllFavorites,
                        onRefresh = { 
                            isLoading = true
                            scope.launch { channels = M3uParser.fetchChannels(m3uUrl); isLoading = false }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainContentArea(
    isLoading: Boolean,
    currentTab: Tab,
    channels: List<Channel>,
    favoriteUrls: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFav: (String) -> Unit,
    onClearAllFavs: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        
        // ** ওপরের কাস্টম হেডার বার (কোনো KOBRA CHANNELS টেক্সট নেই) **
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GppGood, 
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SV FIFA",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // ডি-প্যাড অপ্টিমাইজড রিফ্রেশ বাটন
            val refreshInteraction = remember { MutableInteractionSource() }
            val isRefreshFocused by refreshInteraction.collectIsFocusedAsState()
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = if (isRefreshFocused) AccentYellow else Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .focusable(interactionSource = refreshInteraction)
                    .clickable(interactionSource = refreshInteraction, indication = null) { onRefresh() }
            )
        }

        if (currentTab != Tab.CHANNELS && currentTab != Tab.FAVORITES) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("${currentTab.name} Section Coming Soon...", color = Color.Gray)
            }
            return@Column
        }

        // ** ডি-প্যাড অপ্টিমাইজড সার্চ বার **
        val searchInteractionSource = remember { MutableInteractionSource() }
        val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()
        val searchBorderColor = if (isSearchFocused) AccentYellow else Color.Transparent

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(CardBg, RoundedCornerShape(8.dp))
                .border(2.dp, searchBorderColor, RoundedCornerShape(8.dp))
                .focusable(interactionSource = searchInteractionSource)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (currentTab == Tab.CHANNELS) "Search channels by name..." else "Search your favorites...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(AccentYellow),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear Search",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }
        }

        val tabChannels = if (currentTab == Tab.FAVORITES) {
            channels.filter { favoriteUrls.contains(it.url) }
        } else {
            channels
        }

        val displayChannels = if (searchQuery.isEmpty()) {
            tabChannels
        } else {
            tabChannels.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        // ** Clear All অপশন বার **
        if (currentTab == Tab.FAVORITES && !isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(CardBg, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tabChannels.size} saved",
                    color = AccentYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (tabChannels.isNotEmpty()) {
                    val clearFavInteraction = remember { MutableInteractionSource() }
                    val isClearFavFocused by clearFavInteraction.collectIsFocusedAsState()
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(1.dp, if(isClearFavFocused) AccentYellow else Color.Transparent, RoundedCornerShape(4.dp))
                            .focusable(interactionSource = clearFavInteraction)
                            .clickable(interactionSource = clearFavInteraction, indication = null) { onClearAllFavs() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Clear All", 
                            tint = Color(0xFFF87171), 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentYellow)
            }
        } else if (displayChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No channels found.", color = Color.Gray)
            }
        } else {
            if (currentTab == Tab.CHANNELS) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
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

// ** ৫টি অপশন বিশিষ্ট বটম নেভিগেশন বার (মোবাইল) **
@Composable
fun MobileBottomNav(currentTab: Tab, onTabSelected: (Tab) -> Unit) {
    NavigationBar(containerColor = Color(0xFF13111C), contentColor = Color.Gray) {
        val tabs = Tab.values()
        tabs.forEach { tab ->
            val icon = when(tab) {
                Tab.KOBRA -> Icons.Default.Tune
                Tab.CHANNELS -> Icons.Default.FeaturedPlayList
                Tab.DISCORD -> Icons.Default.Shield
                Tab.FAVORITES -> Icons.Default.Favorite
                Tab.MORE -> Icons.Default.MoreHoriz
            }
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = tab.name) },
                label = { Text(tab.name, fontSize = 10.sp) },
                colors = navigationBarColors()
            )
        }
    }
}

// ** ৫টি অপশন বিশিষ্ট সাইড নেভিগেশন বার (অ্যান্ড্রয়েড টিভি রিমোট ফ্রেন্ডলি) **
@Composable
fun TvSideNav(currentTab: Tab, onTabSelected: (Tab) -> Unit) {
    NavigationRail(
        containerColor = Color(0xFF13111C),
        modifier = Modifier.fillMaxHeight().width(80.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        val tabs = Tab.values()
        tabs.forEach { tab ->
            val icon = when(tab) {
                Tab.KOBRA -> Icons.Default.Tune
                Tab.CHANNELS -> Icons.Default.FeaturedPlayList
                Tab.DISCORD -> Icons.Default.Shield
                Tab.FAVORITES -> Icons.Default.Favorite
                Tab.MORE -> Icons.Default.MoreHoriz
            }
            
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            NavigationRailItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = tab.name) },
                modifier = Modifier
                    .focusable(interactionSource = interactionSource)
                    .border(2.dp, if(isFocused) AccentYellow else Color.Transparent, CircleShape),
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = AccentYellow,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color(0xFF2A273F)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun navigationBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AccentYellow,
    selectedTextColor = AccentYellow,
    unselectedIconColor = Color.Gray,
    unselectedTextColor = Color.Gray,
    indicatorColor = Color.Transparent
)

// ** গ্রিড কার্ড (টিভি রিমোট অপ্টিমাইজড) **
@Composable
fun ChannelGridCard(channel: Channel, isFavorite: Boolean, onPlay: () -> Unit, onToggleFav: () -> Unit) {
    Column(
        modifier = Modifier
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.SignalCellularAlt, 
                contentDescription = null, 
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = channel.name, 
            color = Color.White, 
            fontWeight = FontWeight.Bold, 
            fontSize = 15.sp,
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        TagBadge(channel.group)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val favInteraction = remember { MutableInteractionSource() }
            val isFavFocused by favInteraction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2A273F), RoundedCornerShape(8.dp))
                    .border(2.dp, if(isFavFocused) AccentYellow else Color.Transparent, RoundedCornerShape(8.dp))
                    .focusable(interactionSource = favInteraction)
                    .clickable(interactionSource = favInteraction, indication = null) { onToggleFav() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Fav",
                    tint = if (isFavorite) AccentYellow else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            val watchInteraction = remember { MutableInteractionSource() }
            val isWatchFocused by watchInteraction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if(isWatchFocused) Color.White else AccentYellow, RoundedCornerShape(8.dp))
                    .border(2.dp, if(isWatchFocused) AccentYellow else Color.Transparent, RoundedCornerShape(8.dp))
                    .focusable(interactionSource = watchInteraction)
                    .clickable(interactionSource = watchInteraction, indication = null) { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Watch", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ** ফেভারিট লিস্ট কার্ড (টিভি রিমোট অপ্টিমাইজড) **
@Composable
fun ChannelListCard(channel: Channel, isFavorite: Boolean, onPlay: () -> Unit, onToggleFav: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFF2A273F), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(22.dp))
        }
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                text = channel.name, 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 15.sp, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            TagBadge(channel.group)
        }
        
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.height(65.dp)
        ) {
            val listFavInteraction = remember { MutableInteractionSource() }
            val isListFavFocused by listFavInteraction.collectIsFocusedAsState()
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Fav",
                tint = AccentYellow,
                modifier = Modifier
                    .size(20.dp)
                    .focusable(interactionSource = listFavInteraction)
                    .border(1.dp, if(isListFavFocused) AccentYellow else Color.Transparent, CircleShape)
                    .clickable(interactionSource = listFavInteraction, indication = null) { onToggleFav() }
            )
            
            val listWatchInteraction = remember { MutableInteractionSource() }
            val isListWatchFocused by listWatchInteraction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(75.dp)
                    .background(if(isListWatchFocused) Color.White else AccentYellow, RoundedCornerShape(16.dp))
                    .border(2.dp, if(isListWatchFocused) AccentYellow else Color.Transparent, RoundedCornerShape(16.dp))
                    .focusable(interactionSource = listWatchInteraction)
                    .clickable(interactionSource = listWatchInteraction, indication = null) { onPlay() },
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
        modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text = group.uppercase(), color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
