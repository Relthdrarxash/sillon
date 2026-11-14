package fr.pilmil.sillon.ui.garden

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.Vegetable
import fr.pilmil.sillon.ui.theme.FrauncesFontFamily
import fr.pilmil.sillon.ui.theme.SillonPalette
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Vue « potager vivant » : un carré en bois posé dans un jardin, vu en perspective (caméra de GardenProjection).
 * Tout est dessiné à la main dans un Canvas, du fond vers l'avant, sans effet GPU, pour que le rendu soit le même
 * sur l'appareil et dans les captures Paparazzi.
 */

private const val SCENE_RATIO = 1.15f // hauteur = largeur x 1,15
private const val NEAR_EDGE = 0.83f // part de la hauteur où tombe le bord proche du carré
private const val WALL = 0.05f // épaisseur des planches (m)
private const val FRAME_H = 0.25f // hauteur du cadre (m)
private const val SOIL_SINK = 0.06f // retrait de la terre sous le rebord (m)
private const val FAR_GROUND = 2.3f // profondeur (m) où la pelouse disparaît derrière les collines

// Éléments de décor qui ne correspondent à aucune teinte de SillonPalette.
private val GlowCream = Color(0xFFFFF1B8)
private val GlowGreen = Color(0xFF8FE07A)
private val GlowAmber = Color(0xFFFFB347)

private class Look(val dark: Boolean) {
    private val wash = Color(0xFF14223A)

    /** Assombrit et refroidit une teinte du jour pour la nuit (jamais noir : on tire vers un bleu nuit). */
    fun g(c: Color, amount: Float = 0.5f) = if (dark) lerp(c, wash, amount) else c

    val fogColor = if (dark) Color(0xFF2A4452) else Color(0xFFEAE6CF)
    fun fog(c: Color, t: Float) = if (t > 0f) lerp(c, fogColor, t) else c

    val skyTop = if (dark) Color(0xFF0E1B33) else Color(0xFF9FD0E0)
    val skyMid = if (dark) Color(0xFF173149) else Color(0xFFCBE6E0)
    val skyHorizon = if (dark) Color(0xFF29493F) else Color(0xFFF4E8CB)
    val hillFar = if (dark) Color(0xFF1E3A44) else Color(0xFFA6CBA8)
    val hillNear = if (dark) Color(0xFF1B3639) else Color(0xFF8CBB8A)
    val lawnFar = if (dark) Color(0xFF28463F) else Color(0xFFC6DC9F)
    val lawnMid = if (dark) Color(0xFF244438) else Color(0xFFA2CB6F)
    val lawnNear = if (dark) Color(0xFF1D3A2B) else Color(0xFF5FA03A)
    val grass = if (dark) Color(0xFF2F5A3C) else Color(0xFF3F8A32)
    val pathLight = if (dark) Color(0xFF565861) else Color(0xFFE9D9B6)
    val pathEdge = if (dark) Color(0xFF3B3E48) else Color(0xFFC9B085)
}

private class Placement(
    val index: Int,
    val parcel: Parcel,
    val fp: ParcelFootprint,
    val scale: Float,
    val selected: Boolean,
) {
    val frameH = FRAME_H * scale
    val soilY = (FRAME_H - SOIL_SINK) * scale
}

private class SceneLayout(val proj: GardenProjection, val placements: List<Placement>, val horizonY: Float)

/** Parcelles de l'arrière-plan : (centre x, début z, échelle) ; la sélectionnée prend le premier plan, centrée. */
private val BackSlots = listOf(Triple(-0.72f, 1.5f, 0.46f), Triple(0.72f, 1.5f, 0.46f), Triple(0f, 1.8f, 0.34f))

private fun footprintFor(parcel: Parcel, centerX: Float, z0: Float, scale: Float): ParcelFootprint {
    val cell = scale * 1.1f / max(1, max(parcel.rows, parcel.cols))
    val wall = WALL * scale
    val width = parcel.cols * cell + 2 * wall
    return ParcelFootprint(centerX - width / 2f, z0, parcel.rows, parcel.cols, cell, wall)
}

private fun buildLayout(width: Float, height: Float, parcels: List<Parcel>, selectedIndex: Int): SceneLayout {
    val proj = GardenProjection.forScene(width, height, nearEdgeY = height * NEAR_EDGE)
    val placements = ArrayList<Placement>()
    parcels.indices.filter { it != selectedIndex }.take(BackSlots.size).forEachIndexed { n, index ->
        val (cx, z0, s) = BackSlots[n]
        placements += Placement(index, parcels[index], footprintFor(parcels[index], cx, z0, s), s, false)
    }
    parcels.getOrNull(selectedIndex)?.let {
        placements += Placement(selectedIndex, it, footprintFor(it, 0f, 0f, 1f), 1f, true)
    }
    // De la plus lointaine à la plus proche pour le recouvrement.
    placements.sortByDescending { it.fp.z0 }
    return SceneLayout(proj, placements, proj.project(0f, 0f, FAR_GROUND).y)
}

