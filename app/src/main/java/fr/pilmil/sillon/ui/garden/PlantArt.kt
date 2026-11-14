package fr.pilmil.sillon.ui.garden

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import fr.pilmil.sillon.model.Vegetable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Plantes dessinées en formes vectorielles (aucun effet GPU : le rendu doit être identique sur appareil et en capture).
 * Convention : la lumière vient du haut gauche. Chaque forme est donc une ombre plus sombre décalée en bas à droite,
 * le corps, puis un reflet clair en haut à gauche. Tout le hasard dérive de la graine (ligne, colonne, id) : les
 * captures sont stables d'un lancement à l'autre.
 */

/** Teinte de nuit et brume d'éloignement appliquées à toutes les couleurs d'une plante. */
internal class PlantTone(
    val night: Boolean,
    private val fog: Color = Color.Transparent,
    private val fogAmount: Float = 0f,
) {
    fun c(color: Color): Color {
        var r = color
        if (night) r = lerp(r, NightCast, 0.34f)
        if (fogAmount > 0f) r = lerp(r, fog, fogAmount)
        return r
    }

    companion object {
        val Day = PlantTone(false)
        val Night = PlantTone(true)
    }
}

private val NightCast = Color(0xFF16253A)
private val InkShadow = Color(0xFF16200F)
private const val TAU = (2.0 * PI).toFloat()

private fun Color.lighter(t: Float) = lerp(this, Color.White, t)
private fun Color.darker(t: Float) = lerp(this, InkShadow, t)
private fun deg(d: Float) = d * (PI / 180.0).toFloat()

private class Rnd(seed: Int) {
    private var s = seed * 31 + 12345

    fun next(): Float {
        s = s * 1664525 + 1013904223
        return ((s ushr 8) and 0xFFFF) / 65535f
    }

    fun range(a: Float, b: Float) = a + (b - a) * next()
}

/** Un sujet à dessiner : [k] est sa taille en px (une case pleine = 1 k), [base] le point au sol. */
private class Sub(
    val base: Offset,
    val k: Float,
    val growth: Float,
    val ripe: Boolean,
    val tone: PlantTone,
    val rnd: Rnd,
    val simple: Boolean,
) {
    fun c(color: Color) = tone.c(color)

    /** Point décalé de (dx, dy) tailles ; dy négatif = vers le haut. */
    fun at(dx: Float, dy: Float) = Offset(base.x + dx * k, base.y + dy * k)
}

