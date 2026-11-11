package fr.pilmil.sillon.ui.components

import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.Vegetable

/**
 * Pictogramme décoratif d'une plante (jamais seul porteur d'information : le nom reste toujours affiché).
 * À défaut de pictogramme précis, on retombe sur celui de la famille botanique.
 */
fun Vegetable.pictogram(): String = when (id) {
    "tomate", "tomate_cerise" -> "\uD83C\uDF45"
    "aubergine" -> "\uD83C\uDF46"
    "poivron", "piment" -> "\uD83C\uDF36\uFE0F"
    "pomme_de_terre" -> "\uD83E\uDD54"
    "carotte", "panais" -> "\uD83E\uDD55"
    "oignon", "ail", "echalote", "poireau" -> "\uD83E\uDDC5"
    "mais_doux" -> "\uD83C\uDF3D"
    "concombre", "courgette" -> "\uD83E\uDD52"
    "melon", "pasteque" -> "\uD83C\uDF49"
    "courge_butternut", "potimarron" -> "\uD83C\uDF83"
    "fraise" -> "\uD83C\uDF53"
    "laitue", "mache", "chicoree", "epinard", "blette", "roquette", "chou_kale" -> "\uD83E\uDD6C"
    "brocoli", "chou_fleur" -> "\uD83E\uDD66"
    "tournesol" -> "\uD83C\uDF3B"
    "tagete", "oeillet_d_inde", "souci", "bourrache", "phacelie", "capucine" -> "\uD83C\uDF3C"
    "basilic", "menthe", "romarin", "thym", "origan", "sauge", "persil", "coriandre", "aneth", "ciboulette" -> "\uD83C\uDF3F"
    "haricot_nain", "haricot_rames", "pois", "feve", "lentille", "pois_chiche" -> "\uD83E\uDED8"
    "radis", "navet", "betterave", "chou_rave", "celeri_rave" -> "\uD83E\uDD55"
    else -> when (family) {
        BotanicalFamily.BRASSICACEAE -> "\uD83E\uDD66"
        BotanicalFamily.LAMIACEAE, BotanicalFamily.APIACEAE -> "\uD83C\uDF3F"
        else -> "\uD83C\uDF31"
    }
}
