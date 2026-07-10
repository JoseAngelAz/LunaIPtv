package tv.own.owntv.features.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.features.shell.MainSection
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.NavAccentBar
import tv.own.owntv.ui.components.rememberNavLadderColors
import tv.own.owntv.ui.components.OwnTVAvatar
import tv.own.owntv.ui.components.NavDuotoneIcon
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.OwnTVTheme

/** Golden glow color for the floating tooltip. */
private val GoldGlow = Color(0xFFFFD700)

/** Resolve sidebar label for a section using string resources. */
@Composable
private fun sectionLabel(section: MainSection): String = when (section) {
    MainSection.HOME -> stringResource(R.string.sidebar_home)
    MainSection.LIVE_TV -> stringResource(R.string.sidebar_live_tv)
    MainSection.MOVIES -> stringResource(R.string.sidebar_movies)
    MainSection.SERIES -> stringResource(R.string.sidebar_series)
    MainSection.DOWNLOADS -> stringResource(R.string.sidebar_downloads)
    MainSection.EPG -> stringResource(R.string.sidebar_guide)
    MainSection.SETTINGS -> stringResource(R.string.sidebar_settings)
    MainSection.SEARCH -> stringResource(R.string.sidebar_search)
}

/**
 * Layer 1 — the MD3 navigation panel. A FIXED icon rail: brand logo at the top (Phase 2), the nav items
 * (browse + Settings) vertically centered in the middle (Phase 3), and the profile avatar pinned at the
 * bottom (Phase 1). The logo is display-only (not focusable); everything else is a focusable nav item.
 */
@Composable
fun Sidebar(
    selected: MainSection,
    onSelect: (MainSection) -> Unit,
    avatarId: Int,
    onPickAvatar: () -> Unit,
    profileName: String,
    sourceSummary: String,
    onSwitchProfile: () -> Unit,
    selectedItemFocusRequester: FocusRequester,
    onFocused: () -> Unit,
    counts: (MainSection) -> Int = { 0 },
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    var hasFocus by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val expanded = false
    val focusSection = if (selected == MainSection.SEARCH) MainSection.HOME else selected

    // Track which item is focused for the tooltip
    var focusedSection by remember { mutableStateOf<MainSection?>(null) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .onFocusChanged {
                val entered = it.hasFocus && !hasFocus
                hasFocus = it.hasFocus
                if (it.hasFocus) onFocused()
                if (entered) scope.launch { runCatching { selectedItemFocusRequester.requestFocus() } }
            }
            .focusGroup()
            .width(Dimens.SidebarWidthCollapsed)
            .background(colors.background)
            .padding(horizontal = 6.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Brand logo at the top
        AppLogo()
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MainSection.entries.filter { it.isBrowse }.forEach { section ->
                    NavItem(
                        section = section,
                        active = section == selected,
                        expanded = expanded,
                        count = counts(section),
                        onClick = { onSelect(section) },
                        onFocused = { focusedSection = section },
                        onClearFocus = { if (focusedSection == section) focusedSection = null },
                        modifier = if (section == focusSection) {
                            Modifier.focusRequester(selectedItemFocusRequester)
                        } else Modifier,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                NavItem(
                    section = MainSection.SETTINGS,
                    active = selected == MainSection.SETTINGS,
                    expanded = expanded,
                    count = 0,
                    onClick = { onSelect(MainSection.SETTINGS) },
                    onFocused = { focusedSection = MainSection.SETTINGS },
                    onClearFocus = { if (focusedSection == MainSection.SETTINGS) focusedSection = null },
                    modifier = if (focusSection == MainSection.SETTINGS) {
                        Modifier.focusRequester(selectedItemFocusRequester)
                    } else Modifier,
                )
            }
        }

        // Floating tooltip with golden glow — appears when a nav item is focused
        AnimatedVisibility(
            visible = focusedSection != null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it / 3 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 3 }),
        ) {
            focusedSection?.let { section ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldGlow.copy(alpha = 0.15f))
                        .border(1.dp, GoldGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = sectionLabel(section),
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Divider + profile at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(1.dp)
                .background(colors.outlineVariant),
        )
        ProfileCard(
            expanded = expanded,
            avatarId = avatarId,
            profileName = profileName,
            sourceSummary = sourceSummary,
            onPickAvatar = onPickAvatar,
            onSwitchProfile = onSwitchProfile,
        )
    }
}