/** Avancement de la plante : 1 = prête à récolter ; 0,7 quand la date de plantation est inconnue. */
internal fun plantGrowth(veg: Vegetable, plantedDate: Long?, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Float {
    if (plantedDate == null) return 0.7f
    val planted = Instant.ofEpochMilli(plantedDate).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(planted, today).toFloat()
    return (days / max(1, veg.daysToHarvest.first)).coerceIn(0f, 1f)
}

/** Hauteur approximative de la plante en tailles de case, pour poser une pastille au-dessus. */
internal fun plantHeightFactor(veg: Vegetable, growth: Float): Float {
    val full = when (veg.id) {
        "mais_doux", "tournesol", "papayer" -> 1.9f
        "igname" -> 1.7f
        "haricot_rames", "pois", "feve", "pois_d_angole" -> 1.3f
        "taro_madere" -> 0.9f
        "tomate", "tomate_cerise", "aubergine", "poivron", "piment", "asperge", "fenouil", "aneth" -> 1.0f
        "poireau", "oignon", "ail", "echalote", "ciboulette", "romarin" -> 0.7f
        "chou_pomme", "chou_fleur", "brocoli", "chou_kale", "carotte", "panais", "celeri_branche", "betterave" -> 0.6f
        else -> 0.45f
    }
    val perSubject = if (veg.squareFootDensity >= 2) 0.55f else 1f
    return full * perSubject * if (growth < 0.25f) 0.2f else lerpF(0.35f, 1f, ((growth - 0.25f) / 0.75f).coerceIn(0f, 1f))
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Dessine la plante d'une case centrée sur [base] (le point au sol, au milieu de la case). [unit] est la largeur de la
 * case en px à cette profondeur. Les cases à plusieurs sujets (densité > 1) sont des groupes réduits, plafonnés à 9.
 * [simple] allège le rendu pour les parcelles d'arrière-plan.
 */
internal fun DrawScope.drawPlant(
    veg: Vegetable,
    growth: Float,
    base: Offset,
    unit: Float,
    tone: PlantTone = PlantTone.Day,
    seed: Int = 0,
    simple: Boolean = false,
) {
    val g = growth.coerceIn(0f, 1f)
    val layout = groupLayout(veg.squareFootDensity)
    // Du fond vers l'avant : le sujet le plus éloigné se dessine d'abord.
    for ((i, spot) in layout.spots.sortedBy { it.second }.withIndex()) {
        val subjectBase = Offset(base.x + spot.first * unit, base.y + spot.second * unit * 0.72f)
        val rnd = Rnd(seed * 31 + i * 7919)
        if (g < 0.25f) {
            drawSprout(Sub(subjectBase, unit * (0.3f + 0.7f * layout.size), g, false, tone, rnd, simple), g)
        } else {
            val s = lerpF(0.35f, 1f, ((g - 0.25f) / 0.75f).coerceIn(0f, 1f))
            val p = Sub(subjectBase, unit * layout.size * s, g, g > 0.8f, tone, rnd, simple)
            drawSubject(p, veg)
        }
    }
}

private class GroupLayout(val spots: List<Pair<Float, Float>>, val size: Float)

private fun groupLayout(density: Int): GroupLayout = when {
    density <= 1 -> GroupLayout(listOf(0f to 0f), 1f)
    density == 2 -> GroupLayout(listOf(-0.23f to -0.1f, 0.23f to 0.1f), 0.62f)
    density == 3 -> GroupLayout(listOf(0f to -0.24f, -0.25f to 0.2f, 0.25f to 0.2f), 0.56f)
    density == 4 -> GroupLayout(listOf(-0.24f to -0.22f, 0.24f to -0.22f, -0.24f to 0.22f, 0.24f to 0.22f), 0.56f)
    density <= 5 -> GroupLayout(
        listOf(-0.26f to -0.26f, 0.26f to -0.26f, 0f to 0f, -0.26f to 0.26f, 0.26f to 0.26f), 0.46f,
    )
    density <= 8 -> GroupLayout(
        listOf(-0.3f to -0.2f, 0f to -0.2f, 0.3f to -0.2f, -0.3f to 0.22f, 0f to 0.22f, 0.3f to 0.22f), 0.4f,
    )
    else -> GroupLayout(
        listOf(
            -0.3f to -0.3f, 0f to -0.3f, 0.3f to -0.3f,
            -0.3f to 0f, 0f to 0f, 0.3f to 0f,
            -0.3f to 0.3f, 0f to 0.3f, 0.3f to 0.3f,
        ),
        0.4f,
    )
}

// --- Primitives ------------------------------------------------------------------------------------------------

private fun DrawScope.groundShadow(p: Sub, rx: Float, ry: Float) {
    val c = Offset(p.base.x + rx * 0.22f, p.base.y + ry * 0.15f)
    if (!p.simple) drawOval(Color(0x18000000), Offset(c.x - rx * 1.2f, c.y - ry * 1.25f), Size(rx * 2.4f, ry * 2.5f))
    drawOval(Color(0x26000000), Offset(c.x - rx, c.y - ry), Size(rx * 2f, ry * 2f))
}

private fun DrawScope.shadedOval(c: Offset, rx: Float, ry: Float, base: Color, simple: Boolean = false) {
    if (!simple) drawOval(base.darker(0.3f), Offset(c.x - rx + rx * 0.14f, c.y - ry + ry * 0.18f), Size(2 * rx, 2 * ry))
    drawOval(base, Offset(c.x - rx, c.y - ry), Size(2 * rx, 2 * ry))
    if (!simple) drawOval(base.lighter(0.34f).copy(alpha = 0.6f), Offset(c.x - rx * 0.8f, c.y - ry * 0.82f), Size(rx * 1.2f, ry * 1.1f))
}

private fun DrawScope.sphere(c: Offset, r: Float, base: Color, simple: Boolean = false) {
    drawCircle(base.darker(0.3f), r, Offset(c.x + r * 0.13f, c.y + r * 0.15f))
    drawCircle(base, r, c)
    if (!simple) drawCircle(base.lighter(0.55f).copy(alpha = 0.55f), r * 0.3f, Offset(c.x - r * 0.32f, c.y - r * 0.36f))
}

private fun DrawScope.line(a: Offset, b: Offset, w: Float, color: Color) =
    drawLine(color, a, b, strokeWidth = max(1f, w), cap = StrokeCap.Round)

internal fun leafPath(o: Offset, dx: Float, dy: Float, len: Float, wid: Float): Path {
    val nx = -dy
    val ny = dx
    return Path().apply {
        moveTo(o.x, o.y)
        cubicTo(
            o.x + dx * len * 0.25f + nx * wid, o.y + dy * len * 0.25f + ny * wid,
            o.x + dx * len * 0.8f + nx * wid * 0.7f, o.y + dy * len * 0.8f + ny * wid * 0.7f,
            o.x + dx * len, o.y + dy * len,
        )
        cubicTo(
            o.x + dx * len * 0.8f - nx * wid * 0.7f, o.y + dy * len * 0.8f - ny * wid * 0.7f,
            o.x + dx * len * 0.25f - nx * wid, o.y + dy * len * 0.25f - ny * wid,
            o.x, o.y,
        )
        close()
    }
}

/** Feuille : [angle] en radians écran (-90° = vers le haut), [wid] = demi-largeur max. */
private fun DrawScope.leaf(p: Sub, o: Offset, angle: Float, len: Float, wid: Float, base: Color, rib: Color? = null) {
    val dx = cos(angle)
    val dy = sin(angle)
    drawPath(leafPath(o, dx, dy, len, wid), base.darker(0.1f))
    if (p.simple) return
    // Reflet côté lumière (haut gauche).
    val s = if (-dy - dx > 0f) -1f else 1f
    val sx = -dy * s * wid * 0.32f
    val sy = dx * s * wid * 0.32f
    drawPath(
        leafPath(Offset(o.x + sx, o.y + sy), dx, dy, len * 0.92f, wid * 0.55f),
        base.lighter(0.22f).copy(alpha = 0.8f),
    )
    val vein = rib ?: base.lighter(0.42f).copy(alpha = 0.55f)
    line(o, Offset(o.x + dx * len * 0.86f, o.y + dy * len * 0.86f), if (rib != null) len * 0.05f else len * 0.022f, vein)
}

private fun DrawScope.drawBlade(p: Sub, o: Offset, tip: Offset, wid: Float, bend: Float, color: Color) {
    val mx = (o.x + tip.x) / 2f + bend
    val my = o.y + (tip.y - o.y) * 0.55f
    fun body(w: Float, shift: Float) = Path().apply {
        moveTo(o.x - w / 2f + shift, o.y)
        quadraticBezierTo(mx - w / 2f + shift, my, tip.x + shift * 0.3f, tip.y)
        quadraticBezierTo(mx + w / 2f + shift, my, o.x + w / 2f + shift, o.y)
        close()
    }
    drawPath(body(wid, 0f), color.darker(0.12f))
    if (!p.simple) drawPath(body(wid * 0.45f, -wid * 0.2f), color.lighter(0.25f).copy(alpha = 0.7f))
}

private fun DrawScope.flowerHead(
    p: Sub,
    c: Offset,
    r: Float,
    petals: Int,
    petal: Color,
    core: Color,
    inner: Color? = null,
    petalLen: Float = 0.42f,
    petalWid: Float = 0.2f,
) {
    val layers = if (inner != null) 2 else 1
    for (layer in 0 until layers) {
        val rr = r * (1f - layer * 0.42f)
        val col = if (layer == 0) petal else inner!!
        for (i in 0 until petals) {
            val a = TAU * (i + layer * 0.5f) / petals
            val pc = Offset(c.x + cos(a) * rr * 0.6f, c.y + sin(a) * rr * 0.48f)
            val rot = Math.toDegrees(atan2(sin(a) * 0.5, cos(a).toDouble())).toFloat()
            rotate(rot, pc) {
                drawOval(col.darker(0.22f), Offset(pc.x - rr * petalLen + rr * 0.05f, pc.y - rr * petalWid + rr * 0.06f), Size(rr * petalLen * 2, rr * petalWid * 2))
                drawOval(col, Offset(pc.x - rr * petalLen, pc.y - rr * petalWid), Size(rr * petalLen * 2, rr * petalWid * 2))
            }
        }
    }
    sphere(c, r * 0.3f, core, p.simple)
}

private fun DrawScope.curl(o: Offset, dir: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(o.x, o.y)
        cubicTo(
            o.x + dir * size, o.y - size * 0.4f,
            o.x + dir * size * 1.1f, o.y - size * 1.2f,
            o.x + dir * size * 0.3f, o.y - size * 1.1f,
        )
    }
    drawPath(path, color, style = Stroke(width = max(1f, size * 0.09f), cap = StrokeCap.Round))
}

// --- Jeune pousse ----------------------------------------------------------------------------------------------

private fun DrawScope.drawSprout(p: Sub, growth: Float) {
    val k = p.k
    val grow = 0.6f + growth / 0.25f * 0.4f
    val col = p.c(Color(0xFF8CCB55))
    groundShadow(p, k * 0.16f, k * 0.055f)
    // Petite butte de terre fraîche au pied.
    drawOval(p.c(Color(0xFF8A6446)), Offset(p.base.x - k * 0.1f, p.base.y - k * 0.03f), Size(k * 0.2f, k * 0.07f))
    val o = p.at(0f, -0.02f)
    val top = p.at(0f, -0.1f * grow)
    line(o, top, k * 0.018f, p.c(Color(0xFF6FAF45)))
    leaf(p, top, deg(-168f), k * 0.14f * grow, k * 0.06f * grow, col)
    leaf(p, top, deg(-12f), k * 0.14f * grow, k * 0.06f * grow, col)
    if (growth > 0.12f) leaf(p, top, deg(-92f), k * 0.1f * grow, k * 0.045f * grow, col.lighter(0.1f))
}

// --- Rosettes --------------------------------------------------------------------------------------------------

private enum class Head { NONE, CABBAGE, CAULI, BROCCOLI, KOHLRABI }

private fun DrawScope.rosette(
    p: Sub,
    color: Color,
    count: Int,
    lenF: Float,
    widF: Float,
    head: Head = Head.NONE,
    headColor: Color = Color.White,
    rib: Color? = null,
) {
    val k = p.k
    groundShadow(p, k * 0.52f, k * 0.18f)
    val col = p.c(color)
    var origin = p.at(0f, -0.05f)
    if (head == Head.KOHLRABI) {
        origin = p.at(0f, -0.27f)
        sphere(p.at(0f, -0.14f), k * 0.16f, p.c(headColor), p.simple)
    }
    val rings = if (head == Head.NONE || head == Head.KOHLRABI) 3 else 2
    for (ring in 0 until rings) {
        val n = when (ring) {
            0 -> (count * 0.45f).toInt().coerceAtLeast(3)
            1 -> (count * 0.35f).toInt().coerceAtLeast(3)
            else -> (count * 0.2f).toInt().coerceAtLeast(2)
        }
        val h = floatArrayOf(1f, 0.7f, 0.35f)[ring]
        val v = floatArrayOf(0.3f, 0.75f, 1f)[ring]
        val len = k * lenF * (1f - 0.2f * ring)
        val tint = col.lighter(0.1f * ring)
        val angles = (0 until n).map { i -> TAU * (i + 0.5f * ring + p.rnd.range(-0.15f, 0.15f)) / n }.sortedBy { sin(it) }
        for (t in angles) {
            val a = atan2(sin(t) * h * 0.55f - v * (if (ring == 0) 1.5f else 1f), cos(t) * h)
            // Les feuilles qui retombent vers l'avant sont raccourcies : vues d'en haut, elles s'écrasent au sol.
            val front = 1f - 0.35f * max(0f, sin(t))
            leaf(p, origin, a, len * front * p.rnd.range(0.9f, 1.1f), len * widF, tint, rib?.let { p.c(it) })
        }
    }
    val hc = p.c(headColor)
    when (head) {
        Head.CABBAGE -> {
            val c = p.at(0f, -0.2f)
            sphere(c, k * 0.28f, hc, p.simple)
            if (!p.simple) {
                val r = k * 0.28f
                for (sx in listOf(-1f, 1f)) {
                    val path = Path().apply {
                        moveTo(c.x + sx * r * 0.8f, c.y + r * 0.2f)
                        quadraticBezierTo(c.x + sx * r * 0.1f, c.y - r * 0.2f, c.x + sx * r * 0.15f, c.y - r * 0.95f)
                    }
                    drawPath(path, hc.darker(0.3f).copy(alpha = 0.6f), style = Stroke(width = max(1f, k * 0.02f), cap = StrokeCap.Round))
                }
            }
        }
        Head.CAULI -> {
            val c = p.at(0f, -0.2f)
            sphere(c, k * 0.24f, hc.darker(0.08f), p.simple)
            val bumps = (0 until 9).map { Offset(p.rnd.range(-1f, 1f), p.rnd.range(-1f, 0.7f)) }.sortedBy { it.y }
            for (b in bumps) sphere(Offset(c.x + b.x * k * 0.15f, c.y + b.y * k * 0.14f), k * 0.085f, hc.lighter(0.1f), p.simple)
        }
        Head.BROCCOLI -> {
            line(p.at(0f, -0.06f), p.at(0f, -0.2f), k * 0.07f, p.c(Color(0xFF9CC77A)))
            val c = p.at(0f, -0.3f)
            sphere(c, k * 0.17f, hc, p.simple)
            val bumps = (0 until 9).map { Offset(p.rnd.range(-1f, 1f), p.rnd.range(-1f, 0.6f)) }.sortedBy { it.y }
            for (b in bumps) sphere(Offset(c.x + b.x * k * 0.14f, c.y + b.y * k * 0.11f), k * 0.075f, hc.lighter(0.08f), p.simple)
        }
        else -> Unit
    }
}

// --- Buisson à fruits ------------------------------------------------------------------------------------------

private enum class Fruit { TOMATO, CHERRY, EGGPLANT, PEPPER, CHILI }

private fun DrawScope.fruitBush(
    p: Sub,
    kind: Fruit,
    leafColor: Color,
    ripeColor: Color,
    height: Float = 1f,
    flowerColor: Color = Color(0xFFF5D547),
) {
    val k = p.k
    groundShadow(p, k * 0.4f, k * 0.14f)
    // Tuteur en bois fin, légèrement penché, puis tige.
    line(p.at(0f, 0.02f), p.at(0.06f, -1.1f * height), k * 0.032f, p.c(Color(0xFF9A7448)))
    val stemTop = p.at(0.02f, -0.92f * height)
    line(p.at(0f, 0f), stemTop, k * 0.04f, p.c(Color(0xFF5E8F3A)))
    val leafCol = p.c(leafColor)
    for (i in 0 until 5) {
        val f = i / 4f
        val o = p.at(0.02f * f, -(0.12f + f * 0.78f * height))
        val size = 1f - f * 0.42f
        leaf(p, o, deg(-163f + f * 18f), k * 0.36f * size, k * 0.13f * size, leafCol)
        leaf(p, o, deg(-17f - f * 18f), k * 0.36f * size, k * 0.13f * size, leafCol.lighter(0.06f))
        if (i % 2 == 0) leaf(p, o, deg(-95f), k * 0.26f * size, k * 0.1f * size, leafCol.lighter(0.12f))
    }
    leaf(p, stemTop, deg(-98f), k * 0.24f, k * 0.09f, leafCol.lighter(0.15f))
    val spots = when (kind) {
        Fruit.CHERRY -> listOf(
            -0.26f to -0.62f, -0.2f to -0.54f, -0.3f to -0.5f, 0.26f to -0.6f, 0.2f to -0.5f, 0.3f to -0.44f,
            -0.2f to -0.34f, 0.18f to -0.3f,
        )
        else -> listOf(-0.22f to -0.66f, 0.24f to -0.54f, -0.16f to -0.4f, 0.18f to -0.3f)
    }.map { Offset(it.first, it.second * height) }.sortedBy { it.y }
    val stage = when {
        p.growth < 0.5f -> 0
        !p.ripe -> 1
        else -> 2
    }
    val unripe = p.c(Color(0xFF86AE48))
    val ripe = p.c(ripeColor)
    for (spot in spots) {
        val c = p.at(spot.x, spot.y)
        val stalkTop = p.at(spot.x * 0.15f, spot.y - 0.08f)
        when {
            stage == 0 -> flowerHead(p, c, k * 0.055f, 5, p.c(flowerColor), p.c(Color(0xFFD9A21B)))
            else -> {
                val col = if (stage == 2) ripe else unripe
                line(stalkTop, c, k * 0.016f, p.c(Color(0xFF6A9A3E)))
                when (kind) {
                    Fruit.TOMATO -> {
                        val r = k * if (stage == 2) 0.11f else 0.08f
                        sphere(c, r, col, p.simple)
                        leaf(p, Offset(c.x, c.y - r * 0.85f), deg(-90f), r * 0.7f, r * 0.3f, p.c(Color(0xFF4F8F3A)))
                    }
                    Fruit.CHERRY -> sphere(c, k * if (stage == 2) 0.058f else 0.045f, col, p.simple)
                    Fruit.EGGPLANT -> {
                        val s = if (stage == 2) 1f else 0.75f
                        shadedOval(Offset(c.x, c.y + k * 0.05f), k * 0.075f * s, k * 0.13f * s, if (stage == 2) ripe else unripe, p.simple)
                        shadedOval(Offset(c.x, c.y - k * 0.07f * s), k * 0.06f * s, k * 0.03f * s, p.c(Color(0xFF5E8F3A)), true)
                    }
                    Fruit.PEPPER -> {
                        val s = if (stage == 2) 1f else 0.8f
                        val tl = Offset(c.x - k * 0.09f * s, c.y - k * 0.08f * s)
                        val sz = Size(k * 0.18f * s, k * 0.2f * s)
                        drawRoundRect(col.darker(0.3f), Offset(tl.x + k * 0.012f, tl.y + k * 0.015f), sz, CornerRadius(k * 0.06f))
                        drawRoundRect(col, tl, sz, CornerRadius(k * 0.06f))
                        if (!p.simple) drawRoundRect(col.lighter(0.4f).copy(alpha = 0.5f), Offset(tl.x + sz.width * 0.12f, tl.y + sz.height * 0.1f), Size(sz.width * 0.25f, sz.height * 0.5f), CornerRadius(k * 0.03f))
                    }
                    Fruit.CHILI -> {
                        val col2 = if (stage == 2) ripe else unripe
                        leaf(p, c, deg(96f), k * 0.3f, k * 0.06f, col2)
                    }
                }
            }
        }
    }
}

// --- Racines ---------------------------------------------------------------------------------------------------

private enum class Fane { FEATHER, BROAD, STALK }

private fun DrawScope.feather(p: Sub, o: Offset, angle: Float, len: Float, color: Color) {
    val dx = cos(angle)
    val dy = sin(angle)
    val tip = Offset(o.x + dx * len, o.y + dy * len)
    line(o, tip, len * 0.03f, color.darker(0.1f))
    for (j in 0 until 4) {
        val f = 0.3f + j * 0.19f
        val c = Offset(o.x + dx * len * f, o.y + dy * len * f)
        val l = len * 0.3f * (1f - j * 0.15f)
        leaf(p, c, angle - deg(48f), l, l * 0.22f, color)
        leaf(p, c, angle + deg(48f), l, l * 0.22f, color)
    }
    leaf(p, tip, angle, len * 0.2f, len * 0.05f, color)
}

private fun DrawScope.rootCrop(
    p: Sub,
    fane: Fane,
    faneColor: Color,
    shoulder: Color,
    shoulderSize: Float,
    count: Int,
    len: Float,
    rib: Color? = null,
) {
    val k = p.k
    groundShadow(p, k * 0.36f, k * 0.12f)
    val sc = p.c(shoulder)
    // Épaule colorée à fleur de terre, à demi enterrée.
    shadedOval(p.at(0f, -0.03f), k * 0.1f * shoulderSize, k * 0.062f * shoulderSize, sc, p.simple)
    if (shoulderSize > 1.3f) shadedOval(p.at(0.05f, -0.05f), k * 0.06f, k * 0.04f, sc.lighter(0.1f), p.simple)
    val col = p.c(faneColor)
    val o = p.at(0f, -0.07f * shoulderSize)
    val spread = if (fane == Fane.STALK) 70f else 120f
    val order = (0 until count).sortedByDescending { abs(it - (count - 1) / 2f) }
    for (i in order) {
        val t = if (count == 1) 0f else (i - (count - 1) / 2f) / ((count - 1) / 2f)
        val a = deg(-90f + t * spread / 2f + p.rnd.range(-6f, 6f))
        val l = k * len * (1f - abs(t) * 0.25f) * p.rnd.range(0.92f, 1.08f)
        when (fane) {
            Fane.FEATHER -> feather(p, o, a, l, col)
            Fane.BROAD -> leaf(p, o, a, l, l * 0.38f, col, rib?.let { p.c(it) })
            Fane.STALK -> {
                leaf(p, o, a, l, l * 0.085f, p.c(Color(0xFFC6E39A)))
                val tip = Offset(o.x + cos(a) * l, o.y + sin(a) * l)
                leaf(p, tip, a - deg(35f), l * 0.3f, l * 0.12f, col)
                leaf(p, tip, a + deg(35f), l * 0.3f, l * 0.12f, col)
                leaf(p, tip, a, l * 0.26f, l * 0.1f, col.lighter(0.08f))
            }
        }
    }
}

// --- Lames dressées --------------------------------------------------------------------------------------------

private fun DrawScope.blades(
    p: Sub,
    color: Color,
    count: Int,
    height: Float,
    wid: Float,
    spread: Float,
    bulb: Color? = null,
    bulbSize: Float = 1f,
    pompom: Color? = null,
) {
    val k = p.k
    groundShadow(p, k * 0.32f, k * 0.11f)
    val col = p.c(color)
    if (bulb != null) shadedOval(p.at(0f, -0.06f * bulbSize), k * 0.1f * bulbSize, k * 0.075f * bulbSize, p.c(bulb), p.simple)
    val order = (0 until count).map { (it + 0.5f) / count * 2f - 1f }.sortedByDescending { abs(it) }
    for (t in order) {
        val o = p.at(t * 0.1f, -0.03f)
        val h = height * (1f - 0.25f * abs(t)) * p.rnd.range(0.85f, 1.1f)
        val tip = p.at(t * spread + p.rnd.range(-0.04f, 0.04f), -h)
        drawBlade(p, o, tip, k * wid, t * k * 0.08f, col.lighter(abs(t) * 0.04f))
        if (pompom != null && p.growth > 0.7f && abs(t) < 0.6f) sphere(tip, k * 0.055f, p.c(pompom), p.simple)
    }
}

private fun DrawScope.leek(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.3f, k * 0.11f)
    val shaftTop = p.at(0f, -0.3f)
    val white = p.c(Color(0xFFEAF2D4))
    drawRoundRect(white.darker(0.2f), Offset(shaftTop.x - k * 0.05f + k * 0.012f, shaftTop.y + k * 0.01f), Size(k * 0.1f, k * 0.32f), CornerRadius(k * 0.03f))
    drawRoundRect(white, Offset(shaftTop.x - k * 0.05f, shaftTop.y), Size(k * 0.1f, k * 0.32f), CornerRadius(k * 0.03f))
    val col = p.c(Color(0xFF4E8F6B))
    for (a in listOf(-150f, -35f, -125f, -58f, -105f, -78f)) {
        leaf(p, shaftTop, deg(a), k * 0.5f * p.rnd.range(0.9f, 1.05f), k * 0.1f, col.lighter(if (abs(a + 90f) < 30f) 0.08f else 0f))
    }
}

