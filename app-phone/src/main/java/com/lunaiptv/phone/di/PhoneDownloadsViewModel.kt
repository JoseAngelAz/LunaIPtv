package com.lunaiptv.phone.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.lunaiptv.core.database.dao.DownloadDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.DownloadEntity
import com.lunaiptv.core.download.DownloadManager
import com.lunaiptv.core.download.DownloadStorageInfo
import com.lunaiptv.core.model.DownloadStatus
import com.lunaiptv.core.model.MediaType
import com.lunaiptv.features.settings.data.SettingsRepository
import com.lunaiptv.core.repository.activeProfileSources

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneDownloadsViewModel(
    private val downloadManager: DownloadManager,
    private val downloadDao: DownloadDao,
    private val settings: SettingsRepository,
    private val sourceDao: SourceDao,
) : ViewModel() {

    private data class Ctx(val profileId: Long, val sourceIds: List<Long>)

    private val ctx: StateFlow<Ctx> = activeProfileSources(settings, sourceDao)
        .map { aps -> Ctx(aps.profileId, aps.sourceIds) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ctx(-1L, emptyList()))

    val downloads: StateFlow<List<DownloadEntity>> = ctx
        .flatMapLatest { c ->
            if (c.profileId > 0) downloadManager.observe(c.profileId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _storageInfo = MutableStateFlow<DownloadStorageInfo?>(null)
    val storageInfo: StateFlow<DownloadStorageInfo?> = _storageInfo.asStateFlow()

    init {
        viewModelScope.launch {
            _storageInfo.value = try { downloadManager.storageInfo() } catch (_: Exception) { null }
        }
    }

    fun pause(download: DownloadEntity) = downloadManager.pause(download)
    fun resume(download: DownloadEntity) = downloadManager.resume(download)
    fun retry(download: DownloadEntity) = downloadManager.retry(download)
    fun delete(download: DownloadEntity) = downloadManager.delete(download)

    fun enqueue(
        profileId: Long, mediaType: MediaType, itemId: Long,
        title: String, posterUrl: String?, streamUrl: String,
        relativeDir: String, fileName: String,
    ) {
        downloadManager.enqueue(profileId, mediaType, itemId, title, posterUrl, streamUrl, relativeDir, fileName)
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloads.value.filter { it.status == DownloadStatus.COMPLETED }.forEach { delete(it) }
        }
    }

    private companion object { const val TAG = "PhoneDownloadsVM" }
}
