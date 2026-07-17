package com.lunaiptv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lunaiptv.ui.theme.LunaIPtvTheme
import kotlinx.coroutines.delay

/** A focusable poster tile for the Movies/Series grids: poster, title, rating, resume bar, fav star. */
@Composable
fun PosterCard(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    rating: Double? = null,
    progressFraction: Float? = null,
    completed: Boolean = false,
    isFavorite: Boolean = false,
    selected: Boolean = false,
    plot: String? = null,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = LunaIPtvTheme.colors
    var isFocused by remember { mutableStateOf(false) }
    var showPlot by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused, plot) {
        if (isFocused && !plot.isNullOrBlank()) {
            delay(3000)
            showPlot = true
        } else {
            showPlot = false
        }
    }

    FocusableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.onFocusChanged { state ->
            val wasFocused = isFocused
            isFocused = state.hasFocus
            if (state.hasFocus) onFocus()
            if (!state.hasFocus) showPlot = false
        },
        selected = selected,
        shape = RoundedCornerShape(14.dp),
        focusedScale = 1f,
        glowElevation = 0,
        focusedContainerColor = colors.surfaceContainerHigh,
        unfocusedContainerColor = colors.surfaceContainerHigh,
        selectedContainerColor = colors.surfaceContainerHigh,
        contentAlignment = Alignment.Center,
    ) { focused ->
        Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3.2f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceContainerLowest),
            ) {
                if (showPlot && !plot.isNullOrBlank()) {
                    AutoScrollSynopsisBox(plot)
                } else if (!posterUrl.isNullOrBlank()) {
                    AsyncImage(model = posterUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LunaIPtvIcon(LunaIPtvIcon.MOVIES, tint = colors.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    }
                }

                if (rating != null && rating > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LunaIPtvIcon(LunaIPtvIcon.STAR, tint = colors.accent, filled = true, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(formatRating(rating), style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }

                if (completed) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                    }
                }

                if (isFavorite) {
                    LunaIPtvIcon(
                        LunaIPtvIcon.STAR,
                        tint = colors.favorite,
                        filled = true,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp),
                    )
                }

                if (progressFraction != null && progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.4f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .height(4.dp)
                                .background(colors.primary),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = if (focused) colors.primary else colors.onSurface,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatRating(rating: Double): String = "%.1f".format(rating)

@Composable
private fun AutoScrollSynopsisBox(plot: String) {
    val colors = LunaIPtvTheme.colors
    val scrollState = rememberScrollState()
    val textKey = remember(plot) { plot.hashCode() }
    LaunchedEffect(textKey) {
        delay(300)
        val maxScroll = scrollState.maxValue
        if (maxScroll <= 0) return@LaunchedEffect
        val speed = 25
        val pauseMs = 2500L
        val tweenDuration = (maxScroll.toFloat() / speed * 1000f).toInt().coerceAtLeast(500)
        val easing = LinearEasing
        while (true) {
            scrollState.animateScrollTo(maxScroll, animationSpec = tween(tweenDuration, easing = easing))
            delay(pauseMs)
            scrollState.scrollTo(0)
            delay(pauseMs)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(6.dp),
    ) {
        Column(Modifier.verticalScroll(scrollState)) {
            Text(
                plot,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }
    }
}