private fun DrawScope.asparagus(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.3f, k * 0.1f)
    val col = p.c(Color(0xFF9CC96A))
    for (i in 0 until 4) {
        val t = (i - 1.5f) / 1.5f
        val tip = p.at(t * 0.2f, -1.05f * p.rnd.range(0.85f, 1.05f))
        val o = p.at(t * 0.03f, -0.02f)
        line(o, tip, k * 0.025f, col.darker(0.1f))
        for (j in 0 until 13) {
            val f = 0.32f + j * 0.052f
            val c = Offset(o.x + (tip.x - o.x) * f, o.y + (tip.y - o.y) * f)
            val l = k * 0.17f * (1f - abs(f - 0.62f) * 0.9f)
            val a = if (j % 2 == 0) -150f else -30f
            line(c, Offset(c.x + cos(deg(a)) * l, c.y + sin(deg(a)) * l * 0.8f), k * 0.012f, col)
            line(c, Offset(c.x + cos(deg(a + (if (j % 2 == 0) 40f else -40f))) * l * 0.7f, c.y + sin(deg(a + (if (j % 2 == 0) 40f else -40f))) * l * 0.55f), k * 0.01f, col.lighter(0.12f))
        }
        if (p.ripe) sphere(Offset(tip.x, tip.y + k * 0.28f), k * 0.03f, p.c(Color(0xFFD8362C)), true)
    }
}

