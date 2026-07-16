package com.lunaiptv.phone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunaiptv.phone.di.PhoneDownloadsViewModel
import com.lunaiptv.phone.di.PhoneBackupViewModel
import com.lunaiptv.phone.di.PhoneEPGSourcesViewModel
import com.lunaiptv.phone.di.PhoneHomeViewModel
import com.lunaiptv.phone.di.PhoneLiveViewModel
import com.lunaiptv.phone.di.PhoneMoviesViewModel
import com.lunaiptv.phone.di.PhoneProfileViewModel
import com.lunaiptv.phone.di.PhoneSearchViewModel
import com.lunaiptv.phone.di.PhoneSeriesViewModel
import com.lunaiptv.phone.di.PhoneSettingsViewModel
import com.lunaiptv.phone.di.PhoneSourceViewModel
import com.lunaiptv.phone.R
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.phone.ui.screens.PhoneBackupScreen
import com.lunaiptv.phone.ui.screens.PhoneDownloadsScreen
import com.lunaiptv.phone.ui.screens.PhoneEPGSourcesScreen
import com.lunaiptv.phone.ui.screens.PhoneHomeScreen
import com.lunaiptv.phone.ui.screens.PhoneLiveScreen
import com.lunaiptv.phone.ui.screens.PhoneMovieDetailScreen
import com.lunaiptv.phone.ui.screens.PhoneMoviesScreen
import com.lunaiptv.phone.ui.screens.PhoneNetworkSettingsScreen
import com.lunaiptv.phone.ui.screens.PhoneProfileScreen
import com.lunaiptv.phone.ui.screens.PhoneSearchScreen
import com.lunaiptv.phone.ui.screens.PhoneSeriesDetailScreen
import com.lunaiptv.phone.ui.screens.PhoneSeriesScreen
import com.lunaiptv.phone.ui.screens.PhoneSettingsScreen
import com.lunaiptv.phone.ui.screens.PhoneManageSourcesScreen
import com.lunaiptv.phone.ui.screens.PhoneAddSourceScreen
import com.lunaiptv.phone.ui.screens.PhonePlayerScreen
import com.lunaiptv.phone.ui.screens.PhoneVideoPlayerSettingsScreen

sealed class PhoneScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : PhoneScreen("home", "Home", Icons.Filled.Home)
    data object Live : PhoneScreen("live", "Live", Icons.Filled.PlayArrow)
    data object Movies : PhoneScreen("movies", "Movies", Icons.Filled.Star)
    data object Series : PhoneScreen("series", "Series", Icons.AutoMirrored.Filled.List)
    data object Search : PhoneScreen("search", "Search", Icons.Filled.Search)
    data object Settings : PhoneScreen("settings", "Settings", Icons.Filled.Settings)
}

private val bottomScreens = listOf(
    PhoneScreen.Home,
    PhoneScreen.Live,
    PhoneScreen.Movies,
    PhoneScreen.Series,
    PhoneScreen.Search,
    PhoneScreen.Settings,
)

data class PlayerNav(
    val title: String,
    val url: String,
    val isLive: Boolean = false,
    val userAgent: String? = null,
    val movieId: Long? = null,
    val episodeId: Long? = null,
)

