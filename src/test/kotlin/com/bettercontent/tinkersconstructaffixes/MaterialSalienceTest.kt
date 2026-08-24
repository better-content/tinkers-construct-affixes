package com.bettercontent.tinkersconstructaffixes

import com.google.gson.JsonParser
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

class MaterialSalienceTest {
    @Test
    fun sharedGlyphContractUsesPortableVanillaCharacters() {
        assertEquals(listOf("✦", "»", "⚒", "➜", "∞", "◆", "✚", "⊕"), SalienceAspect.entries.map { it.glyph })
    }

    @Test
    fun paletteMaintainsWorstCaseOklabDistanceAcrossDichromacyModels() {
        val models = listOf(
            arrayOf(doubleArrayOf(1.0, 0.0, 0.0), doubleArrayOf(0.0, 1.0, 0.0), doubleArrayOf(0.0, 0.0, 1.0)),
            arrayOf(doubleArrayOf(.152286, 1.052583, -.204868), doubleArrayOf(.114503, .786281, .099216), doubleArrayOf(-.003882, -.048116, 1.051998)),
            arrayOf(doubleArrayOf(.367322, .860646, -.227968), doubleArrayOf(.280085, .672501, .047413), doubleArrayOf(-.011820, .042940, .968881)),
            arrayOf(doubleArrayOf(1.255528, -.076749, -.178779), doubleArrayOf(-.078411, .930809, .147602), doubleArrayOf(.004733, .691367, .303900))
        )
        models.forEachIndexed { modelIndex, model ->
            val labs = SalienceAspect.entries.map { oklab(simulate(linearRgb(it.color), model)) }
            val minimum = labs.indices.flatMap { i -> (0 until i).map { j -> distance(labs[i], labs[j]) } }.min()
            assertTrue(minimum >= 0.10, "vision model $modelIndex has minimum OKLab distance $minimum")
        }
    }

    @Test
    fun ownedProfilesAreSignedDistinctAndCompressed() {
        assertEquals(15, MaterialSalience.profiles.size)
        assertEquals(
            SalienceAspect.entries.toSet(),
            MaterialSalience.profiles.values.flatMap { profile ->
                profile.ratings.filter { it.value > 0 }.map { it.aspect }
            }.toSet()
        )
        assertEquals(
            MaterialSalience.profiles.size,
            MaterialSalience.profiles.values.map(MaterialProfile::signature).distinct().size
        )
        MaterialSalience.profiles.values.forEach { profile ->
            assertTrue(profile.ratings.all { it.value in -3..3 && it.value != 0 }, profile.materialId)
            assertEquals(2, profile.salientRatings().count { it.value > 0 }, profile.materialId)
            assertEquals(1, profile.salientRatings().count { it.value < 0 }, profile.materialId)
        }
    }

    @Test
    fun dataMetadataMatchesRuntimeProfilesAndPalette() {
        val resource = requireNotNull(javaClass.getResourceAsStream(
            "/data/tinkers_construct_affixes/systemic_salience/material_profiles.json"
        ))
        val root = InputStreamReader(resource).use { JsonParser.parseReader(it).asJsonObject }
        val scope = root.getAsJsonObject("scope")
        assertEquals("pack_authored_materials_only", scope.get("policy").asString)
        assertEquals(TConAffixesMod.MOD_ID, scope.get("namespace").asString)
        val aspects = root.getAsJsonArray("aspects").associate { element ->
            val aspect = element.asJsonObject
            aspect.get("id").asString to aspect
        }
        SalienceAspect.entries.forEach { aspect ->
            val metadata = requireNotNull(aspects[aspect.id])
            assertEquals(aspect.color, metadata.get("color").asString.removePrefix("#").toInt(16))
            assertEquals(aspect.glyph, metadata.get("glyph").asString)
        }

        val materials = root.getAsJsonObject("materials")
        assertEquals(MaterialSalience.profiles.keys, materials.keySet())
        val evidence = root.getAsJsonObject("evidence")
        assertEquals(MaterialSalience.profiles.keys, evidence.keySet())
        evidence.entrySet().forEach { (id, value) ->
            assertTrue(value.asString.length >= 32, "$id lacks behavioral evidence")
        }
        MaterialSalience.profiles.forEach { (id, profile) ->
            val encoded = materials.getAsJsonObject(id)
            assertEquals(
                profile.ratings.associate { it.aspect.id to it.value },
                encoded.entrySet().associate { it.key to it.value.asInt },
                id
            )
        }
    }

