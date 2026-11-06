package fr.pilmil.sillon

import fr.pilmil.sillon.data.VegetableCatalog
import fr.pilmil.sillon.engine.AdjacencyType
import fr.pilmil.sillon.engine.CompanionEngine
import fr.pilmil.sillon.model.CompanionBenefitType
import fr.pilmil.sillon.model.CompanionRiskType
import fr.pilmil.sillon.model.Parcel
import fr.pilmil.sillon.model.ParcelSlot
import fr.pilmil.sillon.model.ParcelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompanionEngineTest {

    private lateinit var engine: CompanionEngine

    @Before
    fun setUp() {
        engine = CompanionEngine()
    }

    @Test
    fun testCarrotAndLeekSynergy() {
        // Carotte + Poireau : synergie emblématique de confusion olfactive croisée
        val eval = engine.evaluatePair("carotte", "poireau")
        assertNotNull("L'évaluation du couple Carotte-Poireau ne doit pas être nulle", eval)
        assertTrue("Carotte et Poireau doivent être identifiés comme bénéfiques", eval!!.isBeneficial)
        assertFalse("Carotte et Poireau ne doivent pas être antagonistes", eval.isAntagonistic)

        val relation = eval.beneficialRelations.find { it.benefitType == CompanionBenefitType.PEST_REPELLENT }
        assertNotNull("La synergie doit être de type PEST_REPELLENT (confusion olfactive)", relation)
        assertTrue(relation!!.rationale.contains("mouche") || relation.rationale.contains("teigne"))
    }

    @Test
    fun testTomatoAndPotatoAntagonism() {
        // Tomate + Pomme de terre : antagonisme majeur (Solanacées, mildiou et doryphores)
        val eval = engine.evaluatePair("tomate", "pomme_de_terre")
        assertNotNull("L'évaluation du couple Tomate-Pomme de terre ne doit pas être nulle", eval)
        assertTrue("Tomate et Pomme de terre doivent être marqués comme antagonistes", eval!!.isAntagonistic)

        val badRelation = eval.antagonisticRelations.find { it.riskType == CompanionRiskType.SHARED_DISEASE_OR_PEST }
        assertNotNull("Le risque doit être de type SHARED_DISEASE_OR_PEST (mildiou partagé)", badRelation)
        assertTrue(badRelation!!.rationale.contains("mildiou", ignoreCase = true))
    }

    @Test
    fun testTomatoAndBasilSynergy() {
        val eval = engine.evaluatePair("tomate", "basilic")
        assertNotNull(eval)
        assertTrue("Tomate et Basilic doivent être synergiques", eval!!.isBeneficial)
        assertFalse(eval.isAntagonistic)
    }

    @Test
    fun testBeansAndOnionsAntagonism() {
        // Haricot nain + Oignon : Allélopathie inhibitrice des nodosités rhizobiennes
        val eval = engine.evaluatePair("haricot_nain", "oignon")
        assertNotNull(eval)
        assertTrue("Haricot et Oignon doivent être antagonistes", eval!!.isAntagonistic)
        val risk = eval.antagonisticRelations.firstOrNull()
        assertEquals(CompanionRiskType.ALLELOPATHY, risk?.riskType)
    }

    @Test
    fun testParcelEvaluationWithAdjacentSynergyAndAntagonism() {
        // Création d'une planche en carrés 2x2:
        // [ (0,0): carotte ]  [ (0,1): poireau ]           --> Synergie horizontale
        // [ (1,0): tomate ]   [ (1,1): pomme_de_terre ]   --> Antagonisme horizontal
        val parcel = Parcel(
            id = "test_parcel",
            name = "Parcelle Mixte",
            type = ParcelType.RAISED_BED_SQUARES,
            rows = 2,
            cols = 2,
            slots = listOf(
                ParcelSlot(row = 0, col = 0, vegetableId = "carotte"),
                ParcelSlot(row = 0, col = 1, vegetableId = "poireau"),
                ParcelSlot(row = 1, col = 0, vegetableId = "tomate"),
                ParcelSlot(row = 1, col = 1, vegetableId = "pomme_de_terre"),
            )
        )

        val result = engine.evaluateParcel(parcel)

        assertTrue("La parcelle doit comporter au moins une synergie", result.hasSynergies)
        assertTrue("La parcelle doit comporter au moins une alerte antagoniste", result.hasAntagonisms)

        // Vérifier la détection de la synergie carotte-poireau
        val carrotLeekSynergy = result.synergies.find {
            (it.vegetable1.id == "carotte" && it.vegetable2.id == "poireau") ||
                (it.vegetable1.id == "poireau" && it.vegetable2.id == "carotte")
        }
        assertNotNull("La synergie Carotte-Poireau doit être détectée dans la parcelle", carrotLeekSynergy)
        assertEquals(AdjacencyType.ORTHOGONAL, carrotLeekSynergy?.adjacency)

        // Vérifier l'alerte biologique tomate-pomme de terre
        val tomatoPotatoAntagonism = result.antagonisms.find {
            (it.vegetable1.id == "tomate" && it.vegetable2.id == "pomme_de_terre") ||
                (it.vegetable1.id == "pomme_de_terre" && it.vegetable2.id == "tomate")
        }
        assertNotNull("L'antagonisme Tomate-Pomme de terre doit être détecté", tomatoPotatoAntagonism)
        assertTrue(tomatoPotatoAntagonism!!.biologicalAlert.contains("Alerte Biologique"))
    }

    @Test
    fun testDiagonalAdjacencyDetection() {
        // Grille 2x2 avec disposition en diagonale:
        // [ (0,0): carotte ]  [ (0,1): vide ]
        // [ (1,0): vide ]     [ (1,1): poireau ]
        val parcel = Parcel(
            id = "diag_parcel",
            name = "Parcelle Diagonale",
            type = ParcelType.RAISED_BED_SQUARES,
            rows = 2,
            cols = 2,
            slots = listOf(
                ParcelSlot(row = 0, col = 0, vegetableId = "carotte"),
                ParcelSlot(row = 1, col = 1, vegetableId = "poireau"),
            )
        )

        val result = engine.evaluateParcel(parcel)
        assertEquals(1, result.synergies.size)
        val synergy = result.synergies.first()
        assertEquals(AdjacencyType.DIAGONAL, synergy.adjacency)
    }

    @Test
    fun testNonAdjacentSlotsDoNotInteract() {
        // Grille 1x3 : (0,0) et (0,2) sont séparés par (0,1) vide
        val parcel = Parcel(
            id = "sparse_parcel",
            name = "Parcelle Espacée",
            type = ParcelType.RAISED_BED_SQUARES,
            rows = 1,
            cols = 3,
            slots = listOf(
                ParcelSlot(row = 0, col = 0, vegetableId = "tomate"),
                ParcelSlot(row = 0, col = 2, vegetableId = "pomme_de_terre"),
            )
        )

        val result = engine.evaluateParcel(parcel)
        assertFalse("Des cases non adjacentes ne doivent pas déclencher d'antagonisme", result.hasAntagonisms)
        assertEquals(0, result.antagonisms.size)
    }

    @Test
    fun testGoodAndBadCompanionsLookup() {
        val goodForTomato = engine.findGoodCompanionsFor("tomate")
        assertTrue(goodForTomato.any { it.id == "basilic" })
        assertTrue(goodForTomato.any { it.id == "tagete" })

        val badForTomato = engine.findBadCompanionsFor("tomate")
        assertTrue(badForTomato.any { it.id == "pomme_de_terre" })
    }
}
