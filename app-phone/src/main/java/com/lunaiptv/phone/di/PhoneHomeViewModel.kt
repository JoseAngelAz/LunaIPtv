@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lunaiptv.phone.di

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lunaiptv.core.database.dao.ChannelDao
import com.lunaiptv.core.database.dao.FavoriteDao
import com.lunaiptv.core.database.dao.HistoryDao
import com.lunaiptv.core.database.dao.MovieDao
import com.lunaiptv.core.database.dao.ProgressDao
import com.lunaiptv.core.database.dao.SeriesDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.ChannelEntity
import com.lunaiptv.core.launcher.LauncherContinuationItem
import com.lunaiptv.core.launcher.LauncherRecommendationPlanner
import com.lunaiptv.core.repository.activeProfileSources
import com.lunaiptv.features.settings.data.SettingsRepository

data class PhoneHomeState(
    val isLoading: Boolean = true,
    val heroItems: List<LauncherContinuationItem> = emptyList(),
    val continueMovies: List<LauncherContinuationItem> = emptyList(),
    val continueSeries: List<LauncherContinuationItem> = emptyList(),
    val recentChannels: List<ChannelEntity> = emptyList(),
    val favoriteChannels: List<ChannelEntity> = emptyList(),
    val hasAnyContent: Boolean = false,
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
            val continuations = planner.buildContinuationItems(profileId)
            val movies = continuations.filter { it.kind == com.lunaiptv.core.launcher.LauncherContinuationKind.MOVIE }
            val episodes = continuations.filter { it.kind == com.lunaiptv.core.launcher.LauncherContinuationKind.EPISODE }
            val hero = continuations.take(5)
            val recent = try { channelDao.recentlyWatched(profileId, 20).first() } catch (_: Exception) { emptyList() }
            val favs = try { channelDao.favoritesListAlpha(profileId).first() } catch (_: Exception) { emptyList() }

            _uiState.value = PhoneHomeState(
                isLoading = false,
                heroItems = hero,
                continueMovies = movies,
                continueSeries = episodes,
                recentChannels = recent,
                favoriteChannels = favs,
                hasAnyContent = continuations.isNotEmpty() || recent.isNotEmpty() || favs.isNotEmpty(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "loadHome failed for profile=$profileId", e)
            _uiState.value = PhoneHomeState(isLoading = false)
        }
    }

    fun refresh() {
        val c = ctx.value
        if (c.profileId > 0) viewModelScope.launch { loadHome(c.profileId) }
    }

    private companion object { const val TAG = "PhoneHomeVM" }
}
