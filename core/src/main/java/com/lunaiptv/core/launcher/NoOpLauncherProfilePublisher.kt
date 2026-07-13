package com.lunaiptv.core.launcher

import android.util.Log

/**
 * Default no-op implementation for the [LauncherProfilePublisher] interface.
 * The app module replaces this with the real TvHomeRepository-backed implementation.
 */
class NoOpLauncherProfilePublisher : LauncherProfilePublisher {
    override suspend fun refreshProfile(profileId: Long, allowBrowsableRequest: Boolean) {
        Log.d("NoOpLauncher", "refreshProfile stub — no-op")
    }

    override suspend fun clearProfile(profileId: Long) {
        Log.d("NoOpLauncher", "clearProfile stub — no-op")
    }

    override suspend fun publishMovieProgress(profileId: Long, movieId: Long, positionMs: Long, durationMs: Long) {
        Log.d("NoOpLauncher", "publishMovieProgress stub — no-op")
    }

    override suspend fun publishEpisodeProgress(profileId: Long, episodeId: Long, positionMs: Long, durationMs: Long) {
        Log.d("NoOpLauncher", "publishEpisodeProgress stub — no-op")
    }

    override suspend fun refreshRecentLive(profileId: Long, allowBrowsableRequest: Boolean) {
        Log.d("NoOpLauncher", "refreshRecentLive stub — no-op")
    }
}