private fun DrawScope.corn(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.3f, k * 0.11f)
    val h = 1.9f
    line(p.at(0f, 0f), p.at(0f, -h), k * 0.07f, p.c(Color(0xFF7FAE4A)))
    val col = p.c(Color(0xFF5E9E3A))
    for (i in 0 until 7) {
        val t = 0.14f + i * 0.115f
        val o = p.at(0f, -t * h)
        val side = if (i % 2 == 0) -1f else 1f
        val a = if (side < 0f) deg(-155f + i * 3f) else deg(-25f - i * 3f)
        leaf(p, o, a, k * 0.8f * (1f - i * 0.06f), k * 0.1f, col.lighter(i * 0.015f))
    }
    if (p.growth > 0.6f) {
        val e = p.at(0.08f, -1.0f)
        shadedOval(e, k * 0.06f, k * 0.14f, p.c(Color(0xFFB9D48A)), p.simple)
        for (j in 0 until 4) line(Offset(e.x, e.y - k * 0.13f), Offset(e.x + (j - 1.5f) * k * 0.03f, e.y - k * 0.24f), k * 0.012f, p.c(Color(0xFFE2C65A)))
    }
    // Panicule : quelques épis fins dorés au sommet.
    val top = p.at(0f, -h)
    for (j in 0 until 7) {
        val a = deg(-90f + (j - 3f) * 16f)
        line(top, Offset(top.x + cos(a) * k * 0.3f, top.y + sin(a) * k * 0.3f), k * 0.018f, p.c(Color(0xFFE2C65A)))
    }
}

// --- Légumineuses ----------------------------------------------------------------------------------------------

private enum class Pod { BEAN, PEA, BROAD }

private fun DrawScope.climber(p: Sub, pod: Pod, leafColor: Color, podColor: Color, flower: Color, stakes: Int = 4, height: Float = 1.35f) {
    val k = p.k
    groundShadow(p, k * 0.4f, k * 0.14f)
    val wood = p.c(Color(0xFF9A7448))
    val apex = p.at(0f, -height)
    val feet = (0 until stakes).map { i ->
        val a = TAU * i / stakes + 0.4f
        p.at(cos(a) * 0.32f, sin(a) * 0.16f)
    }
    for (f in feet.filter { it.y < p.base.y }) line(f, apex, k * 0.028f, wood)
    val leafCol = p.c(leafColor)
    val tmax = 0.3f + 0.7f * ((p.growth - 0.25f) / 0.75f).coerceIn(0f, 1f)
    for (f in feet.sortedBy { it.y }) {
        fun pt(t: Float) = Offset(f.x + (apex.x - f.x) * t, f.y + (apex.y - f.y) * t)
        line(f, pt(tmax), k * 0.02f, p.c(Color(0xFF5E9440)))
        var t = 0.14f
        var n = 0
        while (t <= tmax) {
            val c = pt(t)
            val sz = k * 0.15f * (1f - t * 0.3f)
            leaf(p, c, deg(-92f), sz, sz * 0.5f, leafCol)
            leaf(p, c, deg(-35f), sz * 0.85f, sz * 0.42f, leafCol.lighter(0.06f))
            leaf(p, c, deg(-145f), sz * 0.85f, sz * 0.42f, leafCol.lighter(0.06f))
            if (n % 2 == 1 && !p.simple) curl(c, if (f.x < p.base.x) -1f else 1f, k * 0.09f, p.c(Color(0xFF7FB05A)))
            if (p.growth in 0.35f..0.7f && n % 2 == 0) flowerHead(p, Offset(c.x + k * 0.05f, c.y - k * 0.03f), k * 0.045f, 5, p.c(flower), p.c(Color(0xFFE8D26A)))
            t += 0.17f
            n++
        }
        if (p.ripe) {
            for (tp in listOf(0.38f, 0.58f)) {
                if (tp > tmax) continue
                val c = pt(tp)
                val col = p.c(podColor)
                when (pod) {
                    Pod.BEAN -> leaf(p, Offset(c.x + k * 0.03f, c.y), deg(92f), k * 0.3f, k * 0.022f, col)
                    Pod.PEA -> leaf(p, Offset(c.x + k * 0.03f, c.y), deg(95f), k * 0.18f, k * 0.04f, col.lighter(0.08f))
                    Pod.BROAD -> leaf(p, Offset(c.x + k * 0.03f, c.y), deg(92f), k * 0.22f, k * 0.055f, col)
                }
            }
        }
    }
    for (f in feet.filter { it.y >= p.base.y }) line(f, apex, k * 0.028f, wood)
    sphere(apex, k * 0.03f, wood.darker(0.2f), true)
}

private fun DrawScope.dome(p: Sub, color: Color, n: Int, leafLen: Float, leafWid: Float, radius: Float, domeH: Float) {
    val k = p.k
    val col = p.c(color)
    class Spot(val dx: Float, val dy: Float, val h: Float)
    val spots = (0 until n).map {
        val a = p.rnd.next() * TAU
        val r = sqrt(p.rnd.next()) * radius
        val h = (1f - (r / radius) * (r / radius)) * domeH
        Spot(cos(a) * r, sin(a) * r * 0.5f, h)
    }.sortedBy { it.dy - it.h * 0.4f }
    for (s in spots) {
        val o = p.at(s.dx, s.dy - s.h - 0.03f)
        val a = atan2(-0.75f + s.dy * 0.4f, s.dx * 2f + p.rnd.range(-0.2f, 0.2f))
        leaf(p, o, a, k * leafLen * p.rnd.range(0.85f, 1.15f), k * leafWid, col.lighter(s.h / domeH * 0.22f + p.rnd.range(-0.04f, 0.06f)))
    }
}

private fun DrawScope.legumeBush(p: Sub, leafColor: Color, podColor: Color, podLen: Float, podWid: Float) {
    val k = p.k
    groundShadow(p, k * 0.4f, k * 0.14f)
    dome(p, leafColor, 16, 0.2f, 0.09f, 0.3f, 0.3f)
    if (p.ripe) {
        val col = p.c(podColor)
        for (i in 0 until 5) {
            val x = -0.22f + i * 0.11f
            leaf(p, p.at(x, -0.16f + (i % 2) * 0.03f), deg(90f + (i - 2) * 6f), k * podLen, k * podWid, col)
        }
    } else if (p.growth > 0.35f) {
        for (i in 0 until 3) flowerHead(p, p.at(-0.15f + i * 0.15f, -0.3f), k * 0.045f, 5, p.c(Color(0xFFF4EFE0)), p.c(Color(0xFFE8D26A)))
    }
}

