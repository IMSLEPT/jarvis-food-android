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
    ACard(modifier.fillMaxWidth(), radius = 26.dp, padding = 14.dp, elevation = 8.dp,
        glow = Color(0x2009194D), onClick = onClick) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.size(64.dp).testTag("insight-icon-$art").clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(tint)), contentAlignment = Alignment.Center) {
                Image(painterResource(resource), null, Modifier.size(54.dp), contentScale = ContentScale.Fit)
            }
            GapW(12.dp)
            Column(Modifier.weight(1f), content = content)
        }
    }
}

@Composable
internal fun ReferenceInsightHeading(title: String, subtitle: String, icon: String, action: String, actionModifier: Modifier = Modifier, onAction: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val heading: @Composable (Modifier) -> Unit = { modifier ->
            Row(modifier, verticalAlignment = Alignment.Top) {
                AIcon(icon, RD.OrangeText, 15.dp)
                GapW(4.dp)
                Column {
                    Text(title, style = T.Title2.copy(fontSize = PremiumScale.label, lineHeight = PremiumScale.labelLine, color = RD.Ink))
                    Text(subtitle, style = T.Small.copy(fontSize = 11.sp, lineHeight = 14.sp, color = RD.Grey))
                }
            }
        }
        val button: @Composable () -> Unit = {
            Box(actionModifier.heightIn(min = 36.dp).clickable(role = Role.Button, onClick = onAction), contentAlignment = Alignment.Center) {
                Row(Modifier.clip(CircleShape).background(RD.OrangeBrush).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(action, style = T.Label.copy(fontSize = 11.sp, lineHeight = 14.sp, color = Color.White))
                    GapW(3.dp)
                    AIcon(RIc.Right, Color.White, 11.dp, 2.4f)
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
                    ProgressLine(progress ?: 0f, RD.Navy, barModifier.weight(1f), height = 5.dp)
                    GapW(6.dp)
                }
                Text(value, modifier = valueModifier.then(if (inline) Modifier.width(valueWidth) else Modifier),
                    style = style, textAlign = TextAlign.End)
                GapW(3.dp)
                AIcon(RIc.Right, RD.Ink, 12.dp, 2.4f)
            }
            if (!inline) ProgressLine(progress ?: 0f, RD.Navy, barModifier.fillMaxWidth().padding(top = 4.dp), height = 5.dp)
        }
    }
}

@Composable
internal fun JarvisChefBanner() {
    val large = LocalDensity.current.fontScale > 1.2f
    val shape = RoundedCornerShape(24.dp)
    Row(Modifier.fillMaxWidth().testTag("jarvis-chef-banner").aShadow(shape, 4.dp, Color(0x2609194D))
        .clip(shape).background(RD.NavyBrush)
        .clickable(role = Role.Button) { Nav.go(Screen.WhatNow) }
        .padding(start = 6.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = if (large) 64.dp else 88.dp, height = 96.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawOval(Brush.linearGradient(listOf(Color(0xFFFFA615), Color(0xFFC96712))),
                    Offset(-size.width * .08f, size.height * .76f), androidx.compose.ui.geometry.Size(size.width * 1.16f, size.height * .55f))
            }
            Image(painterResource(R.drawable.reference_jarvis_chef), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        GapW(8.dp)
        Column(Modifier.weight(1f)) {
            Text("Cosa mangio con Jarvis?", style = T.Title2.copy(color = Color.White, fontSize = 17.sp, lineHeight = 22.sp))
            GapH(5.dp)
            Text("Trova il piatto perfetto in base ai tuoi obiettivi di oggi.", style = T.Small.copy(color = Color.White, fontSize = 12.sp, lineHeight = 17.sp))
        }
        GapW(8.dp)
        Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFFFFB526), Color(0xFFFF8900))))
            .border(1.dp, Color(0xFFFFCA54), CircleShape), contentAlignment = Alignment.Center) {
            AIcon(Ic.Right, Color.White, 24.dp, 3f)
        }
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
