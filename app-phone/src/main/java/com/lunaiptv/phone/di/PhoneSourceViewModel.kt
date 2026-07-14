@file:OptIn(FlowPreview::class)

package com.lunaiptv.phone.di

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lunaiptv.core.database.dao.ProfileDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.SourceEntity
import com.lunaiptv.core.network.ConnectivityObserver
import com.lunaiptv.core.repository.SourceRepository
import com.lunaiptv.core.sync.ImportFinalizer
import com.lunaiptv.core.sync.ImportStage
import com.lunaiptv.core.sync.SyncContentTypes
import com.lunaiptv.core.sync.SyncResult
import com.lunaiptv.core.sync.SyncCounts
import com.lunaiptv.core.sync.work.CatalogSyncState
import com.lunaiptv.core.sync.work.CatalogSyncScheduler
import com.lunaiptv.core.util.friendlySyncError
import com.lunaiptv.core.database.dao.resolveExistingProfileId
import com.lunaiptv.core.model.SourceType
import com.lunaiptv.features.settings.data.PlaylistAutoRefresh
import com.lunaiptv.features.settings.data.SettingsRepository

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PhoneSourceViewModel(
    private val profileDao: ProfileDao,
    private val sourceDao: SourceDao,
    private val sourceRepository: SourceRepository,
    private val settings: SettingsRepository,
    private val connectivity: ConnectivityObserver,
    private val importFinalizer: ImportFinalizer,
    private val catalogSyncScheduler: CatalogSyncScheduler,
) : ViewModel() {

    companion object {
        private const val TAG = "PhoneSourceVM"
    }

    sealed interface ImportState {
        data object Idle : ImportState
        data object Running : ImportState
        data class Success(val summary: String) : ImportState
        data class Failed(val message: String) : ImportState
    }

    val sources: StateFlow<List<SourceEntity>> = settings.activeProfileId
        .flatMapLatest { pid -> if (pid < 0) flowOf(emptyList()) else sourceRepository.observeSources(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val defaultSourceId: StateFlow<Long> = settings.defaultSourceId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1L)

    val playlistAutoRefresh: StateFlow<Map<Long, PlaylistAutoRefresh>> = settings.playlistAutoRefresh
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _progress = MutableStateFlow<ImportStage?>(null)
    val progress: StateFlow<ImportStage?> = _progress.asStateFlow()

    private var importJob: Job? = null
    private var _lastFailedSource: SourceEntity? = null
    val lastFailedSource: SourceEntity? get() = _lastFailedSource

    fun contentCounts(sourceId: Long): StateFlow<SyncCounts?> =
        catalogSyncScheduler.observeSync(sourceId)
            .onStart { emit(CatalogSyncState.Idle) }
            .filter { !it.isActive }
            .map { runCatching { importFinalizer.contentCounts(sourceId) }.getOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun syncState(sourceId: Long): StateFlow<CatalogSyncState> =
        catalogSyncScheduler.observeSync(sourceId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogSyncState.Idle)

    fun addXtream(
        name: String,
        server: String,
        user: String,
        pass: String,
        userAgent: String = "",
        epgUrl: String = "",
        autoRefresh: PlaylistAutoRefresh = PlaylistAutoRefresh.OFF,
        syncLive: Boolean = true,
        syncMovies: Boolean = true,
        syncSeries: Boolean = true,
        isDefault: Boolean = false,
    ) {
        val priority = SyncContentTypes(syncLive, syncMovies, syncSeries)
        runImport(autoRefresh, priority, enqueueRemainder = true, requiresNetwork = true, makeDefault = isDefault) { pid ->
            sourceRepository.addXtreamSource(
                pid, name.ifBlank { "My IPTV" }, server.trim(), user.trim(), pass,
                userAgent.trim().takeIf { it.isNotBlank() },
                epgUrl.trim().takeIf { it.isNotBlank() },
            )
        }
    }

    fun addM3u(
        name: String,
        url: String,
        userAgent: String = "",
        epgUrl: String = "",
        autoRefresh: PlaylistAutoRefresh = PlaylistAutoRefresh.OFF,
        isDefault: Boolean = false,
    ) = runImport(
        autoRefresh,
        requiresNetwork = !url.isLocalPlaylistPath(),
        makeDefault = isDefault,
    ) { pid ->
        sourceRepository.addM3uSource(
            pid, name.ifBlank { "My Playlist" }, url.trim(),
            userAgent.trim().takeIf { it.isNotBlank() },
            epgUrl.trim().takeIf { it.isNotBlank() },
        )
    }

    fun updateSource(id: Long, name: String, urlOrServer: String, user: String, pass: String, userAgent: String, epgUrl: String, autoRefresh: PlaylistAutoRefresh, isDefault: Boolean = false) {
        viewModelScope.launch {
            val existing = sourceDao.getById(id) ?: return@launch
            sourceRepository.updateSource(
                existing.copy(
                    name = name.ifBlank { existing.name },
                    url = urlOrServer.trim().ifBlank { existing.url },
                    username = user.trim().takeIf { it.isNotBlank() } ?: existing.username,
                    password = pass.takeIf { it.isNotBlank() } ?: existing.password,
                    userAgent = userAgent.trim().takeIf { it.isNotBlank() },
                    epgUrl = epgUrl.trim().takeIf { it.isNotBlank() },
                ),
            )
            settings.setPlaylistAutoRefresh(id, autoRefresh)
            when {
                isDefault -> settings.setDefaultSource(id)
                settings.defaultSourceId.first() == id -> settings.setDefaultSource(-1L)
            }
        }
    }

    fun resync(source: SourceEntity) {
        Log.d(TAG, "resync sourceId=${source.id}")
        viewModelScope.launch {
            val counts = importFinalizer.contentCounts(source.id)
            catalogSyncScheduler.enqueueSync(
                source.id,
                reason = "manual_resync",
                baseItemCount = counts.channels + counts.movies + counts.series,
            )
        }
    }

    fun cancelResync(source: SourceEntity) {
        Log.d(TAG, "cancelResync sourceId=${source.id}")
        catalogSyncScheduler.cancelSync(source.id)
    }

    fun delete(source: SourceEntity) {
        viewModelScope.launch {
            Log.d(TAG, "delete sourceId=${source.id}")
            catalogSyncScheduler.cancelSync(source.id)
            sourceRepository.deleteSource(source)
            if (defaultSourceId.value == source.id) settings.setDefaultSource(-1L)
        }
    }

    fun resetImport() {
        _importState.value = ImportState.Idle
        _progress.value = null
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _importState.value = ImportState.Idle
        _progress.value = null
    }

    private fun runImport(
        autoRefresh: PlaylistAutoRefresh = PlaylistAutoRefresh.OFF,
        contentTypes: SyncContentTypes = SyncContentTypes(),
        enqueueRemainder: Boolean = false,
        requiresNetwork: Boolean = true,
        makeDefault: Boolean = false,
        addSource: suspend (Long) -> SourceEntity,
    ) {
        importJob?.cancel()
        val job = viewModelScope.launch {
            _importState.value = ImportState.Running
            _progress.value = null
            var source: SourceEntity? = null
            try {
                if (requiresNetwork && !connectivity.isOnlineNow()) {
                    _importState.value = ImportState.Failed(friendlySyncError(null, online = false))
                    return@launch
                }
                val pid = profileDao.resolveExistingProfileId(settings.activeProfileId.first()) ?: return@launch
                Log.d(TAG, "runImport profile=$pid autoRefresh=$autoRefresh")
                source = addSource(pid)
                val freshSync = source.lastSyncAt == null
                val remainder = if (enqueueRemainder) SyncContentTypes().remainderAfter(contentTypes) else SyncContentTypes(live = false, movies = false, series = false)
                settings.setPlaylistAutoRefresh(source.id, autoRefresh)
                when (val r = sourceRepository.sync(source, onProgress = { _progress.value = it }, contentTypes = contentTypes)) {
                    is SyncResult.Success -> {
                        val counts = importFinalizer.finalize(source, deferIndexes = freshSync)
                        if (makeDefault) settings.setDefaultSource(source.id)
                        Log.d(TAG, "runImport sync success sourceId=${source.id} profile=$pid")
                        if (enqueueRemainder) enqueueRemainderSync(source, contentTypes)
                        if (freshSync && !remainder.hasAny) catalogSyncScheduler.enqueueContentIndexBuild(reason = "fresh_add")
                        _lastFailedSource = null
                        _importState.value = ImportState.Success(counts.summary(includeEpg = false))
                    }
                    is SyncResult.Failed -> {
                        cleanupFailedAdd(source)
                        _importState.value = ImportState.Failed(friendlySyncError(r.message, connectivity.isOnlineNow()))
                    }
                    SyncResult.Cancelled -> {
                        cleanupFailedAdd(source)
                        _importState.value = ImportState.Idle
                    }
                }
            } catch (c: CancellationException) {
                cleanupFailedAdd(source)
                _importState.value = ImportState.Idle
                _progress.value = null
                throw c
            } catch (e: Exception) {
                cleanupFailedAdd(source)
                _importState.value = ImportState.Failed(friendlySyncError(e.message, connectivity.isOnlineNow()))
            }
        }
        importJob = job
        job.invokeOnCompletion { if (importJob == job) importJob = null }
    }

    private fun String.isLocalPlaylistPath(): Boolean =
        startsWith("/") || startsWith("file://") || startsWith("content://")

    private suspend fun cleanupFailedAdd(source: SourceEntity?) {
        if (source == null) return
        withContext(NonCancellable) {
            catalogSyncScheduler.cancelSync(source.id)
            runCatching { sourceRepository.deleteSource(source) }
            runCatching { settings.setPlaylistAutoRefresh(source.id, PlaylistAutoRefresh.OFF) }
        }
    }

    private fun enqueueRemainderSync(source: SourceEntity, priority: SyncContentTypes) {
        val remainder = SyncContentTypes().remainderAfter(priority)
        if (remainder.hasAny) {
            catalogSyncScheduler.enqueueSync(source.id, reason = "add_remainder", contentTypes = remainder, completesInitialSync = true)
        }
    }
}
