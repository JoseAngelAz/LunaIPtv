package com.lunaiptv.core.launcher

/**
 * App-facing launcher repository. It keeps the public methods in one place while the old TV Provider
 * publisher remains the implementation detail for now.
 */
class LauncherIntegrationRepository(
    private val planner: LauncherRecommendationPlanner,
    private val resolver: LauncherLaunchResolver,
    private val tvHomeRepository: com.lunaiptv.core.tv.TvHomeRepository,
) : LauncherProfilePublisher {
    suspend fun buildSnapshot(profileId: Long): LauncherSnapshot = planner.buildSnapshot(profileId)

    override suspend fun refreshProfile(profileId: Long, allowBrowsableRequest: Boolean) =
        tvHomeRepository.refreshProfile(profileId, allowBrowsableRequest)

    override suspend fun clearProfile(profileId: Long) = tvHomeRepository.clearProfile(profileId)

    override suspend fun publishMovieProgress(profileId: Long, movieId: Long, positionMs: Long, durationMs: Long) =
        tvHomeRepository.publishMovieProgress(profileId, movieId, positionMs, durationMs)

    override suspend fun publishEpisodeProgress(profileId: Long, episodeId: Long, positionMs: Long, durationMs: Long) =
        tvHomeRepository.publishEpisodeProgress(profileId, episodeId, positionMs, durationMs)

    override suspend fun refreshRecentLive(profileId: Long, allowBrowsableRequest: Boolean) =
        tvHomeRepository.refreshRecentLive(profileId, allowBrowsableRequest)

    suspend fun resolveLaunch(profileId: Long, deepLink: LauncherDeepLink): LauncherLaunch? =
        resolver.resolveLaunch(profileId, deepLink)
}
