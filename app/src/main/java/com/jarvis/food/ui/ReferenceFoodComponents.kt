package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.R
import kotlin.math.roundToInt

internal val InsightTile = Color(0xFFEEF2F8)

@Composable
internal fun ReferenceInsightCard(
    art: TodayArt, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resource = when (art) {
        TodayArt.Vitamins -> R.drawable.insight_vitamins
        TodayArt.Calendar -> R.drawable.insight_daily_tips
        TodayArt.Activity -> R.drawable.insight_activity
        else -> error("Unsupported insight artwork: $art")
    }
    val tint = when (art) {
        TodayArt.Vitamins -> listOf(Color(0xFFFFE4D0), Color(0xFFFFF4EA))
        TodayArt.Calendar -> listOf(Color(0xFFDDE7FA), Color(0xFFF0F5FF))
        else -> listOf(Color(0xFFD5F2E6), Color(0xFFEFFAF5))
    }
    ACard(modifier.fillMaxWidth(), padding = 16.dp, onClick = onClick) {
        Column(Modifier.fillMaxWidth().testTag("insight-icon-$art"), content = content)
    }
}

@Composable
internal fun ReferenceInsightHeading(title: String, subtitle: String, icon: String, action: String, actionModifier: Modifier = Modifier, onAction: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val heading: @Composable (Modifier) -> Unit = { modifier ->
            Row(modifier, verticalAlignment = Alignment.Top) {
                val tone = when (icon) { RIc.Leaf -> Tone.Green; RIc.Bulb -> Tone.Gold; RIc.DumbbellBold -> Tone.Blue; else -> Tone.Orange }
                GlowIcon(icon, tone.first, tone.second, 40.dp, 13.dp, glyph = 23.dp)
                GapW(10.dp)
                Column {
                    Text(title, style = T.Title2.copy(fontSize = 16.sp, lineHeight = 21.sp, color = RD.Ink))
                    Text(subtitle, style = T.Small.copy(fontSize = 12.sp, lineHeight = 16.sp, color = RD.Grey))
                }
            }
        }
        val button: @Composable () -> Unit = {
            Box(actionModifier.heightIn(min = 36.dp).clickable(role = Role.Button, onClick = onAction), contentAlignment = Alignment.Center) {
                Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF1FF)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(action, style = T.Label.copy(fontSize = 13.sp, lineHeight = 16.sp, color = Color(0xFF1B4FD6)))
                    GapW(3.dp)
                    AIcon(RIc.Right, Color(0xFF1B4FD6), 13.dp, 2.4f)
                }
            }
        }
        if (maxWidth / LocalDensity.current.fontScale < 192.dp) {
            Column {
                heading(Modifier.fillMaxWidth())
                Box(Modifier.align(Alignment.End)) { button() }
            }
        } else Row(verticalAlignment = Alignment.CenterVertically) {
            heading(Modifier.weight(1f))
            GapW(4.dp)
            button()
        }
    }
}

