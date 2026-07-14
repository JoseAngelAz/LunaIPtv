@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.lunaiptv.phone.di

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.lunaiptv.core.database.dao.ChannelSearchResult
import com.lunaiptv.core.database.dao.FavoriteDao
import com.lunaiptv.core.database.dao.HistoryDao
import com.lunaiptv.core.database.dao.MovieDao
import com.lunaiptv.core.database.dao.ProfileDao
import com.lunaiptv.core.database.dao.SeriesDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.dao.resolveExistingProfileId
import com.lunaiptv.core.database.entity.ChannelEntity
import com.lunaiptv.core.database.entity.FavoriteEntity
import com.lunaiptv.core.database.entity.MovieEntity
import com.lunaiptv.core.database.entity.SeriesEntity
import com.lunaiptv.core.database.entity.WatchHistoryEntity
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.core.repository.activeProfileSources
import com.lunaiptv.features.settings.data.SettingsRepository

/** Combined results of a global query (each list bounded). */
data class PhoneSearchResults(
    val channels: List<ChannelSearchResult> = emptyList(),
    val movies: List<MovieEntity> = emptyList(),
    val series: List<SeriesEntity> = emptyList(),
) {
    val isEmpty: Boolean get() = channels.isEmpty() && movies.isEmpty() && series.isEmpty()
}

/** Empty-state launcher intents. */
enum class PhoneSearchIntent(val label: String) {
    CONTINUE("Continue watching"),
    UNWATCHED("Unwatched"),
    CHANNELS("Channels"),
}

