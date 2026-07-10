package com.lunaiptv.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lunaiptv.features.home.HeroKind
import com.lunaiptv.features.home.HomeLiveRowMode
import com.lunaiptv.features.home.HomeRow
import com.lunaiptv.R
import com.lunaiptv.ui.components.OwnTVButton
import com.lunaiptv.ui.components.OwnTVButtonStyle
import com.lunaiptv.ui.components.OwnTVIcon
import com.lunaiptv.ui.components.roundedPanel
import com.lunaiptv.ui.Theme.LunaIPtvTheme

@Composable
fun HomeSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: HomeSettingsViewModel = koinViewModel()
    val settingsVm: SettingsViewModel = koinViewModel()
    val config by vm.config.collectAsStateWithLifecycle()
    val androidTvHomeEnabled by settingsVm.androidTvHomeEnabled.collectAsStateWithLifecycle()
    val tvHomeRefresh by settingsVm.tvHomeRefresh.collectAsStateWithLifecycle()
    val colors = OwnTVTheme.colors

    val firstFocus = remember { FocusRequester() }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedPanel()
            .focusProperties {
                onEnter = { runCatching { firstFocus.requestFocus() } }
            }
            .focusGroup()
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Text(stringResource(R.string.home_settings_title), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.home_settings_hide_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(stringResource(R.string.home_settings_sections), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.home_settings_sections_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }

            itemsIndexed(config.settingsRows, key = { _, row -> row.name }) { index, row ->
                HomeRowCard(
                    row = row,
                    hidden = row in config.hidden,
                    canMoveUp = index > 0,
                    canMoveDown = index < config.settingsRows.lastIndex,
                    onMoveUp = { vm.move(row, up = true) },
                    onMoveDown = { vm.move(row, up = false) },
                    onMoveTop = { vm.moveToEdge(row, top = true) },
                    onMoveBottom = { vm.moveToEdge(row, top = false) },
                    onToggleHidden = { vm.setRowHidden(row, row !in config.hidden) },
                    liveMode = when (row) {
                        HomeRow.RECENT_CHANNELS -> config.recentLiveMode
                        HomeRow.FAVORITE_CHANNELS -> config.favoriteLiveMode
                        else -> null
                    },
                    onToggleLiveMode = { mode -> vm.setLiveRowMode(row, mode.toggled()) },
                    firstButtonModifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }

            item {
                Spacer(Modifier.height(14.dp))
                GroupLabel(stringResource(R.string.home_settings_group_keep_watching))
            }

            item {
                Row2(
                    icon = OwnTVIcon.LIVE_TV,
                    title = stringResource(R.string.home_settings_live_channels),
                    desc = stringResource(R.string.home_settings_live_channels_desc),
                    chip = if (config.heroIncludeLive) stringResource(R.string.on) else stringResource(R.string.off),
                    primaryChip = config.heroIncludeLive,
                    onClick = { vm.setHeroInclude(HeroKind.LIVE, !config.heroIncludeLive) },
                )
            }
            item {
                Row2(
                    icon = OwnTVIcon.MOVIES,
                    title = stringResource(R.string.home_settings_movies),
                    desc = stringResource(R.string.home_settings_movies_desc),
                    chip = if (config.heroIncludeMovies) stringResource(R.string.on) else stringResource(R.string.off),
                    primaryChip = config.heroIncludeMovies,
                    onClick = { vm.setHeroInclude(HeroKind.MOVIES, !config.heroIncludeMovies) },
                )
            }
            item {
                Row2(
                    icon = OwnTVIcon.SERIES,
                    title = stringResource(R.string.home_settings_series),
                    desc = stringResource(R.string.home_settings_series_desc),
                    chip = if (config.heroIncludeSeries) stringResource(R.string.on) else stringResource(R.string.off),
                    primaryChip = config.heroIncludeSeries,
                    onClick = { vm.setHeroInclude(HeroKind.SERIES, !config.heroIncludeSeries) },
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                GroupLabel(stringResource(R.string.home_settings_group_android_tv))
            }
            item {
                Row2(
                    icon = OwnTVIcon.HISTORY,
                    title = stringResource(R.string.home_settings_android_tv),
                    desc = stringResource(R.string.home_settings_android_tv_desc),
                    chip = if (androidTvHomeEnabled) stringResource(R.string.on) else stringResource(R.string.off),
                    primaryChip = androidTvHomeEnabled,
                    onClick = { settingsVm.setAndroidTvHomeEnabled(!androidTvHomeEnabled) },
                )
            }
            if (androidTvHomeEnabled) {
                item {
                    Row2(
                        icon = OwnTVIcon.SHARE,
                    title = stringResource(R.string.home_settings_refresh),
                    desc = stringResource(R.string.home_settings_refresh_desc),
                    chip = when (tvHomeRefresh) {
                        SettingsViewModel.TvHomeRefresh.REFRESHING -> stringResource(R.string.home_settings_rebuilding)
                        SettingsViewModel.TvHomeRefresh.DONE -> stringResource(R.string.home_settings_done)
                        else -> null
                    },
                        onClick = {
                            if (tvHomeRefresh == SettingsViewModel.TvHomeRefresh.IDLE) {
                                settingsVm.refreshAndroidTvHome()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRowCard(
    row: HomeRow,
    hidden: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
    onToggleHidden: () -> Unit,
    liveMode: HomeLiveRowMode?,
    onToggleLiveMode: (HomeLiveRowMode) -> Unit,
    firstButtonModifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (hidden) colors.onSurfaceVariant else colors.onSurface,
                maxLines = 1,
            )
            if (hidden) {
                Text(
                    stringResource(R.string.home_settings_hidden),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (liveMode != null) {
            OwnTVButton(
                label = stringResource(R.string.home_settings_mode, liveMode.label),
                onClick = { onToggleLiveMode(liveMode) },
                style = OwnTVButtonStyle.SECONDARY,
            )
            Spacer(Modifier.width(6.dp))
        }
        OwnTVButton("⤒", onClick = onMoveTop, style = OwnTVButtonStyle.SECONDARY, enabled = canMoveUp)
        Spacer(Modifier.width(6.dp))
        OwnTVButton("↑", onClick = onMoveUp, style = OwnTVButtonStyle.SECONDARY, enabled = canMoveUp)
        Spacer(Modifier.width(6.dp))
        OwnTVButton("↓", onClick = onMoveDown, style = OwnTVButtonStyle.SECONDARY, enabled = canMoveDown)
        Spacer(Modifier.width(6.dp))
        OwnTVButton("⤓", onClick = onMoveBottom, style = OwnTVButtonStyle.SECONDARY, enabled = canMoveDown)
        Spacer(Modifier.width(6.dp))
        OwnTVButton(
            label = if (hidden) stringResource(R.string.home_settings_show) else stringResource(R.string.hide),
            onClick = onToggleHidden,
            modifier = firstButtonModifier,
            style = OwnTVButtonStyle.SECONDARY,
        )
    }
}
