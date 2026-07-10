package com.lunaiptv.features.settings

import kotlinx.coroutines.CancellationException
import com.lunaiptv.core.database.entity.SourceEntity
import com.lunaiptv.core.epg.EpgSourceStore
import com.lunaiptv.core.repository.EpgRepository

/**
 * Shared semi-auto EPG sync used by both onboarding and Settings → Playlists. Registers the playlist's own
 * guide feed as an [EpgSource][com.lunaiptv.core.epg.EpgSource] in the [store] (re-using an existing entry
 * with the same URL) so it appears in Settings → EPG, then downloads it keyed by that source id while
 * reporting a live programme count through [setState].
 */
suspend fun runSemiAutoEpgSync(
    source: SourceEntity,
    epgRepository: EpgRepository,
    store: EpgSourceStore,
    setState: (EpgSyncUi) -> Unit,
) {
    val url = epgRepository.guideUrl(source) ?: run { setState(EpgSyncUi.Done); return }
    setState(EpgSyncUi.Syncing(0))
    // Show up in Settings → EPG like any other feed (so the user can re-sync / delete it there).
    val epgSource = store.getAll().firstOrNull { it.url == url } ?: store.add(source.name, url, source.userAgent)
    val now = System.currentTimeMillis()
    try {
        epgRepository.refreshUrl(epgSource.id, epgSource.url, epgSource.userAgent) { _, count -> setState(EpgSyncUi.Syncing(count)) }
        store.setSynced(epgSource.id, now, null)
        setState(EpgSyncUi.Done)
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        store.setSynced(epgSource.id, now, e.message)
        setState(EpgSyncUi.Failed(e.message ?: "Guide sync failed"))
    }
}