/**
 * Phone-specific search ViewModel. Replicates the TV SearchViewModel's search logic
 * without the LunaIPtvPlayer (mpv) dependency — playback is handled by the screen layer.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PhoneSearchViewModel(
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val historyDao: HistoryDao,
    private val profileDao: ProfileDao,
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
    private val customize: CustomizationStore,
    private val favoriteDao: FavoriteDao,
) : ViewModel() {

    private data class Ctx(val profileId: Long, val sourceIds: List<Long>)

    private val ctx: StateFlow<Ctx> = activeProfileSources(settings, sourceDao)
        .map { aps -> Ctx(aps.profileId, aps.sourceIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ctx(-1L, emptyList()))

    // ── Query / Results ─────────────────────────────────────
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<PhoneSearchResults> = _query
        .map { it.trim() }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.length < 2) flowOf(PhoneSearchResults())
            else flowOf(search(q))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhoneSearchResults())

    fun setQuery(q: String) {
        _query.value = q
        if (q.isNotBlank()) _intent.value = null
    }

    // ── Empty-state launcher ────────────────────────────────
    val recentSearches: StateFlow<List<String>> = settings.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearRecentSearches() { viewModelScope.launch { settings.clearRecentSearches() } }
    fun rememberCurrentQuery() { viewModelScope.launch { settings.addRecentSearch(_query.value) } }

    private val _intent = MutableStateFlow<PhoneSearchIntent?>(null)
    val intent: StateFlow<PhoneSearchIntent?> = _intent.asStateFlow()

    fun setIntent(i: PhoneSearchIntent?) {
        _intent.value = i
        if (i != null) _query.value = ""
    }

    val curatedResults: StateFlow<PhoneSearchResults> = combine(_intent, ctx) { i, c -> i to c }
        .flatMapLatest { (i, c) ->
            if (i == null || c.profileId < 0 || c.sourceIds.isEmpty()) flowOf(PhoneSearchResults())
            else flowOf(loadIntent(i, c.profileId, c.sourceIds))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhoneSearchResults())

    private suspend fun loadIntent(intent: PhoneSearchIntent, pid: Long, ids: List<Long>): PhoneSearchResults =
        when (intent) {
            PhoneSearchIntent.CONTINUE -> PhoneSearchResults(
                channels = channelDao.recentlyWatched(pid, LIMIT).first().map { ChannelSearchResult(it, null) },
                movies = movieDao.recentlyWatchedSnapshot(pid, ids, LIMIT),
                series = seriesDao.recentlyWatchedSnapshot(pid, ids, LIMIT),
            )
            PhoneSearchIntent.UNWATCHED -> PhoneSearchResults(
                movies = movieDao.unwatchedFavorites(pid, ids, LIMIT),
                series = seriesDao.unwatchedFavorites(pid, ids, LIMIT),
            )
            PhoneSearchIntent.CHANNELS -> PhoneSearchResults(
                channels = channelDao.favoritesListAlpha(pid).first().take(LIMIT).map { ChannelSearchResult(it, null) },
            )
        }

    private suspend fun search(q: String): PhoneSearchResults {
        val pid = currentProfileId() ?: return PhoneSearchResults()
        val ids = ctx.value.sourceIds.ifEmpty { return PhoneSearchResults() }
        val custLive = customize.observe(pid, MediaType.LIVE).first()
        val custMovie = customize.observe(pid, MediaType.MOVIE).first()
        val custSeries = customize.observe(pid, MediaType.SERIES).first()
        val hiddenLiveCats = hiddenCategoryIds(ids, MediaType.LIVE, custLive)
        val hiddenMovieCats = hiddenCategoryIds(ids, MediaType.MOVIE, custMovie)
        val hiddenSeriesCats = hiddenCategoryIds(ids, MediaType.SERIES, custSeries)
        return PhoneSearchResults(
            channels = channelDao.searchListDetailed(q, ids, LIMIT)
                .filter {
                    CustomizeKeys.channel(it.channel) !in custLive.hiddenItems &&
                        (it.channel.categoryId == null || it.channel.categoryId !in hiddenLiveCats)
                }
                .map { row -> custLive.itemNames[CustomizeKeys.channel(row.channel)]?.let { row.copy(channel = row.channel.copy(name = it)) } ?: row },
            movies = movieDao.searchList(q, ids, LIMIT)
                .filter {
                    CustomizeKeys.movie(it) !in custMovie.hiddenItems &&
                        (it.categoryId == null || it.categoryId !in hiddenMovieCats)
                },
            series = seriesDao.searchList(q, ids, LIMIT)
                .filter {
                    CustomizeKeys.series(it) !in custSeries.hiddenItems &&
                        (it.categoryId == null || it.categoryId !in hiddenSeriesCats)
                },
        )
    }

    private suspend fun hiddenCategoryIds(sourceIds: List<Long>, type: MediaType, cust: com.lunaiptv.core.customize.SectionCustomizations): Set<Long> {
        if (cust.hiddenCategories.isEmpty()) return emptySet()
        return categoryDao.observe(sourceIds, type).first()
            .filter { CustomizeKeys.category(it) in cust.hiddenCategories }
            .map { it.id }
            .toSet()
    }

    // ── Favorites ───────────────────────────────────────────
    val favoriteChannelIds: StateFlow<Set<Long>> = ctx
        .flatMapLatest { favoriteDao.observeFavoriteIds(it.profileId, MediaType.LIVE) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavoriteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            val pid = ctx.value.profileId
            if (pid < 0) return@launch
            if (favoriteChannelIds.value.contains(channel.id)) {
                favoriteDao.remove(pid, MediaType.LIVE, channel.id)
            } else {
                favoriteDao.add(FavoriteEntity(profileId = pid, mediaType = MediaType.LIVE, itemId = channel.id))
            }
        }
    }

    // ── History recording ───────────────────────────────────
    fun recordPlay(type: MediaType, itemId: Long) {
        viewModelScope.launch {
            val pid = currentProfileId() ?: return@launch
            runCatching {
                historyDao.record(WatchHistoryEntity(profileId = pid, mediaType = type, itemId = itemId))
            }.onFailure { t ->
                Log.w(TAG, "record history failed profile=$pid type=$type itemId=$itemId", t)
            }
        }
        rememberCurrentQuery()
    }

    private suspend fun currentProfileId(): Long? {
        val preferred = settings.activeProfileId.first()
        return if (preferred >= 0) profileDao.resolveExistingProfileId(preferred) else null
    }

    private companion object {
        const val TAG = "LunaIPtvPhoneSearch"
        const val LIMIT = 40
    }
}