@Composable
fun GardenScene(
    parcels: List<Parcel>,
    selectedIndex: Int,
    vegetables: List<Vegetable>,
    today: LocalDate,
    selectedCell: Pair<Int, Int>?,
    onSelectParcel: (Int) -> Unit,
    onCellClick: (row: Int, col: Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    windEnabled: Boolean = false,
) {
    val windPhase = if (windEnabled) {
        val transition = rememberInfiniteTransition(label = "gardenWind")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "windPhase",
        )
        phase
    } else {
        0f
    }
    val measurer = rememberTextMeasurer()
    val byId = remember(vegetables) { vegetables.associateBy { it.id } }
    val look = remember(isDark) { Look(isDark) }
    val currentCellClick by rememberUpdatedState(onCellClick)
    val currentSelectParcel by rememberUpdatedState(onSelectParcel)
    val selected = parcels.getOrNull(selectedIndex)
    val description = if (selected == null) {
        "Aucune parcelle à afficher."
    } else {
        "${selected.name}, ${selected.rows * selected.cols} cases dont " +
            "${selected.slots.count { it.vegetableId != null }} plantées. Touchez une case pour la modifier."
    }
    val neighborhood = remember(selected, vegetables) { selected?.let { neighborhoodOf(it, vegetables) } ?: emptyMap() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / SCENE_RATIO)
            .semantics { contentDescription = description }
            .pointerInput(parcels, selectedIndex) {
                detectTapGestures { pos ->
                    val layout = buildLayout(size.width.toFloat(), size.height.toFloat(), parcels, selectedIndex)
                    layout.placements.firstOrNull { it.selected }?.let { pl ->
                        val hit = layout.proj.unprojectToPlane(pos, pl.soilY)
                        val cell = hit?.let { pl.fp.cellAt(it.first, it.second) }
                        if (cell != null) {
                            currentCellClick(cell.first, cell.second)
                            return@detectTapGestures
                        }
                    }
                    // Arrière-plan : la parcelle la plus proche sous le doigt, testée au niveau du rebord.
                    for (pl in layout.placements.filter { !it.selected }.sortedBy { it.fp.z0 }) {
                        val hit = layout.proj.unprojectToPlane(pos, pl.frameH) ?: continue
                        if (pl.fp.containsOuter(hit.first, hit.second, 0.06f)) {
                            currentSelectParcel(pl.index)
                            return@detectTapGestures
                        }
                    }
                }
            },
    ) {
        val layout = buildLayout(size.width, size.height, parcels, selectedIndex)
        drawBackdrop(layout, look)
        drawGround(layout, look)
        for (pl in layout.placements) {
            drawParcel(layout.proj, look, pl, byId, today, neighborhood, selectedCell, measurer, windPhase)
        }
    }
}

// --- Décor -----------------------------------------------------------------------------------------------------

private fun polygon(pts: List<Offset>): Path = Path().apply {
    moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    close()
}

private fun polygon(a: Offset, b: Offset, c: Offset, d: Offset) = polygon(listOf(a, b, c, d))

private fun polygon(a: Offset, b: Offset, c: Offset, d: Offset, e: Offset, f: Offset, g: Offset, h: Offset) =
    polygon(listOf(a, b, c, d, e, f, g, h))

