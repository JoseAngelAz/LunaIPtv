package com.lunaiptv.phone.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.lunaiptv.phone.R
import com.lunaiptv.phone.di.PhoneSeriesViewModel
import com.lunaiptv.core.database.entity.SeriesEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhoneSeriesScreen(
    vm: PhoneSeriesViewModel,
    onSeriesClick: (SeriesEntity) -> Unit,
) {
    val railItems by vm.railItems.collectAsState()
    val selectedKey by vm.selectedKey.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val sortByName by vm.sortByName.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val seriesList = vm.series.collectAsLazyPagingItems()
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var contextMenuSeries by remember { mutableStateOf<SeriesEntity?>(null) }

    val savedFirstVisible = vm.firstVisibleIndex
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = savedFirstVisible,
    )

    DisposableEffect(Unit) {
        onDispose {
            vm.firstVisibleIndex = gridState.firstVisibleItemIndex
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_series)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(railItems.size) { idx ->
                    val item = railItems[idx]
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
            IconButton(onClick = {
                showSearch = !showSearch
                if (!showSearch) vm.setSearchQuery("")
            }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(seriesList.itemCount) { idx ->
                val s = seriesList[idx] ?: return@items
                SeriesPosterCard(
                    series = s,
                    isFavorited = s.id in favoriteIds,
                    onFavoriteToggle = { vm.toggleFavorite(s) },
                    onClick = { onSeriesClick(s) },
                    onLongClick = { contextMenuSeries = s },
                )
            }
        }
    }

    contextMenuSeries?.let { s ->
        val isFav = s.id in favoriteIds
        ContextMenuSheet(
            title = s.name,
            items = listOf(
                ContextMenuItem(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.view_details),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onSeriesClick(s) },
                ),
                ContextMenuItem(
                    icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (isFav) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                    tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    onClick = { vm.toggleFavorite(s) },
                ),
            ),
            onDismiss = { contextMenuSeries = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesPosterCard(
    series: SeriesEntity,
    isFavorited: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.67f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (series.posterUrl != null) {
                AsyncImage(
                    model = series.posterUrl,
                    contentDescription = series.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("📺", style = MaterialTheme.typography.headlineLarge)
                }
            }
            series.rating?.let { r ->
                Text(
                    text = "★ %.1f".format(r),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
            Icon(
                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.favorite),
                tint = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clickable(onClick = onFavoriteToggle),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        )
                    )
                    .padding(6.dp),
            ) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
