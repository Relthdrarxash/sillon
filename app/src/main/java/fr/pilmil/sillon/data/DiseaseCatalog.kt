package fr.pilmil.sillon.data

import fr.pilmil.sillon.model.BotanicalFamily
import fr.pilmil.sillon.model.Disease
import fr.pilmil.sillon.model.PathogenType
import fr.pilmil.sillon.model.PlantOrgan

object DiseaseCatalog {

    val MILDIOU = Disease(
        id = "mildiou",
        name = "Mildiou",
        latinName = "Phytophthora infestans / Plasmopara viticola",
        pathogenType = PathogenType.FUNGUS,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.STEM, PlantOrgan.FRUIT),
        symptoms = listOf(
            "Taches brunes d'aspect huileux sur le dessus des feuilles",
            "Duvet blanc grisâtre au revers des feuilles par temps humide",
            "Nécrose et dessèchement rapide du feuillage",
            "Taches brunes fermes et marbrées sur les fruits (tomates vertes)",
            "Chancres bruns sur les tiges",
        ),
        causes = listOf(
            "Humidité relative supérieure à 90% combinée à un feuillage mouillé",
            "Températures tempérées entre 15 et 25°C",
            "Arrosage par aspersion ou éclaboussures de pluie",
            "Mauvaise aération du feuillage",
        ),
        organicRemedies = listOf(
            "Bouillie bordelaise raisonnée en traitement préventif avant la pluie (max 4g cuivre/m²/an)",
            "Décoction de prêle diluée à 10% pour renforcer la cuticule foliaire en silice",
            "Suppression immédiate et incinération des feuilles et fruits touchés",
            "Arrosage exclusif au pied sans jamais mouiller les feuilles",
            "Purin de saule ou purin d'ortie pour activer les défenses immunitaires de la plante",
        ),
        targetFamilies = listOf(BotanicalFamily.SOLANACEAE, BotanicalFamily.CUCURBITACEAE),
        preventiveMeasures = listOf(
            "Abriter les tomates sous un toit ou une serre ventilée",
            "Espacer généreusement les plants (minimum 60 cm)",
            "Pailler le sol pour éviter les éclaboussures de terre sur les premières feuilles",
            "Ne pas planter de pommes de terre à proximité immédiate des tomates",
        ),
    )

    val OIDIUM = Disease(
        id = "oidium",
        name = "Oïdium (Maladie du blanc)",
        latinName = "Podosphaera xanthii / Erysiphe cichoracearum",
        pathogenType = PathogenType.FUNGUS,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.STEM),
        symptoms = listOf(
            "Feutrage blanc poudreux ou farineux sur la face supérieure des feuilles",
            "Jaunissement puis brunissement et dessèchement des feuilles",
            "Ralentissement net du développement végétatif et des fruits",
        ),
        causes = listOf(
            "Forts écarts thermiques entre le jour chaud et la nuit humide",
            "Temps chaud et sec le jour avec rosée nocturne abondante",
            "Manque de circulation d'air entre les rangs",
        ),
        organicRemedies = listOf(
            "Pulvérisation de bicarbonate de potassium ou de soude (5g/L d'eau avec 1 cuillère de savon noir)",
            "Pulvérisation de lait de vache écrémé dilué à 10% en plein soleil",
            "Poudrage au soufre mouillable en prévention",
            "Élimination des premières feuilles basales atteintes",
        ),
        targetFamilies = listOf(
            BotanicalFamily.CUCURBITACEAE,
            BotanicalFamily.SOLANACEAE,
            BotanicalFamily.ASTERACEAE,
        ),
        preventiveMeasures = listOf(
            "Arroser au pied le matin plutôt que le soir",
            "Respecter de larges distances de plantation pour favoriser la ventilation",
            "Éviter les excès de fumure azotée qui fragilisent les tissus foliaires",
        ),
    )

    val CUL_NOIR = Disease(
        id = "cul_noir",
        name = "Cul noir (Nécrose apicale)",
        latinName = "Blossom end rot (Physiologique)",
        pathogenType = PathogenType.PHYSIOLOGICAL,
        affectedOrgans = listOf(PlantOrgan.FRUIT),
        symptoms = listOf(
            "Tache circulaire vert sombre puis noire et déprimée à l'extrémité opposée à la queue du fruit",
            "Fruit qui durcit et se nécrose sur sa partie inférieure",
            "Maturation prématurée anormale du fruit touché",
        ),
        causes = listOf(
            "Déficit d'assimilation du calcium par la plante causé par des à-coups d'arrosage",
            "Périodes de forte chaleur alternant avec une sécheresse racinaire",
            "Excès d'azote ammoniacal concurrençant le calcium",
        ),
        organicRemedies = listOf(
            "Régulariser strictement les apports d'eau (arrosages réguliers et constants)",
            "Installer un paillage épais d'au moins 8 à 10 cm pour stabiliser l'humidité du sol",
            "Arrosage au pied avec du lait dilué ou apport d'engrais foliaire riche en calcium (lithothamne)",
            "Retirer les fruits atteints pour soulager le plant",
        ),
        targetFamilies = listOf(BotanicalFamily.SOLANACEAE),
        preventiveMeasures = listOf(
            "Arrosage goutte-à-goutte régulier",
            "Amendement calcaire léger à l'automne si le sol est très acide",
            "Paillage généreux dès le repiquage",
        ),
    )

    val PUCERONS = Disease(
        id = "pucerons",
        name = "Pucerons",
        latinName = "Aphidoidea",
        pathogenType = PathogenType.PEST,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.STEM, PlantOrgan.SEEDLING),
        symptoms = listOf(
            "Colonies d'insectes minuscules verts, noirs ou cendrés au revers des feuilles et sur les jeunes pousses",
            "Feuilles crispées, boursouflées et déformées",
            "Substance collante et brillante sur le feuillage (miellat)",
            "Développement de fumagine (dépôt noir fuligineux ressemblant à de la suie)",
            "Afflux de fourmis élevant et protégeant les colonies",
        ),
        causes = listOf(
            "Excès d'engrais azoté stimulant la pousse de tissus tendres et sucrés",
            "Déficit en insectes auxiliaires prédateurs (coccinelles, syrphes, chrysopes)",
            "Temps chaud et calme au printemps",
        ),
        organicRemedies = listOf(
            "Pulvérisation de savon noir liquide (20g/L d'eau tiède) au revers des feuilles",
            "Purin d'ortie dilué à 10% ou purin de rhubarbe riche en acide oxalique",
            "Introduction de larves de coccinelles indigènes (Adalia bipunctata)",
            "Jet d'eau puissant pour déloger mécaniquement les colonies",
        ),
        targetFamilies = emptyList(),
        preventiveMeasures = listOf(
            "Semer des capucines en bordure comme plante piège à pucerons",
            "Préserver les bandes fleuries pour accueillir syrphes et coccinelles",
            "Bannir les insecticides non sélectifs",
        ),
    )

    val ALTISES = Disease(
        id = "altises",
        name = "Altises (Puces de terre)",
        latinName = "Phyllotreta spp.",
        pathogenType = PathogenType.PEST,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.SEEDLING),
        symptoms = listOf(
            "Feuilles criblées d'innombrables petits trous ronds réguliers (aspect passoire)",
            "Petits coléoptères noirs ou métalliques de 2 mm sautant vivement dès qu'on approche",
            "Arrêt de croissance brutal des jeunes radis, roquettes et choux",
        ),
        causes = listOf(
            "Temps chaud, sec et ensoleillé",
            "Terre desséchée et poussiéreuse en surface",
        ),
        organicRemedies = listOf(
            "Arroser en pluie fine et brumiser fréquemment le feuillage (l'altise déteste l'humidité)",
            "Pose d'un filet anti-insectes à maille très fine (< 0,8 mm) dès le semis",
            "Paillage constant et arrosé pour maintenir la fraîcheur au niveau du sol",
            "Poudrage de cendre de bois tamisée ou de terre de diatomée sur les jeunes feuilles",
        ),
        targetFamilies = listOf(BotanicalFamily.BRASSICACEAE),
        preventiveMeasures = listOf(
            "Maintenir le sol humide pendant toute la phase de levée des crucifères",
            "Poser des voiles anti-insectes hermétiquement fermés sur les pourtours",
        ),
    )

    val ROUILLE = Disease(
        id = "rouille",
        name = "Rouille",
        latinName = "Puccinia allii / Puccinia spp.",
        pathogenType = PathogenType.FUNGUS,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.STEM),
        symptoms = listOf(
            "Pustules proéminentes orange vif, brun rougeâtre ou rouille sous les feuilles",
            "Taches jaunes correspondantes sur la face supérieure",
            "Dessèchement progressif de l'extrémité des feuilles d'ail, oignon ou poireau",
        ),
        causes = listOf(
            "Atmosphère humide avec températures douces (15 à 22°C)",
            "Plantations trop denses retenant l'humidité",
        ),
        organicRemedies = listOf(
            "Pulvérisation de décoction de prêle (reminéralisant puissant)",
            "Suppression des feuilles atteintes dès l'apparition des premières pustules",
            "Pulvérisation de macération d'ail avec un tensioactif naturel (savon noir)",
            "Bouillie bordelaise à dose modérée",
        ),
        targetFamilies = listOf(
            BotanicalFamily.ALLIACEAE,
            BotanicalFamily.FABACEAE,
            BotanicalFamily.MALVACEAE,
        ),
        preventiveMeasures = listOf(
            "Planter en billons surélevés pour favoriser l'évacuation de l'eau stagnante",
            "Rotation d'au moins 3 ans pour les Alliacées",
        ),
    )

    val LIMACES = Disease(
        id = "limaces",
        name = "Limaces et Escargots",
        latinName = "Deroceras reticulatum / Arion hortensis",
        pathogenType = PathogenType.PEST,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.SEEDLING, PlantOrgan.FRUIT),
        symptoms = listOf(
            "Jeunes semis sectionnés net au collet au cours de la nuit",
            "Larges trous déchiquetés et irréguliers dans les feuilles de salades et courgettes",
            "Traces luisantes et argentées de mucus séché sur la végétation et le sol",
        ),
        causes = listOf(
            "Temps pluvieux persistant et nuits fraîches et humides",
            "Multiplication des zones d'abri humides (herbes hautes, planches, tas de bois)",
        ),
        organicRemedies = listOf(
            "Granulés de phosphate ferrique (Ferramol), inoffensifs pour les hérissons et animaux domestiques",
            "Barrières physiques : coquilles d'œufs concassées, sciure de bois sèche ou marc de café",
            "Pièges à bière ou tuiles posées au sol relevées chaque matin pour collecte manuelle",
            "Favoriser la biodiversité des auxiliaires (hérissons, crapauds, carabes, orvets)",
        ),
        targetFamilies = emptyList(),
        preventiveMeasures = listOf(
            "Arroser le matin plutôt qu'en fin de journée pour que la terre soit sèche en surface la nuit",
            "Protéger les jeunes plants repiqués avec des cloches ou des bouteilles coupées",
        ),
    )

    val MOUCHE_CAROTTE = Disease(
        id = "mouche_carotte",
        name = "Mouche de la carotte",
        latinName = "Psila rosae",
        pathogenType = PathogenType.PEST,
        affectedOrgans = listOf(PlantOrgan.ROOT, PlantOrgan.LEAF),
        symptoms = listOf(
            "Folioles prenant une teinte rougeâtre, bronzée ou violette puis jaunissante",
            "Galeries sinueuses brunâtres et nécrosées creusées à l'intérieur de la racine",
            "Racines pourrissantes et impropres à la conservation",
        ),
        causes = listOf(
            "Vols printaniers et estivaux des mouches attirées par l'odeur d'huile essentielle de carotte froissée",
            "Éclaircissage des rangs en journée libérant les odeurs attractives",
        ),
        organicRemedies = listOf(
            "Pose immédiate d'un voile anti-insectes (maille 0,8 mm) dès le semis jusqu'à la récolte",
            "Compagnonnage protecteur : alterner un rang de carottes avec un rang de poireaux ou d'oignons",
            "Pulvérisation de purin de tanaisie, de fougère ou d'infusion d'ail pour brouiller les repères olfactifs",
            "Éclaircir les rangs impérativement le soir ou par temps pluvieux sans vent",
        ),
        targetFamilies = listOf(BotanicalFamily.APIACEAE),
        preventiveMeasures = listOf(
            "Décaler les semis (semis très précoces en février sous châssis ou tardifs en juin pour éviter le premier vol)",
            "Association étroite avec les Alliacées pour créer une confusion olfactive mutuelle",
        ),
    )

    val TEIGNE_POIREAU = Disease(
        id = "teigne_poireau",
        name = "Teigne du poireau (Ver du poireau)",
        latinName = "Acrolepiopsis assectella",
        pathogenType = PathogenType.PEST,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.STEM),
        symptoms = listOf(
            "Feuilles trouées de fines galeries blanchâtres transparentes en filet",
            "Feuillage déchiqueté et découpé en lanières",
            "Présence de petites chenilles vert clair de 10 mm à tête brune au cœur du fût",
            "Pourriture bactérienne secondaire du cœur du poireau dégageant une odeur forte",
        ),
        causes = listOf(
            "Vols nocturnes de papillons de teigne d'avril à octobre",
            "Périodes sèches et chaudes stimulant les générations successives",
        ),
        organicRemedies = listOf(
            "Filet anti-insectes à maille fine (0,8 mm) posé sur arceaux dès la plantation",
            "Association avec les carottes ou le céleri pour perturber la ponte par confusion d'odeurs",
            "Pulvérisation de Bacillus thuringiensis (Bt) kurstaki dès l'observation des premières mines",
            "Coupe rase du feuillage au-dessus du blanc : la plante repart sainement et les chenilles sont éliminées",
        ),
        targetFamilies = listOf(BotanicalFamily.ALLIACEAE),
        preventiveMeasures = listOf(
            "Faire tremper les racines et l'extrémité des fûts des jeunes poireaux dans une solution d'eau javellisée très diluée ou au soleil avant repiquage",
            "Maintenir le voile jusqu'à l'arrivée des gelées d'automne",
        ),
    )

    val PIERIDE_CHOU = Disease(
        id = "pieride_chou",
        name = "Piéride du chou",
        latinName = "Pieris brassicae / Pieris rapae",
        pathogenType = PathogenType.PEST,
        affectedOrgans = listOf(PlantOrgan.LEAF),
        symptoms = listOf(
            "Feuilles de choux squelettisées dont il ne reste plus que les grosses nervures",
            "Présence de grappes de chenilles vert-jaunâtre très voraces couvertes de points noirs",
            "Excréments noirs abondants au cœur de la pomme",
            "Vols fréquents de papillons blancs tachés de noir au-dessus du potager",
        ),
        causes = listOf(
            "Pontes groupées sous les feuilles par les papillons de piéride",
            "Climat tempéré chaud de mai à octobre",
        ),
        organicRemedies = listOf(
            "Pulvérisation de Bacillus thuringiensis (Bt) sur le feuillage dès l'éclosion des chenilles",
            "Filet anti-insectes à maille 1 mm maintenu au-dessus des plants",
            "Inspection manuelle régulière du revers des feuilles pour écraser les amas d'œufs jaune vif",
            "Plantation de plantes aromatiques répulsives à proximité (menthe, thym, sauge, mélisse)",
        ),
        targetFamilies = listOf(BotanicalFamily.BRASSICACEAE),
        preventiveMeasures = listOf(
            "Associer les choux avec des aromatiques pour masquer l'odeur des glucosinolates",
            "Favoriser la guêpe parasitoïde Cotesia glomerata prédatrice naturelle des chenilles",
        ),
    )

    val BOTRYTIS = Disease(
        id = "botrytis",
        name = "Pourriture grise (Botrytis)",
        latinName = "Botrytis cinerea",
        pathogenType = PathogenType.FUNGUS,
        affectedOrgans = listOf(PlantOrgan.FRUIT, PlantOrgan.STEM, PlantOrgan.LEAF),
        symptoms = listOf(
            "Feutrage gris souris épais et pelucheux recouvrant les tissus atteints",
            "Pourriture molle et aqueuse des fruits, tiges et collets",
            "Flétrissement rapide des fleurs et des grappes de fruits",
        ),
        causes = listOf(
            "Humidité saturée (> 95%) et manque de ventilation sous serre ou tunnel",
            "Plaies de taille non cicatrisées ou blessures mécaniques",
            "Présence de débris végétaux en décomposition",
        ),
        organicRemedies = listOf(
            "Ventilation maximale quotidienne des serres et abris",
            "Suppression chirurgicale des parties atteintes avec un outil désinfecté à l'alcool",
            "Pulvérisation de décoction de prêle pour assécher le milieu et durcir l'épiderme",
            "Poudrage d'argile kaolin ou de lithothamne sur les plaies",
        ),
        targetFamilies = listOf(
            BotanicalFamily.SOLANACEAE,
            BotanicalFamily.FABACEAE,
            BotanicalFamily.ASTERACEAE,
            BotanicalFamily.CUCURBITACEAE,
        ),
        preventiveMeasures = listOf(
            "Éviter les tailles par temps pluvieux ou très humide",
            "Ramasser scrupuleusement tous les déchets végétaux au sol",
        ),
    )

    val FONTE_DES_SEMIS = Disease(
        id = "fonte_des_semis",
        name = "Fonte des semis",
        latinName = "Pythium debaryanum / Rhizoctonia solani",
        pathogenType = PathogenType.FUNGUS,
        affectedOrgans = listOf(PlantOrgan.SEEDLING, PlantOrgan.ROOT, PlantOrgan.STEM),
        symptoms = listOf(
            "Jeune plantule qui s'étrécit et noircit au niveau du collet puis s'effondre au sol",
            "Disparition subite de zones entières de semis avant même la levée",
            "Pourriture molle et flétrissement des racines embryonnaires",
        ),
        causes = listOf(
            "Substrat de semis excessivement mouillé et froid",
            "Semis trop dense manquant d'aération",
            "Terreau de récupération non stérile",
        ),
        organicRemedies = listOf(
            "Semer très clair pour laisser circuler l'air entre chaque jeune plantule",
            "Arroser avec modération par bassinage avec une eau tempérée",
            "Saupoudrer la surface du terreau avec une fine couche de charbon de bois pulvérisé",
            "Pulvérisation légère d'infusion de camomille ou de décoction d'ail",
        ),
        targetFamilies = emptyList(),
        preventiveMeasures = listOf(
            "Utiliser un terreau de semis sain, léger et bien drainé",
            "Désinfecter les terrines et godets à l'eau chaude savonneuse avant l'usage",
            "Placer les semis dans un endroit tiède, bien ventilé et lumineux",
        ),
    )

    val DORYPHORE = Disease(
        id = "doryphore",
        name = "Doryphore de la pomme de terre",
        latinName = "Leptinotarsa decemlineata",
        pathogenType = PathogenType.PEST,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.STEM),
        symptoms = listOf(
            "Défoliation complète et rapide des plants de pomme de terre ou d'aubergine",
            "Coléoptères rayés jaune et noir caractéristiques (10 rayures)",
            "Grosses larves dodues rouge-orangé avec deux rangées de points noirs sur les flancs",
            "Grappes d'œufs orange brillant sous les feuilles",
        ),
        causes = listOf(
            "Hivernage des adultes enfouis dans le sol",
            "Répétition de cultures de Solanacées au même endroit",
        ),
        organicRemedies = listOf(
            "Ramassage manuel systématique quotidien des adultes, des larves et écrasement des œufs",
            "Traitement biologique ciblé à base de Bacillus thuringiensis var. tenebrionis",
            "Semis de lin bleu ou de ricin entre les rangs comme répulsif naturel",
            "Paillage épais de paille pour entraver la sortie de terre des adultes printaniers",
        ),
        targetFamilies = listOf(BotanicalFamily.SOLANACEAE),
        preventiveMeasures = listOf(
            "Rotation minimale de 4 ans pour les Solanacées",
            "Buttage régulier et paillage abondant",
        ),
    )

    val ANTHRACNOSE = Disease(
        id = "anthracnose",
        name = "Anthracnose",
        latinName = "Colletotrichum lindemuthianum",
        pathogenType = PathogenType.FUNGUS,
        affectedOrgans = listOf(PlantOrgan.LEAF, PlantOrgan.STEM, PlantOrgan.FRUIT),
        symptoms = listOf(
            "Taches circulaires concaves brunes ou noires bordées de rouge sur les gousses",
            "Nécroses le long des nervures sous les feuilles",
            "Graines tachées de brun à l'intérieur des gousses",
        ),
        causes = listOf(
            "Temps doux (15-22°C) et très pluvieux",
            "Semences contaminées",
        ),
        organicRemedies = listOf(
            "Supprimer et brûler les plants malades",
            "Pulvérisation de décoction de prêle ou de bouillie bordelaise avant floraison",
            "Ne jamais intervenir ni récolter dans les rangs lorsque le feuillage est mouillé",
        ),
        targetFamilies = listOf(BotanicalFamily.FABACEAE, BotanicalFamily.CUCURBITACEAE),
        preventiveMeasures = listOf(
            "Utiliser exclusivement des semences certifiées saines et indemnes",
            "Respecter une rotation de 3 ans",
        ),
    )

    val ALL_DISEASES: List<Disease> = listOf(
        MILDIOU,
        OIDIUM,
        CUL_NOIR,
        PUCERONS,
        ALTISES,
        ROUILLE,
        LIMACES,
        MOUCHE_CAROTTE,
        TEIGNE_POIREAU,
        PIERIDE_CHOU,
        BOTRYTIS,
        FONTE_DES_SEMIS,
        DORYPHORE,
        ANTHRACNOSE,
    )

    private val diseaseMap: Map<String, Disease> by lazy {
        ALL_DISEASES.associateBy { it.id }
    }

    fun getById(id: String): Disease? = diseaseMap[id]
}
