@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.lunaiptv.phone.di

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
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
import com.lunaiptv.core.customize.CustomizationStore
import com.lunaiptv.core.customize.CustomizeKeys
import com.lunaiptv.core.database.dao.CategoryDao
import com.lunaiptv.core.database.dao.ChannelDao
import com.lunaiptv.core.database.dao.FavoriteDao
import com.lunaiptv.core.database.dao.HistoryDao
import com.lunaiptv.core.database.dao.ProfileDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.ChannelEntity
import com.lunaiptv.core.database.entity.FavoriteEntity
import com.lunaiptv.core.database.entity.WatchHistoryEntity
import com.lunaiptv.features.live.LiveKey
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.core.repository.activeProfileSources
import com.lunaiptv.features.settings.data.SettingsRepository

/** Simplified rail item for phone live navigation. */
data class PhoneLiveRailItem(
    val key: LiveKey,
    val title: String,
)

/**
 * Phone-specific Live ViewModel. Loads channels with paging, categories, search, favorites.
 * Playback via ExoPlayer (PhoneLivePlayer).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PhoneLiveViewModel(
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val profileDao: com.lunaiptv.core.database.dao.ProfileDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val customize: CustomizationStore,
    private val epgDao: com.lunaiptv.core.database.dao.EpgDao,
    val player: PhoneLivePlayer,
) : ViewModel() {

    private data class Ctx(val profileId: Long, val sourceIds: List<Long>)

    private val ctx: StateFlow<Ctx> = activeProfileSources(settings, sourceDao)
        .map { aps -> Ctx(aps.profileId, aps.sourceIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ctx(-1L, emptyList()))

    // ── Category rail ──────────────────────────────────────
    private val _selectedKey = MutableStateFlow<LiveKey>(LiveKey.All)
    val selectedKey: StateFlow<LiveKey> = _selectedKey.asStateFlow()

    val railItems: StateFlow<List<PhoneLiveRailItem>> = ctx
        .flatMapLatest { c ->
            if (c.profileId < 0 || c.sourceIds.isEmpty()) flowOf(emptyList())
            else categoryDao.observe(c.sourceIds, MediaType.LIVE).map { cats ->
                buildList {
                    add(PhoneLiveRailItem(LiveKey.All, "All"))
                    add(PhoneLiveRailItem(LiveKey.Favorites, "Favorites"))
                    cats.forEach { cat ->
                        add(PhoneLiveRailItem(LiveKey.Folder(cat.id), cat.name))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectKey(key: LiveKey) { _selectedKey.value = key }

    // ── Search ─────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ── Sort ───────────────────────────────────────────────
    private val _sortByName = MutableStateFlow(false)
    val sortByName: StateFlow<Boolean> = _sortByName.asStateFlow()

    fun toggleSort() { _sortByName.value = !_sortByName.value }

    // ── Selected channel ───────────────────────────────────
    private val _selectedChannel = MutableStateFlow<ChannelEntity?>(null)
    val selectedChannel: StateFlow<ChannelEntity?> = _selectedChannel.asStateFlow()

    fun selectChannel(ch: ChannelEntity) { _selectedChannel.value = ch }

    // ── EPG now/next per visible channel ───────────────────
    data class EpgInfo(val nowTitle: String?, val nextTitle: String?, val stopMs: Long)

    private val _epgMap = MutableStateFlow<Map<Long, EpgInfo>>(emptyMap())
    val epgMap: StateFlow<Map<Long, EpgInfo>> = _epgMap.asStateFlow()

    fun loadEpgForVisible(channels: List<ChannelEntity>) {
        viewModelScope.launch {
            val c = ctx.value
            if (c.profileId < 0 || c.sourceIds.isEmpty()) return@launch
            val now = System.currentTimeMillis()
            val map = mutableMapOf<Long, EpgInfo>()
            for (ch in channels) {
                val epgId = ch.epgChannelId?.lowercase()?.trim() ?: continue
                try {
                    val nowProg = epgDao.nowPlaying(epgId, now)
                    val upcoming = epgDao.upcoming(epgId, now, 2).first()
                    val nextProg = upcoming.getOrNull(1)
                    map[ch.id] = EpgInfo(
                        nowTitle = nowProg?.title,
                        nextTitle = nextProg?.title,
                        stopMs = nowProg?.stopMs ?: 0L,
                    )
                } catch (_: Exception) { /* skip */ }
            }
            _epgMap.value = map
        }
    }

    // ── Channel count ──────────────────────────────────────
    val channelCount: StateFlow<Int> = combine(ctx, _selectedKey) { c, k -> Pair(c, k) }
        .flatMapLatest { (c, k) ->
            if (c.profileId < 0) flowOf(0)
            else channelCountFlow(c.profileId, c.sourceIds, k)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Paginated channels ─────────────────────────────────
    val channels: StateFlow<PagingData<ChannelEntity>> = combine(
        ctx, _selectedKey, _searchQuery.debounce(300).distinctUntilChanged(), _sortByName,
    ) { c, k, q, s -> ChannelQuery(c, k, q, s) }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.ctx.profileId < 0 || q.ctx.sourceIds.isEmpty()) flowOf(PagingData.empty())
            else channelPager(q.ctx.profileId, q.ctx.sourceIds, q.key, q.query, q.sortByName)
        }
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PagingData.empty())

    private data class ChannelQuery(
        val ctx: Ctx,
        val key: LiveKey,
        val query: String,
        val sortByName: Boolean,
    )

    // pagingAll = A-Z (name ASC); pagingAllOriginal = playlist order
    private fun channelPager(pid: Long, sourceIds: List<Long>, key: LiveKey, query: String, sortByName: Boolean) =
        Pager(PagingConfig(pageSize = 60, prefetchDistance = 30, initialLoadSize = 90, maxSize = 300)) {
            when (key) {
                is LiveKey.All -> {
                    if (query.isBlank()) {
                        if (sortByName) channelDao.pagingAll(sourceIds)
                        else channelDao.pagingAllOriginal(sourceIds)
                    } else {
                        channelDao.searchAll(query, sourceIds)
                    }
                }
                is LiveKey.Favorites -> channelDao.pagingFavorites(pid)
                is LiveKey.Folder -> {
                    if (query.isBlank()) {
                        if (sortByName) channelDao.pagingByCategoryAlpha(key.id)
                        else channelDao.pagingByCategory(key.id)
                    } else {
                        channelDao.searchInCategory(query, key.id)
                    }
                }
                is LiveKey.History -> channelDao.pagingHistory(pid, sourceIds)
            }
        }.flow

    private fun channelCountFlow(pid: Long, sourceIds: List<Long>, key: LiveKey) =
        when (key) {
            is LiveKey.All -> channelDao.countAll(sourceIds)
            is LiveKey.Favorites -> channelDao.countFavorites(pid, sourceIds)
            is LiveKey.Folder -> channelDao.countByCategory(key.id)
            is LiveKey.History -> channelDao.countHistory(pid, sourceIds)
        }

    // ── Favorites ──────────────────────────────────────────
    val favoriteIds: StateFlow<Set<Long>> = ctx
        .flatMapLatest { favoriteDao.observeFavoriteIds(it.profileId, MediaType.LIVE) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(ch: ChannelEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            if (favoriteIds.value.contains(ch.id)) {
                favoriteDao.remove(pid, MediaType.LIVE, ch.id)
            } else {
                favoriteDao.add(FavoriteEntity(profileId = pid, mediaType = MediaType.LIVE, itemId = ch.id))
            }
        }
    }

    // ── Playback ───────────────────────────────────────────
    fun playChannel(ch: ChannelEntity) {
        _selectedChannel.value = ch
        viewModelScope.launch {
            val source = sourceDao.getById(ch.sourceId)
            player.play(ch.streamUrl, userAgent = source?.userAgent)
            recordHistory(ch)
        }
    }

    fun stopPlayback() { player.stop() }

    private fun recordHistory(ch: ChannelEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            runCatching {
                historyDao.record(WatchHistoryEntity(profileId = pid, mediaType = MediaType.LIVE, itemId = ch.id))
            }.onFailure { t ->
                Log.w(TAG, "record history failed profile=$pid channel=${ch.id}", t)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }

    private companion object {
        const val TAG = "PhoneLiveVM"
    }
}
