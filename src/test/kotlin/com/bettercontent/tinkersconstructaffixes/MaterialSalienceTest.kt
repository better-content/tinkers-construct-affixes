package com.bettercontent.tinkersconstructaffixes

import com.google.gson.JsonParser
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaterialSalienceTest {
    @Test
    fun bitmapFontReferencesPackagedTexture() {
        val resource = requireNotNull(javaClass.getResourceAsStream(
            "/assets/tinkers_construct_affixes/font/salience.json"
        ))
        val root = InputStreamReader(resource).use { JsonParser.parseReader(it).asJsonObject }
        val textureId = root.getAsJsonArray("providers")[0].asJsonObject.get("file").asString
        val (namespace, path) = textureId.split(':', limit = 2)

        javaClass.getResourceAsStream("/assets/$namespace/textures/$path").use { texture ->
            requireNotNull(texture) { "Bitmap font texture $textureId is not packaged under textures/" }
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
        val aspects = root.getAsJsonArray("aspects").associate { element ->
            val aspect = element.asJsonObject
            aspect.get("id").asString to aspect
        }
        SalienceAspect.entries.forEach { aspect ->
            val metadata = requireNotNull(aspects[aspect.id])
            assertEquals(aspect.color, metadata.get("color").asString.removePrefix("#").toInt(16))
            assertEquals(aspect.glyph.single().code, metadata.get("codepoint").asString.toInt(16))
        }

        val materials = root.getAsJsonObject("materials")
        assertEquals(MaterialSalience.profiles.keys, materials.keySet())
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
}