@Composable
fun PhoneShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomScreens.map { it.route }

    // Shared ViewModels scoped to composition
    val homeVm: PhoneHomeViewModel = koinViewModel()
    val liveVm: PhoneLiveViewModel = koinViewModel()
    val moviesVm: PhoneMoviesViewModel = koinViewModel()
    val seriesVm: PhoneSeriesViewModel = koinViewModel()
    val searchVm: PhoneSearchViewModel = koinViewModel()
    val settingsVm: PhoneSettingsViewModel = koinViewModel()
    val profileVm: PhoneProfileViewModel = koinViewModel()
    val sourceVm: PhoneSourceViewModel = koinViewModel()
    val epgVm: PhoneEPGSourcesViewModel = koinViewModel()
    val backupVm: PhoneBackupViewModel = koinViewModel()
    val downloadsVm: PhoneDownloadsViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val downloads by downloadsVm.downloads.collectAsStateWithLifecycle()

    // Detail overlays (for Movies/Series detail navigation)
    var showMovieDetail by remember { mutableStateOf<com.lunaiptv.core.database.entity.MovieEntity?>(null) }
    var showSeriesDetail by remember { mutableStateOf<com.lunaiptv.core.database.entity.SeriesEntity?>(null) }
    var showPlayer by remember { mutableStateOf<PlayerNav?>(null) }

    fun enqueueDownload(title: String, profileId: Long, mediaType: com.lunaiptv.core.model.MediaType, itemId: Long, posterUrl: String?, streamUrl: String, relativeDir: String, fileName: String) {
        downloadsVm.enqueue(profileId, mediaType, itemId, title, posterUrl, streamUrl, relativeDir, fileName)
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.download_started, title)) }
    }

    fun dismissPlayer() {
        showPlayer?.let { nav ->
            nav.movieId?.let { moviesVm.saveProgress(it) }
            nav.episodeId?.let { seriesVm.saveEpisodeProgress(it) }
        }
        showPlayer = null
    }

    fun navigateToTab(route: String) {
        showMovieDetail = null
        showSeriesDetail = null
        dismissPlayer()
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar && showMovieDetail == null && showSeriesDetail == null && showPlayer == null) {
                NavigationBar {
                    bottomScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = { navigateToTab(screen.route) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Back handlers for overlays
        BackHandler(enabled = showPlayer != null) {
            dismissPlayer()
        }
        BackHandler(enabled = showMovieDetail != null) {
            moviesVm.saveProgress(showMovieDetail!!.id)
            showMovieDetail = null
        }
        BackHandler(enabled = showSeriesDetail != null) {
            showSeriesDetail = null
        }

        when {
            showPlayer != null -> {
                PhonePlayerScreen(
                    player = liveVm.player,
                    title = showPlayer!!.title,
                    url = showPlayer!!.url,
                    isLive = showPlayer!!.isLive,
                    userAgent = showPlayer!!.userAgent,
                    onBack = { dismissPlayer() },
                )
            }
            showMovieDetail != null -> {
                val movieDownloadState = downloads.find { it.itemId == showMovieDetail!!.id && it.mediaType == MediaType.MOVIE }
                PhoneMovieDetailScreen(
                    movie = showMovieDetail!!,
                    vm = moviesVm,
                    onBack = {
                        moviesVm.saveProgress(showMovieDetail!!.id)
                        showMovieDetail = null
                    },
                    onPlay = { movie ->
                        moviesVm.playMovie(movie)
                        showPlayer = PlayerNav(movie.name, movie.streamUrl, movieId = movie.id)
                    },
                    onDownload = { movie ->
                        scope.launch {
                            val pid = homeVm.getProfileId()
                            if (pid > 0) {
                                enqueueDownload(
                                    title = movie.name,
                                    profileId = pid,
                                    mediaType = com.lunaiptv.core.model.MediaType.MOVIE,
                                    itemId = movie.id,
                                    posterUrl = movie.posterUrl,
                                    streamUrl = movie.streamUrl,
                                    relativeDir = "Movies",
                                    fileName = "${com.lunaiptv.core.storage.StorageAccess.sanitize(movie.name)}.mp4",
                                )
                            }
                        }
                    },
                    downloadStatus = movieDownloadState?.status,
                )
            }
            showSeriesDetail != null -> {
                val seriesEpisodeDownloads = downloads.filter { it.mediaType == com.lunaiptv.core.model.MediaType.EPISODE }
                PhoneSeriesDetailScreen(
                    series = showSeriesDetail!!,
                    vm = seriesVm,
                    onBack = { showSeriesDetail = null },
                    onPlayEpisode = { ep ->
                        showPlayer = PlayerNav(ep.name, ep.streamUrl, episodeId = ep.id)
                    },
                    onDownloadEpisode = { ep ->
                        scope.launch {
                            val pid = homeVm.getProfileId()
                            if (pid > 0) {
                                enqueueDownload(
                                    title = ep.name,
                                    profileId = pid,
                                    mediaType = com.lunaiptv.core.model.MediaType.EPISODE,
                                    itemId = ep.id,
                                    posterUrl = showSeriesDetail!!.posterUrl,
                                    streamUrl = ep.streamUrl,
                                    relativeDir = "Series/${com.lunaiptv.core.storage.StorageAccess.sanitize(showSeriesDetail!!.name)}/Season ${ep.seasonNumber}",
                                    fileName = "S${ep.seasonNumber}E${ep.episodeNumber}.mp4",
                                )
                            }
                        }
                    },
                    episodeDownloads = seriesEpisodeDownloads,
                )
            }
            else -> {
                NavHost(
                    navController = navController,
                    startDestination = PhoneScreen.Home.route,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable(PhoneScreen.Home.route) {
                        PhoneHomeScreen(
                            vm = homeVm,
                            onPlayContinueMovie = { item ->
                                scope.launch {
                                    val movie = homeVm.getMovieById(item.targetItemId)
                                    if (movie != null) {
                                        moviesVm.playMovie(movie, item.positionMs)
                                        showPlayer = PlayerNav(movie.name, movie.streamUrl, movieId = movie.id)
                                    }
                                }
                            },
                            onPlayContinueSeries = { item ->
                                scope.launch {
                                    val series = homeVm.getSeriesForEpisode(item.targetItemId)
                                    if (series != null) showSeriesDetail = series
                                }
                            },
                            onPlayChannel = { ch ->
                                liveVm.playChannel(ch)
                                navigateToTab(PhoneScreen.Live.route)
                            },
                            onFavMovie = { movie -> showMovieDetail = movie },
                            onFavSeries = { series -> showSeriesDetail = series },
                        )
                    }
                    composable(PhoneScreen.Live.route) {
                        PhoneLiveScreen(
                            vm = liveVm,
                            onOpenFullScreen = {
                                val ch = liveVm.selectedChannel.value
                                if (ch != null) {
                                    showPlayer = PlayerNav(ch.name, ch.streamUrl, isLive = true)
                                }
                            },
                        )
                    }
                    composable(PhoneScreen.Movies.route) {
                        PhoneMoviesScreen(
                            vm = moviesVm,
                            onMovieClick = { movie -> showMovieDetail = movie },
                        )
                    }
                    composable(PhoneScreen.Series.route) {
                        PhoneSeriesScreen(
                            vm = seriesVm,
                            onSeriesClick = { s -> showSeriesDetail = s },
                        )
                    }
                    composable(PhoneScreen.Search.route) {
                        PhoneSearchScreen(
                            vm = searchVm,
                            onPlayChannel = { ch ->
                                liveVm.playChannel(ch)
                                navigateToTab(PhoneScreen.Live.route)
                            },
                            onPlayMovie = { m -> showMovieDetail = m },
                            onOpenSeries = { s -> showSeriesDetail = s },
                        )
                    }
                    composable(PhoneScreen.Settings.route) {
                        PhoneSettingsScreen(
                            vm = settingsVm,
                            onBack = { navController.popBackStack() },
                            onProfiles = { navController.navigate("profiles") },
                            onManageSources = { navController.navigate("manage-sources") },
                            onEpgSources = { navController.navigate("epg-sources") },
                            onNetworkSettings = { navController.navigate("network-settings") },
                            onVideoPlayerSettings = { navController.navigate("video-player-settings") },
                            onBackup = { navController.navigate("backup") },
                            onDownloads = { navController.navigate("downloads") },
                        )
                    }
                    composable("profiles") {
                        PhoneProfileScreen(
                            vm = profileVm,
                            onProfileSelected = {
                                navController.popBackStack()
                                navigateToTab(PhoneScreen.Home.route)
                            },
                        )
                    }
                    composable("manage-sources") {
                        PhoneManageSourcesScreen(
                            vm = sourceVm,
                            onBack = { navController.popBackStack() },
                            onAddSource = { navController.navigate("add-source") },
                            onEditSource = { source -> navController.navigate("edit-source/${source.id}") },
                        )
                    }
                    composable("add-source") {
                        PhoneAddSourceScreen(
                            vm = sourceVm,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("edit-source/{sourceId}") { backStackEntry ->
                        val sourceId = backStackEntry.arguments?.getString("sourceId")?.toLongOrNull()
                        val sources by sourceVm.sources.collectAsStateWithLifecycle()
                        val source = sources.find { it.id == sourceId }
                        val playlistAutoRefresh by sourceVm.playlistAutoRefresh.collectAsStateWithLifecycle()
                        val defaultId by sourceVm.defaultSourceId.collectAsStateWithLifecycle()
                        if (source != null) {
                            PhoneAddSourceScreen(
                                vm = sourceVm,
                                onBack = { navController.popBackStack() },
                                editing = source,
                                initialAutoRefresh = playlistAutoRefresh[source.id] ?: com.lunaiptv.features.settings.data.PlaylistAutoRefresh.OFF,
                                initialIsDefault = source.id == defaultId,
                                showDefaultToggle = sources.size > 1,
                            )
                        }
                    }
                    composable("epg-sources") {
                        PhoneEPGSourcesScreen(
                            vm = epgVm,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("network-settings") {
                        PhoneNetworkSettingsScreen(
                            vm = settingsVm,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("video-player-settings") {
                        PhoneVideoPlayerSettingsScreen(
                            vm = settingsVm,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("backup") {
                        PhoneBackupScreen(
                            vm = backupVm,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("downloads") {
                        PhoneDownloadsScreen(
                            vm = downloadsVm,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