private fun hash01(a: Int, b: Int): Float {
    var h = a * 374761393 + b * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

private fun DrawScope.drawBackdrop(layout: SceneLayout, look: Look) {
    val w = size.width
    val hz = layout.horizonY
    drawRect(
        Brush.verticalGradient(listOf(look.skyTop, look.skyMid, look.skyHorizon), startY = 0f, endY = hz + 8f),
        size = Size(w, hz + 8f),
    )
    if (look.dark) {
        for (i in 0 until 26) {
            val x = hash01(i, 1) * w
            val y = hash01(i, 2) * hz * 0.8f
            drawCircle(Color.White.copy(alpha = 0.25f + 0.45f * hash01(i, 3)), 1.2f + 1.8f * hash01(i, 4), Offset(x, y))
        }
        val moon = Offset(w * 0.82f, hz * 0.34f)
        drawCircle(Brush.radialGradient(listOf(Color(0x40F4F0D8), Color(0x00F4F0D8)), moon, w * 0.09f), w * 0.09f, moon)
        drawCircle(Color(0xFFF2EED8), w * 0.026f, moon)
        drawCircle(Color(0xFF14223A), w * 0.022f, Offset(moon.x + w * 0.012f, moon.y - w * 0.006f))
    } else {
        for ((i, cx) in listOf(0.2f, 0.62f, 0.9f).withIndex()) {
            val c = Offset(w * cx, hz * (0.28f + 0.15f * (i % 2)))
            val cw = w * (0.09f + 0.02f * i)
            for ((dx, dy, s) in listOf(Triple(-0.6f, 0.1f, 0.7f), Triple(0f, -0.1f, 1f), Triple(0.65f, 0.1f, 0.75f))) {
                drawOval(Color.White.copy(alpha = 0.55f), Offset(c.x + dx * cw - cw * s * 0.6f, c.y + dy * cw * 0.5f - cw * s * 0.22f), Size(cw * s * 1.2f, cw * s * 0.44f))
            }
        }
    }
    // Deux bandes de collines, la lointaine plus proche de la brume (perspective atmosphérique).
    fun hill(baseY: Float, amp: Float, freq: Float, phase: Float, color: Color) {
        val path = Path().apply {
            moveTo(0f, hz + 8f)
            var x = 0f
            while (x <= w + 6f) {
                val t = x / w * 2f * PI.toFloat()
                lineTo(x, baseY - amp * (0.6f * sin(t * freq + phase) + 0.4f * sin(t * freq * 2.3f + phase * 2f)))
                x += 6f
            }
            lineTo(w + 6f, hz + 8f)
            close()
        }
        drawPath(path, color)
    }
    hill(hz - w * 0.055f, w * 0.04f, 0.9f, 0.6f, look.hillFar)
    // Arbres et haie minuscules sur la ligne de collines.
    val treeColor = lerp(look.hillFar, look.skyTop, if (look.dark) 0.2f else 0.18f)
    for ((i, fx) in listOf(0.07f, 0.16f, 0.39f, 0.71f, 0.9f).withIndex()) {
        val x = w * fx
        val y = hz - w * 0.05f + w * 0.012f * sin(i * 2.1f)
        val s = w * (0.022f + 0.008f * hash01(i, 9))
        drawRect(treeColor.copy(alpha = 0.9f), Offset(x - s * 0.08f, y - s * 0.8f), Size(s * 0.16f, s * 0.9f))
        drawCircle(treeColor, s * 0.55f, Offset(x, y - s * 1.15f))
        drawCircle(treeColor, s * 0.42f, Offset(x - s * 0.4f, y - s * 0.85f))
        drawCircle(treeColor, s * 0.42f, Offset(x + s * 0.4f, y - s * 0.85f))
    }
    hill(hz - w * 0.012f, w * 0.032f, 1.3f, 2.4f, look.hillNear)
    val bush = lerp(look.hillNear, if (look.dark) Color(0xFF0F2A20) else Color(0xFF3F7A45), 0.45f)
    for (i in 0 until 14) {
        val x = w * (0.2f + 0.045f * i)
        val r = w * (0.016f + 0.01f * hash01(i, 5))
        drawCircle(bush, r, Offset(x, hz - r * 0.35f))
    }
}

private fun DrawScope.drawGround(layout: SceneLayout, look: Look) {
    val proj = layout.proj
    val hz = layout.horizonY
    clipRect(top = hz) {
        drawRect(
            Brush.verticalGradient(
                0f to look.lawnFar, 0.45f to look.lawnMid, 1f to look.lawnNear,
                startY = hz, endY = size.height,
            ),
            topLeft = Offset(0f, hz),
            size = Size(size.width, size.height - hz),
        )
        // Bandes de tonte : une sur deux est plus claire, elles se resserrent avec la distance.
        val band = 0.6f
        var z = -3f
        var i = 0
        while (z < 3f) {
            if (i % 2 == 1) {
                drawPath(
                    polygon(proj.project(-9f, 0f, z), proj.project(9f, 0f, z), proj.project(9f, 0f, z + band), proj.project(-9f, 0f, z + band)),
                    if (look.dark) Color(0x0DFFFFFF) else Color(0x1EF2FFB0),
                )
            }
            z += band
            i++
        }
        // Brume au loin.
        drawRect(
            Brush.verticalGradient(listOf(look.skyHorizon.copy(alpha = if (look.dark) 0.35f else 0.5f), Color.Transparent), startY = hz, endY = hz + size.height * 0.12f),
            topLeft = Offset(0f, hz),
            size = Size(size.width, size.height * 0.12f),
        )
        // Allée de terre battue devant le carré.
        val pathPoly = polygon(
            proj.project(-2.4f, 0f, -1.7f), proj.project(-1.2f, 0f, -0.55f), proj.project(-0.95f, 0f, -0.24f),
            proj.project(-0.7f, 0f, -0.16f), proj.project(0.7f, 0f, -0.16f), proj.project(0.95f, 0f, -0.24f),
            proj.project(1.2f, 0f, -0.55f), proj.project(2.4f, 0f, -1.7f),
        )
        drawPath(pathPoly, look.pathEdge.copy(alpha = 0.5f), style = Stroke(width = proj.scaleAt(-0.3f) * 0.07f, join = StrokeJoin.Round))
        drawPath(pathPoly, look.pathLight)
        for (n in 0 until 70) {
            val x = -2f + 4f * hash01(n, 11)
            val zz = -1.5f + 1.3f * hash01(n, 12)
            if (abs(x) > 0.9f + (-zz - 0.2f) * 0.6f) continue
            val c = proj.project(x, 0f, zz)
            val r = proj.scaleAt(zz) * (0.008f + 0.01f * hash01(n, 13))
            drawOval(look.pathEdge.copy(alpha = 0.45f), Offset(c.x - r, c.y - r * 0.6f + r * 0.2f), Size(r * 2f, r * 1.2f))
            drawOval(Color.White.copy(alpha = if (look.dark) 0.1f else 0.35f), Offset(c.x - r * 0.8f, c.y - r * 0.7f), Size(r * 1.3f, r * 0.8f))
        }
        // Touffes d'herbe et petites fleurs, semées de façon déterministe hors des parcelles et de l'allée.
        val spots = (0 until 90).map { n -> Triple(-3.2f + 6.4f * hash01(n, 21), -0.45f + 2.9f * hash01(n, 22), n) }
            .sortedByDescending { it.second }
        for ((x, zz, n) in spots) {
            if (zz < -0.1f && abs(x) < 0.95f + (-zz - 0.2f) * 0.6f) continue
            if (layout.placements.any { it.fp.containsOuter(x, zz, 0.12f) }) continue
            val c = proj.project(x, 0f, zz)
            val s = proj.scaleAt(zz) * 0.05f
            if (n % 3 != 0) {
                for (b in -2..2) {
                    val h = s * (1.4f - abs(b) * 0.25f) * (0.8f + 0.4f * hash01(n, b + 30))
                    drawLine(look.grass, c, Offset(c.x + b * s * 0.35f, c.y - h), strokeWidth = max(1.5f, s * 0.12f), cap = StrokeCap.Round)
                }
            } else {
                val color = listOf(Color(0xFFFFFFFF), Color(0xFFFFD84D), Color(0xFFFFA8C4))[n % 9 / 3]
                val top = Offset(c.x, c.y - s * 1.1f)
                drawLine(look.grass, c, top, strokeWidth = max(1.2f, s * 0.08f))
                drawCircle(if (look.dark) lerp(color, Color(0xFF14223A), 0.35f) else color, max(2f, s * 0.28f), top)
                drawCircle(Color(0xFFE8B63A), max(1f, s * 0.1f), top)
            }
        }
    }
}

// --- Parcelle --------------------------------------------------------------------------------------------------

private fun DrawScope.drawParcel(
    proj: GardenProjection,
    look: Look,
    pl: Placement,
    byId: Map<String, Vegetable>,
    today: LocalDate,
    neighborhood: Map<Pair<Int, Int>, CellNeighborhood>,
    selectedCell: Pair<Int, Int>?,
    measurer: TextMeasurer,
    windPhase: Float,
) {
    val fp = pl.fp
    val s = pl.scale
    val fogAmount = if (pl.selected) 0f else 0.2f
    fun P(x: Float, y: Float, z: Float) = proj.project(x, y, z)
    fun wood(c: Color, night: Float = 0.45f) = look.fog(look.g(c, night), fogAmount)
    val lineW = max(1.2f, proj.scaleAt(fp.z0) * 0.008f * s)

    // Ombre de contact : plusieurs polygones décalés vers la droite et l'avant, de plus en plus étroits.
    for ((grow, alpha) in listOf(0.18f to 0.045f, 0.13f to 0.06f, 0.09f to 0.075f, 0.055f to 0.09f, 0.02f to 0.11f)) {
        val e = grow * s
        drawPath(
            polygon(
                P(fp.x0 - e + 0.12f * s, 0f, fp.z0 - e - 0.05f * s), P(fp.x1 + e + 0.12f * s, 0f, fp.z0 - e - 0.05f * s),
                P(fp.x1 + e + 0.1f * s, 0f, fp.z1 + e - 0.04f * s), P(fp.x0 - e + 0.1f * s, 0f, fp.z1 + e - 0.04f * s),
            ),
            Color.Black.copy(alpha = alpha * (if (look.dark) 1.4f else 1f)),
        )
    }

    // Une face verticale en deux planches, avec joint, reflet de joint et veinage discret.
    fun face(
        pt: (Float, Float) -> Offset, // (position le long de la face, hauteur) -> écran
        a: Float, b: Float, topColor: Color, botColor: Color, seed: Int,
    ) {
        for (i in 0..1) {
            val y0 = pl.frameH * i / 2f
            val y1 = pl.frameH * (i + 1) / 2f
            val p00 = pt(a, y0); val p10 = pt(b, y0); val p11 = pt(b, y1); val p01 = pt(a, y1)
            val c1 = if (i == 0) wood(botColor) else wood(topColor)
            val c0 = if (i == 0) wood(botColor).let { lerp(it, Color.Black, 0.18f) } else wood(topColor).let { lerp(it, Color.Black, 0.08f) }
            drawPath(polygon(p00, p10, p11, p01), Brush.verticalGradient(listOf(c1, c0), startY = min(p01.y, p11.y), endY = max(p00.y, p10.y)))
            for (v in 0 until 4) {
                val va = a + (b - a) * hash01(seed + i * 7 + v, 5) * 0.7f
                val vb = va + (b - a) * (0.15f + 0.25f * hash01(seed + i * 7 + v, 6))
                val vy = y0 + (y1 - y0) * (0.2f + 0.6f * hash01(seed + i * 7 + v, 7))
                drawLine(Color(0xFF3B2412).copy(alpha = 0.16f), pt(va, vy), pt(min(vb, b), vy), strokeWidth = max(1f, lineW * 0.8f))
            }
        }
        val j0 = pt(a, pl.frameH / 2f)
        val j1 = pt(b, pl.frameH / 2f)
        drawLine(Color(0xFF3A2312).copy(alpha = 0.85f), j0, j1, strokeWidth = lineW * 1.5f)
        drawLine(Color.White.copy(alpha = 0.18f), Offset(j0.x, j0.y - lineW * 1.4f), Offset(j1.x, j1.y - lineW * 1.4f), strokeWidth = lineW)
    }

    fun screw(c: Offset) {
        val r = max(1.8f, proj.scaleAt(fp.z0) * 0.009f * s)
        drawCircle(Color(0xFF3B2A1E), r, c)
        drawCircle(Color.White.copy(alpha = 0.4f), r * 0.4f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
    }

    // Faces extérieures : l'avant toujours, un côté seulement si la parcelle n'est pas dans l'axe de la caméra.
    face({ x, y -> P(x, y, fp.z0) }, fp.x0, fp.x1, Color(0xFFB98450), Color(0xFF9C6A3C), 1)
    val sideLeft = fp.x0 > 0f
    val sideRight = fp.x1 < 0f
    if (sideLeft) {
        face({ z, y -> P(fp.x0, y, z) }, fp.z0, fp.z1, Color(0xFFCB975F), Color(0xFFB0804E), 2)
    } else if (sideRight) {
        face({ z, y -> P(fp.x1, y, z) }, fp.z0, fp.z1, Color(0xFF86573A), Color(0xFF6E4629), 3)
    }
    // Montants d'angle devant, un peu plus clairs, avec leurs vis.
    for (px in listOf(fp.x0, fp.x1 - fp.wall * 1.3f)) {
        drawPath(
            polygon(P(px, 0f, fp.z0), P(px + fp.wall * 1.3f, 0f, fp.z0), P(px + fp.wall * 1.3f, pl.frameH, fp.z0), P(px, pl.frameH, fp.z0)),
            Color.White.copy(alpha = 0.1f),
        )
        drawLine(Color(0xFF3A2312).copy(alpha = 0.5f), P(px + (if (px == fp.x0) fp.wall * 1.3f else 0f), 0f, fp.z0), P(px + (if (px == fp.x0) fp.wall * 1.3f else 0f), pl.frameH, fp.z0), strokeWidth = lineW)
        screw(P(px + fp.wall * 0.65f, pl.frameH * 0.27f, fp.z0))
        screw(P(px + fp.wall * 0.65f, pl.frameH * 0.77f, fp.z0))
    }

    // Terre en retrait, dégradée (plus sombre au fond), cases légèrement variées.
    val soil = polygon(
        P(fp.innerX0, pl.soilY, fp.innerZ0), P(fp.innerX1, pl.soilY, fp.innerZ0),
        P(fp.innerX1, pl.soilY, fp.innerZ1), P(fp.innerX0, pl.soilY, fp.innerZ1),
    )
    val soilTop = P(0f, pl.soilY, fp.innerZ1).y
    val soilBot = P(0f, pl.soilY, fp.innerZ0).y
    drawPath(
        soil,
        Brush.verticalGradient(
            listOf(wood(Color(0xFF55382A), 0.5f), wood(Color(0xFF6E4C36), 0.5f), wood(Color(0xFF7A563B), 0.5f)),
            startY = soilTop, endY = soilBot,
        ),
    )
    for (r in 0 until fp.rows) {
        for (c in 0 until fp.cols) {
            val h = hash01(r * 13 + c, pl.parcel.id.hashCode())
            val cx = fp.cellCenterX(c)
            val cz = fp.cellCenterZ(r)
            val hh = fp.cell / 2f
            val tint = if (h > 0.5f) Color(0xFFB08A60) else Color(0xFF3A2416)
            drawPath(
                polygon(P(cx - hh, pl.soilY, cz - hh), P(cx + hh, pl.soilY, cz - hh), P(cx + hh, pl.soilY, cz + hh), P(cx - hh, pl.soilY, cz + hh)),
                tint.copy(alpha = 0.06f + 0.1f * abs(h - 0.5f)),
            )
            // Petits monticules de terre.
            val mounds = if (pl.selected) 3 else 1
            for (m in 0 until mounds) {
                val mx = cx + (hash01(r * 31 + c * 7 + m, 41) - 0.5f) * fp.cell * 0.8f
                val mz = cz + (hash01(r * 31 + c * 7 + m, 42) - 0.5f) * fp.cell * 0.8f
                val mc = P(mx, pl.soilY, mz)
                val mr = proj.scaleAt(mz, pl.soilY) * fp.cell * (0.07f + 0.05f * hash01(m, c + r))
                drawOval(Color(0xFF2E1D12).copy(alpha = 0.3f), Offset(mc.x - mr * 0.9f, mc.y - mr * 0.5f), Size(mr * 2f, mr * 1.1f))
                drawOval(wood(Color(0xFF8C6A48), 0.5f), Offset(mc.x - mr, mc.y - mr * 0.62f), Size(mr * 2f, mr * 1.1f))
                drawOval(Color.White.copy(alpha = 0.12f), Offset(mc.x - mr * 0.7f, mc.y - mr * 0.6f), Size(mr, mr * 0.55f))
            }
        }
    }
    // Quadrillage fin des cases.
    for (r in 1 until fp.rows) {
        val z = fp.innerZ0 + r * fp.cell
        drawLine(Color(0xFF2E1D12).copy(alpha = 0.3f), P(fp.innerX0, pl.soilY, z), P(fp.innerX1, pl.soilY, z), strokeWidth = lineW)
    }
    for (c in 1 until fp.cols) {
        val x = fp.innerX0 + c * fp.cell
        drawLine(Color(0xFF2E1D12).copy(alpha = 0.3f), P(x, pl.soilY, fp.innerZ0), P(x, pl.soilY, fp.innerZ1), strokeWidth = lineW)
    }
    // Ombres portées par les planches sur la terre (gauche et fond plus marqués pour le relief intérieur).
    val sh = 0.12f * s
    drawPath(polygon(P(fp.innerX0, pl.soilY, fp.innerZ1 - sh), P(fp.innerX1, pl.soilY, fp.innerZ1 - sh), P(fp.innerX1, pl.soilY, fp.innerZ1), P(fp.innerX0, pl.soilY, fp.innerZ1)), Color.Black.copy(alpha = 0.30f))
    drawPath(polygon(P(fp.innerX0, pl.soilY, fp.innerZ0), P(fp.innerX0 + sh, pl.soilY, fp.innerZ0), P(fp.innerX0 + sh, pl.soilY, fp.innerZ1), P(fp.innerX0, pl.soilY, fp.innerZ1)), Color.Black.copy(alpha = 0.28f))
    drawPath(polygon(P(fp.innerX1 - sh * 0.6f, pl.soilY, fp.innerZ0), P(fp.innerX1, pl.soilY, fp.innerZ0), P(fp.innerX1, pl.soilY, fp.innerZ1), P(fp.innerX1 - sh * 0.6f, pl.soilY, fp.innerZ1)), Color.Black.copy(alpha = 0.14f))

    // Faces intérieures visibles : fond, et côtés selon la position de la caméra.
    drawPath(
        polygon(P(fp.innerX0, pl.soilY, fp.innerZ1), P(fp.innerX1, pl.soilY, fp.innerZ1), P(fp.innerX1, pl.frameH, fp.innerZ1), P(fp.innerX0, pl.frameH, fp.innerZ1)),
        wood(Color(0xFF7A5030)),
    )
    if (fp.innerX0 < 0f) {
        drawPath(
            polygon(P(fp.innerX0, pl.soilY, fp.innerZ0), P(fp.innerX0, pl.soilY, fp.innerZ1), P(fp.innerX0, pl.frameH, fp.innerZ1), P(fp.innerX0, pl.frameH, fp.innerZ0)),
            wood(Color(0xFF9A6A3E)),
        )
    }
    if (fp.innerX1 > 0f) {
        drawPath(
            polygon(P(fp.innerX1, pl.soilY, fp.innerZ0), P(fp.innerX1, pl.soilY, fp.innerZ1), P(fp.innerX1, pl.frameH, fp.innerZ1), P(fp.innerX1, pl.frameH, fp.innerZ0)),
            wood(Color(0xFF6A4326)),
        )
    }

    // Rebord supérieur, plus clair, avec arête vive.
    val rim = wood(Color(0xFFD9AC74), 0.4f)
    val rimBack = wood(Color(0xFFC89B64), 0.4f)
    fun strip(x0: Float, z0: Float, x1: Float, z1: Float, c: Color) =
        drawPath(polygon(P(x0, pl.frameH, z0), P(x1, pl.frameH, z0), P(x1, pl.frameH, z1), P(x0, pl.frameH, z1)), c)
    strip(fp.x0, fp.innerZ1, fp.x1, fp.z1, rimBack)
    strip(fp.x0, fp.innerZ0, fp.innerX0, fp.innerZ1, rim)
    strip(fp.innerX1, fp.innerZ0, fp.x1, fp.innerZ1, rimBack)
    strip(fp.x0, fp.z0, fp.x1, fp.innerZ0, rim)
    drawLine(Color.White.copy(alpha = 0.35f), P(fp.x0, pl.frameH, fp.z0), P(fp.x1, pl.frameH, fp.z0), strokeWidth = lineW * 1.2f)
    drawLine(Color(0xFF3A2312).copy(alpha = 0.35f), P(fp.innerX0, pl.frameH, fp.innerZ0), P(fp.innerX1, pl.frameH, fp.innerZ0), strokeWidth = lineW)
    for (v in 0 until 5) {
        val z = fp.z0 + (fp.innerZ0 - fp.z0) * (0.2f + 0.6f * hash01(v, 77))
        val xa = fp.x0 + (fp.x1 - fp.x0) * hash01(v, 78) * 0.8f
        drawLine(Color(0xFF6B4423).copy(alpha = 0.18f), P(xa, pl.frameH, z), P(xa + (fp.x1 - fp.x0) * 0.2f, pl.frameH, z), strokeWidth = lineW * 0.8f)
    }

    // Anneaux d'état sur la terre (sous les plantes) : sélection, affinité, alerte.
    if (pl.selected) {
        for (r in 0 until fp.rows) {
            for (c in 0 until fp.cols) {
                val nb = neighborhood[r to c]
                val isSel = selectedCell == (r to c)
                val color = when {
                    isSel -> GlowCream
                    nb?.hasBad == true -> GlowAmber
                    nb?.hasGood == true -> GlowGreen
                    else -> null
                } ?: continue
                val cx = fp.cellCenterX(c)
                val cz = fp.cellCenterZ(r)
                val hh = fp.cell / 2f * 0.9f
                val ring = Path().apply {
                    val a = P(cx - hh, pl.soilY, cz - hh); val b = P(cx + hh, pl.soilY, cz - hh)
                    val d = P(cx + hh, pl.soilY, cz + hh); val e = P(cx - hh, pl.soilY, cz + hh)
                    moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(d.x, d.y); lineTo(e.x, e.y); close()
                }
                val w = max(1.5f, proj.scaleAt(cz, pl.soilY) * (if (isSel) 0.014f else 0.006f))
                if (isSel) drawPath(ring, color.copy(alpha = 0.16f))
                // Seule la case choisie brille fort : les affinités restent un liseré discret.
                val glow = if (isSel) 1f else 0.3f
                drawPath(ring, color.copy(alpha = 0.22f * glow), style = Stroke(width = w * 4f, join = StrokeJoin.Round))
                drawPath(ring, color.copy(alpha = 0.55f * glow), style = Stroke(width = w * 2.2f, join = StrokeJoin.Round))
                drawPath(ring, color.copy(alpha = if (isSel) 0.95f else 0.8f), style = Stroke(width = w, join = StrokeJoin.Round))
            }
        }
    }

    // Plantes, rangée par rangée du fond (rangée 0) vers l'avant, pour un recouvrement correct.
    val tone = PlantTone(look.dark, look.fogColor, fogAmount)
    for (r in 0 until fp.rows) {
        for (c in 0 until fp.cols) {
            val slot = pl.parcel.getSlot(r, c) ?: continue
            val veg = slot.vegetableId?.let { byId[it] } ?: continue
            val cz = fp.cellCenterZ(r)
            val base = P(fp.cellCenterX(c), pl.soilY, cz)
            val unit = proj.scaleAt(cz, pl.soilY) * fp.cell
            val growth = plantGrowth(veg, slot.plantedDate, today)
            // Ombre de contact douce au pied de la plante (lumière venant du haut gauche -> décalée bas droite).
            val shadowR = unit * (0.22f + 0.16f * growth)
            drawOval(
                Color.Black.copy(alpha = if (look.dark) 0.28f else 0.18f),
                Offset(base.x - shadowR * 0.72f, base.y - shadowR * 0.28f),
                Size(shadowR * 1.7f, shadowR * 0.78f),
            )
            val swayDeg = if (windPhase != 0f) sin(windPhase + r * 0.8f + c * 1.3f) * (1.2f + 0.8f * growth) else 0f
            withTransform({
                if (swayDeg != 0f) rotate(degrees = swayDeg, pivot = base)
            }) {
                drawPlant(veg, growth, base, unit, tone, seed = r * 31 + c * 17 + veg.id.hashCode(), simple = !pl.selected)
            }
        }
    }

    if (pl.selected) {
        drawCellBadges(proj, pl, byId, today, neighborhood)
        drawSign(proj, look, pl, measurer)
    }
}

/** Pastilles au-dessus des plantes : feuille verte pour une affinité, triangle ambre pour une alerte. */
private fun DrawScope.drawCellBadges(
    proj: GardenProjection,
    pl: Placement,
    byId: Map<String, Vegetable>,
    today: LocalDate,
    neighborhood: Map<Pair<Int, Int>, CellNeighborhood>,
) {
    val fp = pl.fp
    for (r in 0 until fp.rows) {
        for (c in 0 until fp.cols) {
            val nb = neighborhood[r to c] ?: continue
            if (!nb.hasGood && !nb.hasBad) continue
            val slot = pl.parcel.getSlot(r, c) ?: continue
            val veg = slot.vegetableId?.let { byId[it] } ?: continue
            val cz = fp.cellCenterZ(r)
            val base = proj.project(fp.cellCenterX(c), pl.soilY, cz)
            val unit = proj.scaleAt(cz, pl.soilY) * fp.cell
            val height = plantHeightFactor(veg, plantGrowth(veg, slot.plantedDate, today)) * unit
            val rad = max(8f, unit * 0.105f)
            val center = Offset(
                base.x + unit * 0.3f,
                (base.y - height - rad * 0.8f).coerceAtLeast(rad + 2f),
            )
            drawCircle(Color.Black.copy(alpha = 0.18f), rad, Offset(center.x + rad * 0.12f, center.y + rad * 0.2f))
            if (nb.hasBad) {
                val tri = Path().apply {
                    moveTo(center.x, center.y - rad * 1.05f)
                    lineTo(center.x + rad * 1.0f, center.y + rad * 0.8f)
                    lineTo(center.x - rad * 1.0f, center.y + rad * 0.8f)
                    close()
                }
                drawPath(tri, Color.White, style = Stroke(width = rad * 0.5f, join = StrokeJoin.Round))
                drawPath(tri, SillonPalette.AlertAmber)
                drawPath(tri, SillonPalette.AlertAmber, style = Stroke(width = rad * 0.3f, join = StrokeJoin.Round))
                drawLine(Color.White, Offset(center.x, center.y - rad * 0.4f), Offset(center.x, center.y + rad * 0.2f), strokeWidth = rad * 0.26f, cap = StrokeCap.Round)
                drawCircle(Color.White, rad * 0.13f, Offset(center.x, center.y + rad * 0.5f))
            } else {
                drawCircle(Color.White.copy(alpha = 0.95f), rad, center)
                drawCircle(SillonPalette.Sage, rad, center, style = Stroke(width = rad * 0.14f))
                val dx = kotlin.math.cos(-0.8f)
                val dy = kotlin.math.sin(-0.8f)
                drawPath(
                    leafPath(Offset(center.x - dx * rad * 0.55f, center.y - dy * rad * 0.55f), dx, dy, rad * 1.1f, rad * 0.42f),
                    SillonPalette.SproutGreen,
                )
            }
        }
    }
}

/** Petit panneau planté dans l'allée, devant le carré, avec le nom de la parcelle. */
private fun DrawScope.drawSign(proj: GardenProjection, look: Look, pl: Placement, measurer: TextMeasurer) {
    val z = -0.34f
    val ground = proj.project(0f, 0f, z)
    val sc = proj.scaleAt(z)
    val maxTextW = sc * 1.0f
    fun measure(px: Float) = measurer.measure(
        pl.parcel.name,
        style = TextStyle(
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = px.toSp(),
            color = if (look.dark) Color(0xFFF3E7CC) else Color(0xFF3A2312),
        ),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxTextW.toInt().coerceAtLeast(1)),
    )
    // Un nom long réduit la police (jusqu'à 55 %) avant de passer à l'ellipse.
    var fontPx = sc * 0.075f
    var layout = measure(fontPx)
    if (layout.hasVisualOverflow) {
        val natural = layout.multiParagraph.maxIntrinsicWidth
        fontPx *= (maxTextW / natural).coerceIn(0.55f, 1f)
        layout = measure(fontPx)
    }
    val boardW = layout.size.width + fontPx * 1.2f
    val boardH = fontPx * 1.9f
    val bottom = proj.project(0f, 0.27f, z).y
    val top = bottom - boardH
    val left = ground.x - boardW / 2f
    val post = look.g(Color(0xFF7A5232), 0.45f)
    val postW = max(3f, sc * 0.022f)
    for (px in listOf(left + boardW * 0.18f, left + boardW * 0.82f)) {
        drawLine(Color.Black.copy(alpha = 0.2f), Offset(px + postW, ground.y + postW * 0.4f), Offset(px + postW * 3f, ground.y + postW * 0.8f), strokeWidth = postW * 0.9f, cap = StrokeCap.Round)
        drawLine(post, Offset(px, ground.y), Offset(px, bottom), strokeWidth = postW, cap = StrokeCap.Butt)
    }
    val corner = CornerRadius(fontPx * 0.28f)
    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(left + fontPx * 0.12f, top + fontPx * 0.18f), Size(boardW, boardH), corner)
    drawRoundRect(look.g(Color(0xFFA6733F), 0.45f), Offset(left, top), Size(boardW, boardH), corner)
    drawRoundRect(look.g(Color(0xFFD2A46C), 0.45f), Offset(left + fontPx * 0.1f, top + fontPx * 0.1f), Size(boardW - fontPx * 0.2f, boardH - fontPx * 0.2f), CornerRadius(fontPx * 0.2f))
    val tx = left + (boardW - layout.size.width) / 2f
    val ty = top + (boardH - layout.size.height) / 2f
    drawText(layout, topLeft = Offset(tx, ty))
    for (sx in listOf(left + fontPx * 0.3f, left + boardW - fontPx * 0.3f)) {
        drawCircle(Color(0xFF3B2A1E), max(1.5f, fontPx * 0.07f), Offset(sx, top + boardH / 2f))
    }
}