/**
 * Brand mark at the top of the rail — the LunaIPtv moon logo. Decorative only: not focusable.
 */
@Composable
private fun AppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(width = 2.dp, color = OwnTVTheme.colors.primary, shape = RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "LunaIPtv",
            modifier = Modifier.size(72.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ProfileCard(
    expanded: Boolean,
    avatarId: Int,
    profileName: String,
    sourceSummary: String,
    onPickAvatar: () -> Unit,
    onSwitchProfile: () -> Unit,
) {
    val colors = OwnTVTheme.colors

    if (!expanded) {
        // Fixed nav: just the avatar — click opens the profile switcher ("who's watching"), long-press
        // changes the avatar picture. Pinned top-left, always in the same spot.
        AvatarButton(avatarId = avatarId, sizeDp = 56, onClick = onSwitchProfile, onLongClick = onPickAvatar)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(colors.primaryContainer.copy(alpha = 0.45f)),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarButton(avatarId = avatarId, sizeDp = 64, onClick = onPickAvatar)
            Spacer(Modifier.height(10.dp))
            Text(
                profileName.ifBlank { stringResource(R.string.sidebar_default_profile) },
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Text(
                sourceSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            // Switch profile without quitting the app (routes back to the "Who's watching?" gate).
            FocusableSurface(
                onClick = onSwitchProfile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                focusedContainerColor = colors.surfaceContainerHigh,
                unfocusedContainerColor = colors.surfaceContainer,
                contentAlignment = Alignment.Center,
            ) { focused ->
                val c = if (focused) colors.onSurface else colors.onSurfaceVariant
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OwnTVIcon(icon = OwnTVIcon.PERSON, tint = c, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.sidebar_switch_profile), style = MaterialTheme.typography.labelLarge, color = c, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AvatarButton(avatarId: Int, sizeDp: Int, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    FocusableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.size(sizeDp.dp),
        shape = CircleShape,
        focusedScale = 1.08f,
        focusedContainerColor = OwnTVTheme.colors.surfaceContainerHighest,
        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        selectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentAlignment = Alignment.Center,
    ) { _ ->
        OwnTVAvatar(avatarId = avatarId, modifier = Modifier.size((sizeDp - 4).dp))
    }
}

@Composable
private fun NavItem(
    section: MainSection,
    active: Boolean,
    expanded: Boolean,
    count: Int,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    onClearFocus: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val shape = RoundedCornerShape(17.dp)
    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.hasFocus) onFocused() else onClearFocus() },
        selected = active,
        shape = shape,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
        showFocusBorder = false,
        contentAlignment = Alignment.Center,
    ) { focused ->
        val ladder = rememberNavLadderColors(selected = active, focused = focused)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(ladder.container)
                .then(
                    if (ladder.focusBorder != null) Modifier.border(Dimens.FocusBorderWidth, ladder.focusBorder, shape)
                    else Modifier
                ),
        ) {
            NavAccentBar(visible = ladder.showAccentBar, height = 26.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (expanded) Arrangement.spacedBy(16.dp, Alignment.Start) else Arrangement.Center,
            ) {
                NavDuotoneIcon(
                    section = section,
                    color = ladder.icon,
                    modifier = Modifier.size(28.dp),
                )
                if (expanded) {
                    Text(
                        text = sectionLabel(section),
                        style = MaterialTheme.typography.titleMedium,
                        color = ladder.content,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val MainSection.navIcon: OwnTVIcon
    get() = when (this) {
        MainSection.SEARCH -> OwnTVIcon.SEARCH
        MainSection.HOME -> OwnTVIcon.HOME
        MainSection.LIVE_TV -> OwnTVIcon.LIVE_TV
        MainSection.MOVIES -> OwnTVIcon.MOVIES
        MainSection.SERIES -> OwnTVIcon.SERIES
        MainSection.DOWNLOADS -> OwnTVIcon.DOWNLOADS
        MainSection.EPG -> OwnTVIcon.EPG
        MainSection.SETTINGS -> OwnTVIcon.SETTINGS
    }

