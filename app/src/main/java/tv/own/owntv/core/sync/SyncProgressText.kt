package tv.own.owntv.core.sync

import android.content.Context
import tv.own.owntv.R
import tv.own.owntv.core.model.SourceType

data class SyncProgressCounts(
    val live: Int,
    val movies: Int,
    val series: Int,
    val liveActive: Boolean,
    val moviesActive: Boolean,
    val seriesActive: Boolean,
) {
    val hasItems: Boolean
        get() = live > 0 || movies > 0 || series > 0

    fun label(context: Context): String = buildList {
        if (liveActive && live > 0) add(context.getString(R.string.sync_count_channels, syncCount(live)))
        if (moviesActive && movies > 0) add(context.getString(R.string.sync_count_movies, syncCount(movies)))
        if (seriesActive && series > 0) add(context.getString(R.string.sync_count_series, syncCount(series)))
    }.joinToString(" · ")
}

data class SyncProgressDisplay(
    val title: String,
    val primaryText: String,
    val detail: String,
)

fun ImportStage.progressCounts(): SyncProgressCounts = SyncProgressCounts(
    live = liveProcessed,
    movies = moviesProcessed,
    series = seriesProcessed,
    liveActive = liveActive,
    moviesActive = moviesActive,
    seriesActive = seriesActive,
)

fun ImportStage.importProgressDisplay(context: Context): SyncProgressDisplay =
    importProgressDisplay(context, progressCounts())

fun syncProgressCountsForSource(
    sourceType: SourceType,
    liveProcessed: Int,
    moviesProcessed: Int,
    seriesProcessed: Int,
    liveActive: Boolean,
    moviesActive: Boolean,
    seriesActive: Boolean,
): SyncProgressCounts = when (sourceType) {
    SourceType.M3U -> SyncProgressCounts(
        live = liveProcessed,
        movies = 0,
        series = 0,
        liveActive = true,
        moviesActive = false,
        seriesActive = false,
    )
    SourceType.XTREAM -> {
        val hasActivePhase = liveActive || moviesActive || seriesActive
        SyncProgressCounts(
            live = liveProcessed,
            movies = moviesProcessed,
            series = seriesProcessed,
            liveActive = if (hasActivePhase) liveActive else true,
            moviesActive = if (hasActivePhase) moviesActive else true,
            seriesActive = if (hasActivePhase) seriesActive else true,
        )
    }
    SourceType.LOCAL_BACKUP -> SyncProgressCounts(
        live = 0,
        movies = 0,
        series = 0,
        liveActive = false,
        moviesActive = false,
        seriesActive = false,
    )
}

fun importProgressDisplay(context: Context, counts: SyncProgressCounts?): SyncProgressDisplay {
    val label = counts?.label(context).orEmpty()
    val primaryText = label.ifBlank { context.getString(R.string.sync_preparing) }
    return SyncProgressDisplay(
        title = context.getString(R.string.sync_importing),
        primaryText = primaryText,
        detail = if (counts?.hasItems == true) context.getString(R.string.sync_syncing) else context.getString(R.string.sync_connecting),
    )
}

fun resyncBadgeText(context: Context, baseItemCount: Int, totalProcessed: Int): String =
    if (baseItemCount > 0 && totalProcessed > 0) {
        context.getString(R.string.sync_badge_percent, ((totalProcessed * 100L) / baseItemCount).coerceIn(1, 99).toInt())
    } else {
        context.getString(R.string.sync_badge)
    }

fun syncProgressDisplay(context: Context, counts: SyncProgressCounts?): SyncProgressDisplay =
    SyncProgressDisplay(
        title = context.getString(R.string.sync_importing),
        primaryText = counts?.label(context)?.ifBlank { null } ?: context.getString(R.string.sync_preparing),
        detail = if (counts?.hasItems == true) context.getString(R.string.sync_syncing) else context.getString(R.string.sync_connecting),
    )

fun syncProgressCountsLabel(context: Context, counts: SyncProgressCounts): String? =
    counts.takeIf { it.hasItems }?.label(context)?.ifBlank { null }

private fun syncCount(count: Int): String = when {
    count >= 1_000_000 -> scaledCount(count / 1_000_000.0, "M")
    count >= 1_000 -> scaledCount(count / 1_000.0, "K")
    else -> count.toString()
}

private fun scaledCount(value: Double, suffix: String): String =
    "%.1f".format(value).removeSuffix(".0") + suffix
