package com.lunaiptv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lunaiptv.features.shell.MainSection

/**
 * Rank 1 — Neo Signal Duotone (Phase 7). SVG shapes from
 * extras/LunaIPtv_future_nav_icons_ranked.html, rendered on a crisp 100-unit Canvas grid
 * with clean integer coordinates so icons stay sharp at any UI zoom.
 */
@Composable
fun NavDuotoneIcon(
    section: MainSection,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val soft = color.copy(alpha = 0.43f)
        val fill = color
        val stroke = Stroke(width = 7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thin = Stroke(width = 5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        fun sz(w: Float, h: Float) = Size(w * s, h * s)

        fun poly(vararg pts: Float): Path = Path().apply {
            var i = 0; while (i < pts.size) { if (i == 0) moveTo(pts[0] * s, pts[1] * s) else lineTo(pts[i] * s, pts[i + 1] * s); i += 2 }
        }
        fun polyC(vararg pts: Float): Path = poly(*pts).apply { close() }

        fun DrawScope.rect(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, st: Stroke?) =
            drawRoundRect(c, o(x, y), sz(w, h), CornerRadius(r * s, r * s), style = st ?: Fill)

        fun DrawScope.dot(cx: Float, cy: Float, r: Float, c: Color) =
            drawCircle(c, radius = r * s, center = o(cx, cy))

        fun DrawScope.arc(c: Color, cx: Float, cy: Float, r: Float, startD: Float, sweepD: Float, st: Stroke) {
            val tl = o(cx - r, cy - r); drawArc(c, startD, sweepD, false, tl, sz(r * 2, r * 2), style = st)
        }

        when (section) {
            // ---- Home — minimal house silhouette + door ----------------------------
            MainSection.HOME -> {
                drawPath(polyC(50f,18f, 84f,48f, 84f,82f, 16f,82f, 16f,48f), soft)
                drawPath(poly(50f,18f, 84f,48f, 16f,48f), fill, style = stroke)
                drawPath(poly(16f,82f, 16f,48f), fill, style = stroke)
                drawPath(poly(84f,82f, 84f,48f), fill, style = stroke)
                rect(39f,58f, 22f,24f, 4f, fill, stroke)
            }

            // ---- Live TV — clean screen + stand + play ----------------------------
            MainSection.LIVE_TV -> {
                rect(16f,24f, 68f,42f, 8f, fill, stroke)
                drawPath(polyC(44f,38f, 44f,56f, 60f,47f), fill, style = Fill)
                drawPath(poly(36f,74f, 64f,74f), soft, style = stroke)
                drawPath(poly(50f,66f, 50f,74f), soft, style = stroke)
            }

            // ---- Movies — clean clapperboard + play -------------------------------
            MainSection.MOVIES -> {
                rect(17f,28f, 66f,46f, 8f, fill, stroke)
                drawPath(poly(17f,38f, 83f,38f), fill, style = stroke)
                drawPath(polyC(44f,52f, 44f,66f, 58f,59f), fill, style = Fill)
                drawPath(poly(24f,22f, 36f,28f), soft, style = thin)
                drawPath(poly(38f,22f, 50f,28f), soft, style = thin)
                drawPath(poly(52f,22f, 64f,28f), soft, style = thin)
            }

            // ---- Series — clean stacked cards + dots ------------------------------
            MainSection.SERIES -> {
                rect(22f,20f, 50f,34f, 7f, soft, stroke)
                rect(28f,34f, 50f,34f, 7f, fill, stroke)
                drawPath(poly(36f,46f, 62f,46f), fill, style = stroke)
                drawPath(poly(36f,56f, 54f,56f), fill, style = stroke)
                dot(36f,80f, 3f, fill)
                dot(50f,80f, 3f, soft)
                dot(64f,80f, 3f, soft)
            }

            // ---- Downloads — clean down arrow + tray ------------------------------
            MainSection.DOWNLOADS -> {
                drawPath(poly(50f,18f, 50f,56f), fill, style = stroke)
                drawPath(poly(36f,44f, 50f,58f, 64f,44f), fill, style = stroke)
                drawPath(poly(24f,72f, 24f,78f, 76f,78f, 76f,72f), soft, style = stroke)
                drawPath(poly(32f,84f, 68f,84f), soft, style = stroke)
            }

            // ---- Guide — clean grid + channel line --------------------------------
            MainSection.EPG -> {
                rect(17f,18f, 66f,64f, 8f, fill, stroke)
                drawPath(poly(33f,18f, 33f,82f), fill, style = stroke)
                drawPath(poly(50f,18f, 50f,82f), soft, style = stroke)
                drawPath(poly(17f,38f, 83f,38f), fill, style = stroke)
                drawPath(poly(17f,58f, 83f,58f), fill, style = stroke)
                drawPath(poly(17f,78f, 83f,78f), soft, style = stroke)
            }

            // ---- Settings — clean sliders + knobs ---------------------------------
            MainSection.SETTINGS -> {
                drawPath(poly(22f,30f, 56f,30f), fill, style = stroke)
                drawPath(poly(68f,30f, 80f,30f), fill, style = stroke)
                drawCircle(fill, radius = 6f * s, center = o(62f, 30f), style = stroke)
                drawPath(poly(22f,50f, 38f,50f), fill, style = stroke)
                drawPath(poly(50f,50f, 80f,50f), fill, style = stroke)
                drawCircle(fill, radius = 6f * s, center = o(44f, 50f), style = stroke)
                drawPath(poly(22f,70f, 62f,70f), fill, style = stroke)
                drawPath(poly(74f,70f, 80f,70f), fill, style = stroke)
                drawCircle(fill, radius = 6f * s, center = o(68f, 70f), style = stroke)
            }

            // ---- Search — clean magnifier ----------------------------------------
            MainSection.SEARCH -> {
                drawCircle(fill, radius = 22f * s, center = o(44f, 44f), style = stroke)
                drawPath(poly(60f,60f, 80f,80f), fill, style = stroke)
                drawPath(poly(34f,44f, 54f,44f), soft, style = stroke)
            }
        }
    }
}

/**
 * Rank 1 — Profile icon (Phase 7). Person silhouette + star accent on 100-grid.
 */
@Composable
fun ProfileIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val soft = color.copy(alpha = 0.43f)
        val fill = color
        val stroke = Stroke(width = 7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun o(x: Float, y: Float) = Offset(x * s, y * s)

        // Head — circle
        drawCircle(fill, radius = 13f * s, center = o(50f, 38f), style = stroke)
        // Body arc — cubic bezier
        val body = Path().apply {
            moveTo(24f * s, 81f * s)
            cubicTo(28f * s, 67f * s, 37f * s, 60f * s, 50f * s, 60f * s)
            cubicTo(63f * s, 60f * s, 72f * s, 67f * s, 76f * s, 81f * s)
        }
        drawPath(body, fill, style = stroke)
        // Star accent
        val star = Path().apply {
            moveTo(76f * s, 19f * s); lineTo(78f * s, 24f * s); lineTo(83f * s, 26f * s)
            lineTo(78f * s, 28f * s); lineTo(76f * s, 33f * s); lineTo(74f * s, 28f * s)
            lineTo(69f * s, 26f * s); lineTo(74f * s, 24f * s); close()
        }
        drawPath(star, soft, style = stroke)
    }
}
