package com.lunaiptv.core.launcher

/**
 * Abstraction for TV-home launcher integration. Core uses this instead of the concrete
 * [TvHomeRepository] so that the core module compiles without the Android TV provider dependency.
 * The app module provides the real implementation.
 */
interface LauncherProfilePublisher {
    suspend fun refreshProfile(profileId: Long, allowBrowsableRequest: Boolean = false)
    suspend fun clearProfile(profileId: Long)
    suspend fun publishMovieProgress(profileId: Long, movieId: Long, positionMs: Long, durationMs: Long)
    suspend fun publishEpisodeProgress(profileId: Long, episodeId: Long, positionMs: Long, durationMs: Long)
    suspend fun refreshRecentLive(profileId: Long, allowBrowsableRequest: Boolean = false)
}