@Composable
internal fun ReferenceMetric(label: String, value: String, progress: Float?, modifier: Modifier = Modifier,
    valueModifier: Modifier = Modifier, barModifier: Modifier = Modifier, onClick: () -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth().heightIn(min = 30.dp)
        .clickable(role = Role.Button, onClick = onClick).padding(vertical = 2.dp), contentAlignment = Alignment.CenterStart) {
        val style = T.Label.copy(fontSize = 12.sp, lineHeight = 15.sp, color = RD.Ink)
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val percentage = value.endsWith('%') || value == "—"
        val labels = if (percentage) listOf("Vitamina B12") else listOf("Calorie attive", "Allenamento")
        val labelWidth = with(density) {
            labels.maxOf { measurer.measure(it, style).size.width }.toDp() + 5.dp
        }.coerceAtLeast(76.dp)
        val valueWidth = with(density) {
            val example = if (percentage) "100%" else "9999 kcal"
            maxOf(measurer.measure(example, style).size.width, measurer.measure(value, style).size.width).toDp() + 2.dp
        }
        val inline = maxWidth >= labelWidth + valueWidth + 42.dp
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = style, modifier = if (inline) Modifier.width(labelWidth) else Modifier.weight(1f))
                if (inline) {
                    SoftBar(progress ?: 0f, barModifier.weight(1f))
                    GapW(6.dp)
                }
                Text(value, modifier = valueModifier.then(if (inline) Modifier.width(valueWidth) else Modifier),
                    style = style, textAlign = TextAlign.End)
                GapW(3.dp)
                AIcon(RIc.Right, RD.Ink, 12.dp, 2.4f)
            }
            if (!inline) SoftBar(progress ?: 0f, barModifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
internal fun JarvisChefBanner() {
    PromoBanner("Cosa mangio con Jarvis?", "Trova il piatto perfetto in base ai tuoi obiettivi di oggi.", "Scopri",
        Modifier.testTag("jarvis-chef-banner"), onClick = { Nav.go(Screen.WhatNow) }) {
        Image(painterResource(R.drawable.jarvis_logo), null, Modifier.size(112.dp).align(Alignment.Center), contentScale = ContentScale.Fit)
    }
}

/** Two-tone line icons, scoped to the plan meal selector. */
@Composable
internal fun ReferenceMealIcon(meal: Int, selected: Boolean, modifier: Modifier = Modifier) {
    val ink = if (selected) Color.White else C.Navy
    Box(modifier, contentAlignment = Alignment.Center) {
        AIcon(when (meal) { 0 -> "M3 10h14v4a6 6 0 0 1-6 6H9a6 6 0 0 1-6-6zM17 11h2a3 3 0 0 1 0 6h-2M6 6c-2-2 2-2 0-4M10 6c-2-2 2-2 0-4M14 6c-2-2 2-2 0-4"; 1 -> Ic.MealLunch; 2, 5 -> Ic.MealDinner; else -> Ic.MealSnack }, ink, 28.dp, 1.9f)
        Canvas(Modifier.size(30.dp)) {
            if (meal == 0) drawLine(DashboardOrange, Offset(size.width * .48f, size.height * .04f), Offset(size.width * .48f, size.height * .22f), 2.dp.toPx())
            else drawCircle(DashboardOrange, 2.dp.toPx(), Offset(size.width * .72f, size.height * .22f))
        }
    }
}

/** Filled ascending bars used by both reference summary controls. */
@Composable
internal fun ReferenceChartIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        listOf(3f to 13f, 10f to 8f, 17f to 3f).forEach { (x, y) ->
            drawRoundRect(DashboardOrange, Offset(x * u, y * u),
                androidx.compose.ui.geometry.Size(4f * u, (21f - y) * u), androidx.compose.ui.geometry.CornerRadius(1.4f * u))
        }
    }
}

internal const val ReferenceCutlery = "M4 3v6c0 4 7 4 7 0V3M7.5 3v18M19 3c-4 0-4 9 0 9h1V3zM20 12v9"

/** Tessera micronutriente: anello colorato con la percentuale al centro, nome e apporto sotto. */
@Composable
internal fun NutrientRing(name: String, percent: String, fraction: Float, tone: Color, sub: String,
    modifier: Modifier = Modifier, valueModifier: Modifier = Modifier, onClick: () -> Unit) {
    val a by androidx.compose.animation.core.animateFloatAsState(fraction.coerceIn(0f, 1f),
        androidx.compose.animation.core.tween(800), label = "ring")
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF6F8FC))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val st = 5.dp.toPx()
                val tl = Offset(st / 2, st / 2)
                val sz = androidx.compose.ui.geometry.Size(size.width - st, size.height - st)
                drawArc(tone.copy(alpha = 0.16f), 0f, 360f, false, tl, sz, style = Stroke(st))
                if (a > 0f) drawArc(tone, -90f, 360f * a, false, tl, sz, style = Stroke(st, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            Text(percent, valueModifier, style = T.Label.copy(fontSize = 14.sp, lineHeight = 17.sp, color = RD.Ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
        GapH(8.dp)
        Text(name, style = T.Label.copy(fontSize = 13.sp, lineHeight = 16.sp, color = RD.Ink), maxLines = 1, textAlign = TextAlign.Center)
        Text(sub, style = T.Small.copy(fontSize = 11.sp, lineHeight = 14.sp, color = RD.Grey), maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Barra a pillola: binario azzurro chiaro, riempimento sfumato blu, pallino bianco sulla punta. */
@Composable
internal fun SoftBar(fraction: Float, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 9.dp) {
    val a by androidx.compose.animation.core.animateFloatAsState(fraction.coerceIn(0f, 1f),
        androidx.compose.animation.core.tween(700), label = "softbar")
    Canvas(modifier.height(height)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2.5f)
        drawRoundRect(Color(0xFFE3EEFA), cornerRadius = r)
        if (a > 0f) {
            val w = (size.width * a).coerceAtLeast(size.height)
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF52C8F5), Color(0xFF1E6FE0)), 0f, w),
                size = androidx.compose.ui.geometry.Size(w, size.height), cornerRadius = r)
        }
    }
}