// --- Cucurbitacées ---------------------------------------------------------------------------------------------

private enum class Gourd { ZUCCHINI, CUCUMBER, MELON, WATERMELON, PUMPKIN, BUTTERNUT }

private fun DrawScope.lobedLeaf(p: Sub, c: Offset, r: Float, base: Color) {
    for (i in 0 until 5) {
        val a = TAU * i / 5f + deg(-90f)
        shadedOval(Offset(c.x + cos(a) * r * 0.5f, c.y + sin(a) * r * 0.4f), r * 0.42f, r * 0.34f, base, p.simple)
    }
    shadedOval(c, r * 0.5f, r * 0.4f, base.lighter(0.04f), p.simple)
    if (!p.simple) for (i in 0 until 5) {
        val a = TAU * i / 5f + deg(-90f)
        line(c, Offset(c.x + cos(a) * r * 0.8f, c.y + sin(a) * r * 0.62f), r * 0.03f, base.lighter(0.4f).copy(alpha = 0.5f))
    }
}

private fun DrawScope.cucurbit(p: Sub, leafColor: Color, kind: Gourd, fruit: Color) {
    val k = p.k
    groundShadow(p, k * 0.5f, k * 0.17f)
    val col = p.c(leafColor)
    val placements = listOf(
        -0.3f to -0.18f, 0.3f to -0.2f, 0f to -0.32f, -0.34f to 0.04f, 0.34f to 0.02f,
    ).sortedBy { it.second }
    val front = p.at(0f, 0f)
    for ((i, pl) in placements.withIndex()) {
        val c = p.at(pl.first, pl.second - 0.06f)
        line(front, c, k * 0.03f, col.darker(0.2f))
        lobedLeaf(p, c, k * (0.24f - i * 0.01f), col.lighter(i * 0.03f))
    }
    val fc = p.c(fruit)
    val fl = p.at(0.02f, 0.07f)
    val stage = if (p.growth < 0.6f) 0 else if (p.ripe) 2 else 1
    if (stage == 0) {
        flowerHead(p, p.at(0.06f, -0.3f), k * 0.1f, 6, p.c(Color(0xFFF6C531)), p.c(Color(0xFFD99A18)), petalLen = 0.5f, petalWid = 0.22f)
        return
    }
    val sz = if (stage == 2) 1f else 0.7f
    when (kind) {
        Gourd.ZUCCHINI, Gourd.CUCUMBER -> {
            val len = if (kind == Gourd.ZUCCHINI) 0.2f else 0.26f
            val w = if (kind == Gourd.ZUCCHINI) 0.12f else 0.095f
            val a = Offset(fl.x - k * len * sz, fl.y - k * 0.02f)
            val b = Offset(fl.x + k * len * sz, fl.y + k * 0.05f)
            line(Offset(a.x + k * 0.01f, a.y + k * 0.012f), Offset(b.x + k * 0.01f, b.y + k * 0.012f), k * w * sz, fc.darker(0.35f))
            line(a, b, k * w * sz, fc)
            if (!p.simple) line(Offset(a.x + k * 0.04f, a.y - k * w * sz * 0.22f), Offset(b.x - k * 0.08f, b.y - k * w * sz * 0.22f), k * w * sz * 0.22f, fc.lighter(0.4f).copy(alpha = 0.6f))
        }
        Gourd.MELON, Gourd.WATERMELON -> {
            val r = k * (if (kind == Gourd.MELON) 0.17f else 0.21f) * sz
            sphere(fl, r, fc, p.simple)
            if (!p.simple) {
                val stripe = if (kind == Gourd.MELON) fc.darker(0.18f) else p.c(Color(0xFF1F5E30))
                for (j in -1..1) {
                    val path = Path().apply {
                        moveTo(fl.x + j * r * 0.5f, fl.y - r * 0.95f)
                        quadraticBezierTo(fl.x + j * r * 0.95f, fl.y, fl.x + j * r * 0.45f, fl.y + r * 0.9f)
                    }
                    drawPath(path, stripe.copy(alpha = 0.7f), style = Stroke(width = max(1f, r * 0.12f), cap = StrokeCap.Round))
                }
            }
        }
        Gourd.PUMPKIN -> {
            shadedOval(fl, k * 0.19f * sz, k * 0.14f * sz, fc, p.simple)
            if (!p.simple) for (j in -1..1) line(Offset(fl.x + j * k * 0.08f * sz, fl.y - k * 0.12f * sz), Offset(fl.x + j * k * 0.11f * sz, fl.y + k * 0.12f * sz), k * 0.012f, fc.darker(0.3f).copy(alpha = 0.7f))
            line(Offset(fl.x, fl.y - k * 0.13f * sz), Offset(fl.x, fl.y - k * 0.2f * sz), k * 0.03f, p.c(Color(0xFF6B8F3A)))
        }
        Gourd.BUTTERNUT -> {
            shadedOval(Offset(fl.x + k * 0.07f * sz, fl.y), k * 0.13f * sz, k * 0.11f * sz, fc, p.simple)
            shadedOval(Offset(fl.x - k * 0.09f * sz, fl.y - k * 0.01f), k * 0.1f * sz, k * 0.07f * sz, fc.lighter(0.05f), p.simple)
        }
    }
}

// --- Aromatiques -----------------------------------------------------------------------------------------------

private fun DrawScope.umbel(p: Sub, c: Offset, r: Float, color: Color) {
    for (i in 0 until 9) {
        val a = deg(-180f + i * 22.5f)
        val tip = Offset(c.x + cos(a) * r, c.y + sin(a) * r * 0.7f)
        line(c, tip, r * 0.06f, color.darker(0.15f))
        drawCircle(color, max(1f, r * 0.12f), tip)
    }
}

private fun DrawScope.feathery(p: Sub, color: Color, stems: Int, height: Float, flower: Boolean, bulb: Boolean) {
    val k = p.k
    groundShadow(p, k * 0.34f, k * 0.12f)
    val col = p.c(color)
    if (bulb) shadedOval(p.at(0f, -0.08f), k * 0.12f, k * 0.1f, p.c(Color(0xFFDCE8BF)), p.simple)
    for (i in 0 until stems) {
        val t = (i - (stems - 1) / 2f) / max(1f, (stems - 1) / 2f)
        val o = p.at(t * 0.06f, -0.03f)
        val tip = p.at(t * 0.26f, -height * p.rnd.range(0.85f, 1.05f))
        line(o, tip, k * 0.02f, col.darker(0.1f))
        for (j in 0 until 6) {
            val f = 0.4f + j * 0.1f
            val c = Offset(o.x + (tip.x - o.x) * f, o.y + (tip.y - o.y) * f)
            for (side in listOf(-1f, 1f)) {
                val a = deg(-90f + side * 62f)
                line(c, Offset(c.x + cos(a) * k * 0.14f, c.y + sin(a) * k * 0.1f), k * 0.009f, col)
            }
        }
        if (flower && p.growth > 0.6f) umbel(p, tip, k * 0.1f, p.c(Color(0xFFE9D44C)))
    }
}

private fun DrawScope.rosemary(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.36f, k * 0.12f)
    val col = p.c(Color(0xFF4F7A55))
    for (i in 0 until 14) {
        val t = (i - 6.5f) / 6.5f
        val a = deg(-90f + t * 58f + p.rnd.range(-6f, 6f))
        val l = k * 0.55f * (1f - abs(t) * 0.25f) * p.rnd.range(0.85f, 1.05f)
        val o = p.at(t * 0.04f, -0.03f)
        val tip = Offset(o.x + cos(a) * l, o.y + sin(a) * l)
        line(o, tip, k * 0.03f, col.darker(0.2f))
        line(Offset(o.x - k * 0.006f, o.y), Offset(tip.x - k * 0.006f, tip.y), k * 0.014f, col.lighter(0.25f))
        if (p.growth > 0.7f && i % 3 == 0) sphere(tip, k * 0.028f, p.c(Color(0xFF8EA7E8)), true)
    }
}

private fun DrawScope.herb(p: Sub, color: Color, n: Int, leafLen: Float, leafWid: Float, extra: Color? = null) {
    val k = p.k
    groundShadow(p, k * 0.42f, k * 0.15f)
    dome(p, color, n, leafLen, leafWid, 0.32f, 0.3f)
    if (extra != null && p.growth > 0.65f) {
        for (i in 0 until 5) sphere(p.at(-0.2f + i * 0.1f, -0.3f - (i % 2) * 0.04f), k * 0.025f, p.c(extra), true)
    }
}

// --- Fleurs ----------------------------------------------------------------------------------------------------

private class Bloom(val petals: Int, val petal: Color, val core: Color, val inner: Color? = null, val radius: Float = 0.1f, val len: Float = 0.42f, val wid: Float = 0.2f)

