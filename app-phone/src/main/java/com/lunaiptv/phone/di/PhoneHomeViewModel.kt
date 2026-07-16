@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lunaiptv.phone.di

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lunaiptv.core.database.dao.ChannelDao
import com.lunaiptv.core.database.dao.FavoriteDao
import com.lunaiptv.core.database.dao.HistoryDao
import com.lunaiptv.core.database.dao.MovieDao
import com.lunaiptv.core.database.dao.ProgressDao
import com.lunaiptv.core.database.dao.SeriesDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.ChannelEntity
import com.lunaiptv.core.database.entity.MovieEntity
import com.lunaiptv.core.database.entity.SeriesEntity
import com.lunaiptv.core.launcher.LauncherContinuationItem
import com.lunaiptv.core.launcher.LauncherContinuationKind
import com.lunaiptv.core.launcher.LauncherRecommendationPlanner
import com.lunaiptv.core.metadata.MetadataRepository
import com.lunaiptv.core.repository.activeProfileSources
import com.lunaiptv.core.weather.WeatherRepository
import com.lunaiptv.features.settings.data.SettingsRepository

data class HeroMeta(
    val backdropUrl: String? = null,
    val logoUrl: String? = null,
    val plot: String? = null,
)

data class PhoneHomeState(
    val isLoading: Boolean = true,
    val heroItems: List<LauncherContinuationItem> = emptyList(),
    val heroMeta: Map<String, HeroMeta> = emptyMap(),
    val continueMovies: List<LauncherContinuationItem> = emptyList(),
    val continueSeries: List<LauncherContinuationItem> = emptyList(),
    val recentChannels: List<ChannelEntity> = emptyList(),
    val favoriteChannels: List<ChannelEntity> = emptyList(),
    val favoriteMovies: List<MovieEntity> = emptyList(),
    val favoriteSeries: List<SeriesEntity> = emptyList(),
    val hasAnyContent: Boolean = false,
    val profileName: String = "",
    val weatherText: String? = null,
)

class PhoneHomeViewModel(
    private val planner: LauncherRecommendationPlanner,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val progressDao: ProgressDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val metadata: MetadataRepository,
    private val weatherRepo: WeatherRepository,
    private val profileDao: com.lunaiptv.core.database.dao.ProfileDao,
) : ViewModel() {

    private data class Ctx(val profileId: Long, val sourceIds: List<Long>)

    private val ctx: StateFlow<Ctx> = activeProfileSources(settings, sourceDao)
        .map { aps -> Ctx(aps.profileId, aps.sourceIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ctx(-1L, emptyList()))

    private val _uiState = MutableStateFlow(PhoneHomeState())
    val uiState: StateFlow<PhoneHomeState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ctx.collect { c ->
                if (c.profileId < 0 || c.sourceIds.isEmpty()) {
                    _uiState.value = PhoneHomeState(isLoading = false)
                    return@collect
                }
                loadHome(c.profileId)
            }
        }
    }

    private suspend fun loadHome(profileId: Long) {
        _uiState.value = PhoneHomeState(isLoading = true)
        try {
            val profileName = profileDao.getById(profileId)?.name ?: ""
            val continuations = planner.buildContinuationItems(profileId)
            val movies = continuations.filter { it.kind == LauncherContinuationKind.MOVIE }
            val episodes = continuations.filter { it.kind == LauncherContinuationKind.EPISODE }
            val hero = continuations.take(5)
            val recent = try { channelDao.recentlyWatched(profileId, 20).first() } catch (_: Exception) { emptyList() }
            val favs = try { channelDao.favoritesListAlpha(profileId).first() } catch (_: Exception) { emptyList() }
            val favMovieIds = try { favoriteDao.observeFavoriteIds(profileId, com.lunaiptv.core.model.MediaType.MOVIE).first() } catch (_: Exception) { emptyList() }
            val favSeriesIds = try { favoriteDao.observeFavoriteIds(profileId, com.lunaiptv.core.model.MediaType.SERIES).first() } catch (_: Exception) { emptyList() }
            val favMovies = try { if (favMovieIds.isNotEmpty()) movieDao.getByIds(favMovieIds) else emptyList() } catch (_: Exception) { emptyList() }
            val favSeries = try { if (favSeriesIds.isNotEmpty()) seriesDao.getByIds(favSeriesIds) else emptyList() } catch (_: Exception) { emptyList() }

            _uiState.value = PhoneHomeState(
                isLoading = false,
                heroItems = hero,
                continueMovies = movies,
                continueSeries = episodes,
                recentChannels = recent,
                favoriteChannels = favs,
                favoriteMovies = favMovies,
                favoriteSeries = favSeries,
                hasAnyContent = continuations.isNotEmpty() || recent.isNotEmpty() || favs.isNotEmpty() || favMovies.isNotEmpty() || favSeries.isNotEmpty(),
                profileName = profileName,
            )

            // Resolve TMDB metadata for hero items (background)
            resolveHeroMeta(hero)

            // Fetch weather in background
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val info = weatherRepo.get() ?: return@launch
                    _uiState.value = _uiState.value.copy(
                        weatherText = "${info.temperatureC.toInt()}\u00B0C ${info.city}"
                    )
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadHome failed for profile=$profileId", e)
            _uiState.value = PhoneHomeState(isLoading = false)
        }
    }

    private suspend fun resolveHeroMeta(items: List<LauncherContinuationItem>) {
        val meta = mutableMapOf<String, HeroMeta>()
        for (item in items) {
            val key = item.stableKey
            try {
                val m = when (item.kind) {
                    LauncherContinuationKind.MOVIE -> {
                        val movie = movieDao.getById(item.targetItemId) ?: continue
                        metadata.resolveMovie(movie)
                    }
                    LauncherContinuationKind.EPISODE -> {
                        val ep = seriesDao.getEpisodeById(item.targetItemId) ?: continue
                        val show = seriesDao.getSeriesById(ep.seriesId) ?: continue
                        metadata.resolveEpisode(show, ep)
                    }
                    LauncherContinuationKind.LIVE -> null
                }
                if (m != null) {
                    val imgBase = "https://image.tmdb.org/t/p/"
                    meta[key] = HeroMeta(
                        backdropUrl = m.backdropPath?.let { "${imgBase}w780$it" },
                        logoUrl = m.logoPath?.let { "${imgBase}w500$it" },
                        plot = m.overview,
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "resolveHeroMeta failed for $key", e)
            }
        }
        if (meta.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(heroMeta = meta)
        }
    }

    fun refresh() {
        val c = ctx.value
        if (c.profileId > 0) viewModelScope.launch { loadHome(c.profileId) }
    }

    suspend fun getMovieById(id: Long): MovieEntity? = movieDao.getById(id)

    suspend fun getSeriesForEpisode(episodeId: Long): SeriesEntity? {
        val episode = seriesDao.getEpisodeById(episodeId) ?: return null
        return seriesDao.getSeriesById(episode.seriesId)
    }

    fun getProfileId(): Long = ctx.value.profileId

    private companion object { const val TAG = "PhoneHomeVM" }
}
