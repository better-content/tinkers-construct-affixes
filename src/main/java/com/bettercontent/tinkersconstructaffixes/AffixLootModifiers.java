package com.bettercontent.tinkersconstructaffixes;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AffixLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTRY =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "tinkers_construct_affixes");
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> FONT_EXCLUSION =
        REGISTRY.register("font_exclusion", () -> FontExclusionLootModifier.CODEC);
    private AffixLootModifiers() {}
}
