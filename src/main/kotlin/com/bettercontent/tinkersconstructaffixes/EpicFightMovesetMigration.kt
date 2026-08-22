package com.bettercontent.tinkersconstructaffixes

import net.minecraft.resources.ResourceLocation
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.registries.ForgeRegistries
import slimeknights.tconstruct.library.modifiers.ModifierId
import slimeknights.tconstruct.library.modifiers.ModifierManager
import slimeknights.tconstruct.library.tools.nbt.ToolStack

/**
 * Tool-definition traits are applied only when a Tinkers tool is forged.  Install the
 * Better Content moveset trait on already-forged supported tools once, at the player's
 * next login, so the Epic Fight migration does not invalidate existing equipment.
 */
object EpicFightMovesetMigration {
    private val migratedKey = ResourceLocation(TConAffixesMod.MOD_ID, "epicfight_moveset_migrated")

    private val profiles = mapOf(
        "additionalweaponry:butcher_knife" to "epicfighttinkercompat:bc_axe",
        "additionalweaponry:cutlass" to "epicfighttinkercompat:bc_sword",
        "additionalweaponry:pitchfork" to "epicfighttinkercompat:bc_spear",
        "additionalweaponry:scepter" to "epicfighttinkercompat:bc_sword",
        "additionalweaponry:sniffer_claws" to "epicfighttinkercompat:bc_fist",
        "additionalweaponry:wrench" to "epicfighttinkercompat:bc_axe",
        "construct_arsenal:buckler" to "epicfighttinkercompat:bc_fist",
        "construct_arsenal:helix_blade" to "epicfighttinkercompat:bc_greatsword",
        "construct_arsenal:quarterstaff" to "epicfighttinkercompat:bc_spear",
        "tconstruct:battlesign" to "epicfighttinkercompat:bc_sword",
        "tconstruct:broad_axe" to "epicfighttinkercompat:bc_axe",
        "tconstruct:cleaver" to "epicfighttinkercompat:bc_greatsword",
        "tconstruct:dagger" to "epicfighttinkercompat:bc_dagger",
        "tconstruct:excavator" to "epicfighttinkercompat:bc_shovel",
        "tconstruct:hand_axe" to "epicfighttinkercompat:bc_axe",
        "tconstruct:javelin" to "epicfighttinkercompat:bc_trident",
        "tconstruct:kama" to "epicfighttinkercompat:bc_hoe",
        "tconstruct:mattock" to "epicfighttinkercompat:bc_pickaxe",
        "tconstruct:minotaur_axe" to "epicfighttinkercompat:bc_axe",
        "tconstruct:pickadze" to "epicfighttinkercompat:bc_pickaxe",
        "tconstruct:pickaxe" to "epicfighttinkercompat:bc_pickaxe",
        "tconstruct:scythe" to "epicfighttinkercompat:bc_greatsword",
        "tconstruct:sledge_hammer" to "epicfighttinkercompat:bc_greatsword",
        "tconstruct:sword" to "epicfighttinkercompat:bc_sword",
        "tconstruct:vein_hammer" to "epicfighttinkercompat:bc_greatsword",
        "tconstruct:war_pick" to "epicfighttinkercompat:bc_pickaxe",
        "tconstruct:swasher" to "epicfighttinkercompat:bc_sword",
        "tinker_rapier:estoc_tic" to "epicfighttinkercompat:bc_longsword",
        "tinker_rapier:rapier_tic" to "epicfighttinkercompat:bc_dagger",
        "tinkers_battle_spades:battle_spade" to "epicfighttinkercompat:bc_sword",
        "tinkers_katanas:fuma_shuriken" to "epicfighttinkercompat:bc_tachi",
        "tinkers_katanas:katana" to "epicfighttinkercompat:bc_tachi",
        "tinkers_khopesh:khopesh" to "epicfighttinkercompat:bc_tachi",
        "tinkers_things:amethyst_staff" to "epicfighttinkercompat:bc_spear",
        "tinkers_things:blockram" to "epicfighttinkercompat:bc_greatsword",
        "tinkers_things:blowpipe" to "epicfighttinkercompat:bc_crossbow",
        "tinkers_things:chisel" to "epicfighttinkercompat:bc_pickaxe",
        "tinkers_things:halberd" to "epicfighttinkercompat:bc_spear",
        "tinkers_things:shortbow" to "epicfighttinkercompat:bc_bow",
        "tinkers_things:shovel" to "epicfighttinkercompat:bc_shovel",
        "tinkersweaponry:greatsword" to "epicfighttinkercompat:bc_greatsword",
        "tinkersweaponry:lance" to "epicfighttinkercompat:bc_spear",
        "tinkersweaponry:pike" to "epicfighttinkercompat:bc_spear"
    )

    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        if (!ModList.get().isLoaded("epicfighttinkercompat")) return
        val inventory = event.entity.inventory
        for (slot in 0 until inventory.containerSize) migrate(inventory.getItem(slot))
        inventory.setChanged()
    }

    private fun migrate(stack: net.minecraft.world.item.ItemStack) {
        if (!TConAffixRewards.looksLikeTConTool(stack)) return
        val profile = profiles[ForgeRegistries.ITEMS.getKey(stack.item)?.toString()] ?: return
        val modifier = ModifierId.tryParse(profile) ?: return
        if (!ModifierManager.INSTANCE.contains(modifier)) return

        val tool = ToolStack.from(stack)
        if (tool.persistentData.getBoolean(migratedKey)) return
        if (tool.getUpgrades().getLevel(modifier) == 0) tool.addModifier(modifier, 1)
        tool.persistentData.putBoolean(migratedKey, true)
    }
}
