@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.lunaiptv.phone.di

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lunaiptv.core.database.dao.CategoryDao
import com.lunaiptv.core.database.dao.FavoriteDao
import com.lunaiptv.core.database.dao.HistoryDao
import com.lunaiptv.core.database.dao.ProgressDao
import com.lunaiptv.core.database.dao.SeriesDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.EpisodeEntity
import com.lunaiptv.core.database.entity.FavoriteEntity
import com.lunaiptv.core.database.entity.MetadataCacheEntity
import com.lunaiptv.core.database.entity.SeriesEntity
import com.lunaiptv.core.database.entity.WatchHistoryEntity
import com.lunaiptv.core.metadata.MetadataRepository
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.core.repository.SeriesRepository
import com.lunaiptv.core.repository.activeProfileSources
import com.lunaiptv.features.live.LiveKey
import com.lunaiptv.features.settings.data.SettingsRepository

data class PhoneSeriesRailItem(val key: LiveKey, val title: String)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PhoneSeriesViewModel(
    private val seriesDao: SeriesDao,
    private val categoryDao: CategoryDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val progressDao: ProgressDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val seriesRepository: SeriesRepository,
    private val player: PhoneLivePlayer,
    private val metadata: MetadataRepository,
) : ViewModel() {

    private data class Ctx(val profileId: Long, val sourceIds: List<Long>)

    private val ctx: StateFlow<Ctx> = activeProfileSources(settings, sourceDao)
        .map { aps -> Ctx(aps.profileId, aps.sourceIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ctx(-1L, emptyList()))

    // ── Category rail ──────────────────────────────────────
    private val _selectedKey = MutableStateFlow<LiveKey>(LiveKey.All)
    val selectedKey: StateFlow<LiveKey> = _selectedKey.asStateFlow()

    var firstVisibleIndex: Int = 0

    val railItems: StateFlow<List<PhoneSeriesRailItem>> = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0 || c.sourceIds.isEmpty()) flowOf(emptyList())
            else categoryDao.observe(c.sourceIds, com.lunaiptv.core.model.MediaType.SERIES).map { cats ->
                buildList {
                    add(PhoneSeriesRailItem(LiveKey.Favorites, "Favorites"))
                    add(PhoneSeriesRailItem(LiveKey.History, "History"))
                    add(PhoneSeriesRailItem(LiveKey.All, "All"))
                    cats.forEach { add(PhoneSeriesRailItem(LiveKey.Folder(it.id), it.name)) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectKey(key: LiveKey) { _selectedKey.value = key }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    private val _sortByName = MutableStateFlow(false)
    val sortByName: StateFlow<Boolean> = _sortByName.asStateFlow()
    fun toggleSort() { _sortByName.value = !_sortByName.value }
    fun setSortByName(v: Boolean) { _sortByName.value = v }

    private val _selectedSeries = MutableStateFlow<SeriesEntity?>(null)
    val selectedSeries: StateFlow<SeriesEntity?> = _selectedSeries.asStateFlow()

    // ── Opened series (episode view) ───────────────────────
    private val _openedSeries = MutableStateFlow<SeriesEntity?>(null)
    val openedSeries: StateFlow<SeriesEntity?> = _openedSeries.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _episodesLoading = MutableStateFlow(false)
    val episodesLoading: StateFlow<Boolean> = _episodesLoading.asStateFlow()

    private val _episodes = MutableStateFlow<List<EpisodeEntity>>(emptyList())
    val episodes: StateFlow<List<EpisodeEntity>> = _episodes.asStateFlow()

    private val _selectedEpisode = MutableStateFlow<EpisodeEntity?>(null)
    val selectedEpisode: StateFlow<EpisodeEntity?> = _selectedEpisode.asStateFlow()

    // TMDB metadata for current series detail screen
    private val _currentSeriesMeta = MutableStateFlow<MetadataCacheEntity?>(null)
    val currentSeriesMeta: StateFlow<MetadataCacheEntity?> = _currentSeriesMeta.asStateFlow()

    fun loadSeriesMeta(series: SeriesEntity) {
        viewModelScope.launch {
            _currentSeriesMeta.value = null
            _currentSeriesMeta.value = try { metadata.resolveSeries(series) } catch (_: Exception) { null }
        }
    }

    fun clearSeriesMeta() { _currentSeriesMeta.value = null }

    val seasons: StateFlow<List<Int>> = _episodes.map { eps ->
        eps.map { it.seasonNumber }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredEpisodes: StateFlow<List<EpisodeEntity>> = combine(_episodes, _selectedSeason) { eps, s ->
        eps.filter { it.seasonNumber == s }.sortedBy { it.episodeNumber }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val episodeProgress: StateFlow<Map<Long, com.lunaiptv.core.database.entity.PlaybackProgressEntity>> =
        combine(ctx, _openedSeries) { c, s -> Pair(c, s) }
            .flatMapLatest { (c, s) ->
                if (c.profileId < 0 || s == null) flowOf(emptyList<com.lunaiptv.core.database.entity.PlaybackProgressEntity>())
                else progressDao.observeSeriesEpisodeProgress(c.profileId, s.id)
            }
            .map { list: List<com.lunaiptv.core.database.entity.PlaybackProgressEntity> -> list.associateBy { it.itemId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ── Paginated series grid ──────────────────────────────
    val seriesCount: StateFlow<Int> = combine(ctx, _selectedKey) { c, k -> Pair(c, k) }
        .flatMapLatest { (c, k) ->
            if (c.profileId < 0) flowOf(0) else countFlow(c.profileId, c.sourceIds, k)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val series: StateFlow<PagingData<SeriesEntity>> = combine(
        ctx, _selectedKey, _searchQuery.debounce(300).distinctUntilChanged(), _sortByName,
    ) { c, k, q, s -> SeriesQuery(c, k, q, s) }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.ctx.profileId < 0 || q.ctx.sourceIds.isEmpty()) flowOf(PagingData.empty())
            else seriesPager(q.ctx.profileId, q.ctx.sourceIds, q.key, q.query, q.sortByName)
        }
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.Eagerly, PagingData.empty())

    private data class SeriesQuery(val ctx: Ctx, val key: LiveKey, val query: String, val sortByName: Boolean)

    private fun seriesPager(pid: Long, sourceIds: List<Long>, key: LiveKey, query: String, sortByName: Boolean) =
        Pager(PagingConfig(pageSize = 60, prefetchDistance = 50, initialLoadSize = 120, maxSize = 300)) {
            when (key) {
                is LiveKey.All -> if (query.isBlank()) {
                    if (sortByName) seriesDao.pagingAll(sourceIds) else seriesDao.pagingAllOriginal(sourceIds)
                } else seriesDao.searchAll(query, sourceIds)
                is LiveKey.Favorites -> seriesDao.pagingFavorites(pid)
                is LiveKey.Folder -> if (query.isBlank()) {
                    if (sortByName) seriesDao.pagingByCategoryAlpha(key.id) else seriesDao.pagingByCategory(key.id)
                } else seriesDao.searchInCategory(query, key.id)
                is LiveKey.History -> seriesDao.pagingHistory(pid, sourceIds)
            }
        }.flow

    private fun countFlow(pid: Long, sourceIds: List<Long>, key: LiveKey) =
        when (key) {
            is LiveKey.All -> seriesDao.countAll(sourceIds)
            is LiveKey.Favorites -> seriesDao.countFavorites(pid, sourceIds)
            is LiveKey.Folder -> seriesDao.countByCategory(key.id)
            is LiveKey.History -> seriesDao.countHistory(pid, sourceIds)
        }

    // Favorites
    val favoriteIds: StateFlow<Set<Long>> = ctx
        .flatMapLatest { favoriteDao.observeFavoriteIds(it.profileId, MediaType.SERIES) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavorite(s: SeriesEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            if (favoriteIds.value.contains(s.id)) favoriteDao.remove(pid, MediaType.SERIES, s.id)
            else favoriteDao.add(FavoriteEntity(profileId = pid, mediaType = MediaType.SERIES, itemId = s.id))
        }
    }

    // ── Series detail navigation ───────────────────────────
    fun openSeries(s: SeriesEntity) {
        _openedSeries.value = s
        _selectedSeason.value = 1
        _episodesLoading.value = true
        viewModelScope.launch {
            try {
                seriesRepository.loadEpisodes(s)
                val eps = seriesDao.episodesBySeries(s.id).first()
                _episodes.value = eps
                // Jump to last-watched episode's season
                val pid = ctx.value.profileId
                if (pid > 0) {
                    val lastId = progressDao.lastWatchedEpisodeId(pid, s.id)
                    if (lastId != null) {
                        val lastEp = eps.find { it.id == lastId }
                        if (lastEp != null) _selectedSeason.value = lastEp.seasonNumber
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "loadEpisodes failed for ${s.id}", e)
            } finally {
                _episodesLoading.value = false
            }
        }
    }

    fun closeSeries() {
        _openedSeries.value = null
        _episodes.value = emptyList()
        _selectedEpisode.value = null
        _currentSeriesMeta.value = null
    }

    fun selectSeason(n: Int) { _selectedSeason.value = n }
    fun selectEpisode(ep: EpisodeEntity) { _selectedEpisode.value = ep }

    // ── Episode playback ───────────────────────────────────
    fun playEpisode(ep: EpisodeEntity, startPositionMs: Long = 0) {
        val show = _openedSeries.value ?: return
        viewModelScope.launch {
            val source = sourceDao.getById(show.sourceId)
            player.play(ep.streamUrl, userAgent = source?.userAgent, startPositionMs = startPositionMs)
            recordHistory(show, ep)
        }
    }

    private fun recordHistory(show: SeriesEntity, ep: EpisodeEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            runCatching {
                historyDao.record(WatchHistoryEntity(profileId = pid, mediaType = MediaType.EPISODE, itemId = ep.id))
                historyDao.record(WatchHistoryEntity(profileId = pid, mediaType = MediaType.SERIES, itemId = show.id))
            }.onFailure { Log.w(TAG, "record history failed", it) }
        }
    }

    fun savedPositionMs(episodeId: Long): Long {
        val ep = episodeProgress.value[episodeId] ?: return 0L
        val pct = ep.positionMs.toFloat() / ep.durationMs.coerceAtLeast(1).toFloat()
        return if (pct < 0.95f) ep.positionMs else 0L
    }

    fun saveEpisodeProgress(episodeId: Long) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            val pos = player.currentPositionMs()
            val dur = player.durationMs()
            if (pos > 0 && dur > 0) {
                runCatching {
                    progressDao.save(
                        com.lunaiptv.core.database.entity.PlaybackProgressEntity(
                            profileId = pid,
                            mediaType = MediaType.EPISODE,
                            itemId = episodeId,
                            positionMs = pos,
                            durationMs = dur,
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() { super.onCleared(); player.release() }

    private companion object { const val TAG = "PhoneSeriesVM" }
}