private fun DrawScope.bloomPlant(p: Sub, leafColor: Color, leafLen: Float, leafWid: Float, bloom: Bloom) {
    val k = p.k
    groundShadow(p, k * 0.4f, k * 0.14f)
    dome(p, leafColor, 14, leafLen, leafWid, 0.3f, 0.2f)
    val spots = listOf(-0.19f to -0.36f, 0f to -0.47f, 0.2f to -0.38f, -0.07f to -0.3f, 0.12f to -0.28f).sortedBy { it.second }
    val open = ((p.growth - 0.45f) / 0.35f).coerceIn(0f, 1f)
    for (s in spots) {
        val c = p.at(s.first, s.second)
        line(p.at(s.first * 0.3f, -0.15f), c, k * 0.014f, p.c(Color(0xFF5E8F3A)))
        if (open <= 0.05f) {
            sphere(c, k * 0.035f, p.c(Color(0xFF9CB84F)), true)
        } else {
            flowerHead(p, c, k * bloom.radius * (0.5f + open * 0.5f), bloom.petals, p.c(bloom.petal), p.c(bloom.core), bloom.inner?.let { p.c(it) }, bloom.len, bloom.wid)
        }
    }
}

private fun DrawScope.nasturtium(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.46f, k * 0.16f)
    val col = p.c(Color(0xFF5FA544))
    val pads = listOf(-0.3f to -0.06f, 0.28f to -0.08f, -0.12f to -0.14f, 0.12f to -0.2f, 0f to -0.02f, -0.28f to -0.24f, 0.3f to -0.26f)
        .sortedBy { it.second }
    for ((i, pad) in pads.withIndex()) {
        val c = p.at(pad.first, pad.second)
        val r = k * (0.16f - i * 0.004f)
        shadedOval(c, r, r * 0.78f, col.lighter(i * 0.02f), p.simple)
        if (!p.simple) for (j in 0 until 6) {
            val a = TAU * j / 6f
            line(c, Offset(c.x + cos(a) * r * 0.85f, c.y + sin(a) * r * 0.66f), r * 0.04f, col.lighter(0.4f).copy(alpha = 0.5f))
        }
    }
    val open = ((p.growth - 0.45f) / 0.35f).coerceIn(0f, 1f)
    if (open > 0.05f) {
        for (s in listOf(-0.2f to -0.3f, 0.18f to -0.34f, 0f to -0.4f)) {
            flowerHead(p, p.at(s.first, s.second), k * 0.085f * (0.5f + open * 0.5f), 5, p.c(Color(0xFFE8571C)), p.c(Color(0xFFF4C430)), petalLen = 0.5f, petalWid = 0.3f)
        }
    }
}

private fun DrawScope.sunflower(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.3f, k * 0.11f)
    val h = 1.9f
    line(p.at(0f, 0f), p.at(0f, -h), k * 0.07f, p.c(Color(0xFF6F9A3F)))
    val col = p.c(Color(0xFF4F8F3A))
    for (i in 0 until 6) {
        val t = 0.15f + i * 0.12f
        val side = if (i % 2 == 0) -1f else 1f
        leaf(p, p.at(0f, -t * h), if (side < 0f) deg(-160f) else deg(-20f), k * 0.42f, k * 0.19f, col.lighter(i * 0.02f))
    }
    val c = p.at(0f, -h - 0.02f)
    if (p.growth < 0.55f) {
        sphere(c, k * 0.1f, p.c(Color(0xFF8DB24A)), p.simple)
        return
    }
    val r = k * 0.3f * (0.6f + 0.4f * ((p.growth - 0.55f) / 0.45f).coerceIn(0f, 1f))
    flowerHead(p, c, r, 16, p.c(Color(0xFFF2C21B)), p.c(Color(0xFF5B3A1F)), null, 0.46f, 0.15f)
    drawCircle(p.c(Color(0xFF3F2814)), r * 0.2f, Offset(c.x + r * 0.05f, c.y + r * 0.05f))
    drawCircle(p.c(Color(0xFF7A5230)), r * 0.42f, c, style = Stroke(width = r * 0.1f))
}

private fun DrawScope.strawberry(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.4f, k * 0.14f)
    val col = p.c(Color(0xFF4F9A45))
    val spots = (0 until 6).map { i ->
        val a = TAU * i / 6f + 0.3f
        Offset(cos(a) * 0.2f, sin(a) * 0.1f - 0.06f)
    }.sortedBy { it.y }
    for (s in spots) {
        val o = p.at(s.x * 0.5f, s.y * 0.6f - 0.05f)
        val a = atan2(s.y * 1.6f - 0.35f, s.x * 3f)
        leaf(p, o, a, k * 0.27f, k * 0.14f, col)
        leaf(p, o, a - deg(36f), k * 0.22f, k * 0.12f, col.lighter(0.05f))
        leaf(p, o, a + deg(36f), k * 0.22f, k * 0.12f, col.lighter(0.05f))
    }
    val stage = when {
        p.growth < 0.45f -> 0
        !p.ripe -> 1
        else -> 2
    }
    val spotsF = listOf(-0.2f to 0.0f, 0.16f to 0.03f, -0.04f to 0.08f, 0.26f to -0.08f)
    for (s in spotsF) {
        val c = p.at(s.first, s.second - 0.02f)
        if (stage == 0) {
            flowerHead(p, p.at(s.first, s.second - 0.22f), k * 0.055f, 5, p.c(Color(0xFFFBF8EE)), p.c(Color(0xFFE8C531)))
        } else {
            val fc = p.c(if (stage == 2) Color(0xFFD8283A) else Color(0xFFC4D68A))
            shadedOval(c, k * 0.05f, k * 0.06f, fc, p.simple)
            if (!p.simple && stage == 2) for (j in 0 until 3) drawCircle(p.c(Color(0xFFF0DC7A)), max(1f, k * 0.006f), Offset(c.x + (j - 1f) * k * 0.02f, c.y + (j % 2) * k * 0.025f))
            leaf(p, Offset(c.x, c.y - k * 0.05f), deg(-90f), k * 0.04f, k * 0.03f, p.c(Color(0xFF3F8F3A)))
        }
    }
}

// --- Cultures tropicales ---------------------------------------------------------------------------------------

/** Feuille en cœur attachée en [c] : la pointe est à [r] de là, dans la direction [angle] (radians écran, -90° = vers le haut). */
private fun DrawScope.heartLeaf(p: Sub, c: Offset, angle: Float, r: Float, base: Color) {
    val phi = angle + deg(90f)
    val cs = cos(phi)
    val sn = sin(phi)
    // Contour dans un repère où la pointe est en (0, -1) et l'échancrure en (0, 0,1), puis tourné et décalé.
    fun heart(o: Offset, s: Float): Path {
        fun pt(x: Float, y: Float) = Offset(o.x + (x * cs - y * sn) * s, o.y + (x * sn + y * cs) * s)
        return Path().apply {
            val a = pt(0f, 0.1f)
            moveTo(a.x, a.y)
            fun curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
                val c1 = pt(x1, y1)
                val c2 = pt(x2, y2)
                val c3 = pt(x3, y3)
                cubicTo(c1.x, c1.y, c2.x, c2.y, c3.x, c3.y)
            }
            curve(-0.2f, 0.5f, -1f, 0.4f, -0.85f, -0.2f)
            curve(-0.7f, -0.6f, -0.25f, -0.7f, 0f, -1f)
            curve(0.25f, -0.7f, 0.7f, -0.6f, 0.85f, -0.2f)
            curve(1f, 0.4f, 0.2f, 0.5f, 0f, 0.1f)
            close()
        }
    }
    drawPath(heart(Offset(c.x + r * 0.07f, c.y + r * 0.08f), r), base.darker(0.3f))
    drawPath(heart(c, r), base)
    if (p.simple) return
    drawPath(heart(Offset(c.x - r * 0.1f, c.y - r * 0.12f), r * 0.55f), base.lighter(0.25f).copy(alpha = 0.6f))
    line(c, Offset(c.x + cos(angle) * r * 0.9f, c.y + sin(angle) * r * 0.9f), r * 0.03f, base.lighter(0.45f).copy(alpha = 0.55f))
}

