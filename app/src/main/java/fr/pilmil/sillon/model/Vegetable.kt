package fr.pilmil.sillon.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable
enum class WaterNeed(val frenchLabel: String) {
    LOW("Faible"),
    MEDIUM("Modéré"),
    HIGH("Élevé"),
}

@Serializable
enum class SunExposure(val frenchLabel: String) {
    FULL_SUN("Plein soleil"),
    PARTIAL_SHADE("Mi-ombre"),
    SHADE("Ombre"),
}

object IntRangeSerializer : KSerializer<IntRange> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("IntRange") {
        element<Int>("start")
        element<Int>("endInclusive")
    }

    override fun serialize(encoder: Encoder, value: IntRange) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.first)
            encodeIntElement(descriptor, 1, value.last)
        }
    }

    override fun deserialize(decoder: Decoder): IntRange {
        return decoder.decodeStructure(descriptor) {
            var start = 0
            var endInclusive = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> start = decodeIntElement(descriptor, 0)
                    1 -> endInclusive = decodeIntElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Index inattendu: $index")
                }
            }
            start..endInclusive
        }
    }
}

@Serializable
data class Vegetable(
    val id: String,
    val name: String,
    val latinName: String,
    val family: BotanicalFamily,
    val squareFootDensity: Int,
    val spacingInRowCm: Int,
    val spacingBetweenRowsCm: Int,
    val tempGerminationMin: Int,
    val tempOptimalMin: Int,
    val tempOptimalMax: Int,
    @Serializable(with = IntRangeSerializer::class)
    val daysToEmerge: IntRange,
    @Serializable(with = IntRangeSerializer::class)
    val daysToHarvest: IntRange,
    val sowIndoorsWeeksBeforeFrost: Int? = null,
    val sowOutdoorsWeeksAfterFrost: Int? = null,
    val transplantWeeksAfterFrost: Int? = null,
    val harvestDurationWeeks: Int,
    val waterNeed: WaterNeed,
    val sunExposure: SunExposure,
    val goodCompanions: List<CompanionRelation> = emptyList(),
    val badCompanions: List<CompanionRelation> = emptyList(),
    val advice: String,
) {
    val daysToEmergeMin: Int get() = daysToEmerge.first
    val daysToEmergeMax: Int get() = daysToEmerge.last
    val daysToHarvestMin: Int get() = daysToHarvest.first
    val daysToHarvestMax: Int get() = daysToHarvest.last
}
