package fr.pilmil.sillon.ui.garden

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Caméra perspective de la scène du potager. Repère monde en mètres : x vers la droite, y vers le haut,
 * z vers le fond (plus grand = plus loin). La caméra est à x = 0, penchée vers le sol (tangage).
 * Pur calcul, sans Compose : seul `Offset` sert de type de point écran. Le dessin (Canvas) et les taps
 * passent par les mêmes formules, d'où l'aller-retour exact entre [project] et [unprojectToPlane].
 */
class GardenProjection private constructor(
    private val focal: Float,
    private val camY: Float,
    private val camZ: Float,
    private val pitchRadians: Float,
    private val centerX: Float,
    private val centerY: Float,
) {
    private val sinP = sin(pitchRadians)
    private val cosP = cos(pitchRadians)

    /** Profondeur le long de l'axe optique (sert de diviseur de perspective). */
    private fun depthOf(y: Float, z: Float) = (camY - y) * sinP + (z - camZ) * cosP

    private fun upOf(y: Float, z: Float) = (y - camY) * cosP + (z - camZ) * sinP

    fun project(x: Float, y: Float, z: Float): Offset {
        val depth = depthOf(y, z)
        return Offset(centerX + focal * x / depth, centerY - focal * upOf(y, z) / depth)
    }

    /** Pixels occupés par un mètre horizontal à la profondeur [z] (au niveau [y]). */
    fun scaleAt(z: Float, y: Float = 0f): Float = focal / depthOf(y, z)

    /**
     * Intersection du rayon de visée du pixel [screen] avec le plan horizontal d'altitude [y].
     * Renvoie (x, z) en mètres, ou null si le rayon ne descend pas vers ce plan (au-dessus de l'horizon).
     */
    fun unprojectToPlane(screen: Offset, y: Float): Pair<Float, Float>? {
        val a = (screen.x - centerX) / focal
        val b = -(screen.y - centerY) / focal
        val dx = a
        val dy = b * cosP - sinP
        val dz = b * sinP + cosP
        if (dy > -1e-6f) return null
        val t = (y - camY) / dy
        if (t <= 0f) return null
        return Pair(dx * t, camZ + dz * t)
    }

    /** Champ de vision vertical résultant, pour information. */
    fun verticalFovDegrees(heightPx: Float): Float =
        Math.toDegrees(2.0 * kotlin.math.atan(heightPx / 2f / focal.toDouble())).toFloat()

    companion object {
        /**
         * Réglage de la scène : un carré de [outerWidth] m posé au bord proche (z = 0) occupe [fill] de la
         * largeur au sol (0,82 : avec le rebord, plus proche de la caméra de 25 cm, la silhouette atteint ~88 %),
         * et son bord lointain mesure [farOverNear] fois le bord proche. [nearEdgeY] est la
         * hauteur écran (px) du bord proche au sol. Le champ de vision découle de ces contraintes.
         */
        fun forScene(
            width: Float,
            height: Float,
            nearEdgeY: Float = height * 0.8f,
            outerWidth: Float = 1.2f,
            fill: Float = 0.82f,
            farOverNear: Float = 0.79f,
            pitchDegrees: Float = 50f,
        ): GardenProjection {
            val pitch = Math.toRadians(pitchDegrees.toDouble()).toFloat()
            val sinP = sin(pitch)
            val cosP = cos(pitch)
            // Profondeur du bord proche telle que depth(z = outerWidth) / depth(0) = 1 / farOverNear.
            val nearDepth = outerWidth * cosP / (1f / farOverNear - 1f)
            val focal = fill * width * nearDepth / outerWidth
            val camZ = -2.5f
            val camY = (nearDepth + camZ * cosP) / sinP
            // On recale le centre optique pour que le bord proche tombe à nearEdgeY.
            val upNear = -camY * cosP - camZ * sinP
            val centerY = nearEdgeY + focal * upNear / nearDepth
            return GardenProjection(focal, camY, camZ, pitch, width / 2f, centerY)
        }
    }
}

/**
 * Emprise d'une parcelle au sol : planches de [wall] m d'épaisseur autour de [rows] x [cols] cases de [cell] m.
 * La rangée 0 est la plus lointaine (z le plus grand), comme dans la grille à plat où elle est en haut.
 */
class ParcelFootprint(
    val x0: Float,
    val z0: Float,
    val rows: Int,
    val cols: Int,
    val cell: Float,
    val wall: Float,
) {
    val outerWidth = cols * cell + 2 * wall
    val outerDepth = rows * cell + 2 * wall
    val x1 = x0 + outerWidth
    val z1 = z0 + outerDepth
    val innerX0 = x0 + wall
    val innerZ0 = z0 + wall
    val innerX1 = x1 - wall
    val innerZ1 = z1 - wall

    fun cellCenterX(col: Int) = innerX0 + (col + 0.5f) * cell

    fun cellCenterZ(row: Int) = innerZ0 + (rows - 1 - row + 0.5f) * cell

    /** Case (row, col) sous le point (x, z) de la terre, ou null hors des cases. */
    fun cellAt(x: Float, z: Float): Pair<Int, Int>? {
        if (x < innerX0 || x >= innerX1 || z < innerZ0 || z >= innerZ1) return null
        val col = ((x - innerX0) / cell).toInt().coerceIn(0, cols - 1)
        val row = rows - 1 - ((z - innerZ0) / cell).toInt().coerceIn(0, rows - 1)
        return row to col
    }

    fun containsOuter(x: Float, z: Float, margin: Float = 0f) =
        x >= x0 - margin && x <= x1 + margin && z >= z0 - margin && z <= z1 + margin
}
