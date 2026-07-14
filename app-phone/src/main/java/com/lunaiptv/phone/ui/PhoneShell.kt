package com.lunaiptv.phone.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.lunaiptv.phone.di.PhoneHomeViewModel
import com.lunaiptv.phone.di.PhoneLiveViewModel
import com.lunaiptv.phone.di.PhoneMoviesViewModel
import com.lunaiptv.phone.di.PhoneProfileViewModel
import com.lunaiptv.phone.di.PhoneSearchViewModel
import com.lunaiptv.phone.di.PhoneSeriesViewModel
import com.lunaiptv.phone.di.PhoneSettingsViewModel
import com.lunaiptv.phone.ui.screens.PhoneHomeScreen
import com.lunaiptv.phone.ui.screens.PhoneLiveScreen
import com.lunaiptv.phone.ui.screens.PhoneMovieDetailScreen
import com.lunaiptv.phone.ui.screens.PhoneMoviesScreen
import com.lunaiptv.phone.ui.screens.PhoneProfileScreen
import com.lunaiptv.phone.ui.screens.PhoneSearchScreen
import com.lunaiptv.phone.ui.screens.PhoneSeriesDetailScreen
import com.lunaiptv.phone.ui.screens.PhoneSeriesScreen
import com.lunaiptv.phone.ui.screens.PhoneSettingsScreen

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
    val scope = rememberCoroutineScope()

    // Detail overlays (for Movies/Series detail navigation)
    var showMovieDetail by remember { mutableStateOf<com.lunaiptv.core.database.entity.MovieEntity?>(null) }
    var showSeriesDetail by remember { mutableStateOf<com.lunaiptv.core.database.entity.SeriesEntity?>(null) }

    fun navigateToTab(route: String) {
        showMovieDetail = null
        showSeriesDetail = null
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar && showMovieDetail == null && showSeriesDetail == null) {
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
        when {
            showMovieDetail != null -> {
                PhoneMovieDetailScreen(
                    movie = showMovieDetail!!,
                    vm = moviesVm,
                    onBack = {
                        moviesVm.saveProgress(showMovieDetail!!.id)
                        showMovieDetail = null
                    },
                )
            }
            showSeriesDetail != null -> {
                PhoneSeriesDetailScreen(
                    series = showSeriesDetail!!,
                    vm = seriesVm,
                    onBack = { showSeriesDetail = null },
                    onPlayEpisode = { /* Episode plays via vm.playEpisode in detail screen */ },
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
                                    if (movie != null) showMovieDetail = movie
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
                        )
                    }
                    composable(PhoneScreen.Live.route) {
                        PhoneLiveScreen(vm = liveVm)
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
                }
            }
        }
    }
}
