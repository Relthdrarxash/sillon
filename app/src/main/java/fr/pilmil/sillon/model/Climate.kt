package fr.pilmil.sillon.model

import kotlinx.serialization.Serializable

/**
 * Régime climatique du jardin : il décide de l'ancre du calendrier. En TEMPERATE c'est la dernière gelée ;
 * en TROPICAL (outre-mer, pas de gel) c'est le début de la saison de plantation du territoire.
 */
@Serializable
enum class Climate(val frenchLabel: String) {
    TEMPERATE("Tempéré"),
    TROPICAL("Tropical"),
}