/** Patate douce : tiges violacées qui rampent, feuilles en cœur au ras du sol, tubercules rose-orangé affleurant à la récolte. */
private fun DrawScope.sweetPotato(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.46f, k * 0.16f)
    val col = p.c(Color(0xFF4F9A45))
    val vine = p.c(Color(0xFF7A5A8A))
    val pads = listOf(-0.3f to -0.04f, 0.3f to -0.06f, -0.14f to -0.12f, 0.14f to -0.18f, 0f to 0f, -0.3f to -0.22f, 0.3f to -0.24f, 0f to -0.28f)
        .sortedBy { it.second }
    for ((i, pad) in pads.withIndex()) {
        val c = p.at(pad.first, pad.second)
        line(p.at(0f, -0.02f), c, k * 0.016f, vine)
        heartLeaf(p, c, deg(-90f + pad.first * 100f + p.rnd.range(-12f, 12f)), k * (0.17f - i * 0.004f), col.lighter(i * 0.015f))
    }
    if (p.growth in 0.45f..0.85f) {
        for (s in listOf(-0.18f to -0.3f, 0.2f to -0.26f)) {
            flowerHead(p, p.at(s.first, s.second), k * 0.05f, 5, p.c(Color(0xFFC9A6E8)), p.c(Color(0xFF7A4FA8)), petalLen = 0.55f, petalWid = 0.35f)
        }
    }
    if (p.ripe) {
        shadedOval(p.at(-0.2f, 0.04f), k * 0.1f, k * 0.05f, p.c(Color(0xFFC46A4A)), p.simple)
        shadedOval(p.at(0.18f, 0.06f), k * 0.09f, k * 0.045f, p.c(Color(0xFFB85A48)), p.simple)
    }
}

/** Igname : liane qui s'enroule sur un tuteur de 2 m, feuilles en cœur alternes ; le tubercule brun affleure à la récolte. */
private fun DrawScope.yamVine(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.3f, k * 0.11f)
    val h = 1.7f
    val wood = p.c(Color(0xFF9A7448))
    line(p.at(0f, 0.02f), p.at(0f, -h), k * 0.035f, wood)
    val tmax = 0.3f + 0.7f * ((p.growth - 0.25f) / 0.75f).coerceIn(0f, 1f)
    val vine = p.c(Color(0xFF6A8F3A))
    val col = p.c(Color(0xFF3F8A3F))
    var prev = p.at(0f, 0f)
    var t = 0.04f
    while (t <= tmax * 0.96f) {
        val next = p.at(sin(t * 24f) * 0.07f, -t * h)
        line(prev, next, k * 0.02f, vine)
        prev = next
        t += 0.04f
    }
    var n = 0
    t = 0.12f
    while (t <= tmax) {
        val c = p.at(sin(t * 24f) * 0.07f, -t * h)
        val side = if (n % 2 == 0) -1f else 1f
        val a = if (side < 0f) deg(-150f + p.rnd.range(-8f, 8f)) else deg(-30f + p.rnd.range(-8f, 8f))
        heartLeaf(p, c, a, k * 0.2f * (1f - t * 0.3f), col.lighter(n % 3 * 0.04f))
        t += 0.1f
        n++
    }
    if (p.ripe) shadedOval(p.at(0.13f, 0.03f), k * 0.12f, k * 0.045f, p.c(Color(0xFF8A6446)), p.simple)
    sphere(p.at(0f, -h), k * 0.03f, wood.darker(0.2f), true)
}

/** Madère (taro) : grandes feuilles en cœur au bout de longs pétioles dressés. */
private fun DrawScope.taro(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.42f, k * 0.15f)
    val col = p.c(Color(0xFF3F8A4A))
    val stem = p.c(Color(0xFF6FA55A))
    val stems = listOf(-0.34f to 0.6f, 0.34f to 0.64f, 0.16f to 0.92f, -0.15f to 0.88f, 0f to 0.74f).sortedByDescending { it.second }
    for ((i, s) in stems.withIndex()) {
        val tip = p.at(s.first, -s.second)
        line(p.at(s.first * 0.25f, -0.02f), tip, k * 0.03f, stem)
        heartLeaf(p, tip, deg(-90f + s.first * 130f), k * 0.3f, col.lighter(i * 0.025f))
    }
}

/** Papayer : tronc fin cicatrisé par les anciennes feuilles, couronne de feuilles palmées, fruits en grappe sous la couronne. */
private fun DrawScope.papaya(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.3f, k * 0.11f)
    val h = 1.75f
    val trunk = p.c(Color(0xFFA6A070))
    fun trunkPath(shift: Float, widen: Float) = Path().apply {
        moveTo(p.base.x - k * 0.058f * widen + shift, p.base.y)
        lineTo(p.base.x - k * 0.036f * widen + k * 0.02f + shift, p.base.y - k * h)
        lineTo(p.base.x + k * 0.036f * widen + k * 0.02f + shift, p.base.y - k * h)
        lineTo(p.base.x + k * 0.058f * widen + shift, p.base.y)
        close()
    }
    drawPath(trunkPath(k * 0.012f, 1f), trunk.darker(0.3f))
    drawPath(trunkPath(0f, 1f), trunk)
    if (!p.simple) {
        drawPath(trunkPath(-k * 0.014f, 0.3f), trunk.lighter(0.3f).copy(alpha = 0.6f))
        for (i in 1..9) {
            val y = p.base.y - k * h * i / 10f
            val half = k * (0.05f - 0.018f * i / 10f)
            drawLine(trunk.darker(0.3f).copy(alpha = 0.7f), Offset(p.base.x - half + k * 0.02f * i / 10f, y), Offset(p.base.x + half + k * 0.02f * i / 10f, y), strokeWidth = max(1f, k * 0.008f))
        }
    }
    val crown = p.at(0.02f, -h)
    val leafCol = p.c(Color(0xFF4F9A4A))
    val petiole = p.c(Color(0xFF8FB46A))
    val angles = listOf(-168f, -140f, -112f, -88f, -64f, -38f, -12f).sortedBy { sin(deg(it)) * -1f }
    for ((i, a) in angles.withIndex()) {
        val ar = deg(a)
        val len = k * 0.42f
        val tip = Offset(crown.x + cos(ar) * len, crown.y + sin(ar) * len * 0.7f + k * 0.04f)
        line(crown, tip, k * 0.022f, petiole)
        for (j in -3..3) {
            leaf(p, tip, ar + deg(j * 24f), k * 0.3f * (1f - abs(j) * 0.07f), k * 0.065f, leafCol.lighter((i % 3) * 0.03f))
        }
    }
    if (p.growth > 0.35f) {
        val spots = listOf(-0.07f to 0.14f, 0.09f to 0.16f, -0.02f to 0.28f, 0.07f to 0.34f)
        for ((i, s) in spots.withIndex()) {
            val c = Offset(crown.x + s.first * k, crown.y + s.second * k)
            when {
                p.growth < 0.6f -> sphere(c, k * 0.03f, p.c(Color(0xFFF3EBC4)), true)
                else -> {
                    val col = if (p.ripe) p.c(Color(0xFFE88A2C)) else p.c(Color(0xFF8FB84A))
                    shadedOval(c, k * 0.065f, k * (0.1f - i % 2 * 0.012f), col, p.simple)
                }
            }
        }
    }
}

/** Pois d'angole : arbuste ramifié à petites feuilles, fleurs jaunes puis gousses pendantes. */
private fun DrawScope.pigeonPea(p: Sub) {
    val k = p.k
    groundShadow(p, k * 0.4f, k * 0.14f)
    val stem = p.c(Color(0xFF7A8A4A))
    val col = p.c(Color(0xFF5E9E4A))
    line(p.at(0f, 0.02f), p.at(0.02f, -0.62f), k * 0.05f, stem)
    val reach = 0.35f + 0.65f * ((p.growth - 0.25f) / 0.75f).coerceIn(0f, 1f)
    val branches = listOf(-160f, -140f, -122f, -104f, -86f, -68f, -50f, -32f, -14f).sortedBy { abs(it + 90f) * -1f }
    for ((i, a) in branches.withIndex()) {
        val ar = deg(a)
        val o = p.at(0.02f, -0.3f - (i % 4) * 0.1f)
        val len = k * (0.55f + (i % 3) * 0.1f) * reach
        val tip = Offset(o.x + cos(ar) * len * 0.8f, o.y + sin(ar) * len)
        line(o, tip, k * 0.022f, stem)
        for (j in 1..3) {
            val f = j / 3f
            val c = Offset(o.x + (tip.x - o.x) * f, o.y + (tip.y - o.y) * f)
            leaf(p, c, ar - deg(50f), k * 0.17f, k * 0.06f, col.lighter(j * 0.03f))
            leaf(p, c, ar + deg(50f), k * 0.17f, k * 0.06f, col.lighter(j * 0.03f))
            leaf(p, c, ar, k * 0.18f, k * 0.06f, col.lighter(0.08f))
            if (p.growth in 0.35f..0.75f && j == 2) flowerHead(p, Offset(c.x, c.y - k * 0.05f), k * 0.06f, 5, p.c(Color(0xFFF2C21B)), p.c(Color(0xFFB8321E)))
            if (p.ripe && j == 3) leaf(p, Offset(c.x, c.y + k * 0.02f), deg(98f), k * 0.22f, k * 0.04f, p.c(Color(0xFF9BB04A)), p.c(Color(0xFF8A4A32)))
        }
    }
}

// --- Dispatch par plante ---------------------------------------------------------------------------------------

