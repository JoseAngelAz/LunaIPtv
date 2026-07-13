package com.lunaiptv.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.ProfileSourceCrossRef
import com.lunaiptv.core.database.entity.SourceEntity
import com.lunaiptv.features.settings.data.SettingsRepository

/**
 * The set of sources the Browse screens should show right now: the active profile's linked sources,
 * narrowed to the user's chosen "active playlist" ([SettingsRepository.defaultSourceId]) when one is set.
 *
 * A default of `-1` (or a default that no longer belongs to the profile) means **All playlists** — the
 * full merged view. This is purely a *display* filter: every playlist still imports and stores all of its
 * content and EPG; this only decides which source ids the grids/guide read from.
 */
data class ActiveProfileSources(val profileId: Long, val sources: List<SourceEntity>) {
    val sourceIds: List<Long> get() = sources.map { it.id }
}

/**
 * Reactive [ActiveProfileSources] for the current profile + active-playlist filter. Emits again whenever
 * the profile, its linked sources, or the chosen default changes, so Browse refreshes live.
 *
 * Auto-repairs orphaned sources: if the active profile has no linked sources but orphaned sources
 * exist in the DB (e.g. from a previous import that didn't create the `profile_source` link),
 * they are automatically linked to the active profile so content appears immediately.
 */
@Suppress("OPT_IN_USAGE")
fun activeProfileSources(
    settings: SettingsRepository,
    sourceDao: SourceDao,
): Flow<ActiveProfileSources> =
    settings.activeProfileId
        .flatMapLatest { pid ->
            if (pid < 0) {
                flowOf(ActiveProfileSources(pid, emptyList()))
            } else {
                sourceDao.observeForProfile(pid)
                    .onEach { srcs ->
                        if (srcs.isEmpty()) {
                            val allIds = sourceDao.allSourceIds()
                            if (allIds.isNotEmpty()) {
                                allIds.forEach { sid ->
                                    sourceDao.link(ProfileSourceCrossRef(profileId = pid, sourceId = sid))
                                }
                            }
                        }
                    }
                    .combine(settings.defaultSourceId) { srcs, defaultId ->
                        val filtered = if (defaultId > 0 && srcs.any { it.id == defaultId }) {
                            srcs.filter { it.id == defaultId }
                        } else {
                            srcs
                        }
                        ActiveProfileSources(pid, filtered)
                    }
            }
        }
        .distinctUntilChanged()

/**
 * One-shot version of [activeProfileSources] for imperative code paths: the [profileId]'s linked source
 * ids, narrowed to the active-playlist filter when one is set (else all). Empty when the profile has none.
 */
suspend fun activeSourceIds(
    settings: SettingsRepository,
    sourceDao: SourceDao,
    profileId: Long,
): List<Long> {
    val all = sourceDao.sourceIdsForProfile(profileId)
    val defaultId = settings.defaultSourceId.first()
    return if (defaultId > 0 && defaultId in all) listOf(defaultId) else all
}
