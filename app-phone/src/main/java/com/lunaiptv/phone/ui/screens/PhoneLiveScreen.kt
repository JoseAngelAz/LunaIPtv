package com.lunaiptv.phone.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.lunaiptv.features.live.LiveKey
import com.lunaiptv.phone.di.PhoneLivePlayer
import com.lunaiptv.phone.di.PhoneLiveViewModel
import com.lunaiptv.phone.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhoneLiveScreen(vm: PhoneLiveViewModel, onOpenFullScreen: (() -> Unit)? = null) {
    val channels = vm.channels.collectAsLazyPagingItems()
    val railItems by vm.railItems.collectAsStateWithLifecycle()
    val selectedKey by vm.selectedKey.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val sortByName by vm.sortByName.collectAsStateWithLifecycle()
    val channelCount by vm.channelCount.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val selectedChannel by vm.selectedChannel.collectAsStateWithLifecycle()
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val isPlaying by vm.player.isPlaying.collectAsStateWithLifecycle()
    val epgMap by vm.epgMap.collectAsStateWithLifecycle()

    var contextMenuChannel by remember { mutableStateOf<com.lunaiptv.core.database.entity.ChannelEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Load EPG when visible channels change
    LaunchedEffect(channels.itemSnapshotList.items) {
        vm.loadEpgForVisible(channels.itemSnapshotList.items)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (selectedChannel != null) {
                PlayerArea(
                    vm = vm,
                    channelName = selectedChannel!!.name,
                    isPlaying = isPlaying,
                    playerState = playerState,
                    onOpenFullScreen = onOpenFullScreen,
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(railItems) { item ->
                    FilterChip(
                        selected = item.key == selectedKey,
                        onClick = { vm.selectKey(item.key) },
                        label = {
                            Text(
                                item.title,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (item.key == selectedKey) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.height(40.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = vm::setSearchQuery,
                    placeholder = { Text(stringResource(R.string.search_channels)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.sort))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_name)) },
                            onClick = { vm.setSortByName(true); showSortMenu = false },
                            leadingIcon = { if (sortByName) Icon(Icons.Filled.Check, null) else Spacer(Modifier.size(24.dp)) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_playlist)) },
                            onClick = { vm.setSortByName(false); showSortMenu = false },
                            leadingIcon = { if (!sortByName) Icon(Icons.Filled.Check, null) else Spacer(Modifier.size(24.dp)) },
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$channelCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(channels.itemCount) { index ->
                    val ch = channels[index] ?: return@items
                    val isFav = favoriteIds.contains(ch.id)
                    val isSelected = ch.id == selectedChannel?.id
                    val epg = epgMap[ch.id]
                    ListItem(
                        headlineContent = {
                            Text(
                                ch.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = {
                            if (epg != null && epg.nowTitle != null) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.now_playing_epg, epg.nowTitle ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (epg.nextTitle != null) {
                                        Text(
                                            text = stringResource(R.string.next_up_epg, epg.nextTitle ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        },
                        leadingContent = {
                            AsyncImage(
                                model = ch.logoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp),
                                    ),
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.toggleFavorite(ch) }) {
                                    Icon(
                                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = stringResource(R.string.favorite),
                                        tint = if (isFav) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(onClick = { vm.playChannel(ch) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.play),
                                        tint = if (isSelected && isPlaying) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { vm.playChannel(ch) },
                            onLongClick = { contextMenuChannel = ch },
                        ),
                    )
                }
            }
        }
    }

    contextMenuChannel?.let { ch ->
        val isFav = favoriteIds.contains(ch.id)
        ContextMenuSheet(
            title = ch.name,
            items = listOf(
                ContextMenuItem(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.play_action),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { vm.playChannel(ch) },
                ),
                ContextMenuItem(
                    icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (isFav) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                    tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    onClick = { vm.toggleFavorite(ch) },
                ),
                ContextMenuItem(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.channel_info),
                    onClick = { /* Could show EPG detail */ },
                ),
            ),
            onDismiss = { contextMenuChannel = null },
        )
    }
}

@Composable
private fun PlayerArea(
    vm: PhoneLiveViewModel,
    channelName: String,
    isPlaying: Boolean,
    playerState: PhoneLivePlayer.State,
    onOpenFullScreen: (() -> Unit)? = null,
) {
    var showControls by remember { mutableStateOf(false) }
    val autoHideJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Video surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val player = vm.player

            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                player.setSurface(holder.surface)
                            }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                                player.setSurface(holder.surface)
                            }
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                player.setSurface(null)
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Tap overlay: shows controls on tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        showControls = !showControls
                        autoHideJob.value?.cancel()
                        if (showControls) {
                            autoHideJob.value = scope.launch {
                                kotlinx.coroutines.delay(4000)
                                showControls = false
                            }
                        }
                    },
            )

            if (playerState == PhoneLivePlayer.State.LOADING) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(24.dp),
                    strokeWidth = 2.5.dp,
                )
            }

            if (playerState == PhoneLivePlayer.State.ERROR) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { vm.player.retry() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.Red,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.retry),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            // Controls overlay
            if (showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                ) {
                    // Top: play/pause
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = {
                            if (isPlaying) vm.player.pause() else vm.player.resume()
                        }) {
                            if (isPlaying) {
                                Text(
                                    "\u23F8",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            } else {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.play),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        IconButton(onClick = { vm.player.toggleMute() }) {
                            val isMuted by vm.player.isMuted.collectAsStateWithLifecycle()
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    // Bottom: fullscreen button + channel name
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            channelName,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (onOpenFullScreen != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable { onOpenFullScreen() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "\u25A1",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Channel name bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                channelName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
