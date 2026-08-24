package com.bettercontent.tinkersconstructaffixes

import net.minecraft.ChatFormatting
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import slimeknights.tconstruct.library.tools.part.ToolPartItem
import kotlin.math.absoluteValue

internal enum class SalienceAspect(
    val id: String,
    val displayName: String,
    val color: Int,
    val glyph: String
) {
    IMPACT("impact", "Impact", 0xD94B4B, "\uE200"),
    TEMPO("tempo", "Tempo", 0xF28E2B, "\uE201"),
    WORK("work", "Work", 0xC5A529, "\uE202"),
    MOBILITY("mobility", "Mobility", 0x62A744, "\uE203"),
    ENDURANCE("endurance", "Endurance", 0x168F96, "\uE204"),
    ROBUSTNESS("robustness", "Robustness", 0x496CC3, "\uE205"),
    RENEWAL("renewal", "Renewal", 0x24966A, "\uE206"),
    CONTROL("control", "Control", 0x9B58B5, "\uE207")
}

internal data class SalienceRating(val aspect: SalienceAspect, val value: Int) {
    init {
        require(value in -3..3 && value != 0) { "Salience ratings must be signed values from -3 through 3" }
    }
}

internal data class MaterialProfile(val materialId: String, val ratings: List<SalienceRating>) {
    init {
        require(ratings.isNotEmpty()) { "Material $materialId must have a behavioural profile" }
        require(ratings.map { it.aspect }.distinct().size == ratings.size) { "Material $materialId repeats an aspect" }
    }

    /** Player-facing compression: the two strongest affinities and the strongest weakness. */
    fun salientRatings(): List<SalienceRating> {
        val positives = ratings.filter { it.value > 0 }
            .sortedWith(compareByDescending<SalienceRating> { it.value }.thenBy { it.aspect.ordinal })
            .take(2)
        val weakness = ratings.filter { it.value < 0 }.minByOrNull { it.value }
        return positives + listOfNotNull(weakness)
    }

    fun signature(): String = ratings.sortedBy { it.aspect.ordinal }
        .joinToString("|") { "${it.aspect.id}:${it.value}" }
}

internal object MaterialSalience {
    private val glyphFont = ResourceLocation(TConAffixesMod.MOD_ID, "salience")

    internal val profiles: Map<String, MaterialProfile> = listOf(
        profile("ambrosium", SalienceAspect.RENEWAL to 3, SalienceAspect.ENDURANCE to 1, SalienceAspect.IMPACT to -1),
        profile("cloggrum", SalienceAspect.ROBUSTNESS to 2, SalienceAspect.IMPACT to 1, SalienceAspect.TEMPO to -1),
        profile("echo_wood", SalienceAspect.CONTROL to 2, SalienceAspect.TEMPO to 1, SalienceAspect.ROBUSTNESS to -2),
        profile("forgotten", SalienceAspect.IMPACT to 3, SalienceAspect.ROBUSTNESS to 2, SalienceAspect.TEMPO to -2),
        profile("froststeel", SalienceAspect.ROBUSTNESS to 3, SalienceAspect.CONTROL to 1, SalienceAspect.MOBILITY to -1),
        profile("gravitite", SalienceAspect.IMPACT to 3, SalienceAspect.MOBILITY to 1, SalienceAspect.CONTROL to -1),
        profile("holystone", SalienceAspect.WORK to 2, SalienceAspect.ROBUSTNESS to 1, SalienceAspect.TEMPO to -1),
        profile("regalium", SalienceAspect.ENDURANCE to 2, SalienceAspect.CONTROL to 1, SalienceAspect.ROBUSTNESS to -1),
        profile("reinforced_echo", SalienceAspect.ROBUSTNESS to 3, SalienceAspect.ENDURANCE to 1, SalienceAspect.TEMPO to -2),
        profile("resonarium", SalienceAspect.TEMPO to 3, SalienceAspect.CONTROL to 2, SalienceAspect.ROBUSTNESS to -2),
        profile("sculk_bone", SalienceAspect.RENEWAL to 2, SalienceAspect.IMPACT to 1, SalienceAspect.ENDURANCE to -1),
        profile("skyroot", SalienceAspect.TEMPO to 2, SalienceAspect.MOBILITY to 1, SalienceAspect.ROBUSTNESS to -1),
        profile("utherium", SalienceAspect.WORK to 3, SalienceAspect.TEMPO to 1, SalienceAspect.ENDURANCE to -1),
        profile("warden_carapace", SalienceAspect.ROBUSTNESS to 3, SalienceAspect.IMPACT to 2, SalienceAspect.MOBILITY to -2),
        profile("zanite", SalienceAspect.CONTROL to 2, SalienceAspect.WORK to 1, SalienceAspect.ENDURANCE to -1)
    ).associateBy(MaterialProfile::materialId)

    internal val ownedMaterialIds: Set<String> = profiles.keys.mapTo(linkedSetOf()) {
        "${TConAffixesMod.MOD_ID}:$it"
    }

    fun appendTooltip(stack: ItemStack, tooltip: MutableList<Component>) {
        val displayed = materialIds(stack).mapNotNull { rawId ->
            val path = rawId.removePrefix("${TConAffixesMod.MOD_ID}:")
            profiles[path]?.let { path to it }
        }.distinctBy { it.first }
        if (displayed.isEmpty()) return

        tooltip += Component.translatable("tooltip.tinkers_construct_affixes.material_profile")
            .withStyle(ChatFormatting.DARK_GRAY)
        displayed.forEach { (path, profile) ->
            val line = Component.literal("  ${displayName(path)}: ").withStyle(ChatFormatting.GRAY)
            profile.salientRatings().forEachIndexed { index, rating ->
                if (index > 0) line.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                line.append(
                    Component.literal(rating.aspect.glyph)
                        .withStyle(Style.EMPTY.withFont(glyphFont).withColor(rating.aspect.color))
                )
                val sign = if (rating.value > 0) "+" else "−"
                line.append(
                    Component.literal("$sign${rating.value.absoluteValue} ${rating.aspect.displayName}")
                        .withStyle(Style.EMPTY.withColor(rating.aspect.color))
                )
            }
            tooltip += line
        }
    }

    internal fun materialIds(stack: ItemStack): List<String> {
        val part = stack.item as? ToolPartItem
        if (part != null) return listOf(part.getMaterial(stack).toString())

        val list = stack.tag?.getList("tic_materials", Tag.TAG_STRING.toInt()) ?: return emptyList()
        return List(list.size) { index -> list.getString(index) }
    }

    private fun profile(path: String, vararg ratings: Pair<SalienceAspect, Int>): MaterialProfile =
        MaterialProfile(path, ratings.map { (aspect, value) -> SalienceRating(aspect, value) })

    private fun displayName(path: String): String = path.split('_').joinToString(" ") { word ->
        word.replaceFirstChar { character -> character.uppercase() }
    }
}
