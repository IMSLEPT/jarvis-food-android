package com.jarvis.food.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.food.data.BodyMeasurements
import kotlin.math.min

/** Original flat vector artwork. Coordinates are shared by drawing, hit testing and labels. */
private data class BodySite(val key: String, val point: Offset, val halfWidth: Float, val label: Offset)
private val bodySites = listOf(
    BodySite("collo", Offset(180f, 89f), 15f, Offset(48f, 82f)),
    BodySite("petto", Offset(180f, 155f), 44f, Offset(312f, 145f)),
    BodySite("braccio", Offset(111f, 171f), 12f, Offset(48f, 174f)),
    BodySite("vita", Offset(180f, 215f), 35f, Offset(48f, 264f)),
    BodySite("fianchi", Offset(180f, 265f), 46f, Offset(312f, 252f)),
    BodySite("coscia", Offset(151f, 319f), 23f, Offset(312f, 355f))
)
// Relaxed front-facing silhouette, with separate arm/torso gaps and natural proportions.
private const val BODY_OUTLINE = "M166 75 C168 84 166 93 157 97 L130 104 C115 107 109 116 106 131 L98 169 C94 186 95 204 91 224 L89 247 L82 267 C79 276 80 286 84 291 C87 293 89 289 90 282 L95 272 L94 291 C95 297 100 297 102 291 L108 275 C111 266 108 255 106 248 L113 222 L118 201 L126 162 C130 181 137 195 139 213 C139 228 132 245 131 265 C130 282 132 299 134 316 L140 365 C143 378 139 390 138 404 C137 423 143 443 144 461 L143 480 L136 498 C134 502 138 506 145 506 L162 506 C167 505 167 500 164 496 L159 480 L160 458 L169 412 C171 397 167 385 169 371 L178 302 Q180 294 182 302 L191 371 C193 385 189 397 191 412 L200 458 L201 480 L196 496 C193 500 193 505 198 506 L215 506 C222 506 226 502 224 498 L217 480 L216 461 C217 443 223 423 222 404 C221 390 217 378 220 365 L226 316 C228 299 230 282 229 265 C228 245 221 228 221 213 C223 195 230 181 234 162 L242 201 L247 222 L254 248 C252 255 249 266 252 275 L258 291 C260 297 265 297 266 291 L265 272 L270 282 C271 289 273 293 276 291 C280 286 281 276 278 267 L271 247 L269 224 C265 204 266 186 262 169 L254 131 C251 116 245 107 230 104 L203 97 C194 93 192 84 194 75 C203 68 209 56 208 42 C208 23 198 12 180 12 C162 12 152 23 152 42 C151 56 157 68 166 75 Z"

