package fr.pilmil.sillon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.Vegetable
import fr.pilmil.sillon.shared.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Ressource vectorielle (CC0, Open Crop Icons) associée à une variété de plante.
 */
fun Vegetable.iconResource(): DrawableResource = when (id) {
    "tomate" -> Res.drawable.veg_tomato
    "tomate_cerise" -> Res.drawable.veg_cherry_tomato
    "aubergine" -> Res.drawable.veg_eggplant
    "poivron" -> Res.drawable.veg_red_bell_pepper
    "piment" -> Res.drawable.veg_cayenne_pepper
    "pomme_de_terre" -> Res.drawable.veg_russet_potato
    "haricot_nain" -> Res.drawable.veg_blue_lake_bean
    "haricot_rames" -> Res.drawable.veg_runner_bean
    "pois" -> Res.drawable.veg_snap_pea
    "feve" -> Res.drawable.veg_broad_bean
    "lentille" -> Res.drawable.veg_soy_bean
    "pois_chiche" -> Res.drawable.veg_lima_bean
    "chou_pomme" -> Res.drawable.veg_green_cabbage
    "chou_fleur" -> Res.drawable.veg_cauliflower
    "brocoli" -> Res.drawable.veg_broccoli
    "radis" -> Res.drawable.veg_cherry_bell_radish
    "navet" -> Res.drawable.veg_turnip
    "roquette" -> Res.drawable.veg_arugula
    "chou_kale" -> Res.drawable.veg_curly_kale
    "chou_rave" -> Res.drawable.veg_kohlrabi
    "carotte" -> Res.drawable.veg_carrot
    "panais" -> Res.drawable.veg_parsnip
    "celeri_rave" -> Res.drawable.veg_celeriac_root
    "celeri_branche" -> Res.drawable.veg_celery
    "oignon" -> Res.drawable.veg_yellow_onion
    "ail" -> Res.drawable.veg_garlic
    "echalote" -> Res.drawable.veg_shallot
    "poireau" -> Res.drawable.veg_leek
    "ciboulette" -> Res.drawable.veg_onion_chive
    "courgette" -> Res.drawable.veg_zucchini
    "concombre" -> Res.drawable.veg_cucumber
    "melon" -> Res.drawable.veg_cantaloupe
    "courge_butternut" -> Res.drawable.veg_butternut_squash
    "potimarron" -> Res.drawable.veg_red_kuri_squash
    "pasteque" -> Res.drawable.veg_watermelon
    "betterave" -> Res.drawable.veg_beet
    "epinard" -> Res.drawable.veg_spinach
    "blette" -> Res.drawable.veg_swiss_chard
    "laitue" -> Res.drawable.veg_bibb_lettuce
    "mache" -> Res.drawable.veg_claytonia
    "chicoree" -> Res.drawable.veg_curly_endive
    "basilic" -> Res.drawable.veg_basil
    "menthe" -> Res.drawable.veg_mint
    "romarin" -> Res.drawable.veg_rosemary
    "thym" -> Res.drawable.veg_thyme
    "origan" -> Res.drawable.veg_oregano
    "sauge" -> Res.drawable.veg_sage
    "persil" -> Res.drawable.veg_parsley
    "coriandre" -> Res.drawable.veg_cilantro
    "aneth" -> Res.drawable.veg_dill
    "tagete" -> Res.drawable.veg_generic_flower
    "oeillet_d_inde" -> Res.drawable.veg_california_poppy
    "bourrache" -> Res.drawable.veg_borage
    "capucine" -> Res.drawable.veg_california_poppy
    "phacelie" -> Res.drawable.veg_lavender
    "souci" -> Res.drawable.veg_chamomile
    "fenouil" -> Res.drawable.veg_fennel
    "fraise" -> Res.drawable.veg_strawberry
    "asperge" -> Res.drawable.veg_asparagus
    "tournesol" -> Res.drawable.veg_sunflower
    "mais_doux" -> Res.drawable.veg_corn
    else -> when (family) {
        BotanicalFamily.BRASSICACEAE -> Res.drawable.veg_green_cabbage
        BotanicalFamily.LAMIACEAE, BotanicalFamily.APIACEAE -> Res.drawable.veg_parsley
        else -> Res.drawable.veg_generic_flower
    }
}

/**
 * Icône vectorielle décorative d'une plante (le nom reste toujours affiché à côté).
 */
@Composable
fun VegetableIcon(
    vegetable: Vegetable,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(vegetable.iconResource()),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

/**
 * Pictogramme texte de secours pour une plante.
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
