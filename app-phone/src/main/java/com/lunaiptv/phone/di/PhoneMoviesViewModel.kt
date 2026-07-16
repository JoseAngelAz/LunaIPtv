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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lunaiptv.core.customize.CustomizationStore
import com.lunaiptv.core.database.dao.CategoryDao
import com.lunaiptv.core.database.dao.FavoriteDao
import com.lunaiptv.core.database.dao.HistoryDao
import com.lunaiptv.core.database.dao.MovieDao
import com.lunaiptv.core.database.dao.ProgressDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.FavoriteEntity
import com.lunaiptv.core.database.entity.MetadataCacheEntity
import com.lunaiptv.core.database.entity.MovieEntity
import com.lunaiptv.core.database.entity.WatchHistoryEntity
import com.lunaiptv.core.metadata.MetadataRepository
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.core.repository.activeProfileSources
import com.lunaiptv.features.live.LiveKey
import com.lunaiptv.features.settings.data.SettingsRepository

data class PhoneMovieRailItem(val key: LiveKey, val title: String)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PhoneMoviesViewModel(
    private val movieDao: MovieDao,
    private val categoryDao: CategoryDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val progressDao: ProgressDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val player: PhoneLivePlayer,
    private val metadata: MetadataRepository,
) : ViewModel() {

    private data class Ctx(val profileId: Long, val sourceIds: List<Long>)

    private val ctx: StateFlow<Ctx> = activeProfileSources(settings, sourceDao)
        .map { aps -> Ctx(aps.profileId, aps.sourceIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ctx(-1L, emptyList()))

    private val _selectedKey = MutableStateFlow<LiveKey>(LiveKey.All)
    val selectedKey: StateFlow<LiveKey> = _selectedKey.asStateFlow()

    var firstVisibleIndex: Int = 0

    val railItems: StateFlow<List<PhoneMovieRailItem>> = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0 || c.sourceIds.isEmpty()) flowOf(emptyList())
            else categoryDao.observe(c.sourceIds, com.lunaiptv.core.model.MediaType.MOVIE).map { cats ->
                buildList {
                    add(PhoneMovieRailItem(LiveKey.Favorites, "Favorites"))
                    add(PhoneMovieRailItem(LiveKey.History, "History"))
                    add(PhoneMovieRailItem(LiveKey.All, "All"))
                    cats.forEach { add(PhoneMovieRailItem(LiveKey.Folder(it.id), it.name)) }
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

    private val _selectedMovie = MutableStateFlow<MovieEntity?>(null)
    val selectedMovie: StateFlow<MovieEntity?> = _selectedMovie.asStateFlow()
    fun selectMovie(m: MovieEntity) { _selectedMovie.value = m }

    // TMDB metadata for current detail screen
    private val _currentMovieMeta = MutableStateFlow<MetadataCacheEntity?>(null)
    val currentMovieMeta: StateFlow<MetadataCacheEntity?> = _currentMovieMeta.asStateFlow()

    fun loadMovieMeta(movie: MovieEntity) {
        viewModelScope.launch {
            _currentMovieMeta.value = null
            try {
                val result = metadata.resolveMovie(movie)
                Log.d(TAG, "loadMovieMeta: movie=${movie.name}, result=${result?.overview?.take(50)}")
                _currentMovieMeta.value = result
            } catch (e: Exception) {
                Log.w(TAG, "loadMovieMeta failed for ${movie.name}", e)
                _currentMovieMeta.value = null
            }
        }
    }

    fun clearMovieMeta() { _currentMovieMeta.value = null }

    // Progress map for all visible movies (resume indicators on poster grid)
    private val _progressMap = MutableStateFlow<Map<Long, com.lunaiptv.core.database.entity.PlaybackProgressEntity>>(emptyMap())

    val channelCount: StateFlow<Int> = combine(ctx, _selectedKey) { c, k -> Pair(c, k) }
        .flatMapLatest { (c, k) ->
            if (c.profileId < 0) flowOf(0) else countFlow(c.profileId, c.sourceIds, k)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val movies: StateFlow<PagingData<MovieEntity>> = combine(
        ctx, _selectedKey, _searchQuery.debounce(300).distinctUntilChanged(), _sortByName,
    ) { c, k, q, s -> MovieQuery(c, k, q, s) }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.ctx.profileId < 0 || q.ctx.sourceIds.isEmpty()) flowOf(PagingData.empty())
            else moviePager(q.ctx.profileId, q.ctx.sourceIds, q.key, q.query, q.sortByName)
        }
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.Eagerly, PagingData.empty())

    private data class MovieQuery(val ctx: Ctx, val key: LiveKey, val query: String, val sortByName: Boolean)

    private fun moviePager(pid: Long, sourceIds: List<Long>, key: LiveKey, query: String, sortByName: Boolean) =
        Pager(PagingConfig(pageSize = 60, prefetchDistance = 50, initialLoadSize = 120, maxSize = 300)) {
            when (key) {
                is LiveKey.All -> if (query.isBlank()) {
                    if (sortByName) movieDao.pagingAll(sourceIds) else movieDao.pagingAllOriginal(sourceIds)
                } else movieDao.searchAll(query, sourceIds)
                is LiveKey.Favorites -> movieDao.pagingFavorites(pid)
                is LiveKey.Folder -> if (query.isBlank()) {
                    if (sortByName) movieDao.pagingByCategoryAlpha(key.id) else movieDao.pagingByCategory(key.id)
                } else movieDao.searchInCategory(query, key.id)
                is LiveKey.History -> movieDao.pagingHistory(pid, sourceIds)
            }
        }.flow

    private fun countFlow(pid: Long, sourceIds: List<Long>, key: LiveKey) =
        when (key) {
            is LiveKey.All -> movieDao.countAll(sourceIds)
            is LiveKey.Favorites -> movieDao.countFavorites(pid, sourceIds)
            is LiveKey.Folder -> movieDao.countByCategory(key.id)
            is LiveKey.History -> movieDao.countHistory(pid, sourceIds)
        }

    // Favorites
    val favoriteIds: StateFlow<Set<Long>> = ctx
        .flatMapLatest { favoriteDao.observeFavoriteIds(it.profileId, MediaType.MOVIE) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavorite(m: MovieEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            if (favoriteIds.value.contains(m.id)) favoriteDao.remove(pid, MediaType.MOVIE, m.id)
            else favoriteDao.add(FavoriteEntity(profileId = pid, mediaType = MediaType.MOVIE, itemId = m.id))
        }
    }

    // Playback
    fun playMovie(m: MovieEntity, startPositionMs: Long = 0) {
        _selectedMovie.value = m
        viewModelScope.launch {
            val source = sourceDao.getById(m.sourceId)
            player.play(m.streamUrl, userAgent = source?.userAgent, startPositionMs = startPositionMs)
            recordHistory(m)
        }
    }

    private fun recordHistory(m: MovieEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            runCatching {
                historyDao.record(WatchHistoryEntity(profileId = pid, mediaType = MediaType.MOVIE, itemId = m.id))
            }.onFailure { Log.w(TAG, "record history failed", it) }
        }
    }

    fun saveProgress(movieId: Long) {
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
                            mediaType = MediaType.MOVIE,
                            itemId = movieId,
                            positionMs = pos,
                            durationMs = dur,
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() { super.onCleared(); player.release() }

    private companion object { const val TAG = "PhoneMoviesVM" }
}