@Composable
fun BodyDiagram(selectedKey: String, onSelect: (String) -> Unit) {
    val outline = remember { PathParser().parsePathString(BODY_OUTLINE).toPath() }
    val guide = BodyMeasurements.guide(selectedKey)
    val active = bodySites.firstOrNull { it.key == selectedKey }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LA TUA MAPPA CORPOREA", style = T.Small.copy(fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            Text("2D · FRONTE", style = T.Small.copy(fontSize = 10.sp, color = C.Navy))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F7FD), Color(0xFFFAFBFE))))
            .testTag("body-model-2d").semantics { stateDescription = "Sagoma 2D · ${guide.name}" }) {
            val scale = min(maxWidth.value / 360f, 380f / 520f)
            val startX = (maxWidth.value - 360f * scale) / 2f
            val startY = (380f - 520f * scale) / 2f
            Canvas(Modifier.fillMaxSize().pointerInput(onSelect) {
                detectTapGestures { tap ->
                    val p = Offset((tap.x / density - startX) / scale, (tap.y / density - startY) / scale)
                    // Only taps close to the drawn body/measurement sites choose a zone.
                    bodySites.minByOrNull { site ->
                        val dx = (p.x - site.point.x) / (site.halfWidth + 12f)
                        val dy = (p.y - site.point.y) / 22f
                        dx * dx + dy * dy
                    }?.let { site ->
                        val dx = (p.x - site.point.x) / (site.halfWidth + 12f)
                        val dy = (p.y - site.point.y) / 22f
                        if (dx * dx + dy * dy <= 2f) onSelect(site.key)
                    }
                }
            }) {
                val pixelScale = scale * density
                withTransform({ translate(startX * density, startY * density); scale(pixelScale, pixelScale, Offset.Zero) }) {
                    drawOval(Color(0xFFDDE5F4).copy(alpha = .5f), Offset(128f, 505f), Size(104f, 7f))
                    drawPath(outline, Brush.verticalGradient(listOf(Color(0xFFDCE5F4), Color(0xFFC3D1E8)), startY = 12f, endY = 506f))
                    drawPath(outline, C.Navy.copy(alpha = .24f), style = Stroke(1.3f))
                    if (selectedKey == "grasso") drawPath(outline, C.Accent.copy(alpha = .16f))
                    // Subtle internal contours keep the illustration flat and legible.
                    drawLine(C.Navy.copy(alpha = .10f), Offset(180f, 113f), Offset(180f, 192f), 1.2f, StrokeCap.Round)
                    drawLine(C.Navy.copy(alpha = .10f), Offset(143f, 154f), Offset(173f, 160f), 1.2f, StrokeCap.Round)
                    drawLine(C.Navy.copy(alpha = .10f), Offset(187f, 160f), Offset(217f, 154f), 1.2f, StrokeCap.Round)
                    bodySites.forEach { site ->
                        val chosen = site.key == selectedKey
                        val color = if (chosen) C.Accent else C.Navy.copy(alpha = .19f)
                        val left = Offset(site.point.x - site.halfWidth, site.point.y)
                        val right = Offset(site.point.x + site.halfWidth, site.point.y)
                        if (chosen) clipPath(outline) {
                            drawRect(C.Accent.copy(alpha = .21f), Offset(left.x - 3f, site.point.y - 9f), Size(site.halfWidth * 2f + 6f, 18f))
                        }
                        drawLine(color, left, right, if (chosen) 4f else 1.5f, StrokeCap.Round,
                            pathEffect = if (chosen) null else PathEffect.dashPathEffect(floatArrayOf(3f, 4f)))
                        val edge = if (site.label.x < 180f) left else right
                        val end = Offset(if (site.label.x < 180f) 88f else 272f, site.label.y)
                        drawLine(color, edge, end, if (chosen) 1.8f else 1f, StrokeCap.Round)
                        drawCircle(Color.White, if (chosen) 6.5f else 4f, edge)
                        drawCircle(if (chosen) C.Accent else Color(0xFF8F9EBB), if (chosen) 4f else 2.5f, edge)
                    }
                }
            }
            bodySites.forEach { site ->
                val chosen = site.key == selectedKey
                val title = BodyMeasurements.guide(site.key).name
                Box(Modifier.offset(x = (startX + site.label.x * scale).dp - 35.dp, y = (startY + site.label.y * scale).dp - 24.dp)
                    .width(70.dp).height(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (chosen) C.AccentSoft else Color.White.copy(alpha = .8f))
                    .border(1.dp, if (chosen) C.Accent.copy(alpha = .6f) else C.Line, RoundedCornerShape(14.dp))
                    .testTag("body-point-${site.key}")
                    .semantics { contentDescription = "Seleziona $title sulla sagoma"; selected = chosen }
                    .clickable(role = Role.RadioButton) { onSelect(site.key) }, contentAlignment = Alignment.Center) {
                    Text(title, style = T.Label.copy(fontSize = 11.sp, lineHeight = 14.sp, color = if (chosen) C.AccentDeep else C.Navy))
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.AccentSoft).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(C.Accent))
            Text(if (active != null) "${guide.name} · il riferimento è evidenziato in arancione" else "Massa grassa · inserisci una valutazione in percentuale", style = T.Small.copy(color = C.Navy), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
