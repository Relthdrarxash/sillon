package fr.pilmil.sillon.data

import fr.pilmil.sillon.model.FrostDate

data class DepartmentFrost(
    val code: String,
    val name: String,
    val medianLastFrost: FrostDate,
    val region: String,
)

object FrenchFrostCatalog {

    val DEPARTMENTS: List<DepartmentFrost> = listOf(
        DepartmentFrost("01", "Ain", FrostDate(4, 25), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("02", "Aisne", FrostDate(5, 1), "Hauts-de-France"),
        DepartmentFrost("03", "Allier", FrostDate(5, 1), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("04", "Alpes-de-Haute-Provence", FrostDate(4, 25), "Provence-Alpes-Côte d'Azur"),
        DepartmentFrost("05", "Hautes-Alpes", FrostDate(5, 15), "Provence-Alpes-Côte d'Azur"),
        DepartmentFrost("06", "Alpes-Maritimes", FrostDate(3, 20), "Provence-Alpes-Côte d'Azur"),
        DepartmentFrost("07", "Ardèche", FrostDate(4, 15), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("08", "Ardennes", FrostDate(5, 5), "Grand Est"),
        DepartmentFrost("09", "Ariège", FrostDate(4, 25), "Occitanie"),
        DepartmentFrost("10", "Aube", FrostDate(4, 25), "Grand Est"),
        DepartmentFrost("11", "Aude", FrostDate(3, 25), "Occitanie"),
        DepartmentFrost("12", "Aveyron", FrostDate(5, 5), "Occitanie"),
        DepartmentFrost("13", "Bouches-du-Rhône", FrostDate(3, 20), "Provence-Alpes-Côte d'Azur"),
        DepartmentFrost("14", "Calvados", FrostDate(4, 20), "Normandie"),
        DepartmentFrost("15", "Cantal", FrostDate(5, 15), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("16", "Charente", FrostDate(4, 15), "Nouvelle-Aquitaine"),
        DepartmentFrost("17", "Charente-Maritime", FrostDate(4, 10), "Nouvelle-Aquitaine"),
        DepartmentFrost("18", "Cher", FrostDate(4, 20), "Centre-Val de Loire"),
        DepartmentFrost("19", "Corrèze", FrostDate(5, 1), "Nouvelle-Aquitaine"),
        DepartmentFrost("2A", "Corse-du-Sud", FrostDate(3, 15), "Corse"),
        DepartmentFrost("2B", "Haute-Corse", FrostDate(3, 15), "Corse"),
        DepartmentFrost("21", "Côte-d'Or", FrostDate(5, 1), "Bourgogne-Franche-Comté"),
        DepartmentFrost("22", "Côtes-d'Armor", FrostDate(4, 10), "Bretagne"),
        DepartmentFrost("23", "Creuse", FrostDate(5, 10), "Nouvelle-Aquitaine"),
        DepartmentFrost("24", "Dordogne", FrostDate(4, 15), "Nouvelle-Aquitaine"),
        DepartmentFrost("25", "Doubs", FrostDate(5, 15), "Bourgogne-Franche-Comté"),
        DepartmentFrost("26", "Drôme", FrostDate(4, 15), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("27", "Eure", FrostDate(4, 25), "Normandie"),
        DepartmentFrost("28", "Eure-et-Loir", FrostDate(4, 25), "Centre-Val de Loire"),
        DepartmentFrost("29", "Finistère", FrostDate(4, 5), "Bretagne"),
        DepartmentFrost("30", "Gard", FrostDate(3, 25), "Occitanie"),
        DepartmentFrost("31", "Haute-Garonne", FrostDate(4, 15), "Occitanie"),
        DepartmentFrost("32", "Gers", FrostDate(4, 15), "Occitanie"),
        DepartmentFrost("33", "Gironde", FrostDate(4, 10), "Nouvelle-Aquitaine"),
        DepartmentFrost("34", "Hérault", FrostDate(3, 20), "Occitanie"),
        DepartmentFrost("35", "Ille-et-Vilaine", FrostDate(4, 15), "Bretagne"),
        DepartmentFrost("36", "Indre", FrostDate(4, 20), "Centre-Val de Loire"),
        DepartmentFrost("37", "Indre-et-Loire", FrostDate(4, 15), "Centre-Val de Loire"),
        DepartmentFrost("38", "Isère", FrostDate(5, 5), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("39", "Jura", FrostDate(5, 15), "Bourgogne-Franche-Comté"),
        DepartmentFrost("40", "Landes", FrostDate(4, 10), "Nouvelle-Aquitaine"),
        DepartmentFrost("41", "Loir-et-Cher", FrostDate(4, 20), "Centre-Val de Loire"),
        DepartmentFrost("42", "Loire", FrostDate(5, 5), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("43", "Haute-Loire", FrostDate(5, 20), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("44", "Loire-Atlantique", FrostDate(4, 10), "Pays de la Loire"),
        DepartmentFrost("45", "Loiret", FrostDate(4, 20), "Centre-Val de Loire"),
        DepartmentFrost("46", "Lot", FrostDate(4, 20), "Occitanie"),
        DepartmentFrost("47", "Lot-et-Garonne", FrostDate(4, 15), "Nouvelle-Aquitaine"),
        DepartmentFrost("48", "Lozère", FrostDate(5, 20), "Occitanie"),
        DepartmentFrost("49", "Maine-et-Loire", FrostDate(4, 15), "Pays de la Loire"),
        DepartmentFrost("50", "Manche", FrostDate(4, 15), "Normandie"),
        DepartmentFrost("51", "Marne", FrostDate(4, 25), "Grand Est"),
        DepartmentFrost("52", "Haute-Marne", FrostDate(5, 5), "Grand Est"),
        DepartmentFrost("53", "Mayenne", FrostDate(4, 20), "Pays de la Loire"),
        DepartmentFrost("54", "Meurthe-et-Moselle", FrostDate(5, 5), "Grand Est"),
        DepartmentFrost("55", "Meuse", FrostDate(5, 5), "Grand Est"),
        DepartmentFrost("56", "Morbihan", FrostDate(4, 10), "Bretagne"),
        DepartmentFrost("57", "Moselle", FrostDate(5, 5), "Grand Est"),
        DepartmentFrost("58", "Nièvre", FrostDate(4, 25), "Bourgogne-Franche-Comté"),
        DepartmentFrost("59", "Nord", FrostDate(4, 25), "Hauts-de-France"),
        DepartmentFrost("60", "Oise", FrostDate(4, 25), "Hauts-de-France"),
        DepartmentFrost("61", "Orne", FrostDate(4, 25), "Normandie"),
        DepartmentFrost("62", "Pas-de-Calais", FrostDate(4, 25), "Hauts-de-France"),
        DepartmentFrost("63", "Puy-de-Dôme", FrostDate(5, 10), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("64", "Pyrénées-Atlantiques", FrostDate(4, 10), "Nouvelle-Aquitaine"),
        DepartmentFrost("65", "Hautes-Pyrénées", FrostDate(4, 25), "Occitanie"),
        DepartmentFrost("66", "Pyrénées-Orientales", FrostDate(3, 20), "Occitanie"),
        DepartmentFrost("67", "Bas-Rhin", FrostDate(5, 5), "Grand Est"),
        DepartmentFrost("68", "Haut-Rhin", FrostDate(5, 5), "Grand Est"),
        DepartmentFrost("69", "Rhône", FrostDate(4, 25), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("70", "Haute-Saône", FrostDate(5, 5), "Bourgogne-Franche-Comté"),
        DepartmentFrost("71", "Saône-et-Loire", FrostDate(4, 25), "Bourgogne-Franche-Comté"),
        DepartmentFrost("72", "Sarthe", FrostDate(4, 20), "Pays de la Loire"),
        DepartmentFrost("73", "Savoie", FrostDate(5, 15), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("74", "Haute-Savoie", FrostDate(5, 15), "Auvergne-Rhône-Alpes"),
        DepartmentFrost("75", "Paris", FrostDate(4, 15), "Île-de-France"),
        DepartmentFrost("76", "Seine-Maritime", FrostDate(4, 20), "Normandie"),
        DepartmentFrost("77", "Seine-et-Marne", FrostDate(4, 25), "Île-de-France"),
        DepartmentFrost("78", "Yvelines", FrostDate(4, 20), "Île-de-France"),
        DepartmentFrost("79", "Deux-Sèvres", FrostDate(4, 15), "Nouvelle-Aquitaine"),
        DepartmentFrost("80", "Somme", FrostDate(4, 25), "Hauts-de-France"),
        DepartmentFrost("81", "Tarn", FrostDate(4, 15), "Occitanie"),
        DepartmentFrost("82", "Tarn-et-Garonne", FrostDate(4, 15), "Occitanie"),
        DepartmentFrost("83", "Var", FrostDate(3, 20), "Provence-Alpes-Côte d'Azur"),
        DepartmentFrost("84", "Vaucluse", FrostDate(3, 25), "Provence-Alpes-Côte d'Azur"),
        DepartmentFrost("85", "Vendée", FrostDate(4, 10), "Pays de la Loire"),
        DepartmentFrost("86", "Vienne", FrostDate(4, 15), "Nouvelle-Aquitaine"),
        DepartmentFrost("87", "Haute-Vienne", FrostDate(5, 1), "Nouvelle-Aquitaine"),
        DepartmentFrost("88", "Vosges", FrostDate(5, 10), "Grand Est"),
        DepartmentFrost("89", "Yonne", FrostDate(4, 25), "Bourgogne-Franche-Comté"),
        DepartmentFrost("90", "Territoire de Belfort", FrostDate(5, 10), "Bourgogne-Franche-Comté"),
        DepartmentFrost("91", "Essonne", FrostDate(4, 20), "Île-de-France"),
        DepartmentFrost("92", "Hauts-de-Seine", FrostDate(4, 15), "Île-de-France"),
        DepartmentFrost("93", "Seine-Saint-Denis", FrostDate(4, 15), "Île-de-France"),
        DepartmentFrost("94", "Val-de-Marne", FrostDate(4, 15), "Île-de-France"),
        DepartmentFrost("95", "Val-d'Oise", FrostDate(4, 20), "Île-de-France"),
        // DOM-TOM
        DepartmentFrost("971", "Guadeloupe", FrostDate(1, 1), "Guadeloupe"),
        DepartmentFrost("972", "Martinique", FrostDate(1, 1), "Martinique"),
        DepartmentFrost("973", "Guyane", FrostDate(1, 1), "Guyane"),
        DepartmentFrost("974", "La Réunion", FrostDate(1, 1), "La Réunion"),
        DepartmentFrost("976", "Mayotte", FrostDate(1, 1), "Mayotte"),
    )

    private val departmentMap: Map<String, DepartmentFrost> by lazy {
        DEPARTMENTS.associateBy { it.code }
    }

    val DEFAULT_FROST_DATE = FrostDate(4, 20)

    fun getByCode(code: String): DepartmentFrost? {
        val normalized = code.trim().uppercase().padStart(2, '0')
        return departmentMap[normalized] ?: departmentMap[code.trim().uppercase()]
    }

    fun getFrostDate(code: String): FrostDate {
        return getByCode(code)?.medianLastFrost ?: DEFAULT_FROST_DATE
    }
}