    @Test
    fun rewardPoolsExcludeInertAndTraversalMaterials() {
        val blocked = setOf(
            "tconstruct:blood", "tconstruct:clay", "tconstruct:honey",
            "tconstruct:end_rod", "tconstruct:ender_pearl", "tconstruct:enderslime_vine",
            "tconstruct:skyslime_vine"
        )
        assertTrue(TConAffixConfig.defaultMaterialIds().intersect(blocked).isEmpty())
        assertTrue(AffixOrigins.classifiedMaterialIds().intersect(blocked).isEmpty())
        assertEquals(
            listOf("tconstruct:iron"),
            TConAffixConfig.sanitizeMaterialIds(blocked.toList() + "tconstruct:iron")
        )
    }

    @Test
    fun profilesAndAffixesRespectTraversalAndDamageCeilings() {
        val blockedModifiers = setOf(
            "tconstruct:double_jump", "tconstruct:enderporting", "tconstruct:enderdodging",
            "tconstruct:hover", "tconstruct:skyfall", "tconstruct:airborne", "tconstruct:bouncy",
            "tconstruct:flinging", "tconstruct:springing", "tconstruct:grapple"
        )
        val granted = TConAffixRewards.affixPool.flatMap { it.modifiers }.map { it.id }.toSet()
        assertTrue(granted.intersect(blockedModifiers).isEmpty())
        assertTrue(TConAffixRewards.affixPool.none { it.id.contains("flight") })
        TConAffixRewards.affixPool.forEach { affix ->
            affix.stats.filter { it.stat == "tconstruct:attack_damage" }.forEach { stat ->
                val maximum = affix.tiers.maxOf { it.maxPercent } * stat.scale
                assertTrue(maximum <= 0.30, "${affix.id} grants ${(maximum * 100).toInt()}% damage")
            }
        }
    }

    @Test
    fun ownedHeadStatsAndTraitsRemainGrounded() {
        val blockedTraits = setOf(
            "tconstruct:double_jump", "tconstruct:enderporting", "tconstruct:enderdodging",
            "tconstruct:hover", "tconstruct:skyfall", "tconstruct:airborne", "tconstruct:bouncy",
            "tconstruct:flinging", "tconstruct:springing", "tconstruct:grapple"
        )
        MaterialSalience.profiles.keys.forEach { path ->
            val stats = resourceJson("/data/tinkers_construct_affixes/tinkering/materials/stats/$path.json")
            val head = stats.getAsJsonObject("stats").getAsJsonObject("tconstruct:head")
            if (head != null && head.has("melee_attack")) {
                assertTrue(head.get("melee_attack").asDouble <= 3.5, "$path exceeds the head damage ceiling")
            }
            val traits = resourceJson("/data/tinkers_construct_affixes/tinkering/materials/traits/$path.json")
            val serialized = traits.toString()
            assertTrue(blockedTraits.none(serialized::contains), "$path grants traversal")
            assertTrue(serialized != "{}", "$path has no defining material trait")
        }
    }

    private fun resourceJson(path: String) = InputStreamReader(requireNotNull(javaClass.getResourceAsStream(path))).use {
        JsonParser.parseReader(it).asJsonObject
    }

    private fun linearRgb(color: Int): DoubleArray = doubleArrayOf(
        (color shr 16 and 255).toDouble(), (color shr 8 and 255).toDouble(), (color and 255).toDouble()
    )
        .map { it / 255.0 }.map { if (it <= .04045) it / 12.92 else ((it + .055) / 1.055).pow(2.4) }.toDoubleArray()

    private fun simulate(rgb: DoubleArray, matrix: Array<DoubleArray>): DoubleArray = DoubleArray(3) { row ->
        matrix[row].indices.sumOf { column -> matrix[row][column] * rgb[column] }.coerceIn(0.0, 1.0)
    }

    private fun oklab(rgb: DoubleArray): DoubleArray {
        val l = cbrt(.4122214708 * rgb[0] + .5363325363 * rgb[1] + .0514459929 * rgb[2])
        val m = cbrt(.2119034982 * rgb[0] + .6806995451 * rgb[1] + .1073969566 * rgb[2])
        val s = cbrt(.0883024619 * rgb[0] + .2817188376 * rgb[1] + .6299787005 * rgb[2])
        return doubleArrayOf(.2104542553 * l + .793617785 * m - .0040720468 * s,
            1.9779984951 * l - 2.428592205 * m + .4505937099 * s,
            .0259040371 * l + .7827717662 * m - .808675766 * s)
    }

    private fun distance(first: DoubleArray, second: DoubleArray): Double = sqrt(first.indices.sumOf { (first[it] - second[it]).pow(2) })
}
