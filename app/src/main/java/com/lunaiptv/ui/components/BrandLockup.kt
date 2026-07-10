package com.lunaiptv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.lunaiptv.R
import com.lunaiptv.ui.theme.AccentSilver
import com.lunaiptv.ui.Theme.LunaIPtvTheme

/**
 * Theme-adaptive "LunaIPtv" wordmark. Silver moon accent for the play mark and "IPtv" suffix,
 * white "Luna" text on dark backgrounds.
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Int = 36,
    textSize: Int = 26,
) {
    val colors = OwnTVTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Rounded-square moon mark
        val markShape = RoundedCornerShape(percent = 28)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(markSize.dp)
                .clip(markShape)
                .background(colors.card)
                .border(2.dp, AccentSilver, markShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "LunaIPtv",
                modifier = Modifier
                    .padding(4.dp)
                    .size((markSize * 0.7f).dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = buildAnnotatedString {
                withStyle(androidx.compose.ui.text.SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold)) {
                    append("Luna")
                }
                withStyle(androidx.compose.ui.text.SpanStyle(color = AccentSilver, fontWeight = FontWeight.Bold)) {
                    append("IPtv")
                }
            },
            fontSize = textSize.sp,
        )
    }
}
