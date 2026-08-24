package com.bettercontent.tinkersconstructaffixes

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import slimeknights.tconstruct.library.modifiers.Modifier
import slimeknights.tconstruct.library.modifiers.ModifierEntry
import slimeknights.tconstruct.library.modifiers.ModifierHooks
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister
import slimeknights.tconstruct.library.module.ModuleHookMap
import slimeknights.tconstruct.library.tools.nbt.IToolContext
import slimeknights.tconstruct.library.tools.nbt.IToolStackView
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder
import slimeknights.tconstruct.library.tools.stat.ToolStatId
import slimeknights.tconstruct.library.tools.stat.ToolStats
import java.util.UUID
import java.util.function.BiConsumer

object AffixModifiers {
    val REGISTRY: ModifierDeferredRegister = ModifierDeferredRegister.create(TConAffixesMod.MOD_ID)
    internal val STAT_DRIVER = REGISTRY.register("affix_stats") { AffixStatsModifier() }
    val MULTIPLIERS_KEY = ResourceLocation(TConAffixesMod.MOD_ID, "stat_multipliers")
    val OWNED_MODIFIERS_KEY = ResourceLocation(TConAffixesMod.MOD_ID, "owned_modifiers")
}

internal object AffixAttackSpeed {
    fun attributeBonus(baseAttackSpeed: Double, multiplier: Float): Double =
        if (multiplier > 0.0f && multiplier != 1.0f) baseAttackSpeed * (multiplier - 1.0f) else 0.0
}

internal class AffixStatsModifier : Modifier(), ToolStatsModifierHook, AttributesModifierHook {
    private companion object {
        const val ATTACK_SPEED_STAT = "tconstruct:attack_speed"
        val ATTACK_SPEED_MODIFIER_ID: UUID = UUID.fromString("13de17c1-bf64-45e7-86af-74ba7467e59f")
    }

    override fun registerHooks(builder: ModuleHookMap.Builder) {
        super.registerHooks(builder)
        builder.addHook(this, ModifierHooks.TOOL_STATS)
        builder.addHook(this, ModifierHooks.ATTRIBUTES)
    }

    override fun addToolStats(context: IToolContext, modifier: ModifierEntry, builder: ModifierStatsBuilder) {
        val multipliers = context.persistentData.getCompound(AffixModifiers.MULTIPLIERS_KEY)
        multipliers.allKeys.forEach { statId ->
            // TCon persists built tool stats after durability changes. Keeping attack speed in that
            // cache lets integrations that read tool stats directly retain an affix bonus on broken
            // weapons, despite TCon correctly suppressing their held attributes. Apply it dynamically
            // below instead, where TCon's broken-tool attribute gate is authoritative.
            if (statId == ATTACK_SPEED_STAT) return@forEach
            val multiplier = multipliers.getFloat(statId)
            if (multiplier <= 0.0f || multiplier == 1.0f) return@forEach
            val stat = ToolStats.getToolStat(ToolStatId.tryParse(statId) ?: return@forEach)
            val numeric = stat as? slimeknights.tconstruct.library.tools.stat.INumericToolStat<*> ?: return@forEach
            if (!numeric.supports(context.item)) return@forEach
            builder.multiplier(numeric, multiplier.toDouble())
        }
    }

    override fun addAttributes(
        tool: IToolStackView,
        modifier: ModifierEntry,
        slot: EquipmentSlot,
        consumer: BiConsumer<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier>
    ) {
        if (slot != EquipmentSlot.MAINHAND || tool.isBroken) return
        val multiplier = tool.persistentData.getCompound(AffixModifiers.MULTIPLIERS_KEY).getFloat(ATTACK_SPEED_STAT)
        val bonus = AffixAttackSpeed.attributeBonus(tool.stats.get(ToolStats.ATTACK_SPEED).toDouble(), multiplier)
        if (bonus != 0.0) {
            consumer.accept(
                Attributes.ATTACK_SPEED,
                AttributeModifier(ATTACK_SPEED_MODIFIER_ID, "tinkers_construct_affixes.attack_speed", bonus, AttributeModifier.Operation.ADDITION)
            )
        }
    }

    override fun shouldDisplay(advanced: Boolean): Boolean = false
}