private fun DrawScope.drawSubject(p: Sub, veg: Vegetable) {
    when (veg.id) {
        "laitue" -> rosette(p, Color(0xFF9BCB5A), 16, 0.46f, 0.3f)
        "chicoree" -> rosette(p, Color(0xFF86B84A), 18, 0.48f, 0.2f)
        "mache" -> rosette(p, Color(0xFF63AE46), 12, 0.32f, 0.2f)
        "epinard" -> rosette(p, Color(0xFF3F8A3A), 12, 0.42f, 0.24f)
        "blette" -> rosette(p, Color(0xFF2F7D3A), 10, 0.5f, 0.26f, rib = Color(0xFFE8D9A8))
        "roquette" -> rosette(p, Color(0xFF55A03C), 14, 0.44f, 0.12f)
        "persil" -> rosette(p, Color(0xFF2E8B3E), 16, 0.36f, 0.17f)
        "chou_kale" -> rosette(p, Color(0xFF3E7F6B), 12, 0.5f, 0.27f)
        "chou_pomme" -> rosette(p, Color(0xFF5E9E7A), 10, 0.5f, 0.3f, Head.CABBAGE, Color(0xFF8FC0A0))
        "chou_fleur" -> rosette(p, Color(0xFF6AA84F), 10, 0.5f, 0.24f, Head.CAULI, Color(0xFFF1EBD0))
        "brocoli" -> rosette(p, Color(0xFF4F8F5A), 10, 0.5f, 0.24f, Head.BROCCOLI, Color(0xFF3E8A3A))
        "chou_rave" -> rosette(p, Color(0xFF5FA55A), 8, 0.4f, 0.17f, Head.KOHLRABI, Color(0xFFB8D98A))

        "tomate" -> fruitBush(p, Fruit.TOMATO, Color(0xFF4F9A3F), Color(0xFFD8352A))
        "tomate_cerise" -> fruitBush(p, Fruit.CHERRY, Color(0xFF4F9A3F), Color(0xFFE0402C), 0.95f)
        "aubergine" -> fruitBush(p, Fruit.EGGPLANT, Color(0xFF5C9547), Color(0xFF5B2A6E), 0.85f, Color(0xFFB08ADB))
        "poivron" -> fruitBush(p, Fruit.PEPPER, Color(0xFF4F9A3F), Color(0xFFD83A24), 0.8f, Color(0xFFF4EFE0))
        "piment" -> fruitBush(p, Fruit.CHILI, Color(0xFF3F8A3A), Color(0xFFD8321E), 0.75f, Color(0xFFF4EFE0))

        "carotte" -> rootCrop(p, Fane.FEATHER, Color(0xFF5FA544), Color(0xFFEE8A2A), 1f, 7, 0.55f)
        "radis" -> rootCrop(p, Fane.BROAD, Color(0xFF55A03C), Color(0xFFD8305A), 0.9f, 6, 0.36f)
        "betterave" -> rootCrop(p, Fane.BROAD, Color(0xFF3F7A3A), Color(0xFF8A2A52), 1.4f, 6, 0.5f, rib = Color(0xFFB03A55))
        "navet" -> rootCrop(p, Fane.BROAD, Color(0xFF5FA544), Color(0xFFEDE6F0), 1.3f, 6, 0.42f)
        "panais" -> rootCrop(p, Fane.FEATHER, Color(0xFF6FAE48), Color(0xFFEFE2BC), 1.1f, 7, 0.6f)
        "celeri_rave" -> rootCrop(p, Fane.BROAD, Color(0xFF4F8F3A), Color(0xFFC9B58C), 1.6f, 6, 0.5f)
        "celeri_branche" -> rootCrop(p, Fane.STALK, Color(0xFF5FA544), Color(0xFFC6E39A), 0.8f, 7, 0.62f)

        "oignon" -> blades(p, Color(0xFF6FAE4A), 7, 0.6f, 0.06f, 0.18f, Color(0xFFE2B66A), 1.1f)
        "ail" -> blades(p, Color(0xFF7FAA68), 6, 0.62f, 0.07f, 0.2f, Color(0xFFF1E6DA), 1f)
        "echalote" -> blades(p, Color(0xFF5E9E46), 8, 0.55f, 0.05f, 0.16f, Color(0xFFC9805A), 0.9f)
        "poireau" -> leek(p)
        "ciboulette" -> blades(p, Color(0xFF2F8F46), 14, 0.5f, 0.035f, 0.18f, null, 1f, Color(0xFFB07AD8))
        "asperge" -> asparagus(p)
        "mais_doux" -> corn(p)

        "haricot_rames" -> climber(p, Pod.BEAN, Color(0xFF4F9A3F), Color(0xFF7DB844), Color(0xFFF4EFE0), 4, 1.4f)
        "pois" -> climber(p, Pod.PEA, Color(0xFF5FAE4A), Color(0xFF8FCB5A), Color(0xFFFFFFFF), 3, 1.1f)
        "feve" -> climber(p, Pod.BROAD, Color(0xFF5E9E7A), Color(0xFF6FA84A), Color(0xFFFFFFFF), 3, 1.15f)
        "haricot_nain" -> legumeBush(p, Color(0xFF4F9A3F), Color(0xFF7DB844), 0.26f, 0.022f)
        "lentille" -> legumeBush(p, Color(0xFF6FAE5A), Color(0xFFA6B86A), 0.1f, 0.03f)
        "pois_chiche" -> legumeBush(p, Color(0xFF5E9E4A), Color(0xFFB0C070), 0.12f, 0.04f)

        "courgette" -> cucurbit(p, Color(0xFF3F8A3A), Gourd.ZUCCHINI, Color(0xFF2F6B35))
        "concombre" -> cucurbit(p, Color(0xFF4F9A3F), Gourd.CUCUMBER, Color(0xFF4E8F3A))
        "melon" -> cucurbit(p, Color(0xFF5FA544), Gourd.MELON, Color(0xFFD6D890))
        "courge_butternut" -> cucurbit(p, Color(0xFF4F8F3A), Gourd.BUTTERNUT, Color(0xFFE2B77A))
        "potimarron" -> cucurbit(p, Color(0xFF3F8A3A), Gourd.PUMPKIN, Color(0xFFE0662A))
        "pasteque" -> cucurbit(p, Color(0xFF4F9A3F), Gourd.WATERMELON, Color(0xFF3F8F4A))

        "basilic" -> herb(p, Color(0xFF4FA83F), 24, 0.17f, 0.1f)
        "menthe" -> herb(p, Color(0xFF3E9A4A), 26, 0.15f, 0.07f)
        "thym" -> herb(p, Color(0xFF7E9F6E), 40, 0.08f, 0.035f, Color(0xFFE59BC0))
        "origan" -> herb(p, Color(0xFF5E9A4A), 32, 0.1f, 0.05f, Color(0xFFD9A6E0))
        "sauge" -> herb(p, Color(0xFF9DB89A), 20, 0.2f, 0.09f)
        "coriandre" -> herb(p, Color(0xFF86C45A), 26, 0.13f, 0.07f, Color(0xFFFFFFFF))
        "romarin" -> rosemary(p)
        "aneth" -> feathery(p, Color(0xFF7FB05A), 6, 0.95f, flower = true, bulb = false)
        "fenouil" -> feathery(p, Color(0xFF8FBC6A), 5, 1.0f, flower = true, bulb = true)

        "tagete" -> bloomPlant(p, Color(0xFF3E7A3A), 0.2f, 0.07f, Bloom(12, Color(0xFFF29A1F), Color(0xFFD9701C), Color(0xFFF7C23A), 0.1f))
        "oeillet_d_inde" -> bloomPlant(p, Color(0xFF3C7A3C), 0.2f, 0.07f, Bloom(12, Color(0xFFF0B320), Color(0xFFD97A1C), Color(0xFFB83A18), 0.1f))
        "souci" -> bloomPlant(p, Color(0xFF8EBA4F), 0.22f, 0.08f, Bloom(16, Color(0xFFF4A21B), Color(0xFFB86B22), null, 0.1f, 0.46f, 0.12f))
        "capucine" -> nasturtium(p)
        "bourrache" -> bloomPlant(p, Color(0xFF8BA586), 0.26f, 0.12f, Bloom(5, Color(0xFF4D86E0), Color(0xFF2B2160), null, 0.075f, 0.5f, 0.17f))
        "phacelie" -> bloomPlant(p, Color(0xFF6FA55E), 0.2f, 0.05f, Bloom(7, Color(0xFF9D7ED8), Color(0xFFC9B6EA), null, 0.065f, 0.36f, 0.2f))
        "tournesol" -> sunflower(p)
        "fraise" -> strawberry(p)

        "patate_douce" -> sweetPotato(p)
        "igname" -> yamVine(p)
        "taro_madere" -> taro(p)
        "papayer" -> papaya(p)
        "pois_d_angole" -> pigeonPea(p)

        else -> {
            rosette(p, Color(0xFF3F8A3F), 14, 0.46f, 0.26f)
            if (p.growth > 0.5f) {
                for (i in 0 until 4) flowerHead(p, p.at(-0.15f + i * 0.1f, -0.3f), p.k * 0.05f, 5, p.c(Color(0xFFF4EFE0)), p.c(Color(0xFFE8D26A)))
            }
        }
    }
}
