package com.lunaiptv.core.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lunaiptv.core.database.dao.SeriesDao
import com.lunaiptv.core.database.dao.SourceDao
import com.lunaiptv.core.database.entity.EpisodeEntity
import com.lunaiptv.core.database.entity.SeriesEntity
import com.lunaiptv.core.model.SourceType
import com.lunaiptv.core.parser.XtreamClient

/** Loads a series' seasons/episodes on demand (Xtream `get_series_info`). Episodes carry their
 *  season number directly, so no separate Season rows are needed for browsing. */
class SeriesRepository(
    private val seriesDao: SeriesDao,
    private val sourceDao: SourceDao,
    private val xtream: XtreamClient,
    private val userData: com.lunaiptv.core.backup.UserDataResolver,
) {
    /** Returns true if episodes are available (cached or freshly fetched). Xtream-only. */
    suspend fun loadEpisodes(series: SeriesEntity): Boolean = withContext(Dispatchers.IO) {
        if (seriesDao.episodeCount(series.id) > 0) return@withContext true
        val source = sourceDao.getById(series.sourceId) ?: return@withContext false
        val remoteId = series.remoteId
        if (source.type != SourceType.XTREAM || remoteId.isNullOrBlank()) return@withContext false
        try {
            val info = xtream.getSeriesInfo(source, remoteId)
            seriesDao.deleteEpisodes(series.id)
            val episodes = info.episodes.map { e ->
                EpisodeEntity(
                    seriesId = series.id,
                    seasonId = null,
                    seasonNumber = e.seasonNumber,
                    episodeNumber = e.episodeNumber,
                    name = e.title,
                    streamUrl = xtream.seriesEpisodeUrl(source, e.id, e.containerExt),
                    containerExt = e.containerExt,
                    remoteId = e.id,
                )
            }
            episodes.chunked(500).forEach { seriesDao.upsertEpisodes(it) }
            // Episode rows just appeared — restored episode history/resume can attach now.
            if (episodes.isNotEmpty()) runCatching { userData.resolvePending() }
            episodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
