package com.bettercontent.tinkersconstructaffixes

import net.minecraft.resources.ResourceLocation

internal enum class AffixOrigin(val id: String) {
    GLOBAL("global"),
    NETHER("nether"),
    AETHER("aether"),
    MUTATION("mutation")
}

internal object AffixOrigins {
    private val dimensions = mapOf(
        ResourceLocation.tryParse("minecraft:the_nether")!! to AffixOrigin.NETHER,
        ResourceLocation.tryParse("aether:the_aether")!! to AffixOrigin.AETHER
    )

    val exclusiveParts = mapOf(
        "tinkersweaponry:great_blade" to AffixOrigin.NETHER,
        "tinker_rapier:slender_blade" to AffixOrigin.AETHER
    )

    private val materials = buildMap {
        assign(AffixOrigin.NETHER,
            "tconstruct:blaze", "tconstruct:blazewood", "tconstruct:blazing_bone", "tconstruct:cinderslime",
            "tconstruct:cobalt", "tconstruct:fiery", "tconstruct:magma", "tconstruct:manyullyn", "tconstruct:nahuatl",
            "tconstruct:necronium", "tconstruct:necrotic_bone", "tconstruct:pig_iron", "tconstruct:scorched_stone",
            "tconstruct:twisting_vine", "tconstruct:weeping_vine", "tinkers_things:magmaskin")
        assign(AffixOrigin.AETHER,
            "tconstruct:skyslime", "tconstruct:feather", "tconstruct:phantom",
            "tconstruct:whitestone", "tconstruct:ice", "tinkers_construct_affixes:skyroot", "tinkers_construct_affixes:holystone",
            "tinkers_construct_affixes:zanite", "tinkers_construct_affixes:ambrosium", "tinkers_construct_affixes:gravitite")
    }

    private val exclusiveAffixes = buildMap {
        assign(AffixOrigin.NETHER, "charward", "sundercall", "emberspite", "of_live_coals", "fontbound_assault")
        assign(AffixOrigin.AETHER, "windward_guard", "of_held_breath", "drawn_pulse", "straight_shot", "of_the_still_breath", "of_sure_arcs", "bowyers_lattice")
    }

    private val themedGlobalParts = mapOf(
        AffixOrigin.NETHER to setOf("tconstruct:small_blade", "tconstruct:broad_blade", "tconstruct:hammer_head", "tconstruct:broad_axe_head", "tconstruct:tool_handle", "tconstruct:tough_handle"),
        AffixOrigin.AETHER to setOf("tconstruct:bow_limb", "tconstruct:bow_grip", "tconstruct:bowstring", "tconstruct:arrow_head", "tconstruct:arrow_shaft", "tconstruct:fletching", "tconstruct:boots_plating")
    )

    fun fromDimension(dimension: ResourceLocation): AffixOrigin = dimensions[dimension] ?: AffixOrigin.GLOBAL

    fun materialOrigin(id: String): AffixOrigin = materials[id] ?: AffixOrigin.GLOBAL

    fun classifiedMaterialIds(): Set<String> = materials.keys

    fun physicalOrigin(partId: String, materialId: String): AffixOrigin {
        val part = exclusiveParts[partId] ?: AffixOrigin.GLOBAL
        val material = materialOrigin(materialId)
        return when {
            part == AffixOrigin.GLOBAL -> material
            material == AffixOrigin.GLOBAL || material == part -> part
            else -> AffixOrigin.GLOBAL
        }
    }

    fun allowsAffix(origin: AffixOrigin, affixId: String): Boolean {
        val exclusive = exclusiveAffixes[affixId]
        return exclusive == null || exclusive == origin
    }

    fun isExclusiveAffix(origin: AffixOrigin, affixId: String): Boolean = exclusiveAffixes[affixId] == origin

    fun allowsPart(origin: AffixOrigin, partId: String, exclusiveRoll: Boolean): Boolean {
        if (origin == AffixOrigin.GLOBAL) return partId !in exclusiveParts
        return if (exclusiveRoll) exclusiveParts[partId] == origin
        else partId in themedGlobalParts[origin].orEmpty()
    }

    fun materialIds(origin: AffixOrigin, tier: Int): List<String> {
        val configured = TConAffixConfig.materialsForTier(tier)
        if (origin == AffixOrigin.GLOBAL) return configured.filter { materialOrigin(it) == AffixOrigin.GLOBAL }
        val reserved = materials.filterValues { it == origin }.keys
        return (configured.filter { it in reserved } + localMaterialIds(origin, tier)).distinct()
    }

    private fun localMaterialIds(origin: AffixOrigin, tier: Int): List<String> = when (origin to tier) {
        AffixOrigin.AETHER to 1 -> listOf("tinkers_construct_affixes:skyroot", "tinkers_construct_affixes:holystone")
        AffixOrigin.AETHER to 2 -> listOf("tinkers_construct_affixes:zanite")
        AffixOrigin.AETHER to 3 -> listOf("tinkers_construct_affixes:ambrosium")
        AffixOrigin.AETHER to 4 -> listOf("tinkers_construct_affixes:gravitite")
        else -> emptyList()
    }

    private fun MutableMap<String, AffixOrigin>.assign(origin: AffixOrigin, vararg ids: String) {
        ids.forEach { put(it, origin) }
    }
}
