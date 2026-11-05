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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.R

internal val InsightTile = Color(0xFFEEF2F8)

@Composable
internal fun ReferenceInsightBody(art: TodayArt, content: @Composable ColumnScope.() -> Unit) {
    val resource = when (art) {
        TodayArt.Vitamins -> R.drawable.approved_vitamins
        TodayArt.Calendar -> R.drawable.approved_daily_tips
        TodayArt.Activity -> R.drawable.approved_activity
        else -> null
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth / LocalDensity.current.fontScale < 258.dp
        val artwork: @Composable () -> Unit = {
            val illustration = Modifier.size(width = if (stacked) 60.dp else 90.dp, height = if (stacked) 66.dp else 112.dp)
                .testTag("insight-icon-$art")
            if (resource != null) Image(painterResource(resource), null, illustration, contentScale = ContentScale.Fit)
            else TodayArtwork(art, illustration)
        }
        if (stacked) {
            Column(Modifier.fillMaxWidth()) {
                artwork()
                GapH(8.dp)
                Column(Modifier.fillMaxWidth(), content = content)
            }
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            artwork()
            GapW(8.dp)
            Column(Modifier.weight(1f), content = content)
        }
    }
}

@Composable
internal fun ReferenceInsightHeading(title: String, subtitle: String, icon: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AIcon(icon, RD.OrangeText, 15.dp)
                GapW(4.dp)
                Text(title, modifier = Modifier.weight(1f), style = T.Title2.copy(fontSize = 15.sp, lineHeight = 18.sp, color = RD.Ink))
            }
            Text(subtitle, style = T.Small.copy(fontSize = 11.sp, lineHeight = 14.sp, color = RD.Grey))
        }
        GapW(4.dp)
        Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onAction), contentAlignment = Alignment.Center) {
            Row(Modifier.clip(CircleShape).background(RD.OrangeBrush).padding(horizontal = 7.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = T.Label.copy(fontSize = 11.sp, lineHeight = 14.sp, color = Color.White))
                GapW(3.dp)
                AIcon(RIc.Right, Color.White, 11.dp, 2.4f)
            }
        }
    }
}

@Composable
internal fun ReferenceMetric(label: String, value: String, progress: Float?, modifier: Modifier = Modifier,
    valueModifier: Modifier = Modifier, barModifier: Modifier = Modifier, onClick: () -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth().heightIn(min = 43.dp)
        .clickable(role = Role.Button, onClick = onClick).padding(vertical = 3.dp), contentAlignment = Alignment.CenterStart) {
        val inline = maxWidth / LocalDensity.current.fontScale >= 203.dp
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = T.Label.copy(fontSize = 12.sp, lineHeight = 15.sp, color = RD.Ink),
                    modifier = if (inline) Modifier.width(94.dp) else Modifier.weight(1f))
                if (inline) {
                    ProgressLine(progress ?: 0f, RD.Navy, barModifier.weight(1f), height = 6.dp)
                    GapW(6.dp)
                }
                Text(value, modifier = valueModifier, style = T.Label.copy(fontSize = 12.sp, lineHeight = 15.sp, color = RD.Ink))
                GapW(3.dp)
                AIcon(RIc.Right, RD.Ink, 12.dp, 2.4f)
            }
            if (!inline) ProgressLine(progress ?: 0f, RD.Navy, barModifier.fillMaxWidth().padding(top = 4.dp), height = 6.dp)
        }
    }
}

@Composable
internal fun JarvisChefBanner() {
    val large = LocalDensity.current.fontScale > 1.2f
    val shape = RoundedCornerShape(24.dp)
    Row(Modifier.fillMaxWidth().testTag("jarvis-chef-banner").aShadow(shape, 4.dp, Color(0x2609194D))
        .clip(shape).background(Brush.linearGradient(listOf(Color(0xFF09254B), Color(0xFF0B3B73), C.Navy)))
        .border(1.dp, Color.White, shape).clickable(role = Role.Button) { Nav.go(Screen.WhatNow) }
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
